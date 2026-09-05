package dev.inkling.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

interface RecognitionInput {
    val available: Boolean
    val lastMode: String
    fun listen(onResult: (String, Float) -> Unit, onError: (String) -> Unit)
    fun cancel()
    fun destroy()
}

interface RecognitionEngine {
    fun listen(intent: Intent, onResult: (String, Float) -> Unit, onError: (Int) -> Unit)
    fun cancel()
    fun destroy()
}

private class AndroidRecognitionEngine(private val recognizer: SpeechRecognizer) : RecognitionEngine {
    override fun listen(intent: Intent, onResult: (String, Float) -> Unit, onError: (Int) -> Unit) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                onResult(texts.firstOrNull().orEmpty(), scores?.firstOrNull() ?: -1f)
            }
            override fun onError(error: Int) { onError(error) }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    override fun cancel() { recognizer.cancel() }
    override fun destroy() { recognizer.destroy() }
}

/** Uses only Android's on-device recognition service. Never retries through a network service. */
class Recognizer private constructor(
    private val availability: () -> Boolean,
    private val createEngine: () -> RecognitionEngine,
) : RecognitionInput {
    constructor(context: Context) : this(
        availability = {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        },
        createEngine = {
            AndroidRecognitionEngine(SpeechRecognizer.createOnDeviceSpeechRecognizer(context))
        },
    )

    internal constructor(available: Boolean, createEngine: () -> RecognitionEngine) :
        this(availability = { available }, createEngine = createEngine)

    private var current: RecognitionEngine? = null

    override val available: Boolean get() = availability()

    /** Every attempt stays on the device. */
    override val lastMode: String = "offline"

    override fun listen(onResult: (String, Float) -> Unit, onError: (String) -> Unit) {
        cancel()
        if (!available) return onError(OFFLINE_UNAVAILABLE)
        val engine = createEngine()
        synchronized(this) { current = engine }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
        }
        engine.listen(
            intent,
            onResult = { text, confidence ->
                if (claim(engine)) {
                    try { onResult(text, confidence) } finally { engine.destroy() }
                }
            },
            onError = { error ->
                if (claim(engine)) {
                    try {
                        onError(
                            if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                                error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) OFFLINE_UNAVAILABLE
                            else "recognizer error $error",
                        )
                    } finally {
                        engine.destroy()
                    }
                }
            },
        )
    }

    /** Stop the mic and throw the result away. What the child means by tapping "Listening…" off. */
    override fun cancel() {
        val engine = synchronized(this) { current.also { current = null } } ?: return
        engine.cancel()
        engine.destroy()
    }

    override fun destroy() { cancel() }

    @Synchronized private fun claim(engine: RecognitionEngine): Boolean {
        if (current !== engine) return false
        current = null
        return true
    }

    companion object {
        const val OFFLINE_UNAVAILABLE = "Offline speech isn't available. A parent can set it up; you can still read or skip."
    }
}
