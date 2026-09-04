package dev.inkling.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.inkling.core.BlockDecision
import dev.inkling.core.Budget
import dev.inkling.core.QuietHours
import dev.inkling.core.Rule
import dev.inkling.core.Span
import dev.inkling.core.Verdict
import dev.inkling.data.AppRule
import dev.inkling.data.Repo
import dev.inkling.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class Tile(val packageName: String, val label: String, val fraction: Float, val done: Boolean)
data class HomeState(val childName: String = "", val booksToday: Int = 0, val tiles: List<Tile> = emptyList())

/** Pure. Turns rules, today's spans, and settings into what the kid sees. */
fun buildHomeState(
    childName: String, rules: List<AppRule>, spans: List<Span>, settings: Settings,
    now: Long, zoneOffsetMillis: Long, minuteOfDay: Int,
): HomeState {
    val enabled = rules.filter { it.enabled }
    val coreRules = enabled.map { Rule(it.packageName, it.enabled, it.dailyCapMinutes) }
    val quiet = QuietHours.isQuiet(minuteOfDay, settings.quietStartMinute, settings.quietEndMinute)
    val usedAll = Budget.usedMinutesTodayAll(spans, enabled.map { it.packageName }.toSet(), now, zoneOffsetMillis)
    val tiles = enabled.map { r ->
        val used = Budget.usedMinutesToday(spans, r.packageName, now, zoneOffsetMillis)
        val verdict = BlockDecision.decide(
            r.packageName, selfPackage = "dev.inkling", rules = coreRules, kidsModeOn = true,
            usedMinutes = used, usedAllMinutes = usedAll, ceilingMinutes = settings.deviceCeilingMinutes,
            quiet = quiet, readPackage = "dev.inkling",
        )
        val fraction = if (r.dailyCapMinutes > 0) (used.toFloat() / r.dailyCapMinutes).coerceIn(0f, 1f) else 0f
        Tile(r.packageName, r.label, fraction, done = verdict != Verdict.ALLOW)
    }
    return HomeState(childName, booksToday = 0, tiles = tiles)
}

class HomeViewModel(private val repo: Repo) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    fun refresh() = viewModelScope.launch {
        val child = repo.ensureChild()
        val settings = repo.settings(child.id)
        val rules = repo.rules(child.id)
        val now = System.currentTimeMillis()
        val tz = TimeZone.getDefault().getOffset(now).toLong()
        val cal = Calendar.getInstance()
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val spans = repo.spansToday(child.id, Budget.startOfDay(now, tz))
        _state.value = buildHomeState(child.name, rules, spans, settings, now, tz, minuteOfDay)
    }
}
