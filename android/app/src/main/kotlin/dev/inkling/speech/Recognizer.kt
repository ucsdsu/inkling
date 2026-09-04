package dev.inkling.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Thin wrapper over Android's on-device SpeechRecognizer.
 *
 * Offline is requested via EXTRA_PREFER_OFFLINE; the device decides. Wi-Fi off during the spike
 * proves whether that request is honored. Emulators ship no offline language pack, so a
 * language-missing error falls back to one online attempt. Offline keeps kid audio on the device;
 * online is an emulator crutch and is labeled as such in [lastMode].
 */
class Recognizer(context: Context) {
    private val sr: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    private val main = Handler(Looper.getMainLooper())

    val available: Boolean get() = sr != null

    /** "offline" or "online" — which attempt produced the last result or error. */
    var lastMode: String = "offline"
        private set

    fun listen(onResult: (String, Float) -> Unit, onError: (String) -> Unit) =
        start(preferOffline = true, canRetryOnline = true, onResult = onResult, onError = onError)

    /**
     * Starts one recognition attempt.
     *
     * @param preferOffline ask the engine to stay on the device
     * @param canRetryOnline retry once online when the engine has no offline language pack
     */
    private fun start(
        preferOffline: Boolean,
        canRetryOnline: Boolean,
        onResult: (String, Float) -> Unit,
        onError: (String) -> Unit,
    ) {
        val s = sr ?: return onError("No speech recognizer on this device")
        lastMode = if (preferOffline) "offline" else "online"
        s.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                val conf = scores?.firstOrNull() ?: -1f   // -1 means the engine gave no score
                onResult(texts.firstOrNull().orEmpty(), conf)
            }
            override fun onError(error: Int) {
                val noOfflineLanguage = error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                if (canRetryOnline && noOfflineLanguage) {
                    // startListening from inside onError is unreliable; hand it back to the looper.
                    main.post { start(preferOffline = false, canRetryOnline = false, onResult = onResult, onError = onError) }
                } else {
                    onError("recognizer error $error")
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }
        s.startListening(intent)
    }

    fun stop() { sr?.stopListening() }
    fun destroy() { sr?.destroy() }
}
