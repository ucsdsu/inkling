package dev.inkling.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.inkling.books.Book
import dev.inkling.books.BookStore
import dev.inkling.core.BookHistory
import dev.inkling.core.Chunk
import dev.inkling.core.Chunker
import dev.inkling.core.ReadingDiff
import dev.inkling.core.ShelfTags
import dev.inkling.core.Tag
import dev.inkling.data.Repo
import dev.inkling.data.TutorAttempt
import dev.inkling.speech.Recognizer
import dev.inkling.speech.Speaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the reader is doing about the child's voice right now. */
enum class TutorPhase { IDLE, LISTENING, COACH, UNCLEAR, GOOD }

/** How the page was read, for the reading log. */
object ReadMode {
    const val TTS = "tts"
    const val SELF = "self"
    const val LOOK = "look"
}

data class ReaderState(
    val book: Book? = null,
    val page: Int = 0,
    val speakingWord: Int = -1,
    val phase: TutorPhase = TutorPhase.IDLE,
    val missedWord: String? = null,
    val chunks: List<Chunk> = emptyList(),
    val speechMode: String = "",
)

/** One book on the shelf, with the word the shelf shows for it. */
data class ShelfRow(val book: Book, val tag: Tag, val subtitle: String)

/**
 * What happens when recognition returns. Pure.
 *
 * @param confidence the engine's score, or a negative number when it scored nothing at all
 */
fun onRecognized(state: ReaderState, transcript: String, confidence: Float): ReaderState {
    val book = state.book ?: return state
    val line = book.pages[state.page]
    val r = ReadingDiff.score(line, transcript, if (confidence < 0) 1f else confidence)
    return when {
        r.lowConfidence -> state.copy(phase = TutorPhase.UNCLEAR, missedWord = null, chunks = emptyList())
        r.missed.isEmpty() -> state.copy(phase = TutorPhase.GOOD, missedWord = null, chunks = emptyList())
        else -> {
            val w = r.missed.first()
            state.copy(phase = TutorPhase.COACH, missedWord = w, chunks = Chunker.chunk(w, book.target))
        }
    }
}

/**
 * One turn at the mic, and whether its result is still wanted.
 *
 * SpeechRecognizer.cancel() is not instant: a result or an error can still land after the child
 * has tapped "Listening…" off, and coaching him on a read he abandoned is the same as telling him
 * he got it wrong. The cancel is spent on the first thing that arrives.
 */
class ListenSession {
    private var cancelled = false

    fun start() { cancelled = false }

    fun cancel() { cancelled = true }

    /** True when what just arrived should be used. Clears the cancel it consumes. */
    fun accept(): Boolean {
        if (!cancelled) return true
        cancelled = false
        return false
    }
}

/**
 * The two transcripts the debug long-press feeds the tutor, built from the line on the page so
 * the coach names a word the child can actually see.
 *
 * The first reads r as w, which the diff forgives as articulation, so it lands on "nice reading".
 * The second starts the last word with the wrong letter, so the coach sounds that word out.
 */
fun fakeTranscripts(line: String): List<String> {
    val words = line.trim().split(" ").filter { it.isNotEmpty() }
    val last = words.lastOrNull().orEmpty()
    val wrongLetter = if (last.firstOrNull()?.lowercaseChar() == 'b') 'd' else 'b'
    val misread = words.dropLast(1) + (wrongLetter + last.drop(1))
    return listOf(line.replace("r", "w"), misread.joinToString(" "))
}

/**
 * How a page gets logged. TTS wins over the tutor: once the line has been read to him, that is
 * how he got through the page, whatever he did after.
 */
fun pageMode(usedTts: Boolean, usedSelf: Boolean): String = when {
    usedTts -> ReadMode.TTS
    usedSelf -> ReadMode.SELF
    else -> ReadMode.LOOK
}

/**
 * Whether the reader should load [requested]. False when that book is already open: rotation
 * recreates the route and re-runs its effect, and reopening rewound the child to page 0.
 */
fun shouldOpen(currentId: String?, requested: String): Boolean = currentId != requested

/** "cvc-e" reads as "short e"; "digraph-sh" as "sh digraph". */
fun stageLabel(stage: String): String = when {
    stage.startsWith("cvc-") -> "short ${stage.removePrefix("cvc-")}"
    stage.startsWith("digraph-") -> "${stage.removePrefix("digraph-")} digraph"
    else -> stage
}

/**
 * The stage the child is working on: the first book he has not read accurately enough to call easy.
 * Everything below it is behind him, everything above it is a stretch.
 */
fun currentStageIndex(books: List<Book>, histories: Map<String, BookHistory>): Int {
    val i = books.indexOfFirst { (histories[it.id]?.lastAccuracy ?: 0f) < ShelfTags.EASY_FLOOR }
    return if (i < 0) books.lastIndex.coerceAtLeast(0) else i
}

fun buildShelf(books: List<Book>, histories: Map<String, BookHistory>): List<ShelfRow> {
    val current = currentStageIndex(books, histories)
    return books.mapIndexed { i, b ->
        ShelfRow(b, ShelfTags.tag(histories[b.id], i, current), "Decodable · ${stageLabel(b.stage)} · ${b.pages.size} pages")
    }
}

/**
 * Drives the shelf and the reader: which page is open, what the tutor heard, and the log rows
 * that come out of both.
 *
 * @param context application context; the TTS engine and the recognizer outlive any one screen
 */
class ReaderViewModel(private val repo: Repo, private val store: BookStore, context: Context) : ViewModel() {
    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state

    private val _shelf = MutableStateFlow<List<ShelfRow>>(emptyList())
    val shelf: StateFlow<List<ShelfRow>> = _shelf

    private val speaker = Speaker(context) { }
    private val recognizer = Recognizer(context)
    private val session = ListenSession()

    private var childId: Long = 0
    private var pageStartedAt: Long = 0
    private var usedTts = false
    private var usedSelf = false
    private var fakeIndex = 0

    fun loadShelf() = viewModelScope.launch {
        val child = repo.ensureChild()
        childId = child.id
        val books = store.all()
        val histories = books.associate { it.id to repo.history(child.id, it.id, it.pages.size) }
        _shelf.value = buildShelf(books, histories)
    }

    fun open(bookId: String) = viewModelScope.launch {
        if (!shouldOpen(_state.value.book?.id, bookId)) return@launch
        childId = repo.ensureChild().id
        _state.value = ReaderState(book = store.byId(bookId), page = 0)
        startPage()
    }

    /** Moves [delta] pages and logs the page being left. Stops at the covers. */
    fun turn(delta: Int) {
        val book = _state.value.book ?: return
        val next = (_state.value.page + delta).coerceIn(0, book.pages.lastIndex)
        if (next == _state.value.page) return
        logPage()
        speaker.stop()
        _state.value = _state.value.copy(page = next, phase = TutorPhase.IDLE, speakingWord = -1, missedWord = null, chunks = emptyList())
        startPage()
    }

    /** Leaving the book forward off the last page. Logs the page that makes it "finished". */
    fun finish() {
        logPage()
        speaker.stop()
        _state.value = ReaderState()
    }

    fun speakLine() {
        val s = _state.value
        val book = s.book ?: return
        usedTts = true
        speaker.speakLine(
            line = book.pages[s.page],
            onWord = { i -> _state.value = _state.value.copy(speakingWord = i) },
            onDone = { _state.value = _state.value.copy(speakingWord = -1) },
        )
    }

    fun listen() {
        if (_state.value.book == null) return
        usedSelf = true
        _state.value = _state.value.copy(phase = TutorPhase.LISTENING, speakingWord = -1, missedWord = null, chunks = emptyList())
        speaker.stop()
        session.start()
        recognizer.listen(
            onResult = { transcript, confidence -> handleRecognition(transcript, confidence) },
            // An engine error is the same to the child as silence: nothing gets named or flagged.
            onError = {
                if (session.accept()) {
                    _state.value = _state.value.copy(phase = TutorPhase.UNCLEAR, speechMode = recognizer.lastMode)
                }
            },
        )
    }

    /** He tapped the mic off. Nothing he half-said gets coached or logged. */
    fun stopListening() {
        recognizer.cancel()
        session.cancel()
        if (_state.value.phase == TutorPhase.LISTENING) _state.value = _state.value.copy(phase = TutorPhase.IDLE)
    }

    /** Says the missed word in chunks, then whole. Tapping the coach card replays it. */
    fun replayChunks() {
        val s = _state.value
        val word = s.missedWord ?: return
        speaker.speakChunks(s.chunks.map { it.text }, word) { }
    }

    /**
     * Debug only: pretends the recognizer heard the line on the page, so the coach and the
     * "nice reading" states can be shown on an emulator with no microphone. Cycles a good read
     * and a missed word, both built from whatever book is open.
     */
    fun fakeRecognition() {
        val s = _state.value
        val book = s.book ?: return
        val fakes = fakeTranscripts(book.pages[s.page])
        session.start()
        handleRecognition(fakes[fakeIndex % fakes.size], 1f)
        fakeIndex++
    }

    private fun handleRecognition(transcript: String, confidence: Float) {
        if (!session.accept()) return
        val before = _state.value
        val book = before.book ?: return
        _state.value = onRecognized(before, transcript, confidence).copy(speechMode = recognizer.lastMode)
        val line = book.pages[before.page]
        // Scored a second time on purpose: the reducer owns the screen and coaches one word, while
        // the log wants every word he missed so the parent's practice list is complete.
        val r = ReadingDiff.score(line, transcript, if (confidence < 0) 1f else confidence)
        val id = childId
        viewModelScope.launch {
            repo.logAttempt(
                TutorAttempt(
                    childId = id, bookId = book.id, page = before.page, transcript = transcript,
                    confidence = confidence, missed = r.missed.joinToString(" "), totalWords = r.words.size,
                    lowConfidence = r.lowConfidence, at = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun startPage() {
        pageStartedAt = System.currentTimeMillis()
        usedTts = false
        usedSelf = false
    }

    private fun logPage() {
        val s = _state.value
        val book = s.book ?: return
        val id = childId
        val page = s.page
        val mode = pageMode(usedTts, usedSelf)
        val startedAt = pageStartedAt
        viewModelScope.launch { repo.logPage(id, book.id, page, mode, startedAt, System.currentTimeMillis()) }
    }

    override fun onCleared() {
        speaker.shutdown()
        recognizer.destroy()
    }
}
