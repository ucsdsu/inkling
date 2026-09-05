package dev.inkling.spike

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.inkling.core.DiffResult
import dev.inkling.core.ReadingDiff
import dev.inkling.data.Repo
import dev.inkling.data.SpikeRow
import dev.inkling.speech.CallbackSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SpikeSummary(val correctReads: Int, val falselyFlagged: Int) {
    val falseFlagRate: Float? = if (correctReads == 0) null else falselyFlagged.toFloat() / correctReads
    val hasGateSample: Boolean = correctReads >= REQUIRED_CORRECT_READS
    val passesGate: Boolean? = if (!hasGateSample) null else falseFlagRate?.let { it <= MAX_FALSE_FLAG_RATE }

    companion object {
        const val REQUIRED_CORRECT_READS = 20
        const val MAX_FALSE_FLAG_RATE = 0.10f
    }
}

/** Wrong and unclear reads cannot prove a false positive, so they do not enter the denominator. */
fun summarizeSpike(rows: List<SpikeRow>): SpikeSummary {
    val correct = rows.filter { it.verdict == "correct" }
    return SpikeSummary(
        correctReads = correct.size,
        falselyFlagged = correct.count { it.flagged.isNotBlank() },
    )
}

/** Prevents two rapid verdict taps from saving the same recognition result twice. */
class SpikeSaveGuard {
    private var saving: Long? = null
    private val saved = mutableSetOf<Long>()

    fun begin(resultId: Long): Boolean {
        if (saving != null || resultId in saved) return false
        saving = resultId
        return true
    }

    fun succeeded(resultId: Long) {
        if (saving == resultId) {
            saved += resultId
            saving = null
        }
    }

    fun failed(resultId: Long) {
        if (saving == resultId) saving = null
    }
}

data class PendingSpikeResult(
    val id: Long,
    val expected: String,
    val transcript: String,
    val confidence: Float,
    val diff: DiffResult,
)

data class SpikeRunState(
    val savedRows: List<SpikeRow> = emptyList(),
    val pending: PendingSpikeResult? = null,
    val saving: Boolean = false,
    val status: String = "Tap Listen, then read the line.",
) {
    val complete: Boolean get() = savedRows.size >= SpikeLines.lines.size
    val lineIndex: Int get() = savedRows.size.coerceAtMost(SpikeLines.lines.lastIndex)
}

/** Route-scoped state. It survives rotation, including a Room write already in flight. */
class SpikeRunViewModel(
    private val repo: Repo,
    private val saveRow: suspend (SpikeRow) -> Unit = repo::addSpike,
) : ViewModel() {
    private val recognition = CallbackSession()
    private val saves = SpikeSaveGuard()
    private val _state = MutableStateFlow(SpikeRunState())
    val state: StateFlow<SpikeRunState> = _state

    fun beginListen(): Long? {
        val s = _state.value
        if (s.saving || s.complete) return null
        val token = recognition.start()
        _state.value = s.copy(pending = null, status = "Listening…")
        return token
    }

    fun onResult(token: Long, transcript: String, confidence: Float) {
        if (!recognition.accept(token)) return
        val s = _state.value
        if (s.complete) return
        val expected = SpikeLines.lines[s.lineIndex]
        val diff = ReadingDiff.score(expected, transcript, if (confidence < 0) 1f else confidence)
        val conf = if (confidence < 0) "n/a" else "%.2f".format(confidence)
        _state.value = s.copy(
            pending = PendingSpikeResult(token, expected, transcript, confidence, diff),
            status = "Heard: \"$transcript\"  conf=$conf via offline",
        )
    }

    fun onError(token: Long, message: String) {
        if (recognition.accept(token)) _state.value = _state.value.copy(status = "$message via offline")
    }

    fun cancelLiveAttempt() {
        recognition.cancel()
        if (_state.value.status == "Listening…") {
            _state.value = _state.value.copy(status = "Listening stopped. Tap Listen to try again.")
        }
    }

    /** Returns null when this result is already saving or saved. */
    fun record(verdict: String): Job? {
        val pending = _state.value.pending ?: return null
        if (_state.value.complete || !saves.begin(pending.id)) return null
        val row = SpikeRow(
            expected = pending.expected, transcript = pending.transcript, confidence = pending.confidence,
            flagged = pending.diff.missed.joinToString(" "), verdict = verdict, at = System.currentTimeMillis(),
        )
        _state.value = _state.value.copy(saving = true)
        return viewModelScope.launch {
            try {
                saveRow(row)
                saves.succeeded(pending.id)
                val rows = _state.value.savedRows + row
                _state.value = _state.value.copy(
                    savedRows = rows, pending = null, saving = false,
                    status = if (rows.size >= SpikeLines.lines.size) "Run complete." else "Saved. Next line.",
                )
            } catch (_: Exception) {
                saves.failed(pending.id)
                _state.value = _state.value.copy(saving = false, status = "Couldn't save. Try that verdict again.")
            }
        }
    }

    override fun onCleared() { recognition.cancel() }
}
