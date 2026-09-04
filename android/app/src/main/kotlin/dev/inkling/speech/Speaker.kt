package dev.inkling.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Reads a line aloud and reports which word is being spoken, for highlighting.
 * Word index comes from onRangeStart's character offset mapped onto the line's words.
 * Also speaks a missed word in chunks ("p. e. n. pen") for the coach.
 */
class Speaker(context: Context, private val onReady: (Boolean) -> Unit) {
    private var ready = false
    private var configured = false

    // The init listener must not touch `tts`: it can fire before the constructor has assigned it.
    // Language and rate are set on first use instead, when the field is certainly there.
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        onReady(ready)
    }

    /** The engine, configured, or null when it never came up. */
    private fun engine(): TextToSpeech? {
        if (!ready) return null
        if (!configured) {
            tts.language = Locale.US
            tts.setSpeechRate(SPEECH_RATE)
            configured = true
        }
        return tts
    }

    fun speakLine(line: String, onWord: (Int) -> Unit, onDone: () -> Unit) {
        val tts = engine() ?: run { onDone(); return }
        val starts = wordStarts(line)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val idx = starts.indexOfLast { it <= start }
                if (idx >= 0) onWord(idx)
            }
            override fun onDone(utteranceId: String?) { onDone() }
            override fun onError(utteranceId: String?) { onDone() }
            override fun onStart(utteranceId: String?) {}
        })
        tts.speak(line, TextToSpeech.QUEUE_FLUSH, Bundle(), "line")
    }

    /** "p. e. n. pen": chunks with pauses, then the whole word. Letter names, not phonemes, until we ship phoneme clips. */
    fun speakChunks(chunks: List<String>, word: String, onDone: () -> Unit) {
        val tts = engine() ?: run { onDone(); return }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) { if (utteranceId == "word") onDone() }
            override fun onError(utteranceId: String?) { onDone() }
            override fun onStart(utteranceId: String?) {}
        })
        tts.speak(chunks.joinToString(". ") + ".", TextToSpeech.QUEUE_FLUSH, Bundle(), "chunks")
        tts.playSilentUtterance(400, TextToSpeech.QUEUE_ADD, "gap")
        tts.speak(word, TextToSpeech.QUEUE_ADD, Bundle(), "word")
    }

    fun stop() { tts.stop() }
    fun shutdown() { tts.shutdown() }

    private fun wordStarts(line: String): List<Int> {
        val out = mutableListOf<Int>()
        var inWord = false
        line.forEachIndexed { i, c -> if (!c.isWhitespace() && !inWord) { out += i; inWord = true } else if (c.isWhitespace()) inWord = false }
        return out
    }

    private companion object {
        /** Slow enough for a four-year-old to follow the highlighted word. */
        const val SPEECH_RATE = 0.85f
    }
}
