package dev.inkling.ui

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.inkling.apps.InstalledApps
import dev.inkling.books.Book
import dev.inkling.books.BookStore
import dev.inkling.core.BookHistory
import dev.inkling.core.Budget
import dev.inkling.core.NextUp
import dev.inkling.core.PinPolicy
import dev.inkling.core.Recommendation
import dev.inkling.core.ShelfEntry
import dev.inkling.core.Span
import dev.inkling.data.AppRule
import dev.inkling.data.Child
import dev.inkling.data.Pin
import dev.inkling.data.Repo
import dev.inkling.data.Settings
import dev.inkling.data.TutorAttempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

data class AppRow(val packageName: String, val label: String, val enabled: Boolean, val cap: Int)
data class TodayRow(val label: String, val minutes: Int)

/** One book on the parent's reading list. [accuracy] is null until he has read it aloud. */
data class ReadingBookRow(val title: String, val finished: Int, val accuracy: Float?)

data class ReadingState(
    val books: List<ReadingBookRow> = emptyList(),
    val missedTwice: List<String> = emptyList(),
    val attemptsToday: Int = 0,
    val accuracyToday: Float? = null,
)
data class ParentState(
    val settings: Settings? = null,
    val apps: List<AppRow> = emptyList(),
    val today: List<TodayRow> = emptyList(),
    val totalMinutes: Int = 0,
    val setupProblems: List<String> = emptyList(),
    val reading: ReadingState = ReadingState(),
    /** Every child on the device, oldest first, for the Rules > Children row. */
    val children: List<Child> = emptyList(),
    /** Which of [children] the device is set to. 0 when nobody is onboarded. */
    val activeChildId: Long = 0,
    /** What the Next up tab shows. Empty until he has a shelf worth suggesting from. */
    val nextUp: List<Recommendation> = emptyList(),
    /** False until [ParentViewModel.refresh] has published a settings row at least once. */
    val loaded: Boolean = false,
    /** True when no child has been onboarded. Nav sends this to the onboarding flow. */
    val noChild: Boolean = false,
)

/** Quotes one CSV field. A double quote inside the value is doubled, per RFC 4180. */
fun csvField(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

/**
 * Pure. What the Reading tab shows.
 *
 * Low-confidence attempts are left out of both the count and the average: the recognizer named
 * nothing on those, so scoring them as clean reads would flatter him.
 */
fun buildReading(
    books: List<Book>, histories: Map<String, BookHistory>, missedTwice: List<String>,
    attempts: List<TutorAttempt>, dayStart: Long,
): ReadingState {
    val today = attempts.filter { it.at >= dayStart && !it.lowConfidence }
    val words = today.sumOf { it.totalWords }
    val missed = today.sumOf { a -> a.missed.split(" ").count { it.isNotBlank() } }
    return ReadingState(
        books = books.map { b ->
            val h = histories[b.id]
            ReadingBookRow(b.title, h?.finished ?: 0, h?.lastAccuracy)
        },
        missedTwice = missedTwice,
        attemptsToday = today.size,
        accuracyToday = if (words == 0) null else 1f - missed.toFloat() / words,
    )
}

/**
 * Pure. Flattens the shelf into the Android-free rows [NextUp] reads.
 *
 * The shelf already carries the tag, so Next up and the shelf can never disagree about whether a
 * book is a stretch: one calculation, two screens.
 */
fun toShelfEntries(rows: List<ShelfRow>, histories: Map<String, BookHistory>): List<ShelfEntry> =
    rows.map { r ->
        ShelfEntry(
            bookId = r.book.id,
            title = r.book.title,
            stageLabel = stageLabel(r.book.stage),
            tag = r.tag,
            lastAccuracy = histories[r.book.id]?.lastAccuracy,
        )
    }

/** "finished×2 · 91%", or "not started" when he has never opened it. */
fun readingRowValue(row: ReadingBookRow): String {
    val parts = buildList {
        if (row.finished > 0) add("finished×${row.finished}")
        row.accuracy?.let { add("${Math.round(it * 100)}%") }
    }
    return if (parts.isEmpty()) "not started" else parts.joinToString(" · ")
}

fun buildToday(rules: List<AppRule>, spans: List<Span>, now: Long, zone: Long): Pair<List<TodayRow>, Int> {
    val rows = rules.filter { it.enabled }
        .map { TodayRow(it.label, Budget.usedMinutesToday(spans, it.packageName, now, zone)) }
        .sortedByDescending { it.minutes }
    return rows to rows.sumOf { it.minutes }
}

class ParentViewModel(
    private val repo: Repo,
    private val pm: PackageManager,
    private val self: String,
    private val books: BookStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentState())
    val state: StateFlow<ParentState> = _state
    var setupProblems: () -> List<String> = { emptyList() }

    fun refresh() = viewModelScope.launch {
        val child = repo.activeChild()
        if (child == null) {
            _state.value = ParentState(loaded = true, noChild = true)
            return@launch
        }
        val children = repo.children()
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
            children = children, activeChildId = child.id, noChild = false,
        )
        val all = books.all()
        val histories = all.associate { it.id to repo.history(child.id, it.id, it.pages.size) }
        val missedTwice = repo.missedTwice(child.id)
        _state.value = _state.value.copy(
            reading = buildReading(
                books = all,
                histories = histories,
                missedTwice = missedTwice,
                attempts = repo.recentAttempts(child.id, ATTEMPT_WINDOW),
                dayStart = Budget.startOfDay(now, zone),
            ),
            nextUp = NextUp.recommend(
                toShelfEntries(buildShelf(all, histories, child.startStage), histories),
                missedTwice,
            ),
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

    /** Points the device at another child. Everything on the parent screen reloads for them. */
    fun switchChild(id: Long) = viewModelScope.launch {
        repo.setActiveChild(id)
        // Reset first: the old child's books and recommendations must not sit on screen while the
        // new child's rows load.
        _state.value = ParentState()
        refresh().join()
    }

    fun setApp(row: AppRow, enabled: Boolean, cap: Int) = viewModelScope.launch {
        val child = repo.activeChild() ?: return@launch
        repo.upsertRule(AppRule(childId = child.id, packageName = row.packageName, label = row.label, enabled = enabled, dailyCapMinutes = cap.coerceIn(0, 600)))
        refresh()
    }

    /** Returns true when the PIN is right. Wrong entries count toward lockout. */
    suspend fun tryPin(pin: String, now: Long): Boolean {
        // No child, no settings row, so no PIN. Onboarding is the only way past this screen.
        val child = repo.activeChild() ?: return false
        val s = repo.settings(child.id)
        if (now < s.lockoutUntil) return false
        // No PIN set means nothing can unlock. First-run PIN creation goes through PinMode.SET, never here.
        if (s.pinHash == null) return false
        if (s.pinHash == Pin.hash(pin)) {
            repo.saveSettings(s.copy(pinFailures = 0, lockoutUntil = 0)); return true
        }
        val failures = s.pinFailures + 1
        val lock = PinPolicy.lockoutSeconds(failures) * 1000L
        repo.saveSettings(s.copy(pinFailures = failures, lockoutUntil = if (lock > 0) now + lock else 0))
        return false
    }

    suspend fun lockoutRemainingSeconds(now: Long): Int {
        val child = repo.activeChild() ?: return 0
        val s = repo.settings(child.id)
        return ((s.lockoutUntil - now) / 1000).coerceAtLeast(0).toInt()
    }

    suspend fun spikeCsv(): String = buildString {
        append("at,expected,transcript,confidence,flagged,verdict\n")
        for (r in repo.spikes()) {
            append("${r.at},${csvField(r.expected)},${csvField(r.transcript)},${r.confidence},${csvField(r.flagged)},${r.verdict}\n")
        }
    }

    private companion object {
        /** Enough recent attempts to cover a day of reading without pulling the whole table. */
        const val ATTEMPT_WINDOW = 200
    }

    private fun update(f: (Settings) -> Settings) = viewModelScope.launch {
        val child = repo.activeChild() ?: return@launch
        repo.saveSettings(f(repo.settings(child.id)))
        refresh()
    }
}
