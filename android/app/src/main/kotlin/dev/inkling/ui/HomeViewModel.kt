package dev.inkling.ui

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

/** Square edge the launcher icon is rasterized to before Compose scales it down to 40.dp. */
private const val ICON_PX = 96

/** `icon` is null in pure code and tests; the view model fills it in from the package manager. */
data class Tile(
    val packageName: String,
    val label: String,
    val fraction: Float,
    val done: Boolean,
    val icon: ImageBitmap? = null,
)

data class HomeState(val childName: String = "", val booksToday: Int = 0, val tiles: List<Tile> = emptyList())

/** "1 book today." reads wrong as "1 books today.", and he will notice. */
fun booksTodayLine(n: Int): String = if (n == 1) "1 book today." else "$n books today."

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

class HomeViewModel(private val repo: Repo, private val pm: PackageManager) : ViewModel() {
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
        val dayStart = Budget.startOfDay(now, tz)
        val spans = repo.spansToday(child.id, dayStart)
        val base = buildHomeState(child.name, rules, spans, settings, now, tz, minuteOfDay)
            .copy(booksToday = repo.booksFinishedToday(child.id, dayStart))
        // Icon decoding hits the package manager and rasterizes a drawable, so keep it off the main thread.
        val icons = withContext(Dispatchers.IO) { base.tiles.associate { it.packageName to loadIcon(it.packageName) } }
        _state.value = base.copy(tiles = base.tiles.map { it.copy(icon = icons[it.packageName]) })
    }

    /**
     * The app's launcher icon as a bitmap.
     *
     * @param packageName package to look up
     * @return the icon, or null when the app is no longer installed or its icon will not rasterize
     */
    private fun loadIcon(packageName: String): ImageBitmap? = try {
        pm.getApplicationIcon(packageName).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: IllegalArgumentException) {
        // toBitmap throws this for a drawable with no intrinsic size it can honor.
        null
    }
}
