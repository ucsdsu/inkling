package dev.inkling.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

const val OFFLINE_TTS_UNAVAILABLE =
    "Read aloud isn't available offline. A parent can set up an offline English voice; you can still read this page."
const val TTS_STARTING = "Read aloud is still starting. Try again in a moment."

data class VoiceDescriptor(val name: String, val locale: Locale, val networkRequired: Boolean)

/** Prefer a US voice, but accept any installed English voice that never needs a connection. */
fun chooseOfflineEnglishVoice(voices: Collection<VoiceDescriptor>): VoiceDescriptor? = voices
    .filter { it.locale.language == Locale.ENGLISH.language && !it.networkRequired }
    .sortedWith(compareByDescending<VoiceDescriptor> { it.locale == Locale.US }.thenBy { it.name })
    .firstOrNull()

interface SpeechSpeaker {
    fun speakLine(
        line: String,
        onWord: (Int) -> Unit,
        onDone: () -> Unit,
        onUnavailable: (String) -> Unit,
    ): Boolean
    fun speakChunks(
        chunks: List<String>,
        word: String,
        onDone: () -> Unit,
        onUnavailable: (String) -> Unit,
    ): Boolean
    fun stop()
    fun shutdown()
}

/** Reads aloud through an installed offline English voice. */
class Speaker(context: Context) : SpeechSpeaker {
    private var ready = false
    private var failed = false
    private var configured = false
    private val playback = CallbackSession()

    // The init listener must not touch `tts`: it can fire before the constructor has assigned it.
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        failed = status != TextToSpeech.SUCCESS
    }

    private fun engine(onUnavailable: (String) -> Unit): TextToSpeech? {
        if (!ready) {
            onUnavailable(if (failed) OFFLINE_TTS_UNAVAILABLE else TTS_STARTING)
            return null
        }
        if (!configured) {
            val voices = tts.voices.orEmpty()
            val selected = chooseOfflineEnglishVoice(
                voices.map { VoiceDescriptor(it.name, it.locale, it.isNetworkConnectionRequired) },
            )
            val voice = selected?.let { choice ->
                voices.firstOrNull { it.name == choice.name && it.locale == choice.locale }
            }
            if (voice == null || tts.setVoice(voice) == TextToSpeech.ERROR) {
                onUnavailable(OFFLINE_TTS_UNAVAILABLE)
                return null
            }
            tts.setSpeechRate(SPEECH_RATE)
            configured = true
        }
        return tts
    }

    /** Returns true only when the line was accepted by an offline engine. */
    override fun speakLine(
        line: String,
        onWord: (Int) -> Unit,
        onDone: () -> Unit,
        onUnavailable: (String) -> Unit,
    ): Boolean {
        val engine = engine(onUnavailable) ?: return false
        val token = playback.start()
        val utteranceId = "line-$token"
        val starts = wordStarts(line)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
                if (id != utteranceId || !playback.isCurrent(token)) return
                val idx = starts.indexOfLast { it <= start }
                if (idx >= 0) onWord(idx)
            }
            override fun onDone(id: String?) {
                if (id == utteranceId && playback.accept(token)) onDone()
            }
            override fun onError(id: String?) {
                if (id == utteranceId && playback.accept(token)) onUnavailable(OFFLINE_TTS_UNAVAILABLE)
            }
            override fun onStart(id: String?) {}
        })
        if (engine.speak(line, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId) == TextToSpeech.ERROR) {
            playback.cancel()
            onUnavailable(OFFLINE_TTS_UNAVAILABLE)
            return false
        }
        return true
    }

    /** "p. e. n. pen": chunks with pauses, then the whole word. */
    override fun speakChunks(
        chunks: List<String>,
        word: String,
        onDone: () -> Unit,
        onUnavailable: (String) -> Unit,
    ): Boolean {
        val engine = engine(onUnavailable) ?: return false
        val token = playback.start()
        val prefix = "chunks-$token"
        val wordId = "$prefix-word"
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(id: String?) {
                if (id == wordId && playback.accept(token)) onDone()
            }
            override fun onError(id: String?) {
                if (id?.startsWith(prefix) == true && playback.accept(token)) onUnavailable(OFFLINE_TTS_UNAVAILABLE)
            }
            override fun onStart(id: String?) {}
        })
        val results = listOf(
            engine.speak(chunks.joinToString(". ") + ".", TextToSpeech.QUEUE_FLUSH, Bundle(), "$prefix-parts"),
            engine.playSilentUtterance(400, TextToSpeech.QUEUE_ADD, "$prefix-gap"),
            engine.speak(word, TextToSpeech.QUEUE_ADD, Bundle(), wordId),
        )
        if (results.any { it == TextToSpeech.ERROR }) {
            playback.cancel()
            engine.stop()
            onUnavailable(OFFLINE_TTS_UNAVAILABLE)
            return false
        }
        return true
    }

    override fun stop() {
        playback.cancel()
        tts.stop()
    }

    override fun shutdown() {
        playback.cancel()
        tts.shutdown()
    }

    private fun wordStarts(line: String): List<Int> {
        val out = mutableListOf<Int>()
        var inWord = false
        line.forEachIndexed { i, c ->
            if (!c.isWhitespace() && !inWord) { out += i; inWord = true }
            else if (c.isWhitespace()) inWord = false
        }
        return out
    }

    private companion object {
        const val SPEECH_RATE = 0.85f
    }
}
