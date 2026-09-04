package dev.inkling.ui

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.inkling.apps.InstalledApps
import dev.inkling.core.Budget
import dev.inkling.core.PinPolicy
import dev.inkling.core.Span
import dev.inkling.data.AppRule
import dev.inkling.data.Pin
import dev.inkling.data.Repo
import dev.inkling.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

data class AppRow(val packageName: String, val label: String, val enabled: Boolean, val cap: Int)
data class TodayRow(val label: String, val minutes: Int)
data class ParentState(
    val settings: Settings? = null,
    val apps: List<AppRow> = emptyList(),
    val today: List<TodayRow> = emptyList(),
    val totalMinutes: Int = 0,
    val setupProblems: List<String> = emptyList(),
    /** False until [ParentViewModel.refresh] has published a settings row at least once. */
    val loaded: Boolean = false,
)

fun buildToday(rules: List<AppRule>, spans: List<Span>, now: Long, zone: Long): Pair<List<TodayRow>, Int> {
    val rows = rules.filter { it.enabled }
        .map { TodayRow(it.label, Budget.usedMinutesToday(spans, it.packageName, now, zone)) }
        .sortedByDescending { it.minutes }
    return rows to rows.sumOf { it.minutes }
}

class ParentViewModel(private val repo: Repo, private val pm: PackageManager, private val self: String) : ViewModel() {
    private val _state = MutableStateFlow(ParentState())
    val state: StateFlow<ParentState> = _state
    var setupProblems: () -> List<String> = { emptyList() }

    fun refresh() = viewModelScope.launch {
        val child = repo.ensureChild()
        val settings = repo.settings(child.id)
        val rules = repo.rules(child.id)
        val now = System.currentTimeMillis()
        val zone = TimeZone.getDefault().getOffset(now).toLong()
        val (today, total) = buildToday(rules, repo.spansToday(child.id, Budget.startOfDay(now, zone)), now, zone)
        // Publish settings before enumerating apps. The PIN pad reads pinHash, and anything that
        // waits on PackageManager first leaves it looking at a null settings row for that long.
        _state.value = _state.value.copy(
            settings = settings, today = today, totalMinutes = total,
            setupProblems = setupProblems(), loaded = true,
        )
        val installed = withContext(Dispatchers.IO) { InstalledApps.launchable(pm, self) }
        _state.value = _state.value.copy(
            apps = installed.map { (pkg, label) ->
                val r = rules.firstOrNull { it.packageName == pkg }
                AppRow(pkg, label, r?.enabled ?: false, r?.dailyCapMinutes ?: 0)
            },
        )
    }

    fun setKidsMode(on: Boolean) = update { it.copy(kidsModeOn = on) }
    fun setCeiling(minutes: Int) = update { it.copy(deviceCeilingMinutes = minutes.coerceIn(0, 600)) }
    /** Stores a new parent PIN. An empty [pin] is rejected: clearing the hash would open the device. */
    fun setPin(pin: String) {
        if (pin.isEmpty()) return
        update { it.copy(pinHash = Pin.hash(pin), pinFailures = 0, lockoutUntil = 0) }
    }

    fun setApp(row: AppRow, enabled: Boolean, cap: Int) = viewModelScope.launch {
        val child = repo.ensureChild()
        repo.upsertRule(AppRule(childId = child.id, packageName = row.packageName, label = row.label, enabled = enabled, dailyCapMinutes = cap.coerceIn(0, 600)))
        refresh()
    }

    /** Returns true when the PIN is right. Wrong entries count toward lockout. */
    suspend fun tryPin(pin: String, now: Long): Boolean {
        val child = repo.ensureChild()
        val s = repo.settings(child.id)
        if (now < s.lockoutUntil) return false
        if (s.pinHash == null || s.pinHash == Pin.hash(pin)) {
            repo.saveSettings(s.copy(pinFailures = 0, lockoutUntil = 0)); return true
        }
        val failures = s.pinFailures + 1
        val lock = PinPolicy.lockoutSeconds(failures) * 1000L
        repo.saveSettings(s.copy(pinFailures = failures, lockoutUntil = if (lock > 0) now + lock else 0))
        return false
    }

    suspend fun lockoutRemainingSeconds(now: Long): Int {
        val s = repo.settings(repo.ensureChild().id)
        return ((s.lockoutUntil - now) / 1000).coerceAtLeast(0).toInt()
    }

    suspend fun spikeCsv(): String = buildString {
        append("at,expected,transcript,confidence,flagged,verdict\n")
        for (r in repo.spikes()) append("${r.at},\"${r.expected}\",\"${r.transcript}\",${r.confidence},\"${r.flagged}\",${r.verdict}\n")
    }

    private fun update(f: (Settings) -> Settings) = viewModelScope.launch {
        val child = repo.ensureChild()
        repo.saveSettings(f(repo.settings(child.id)))
        refresh()
    }
}
