package dev.inkling.speech

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSpeechRecognizer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecognizerTest {
    private class Engine : RecognitionEngine {
        var result: ((String, Float) -> Unit)? = null
        var error: ((Int) -> Unit)? = null
        var cancelled = false
        var destroyed = false

        override fun listen(intent: Intent, onResult: (String, Float) -> Unit, onError: (Int) -> Unit) {
            result = onResult
            error = onError
        }
        override fun cancel() { cancelled = true }
        override fun destroy() { destroyed = true }
    }

    @Test fun unavailableOfflineEngineDoesNotCreateNetworkRecognizer() {
        ShadowSpeechRecognizer.setIsOnDeviceRecognitionAvailable(false)
        val recognizer = Recognizer(ApplicationProvider.getApplicationContext<Context>())
        var error: String? = null
        recognizer.listen({ _, _ -> fail("Unexpected result") }, { error = it })
        assertEquals(Recognizer.OFFLINE_UNAVAILABLE, error)
        assertNull(ShadowSpeechRecognizer.getLatestSpeechRecognizer())
    }

    @Test fun missingLanguageReportsSetupWithoutOnlineRetry() {
        ShadowSpeechRecognizer.setIsOnDeviceRecognitionAvailable(true)
        val recognizer = Recognizer(ApplicationProvider.getApplicationContext<Context>())
        var error: String? = null
        recognizer.listen({ _, _ -> fail("Unexpected result") }, { error = it })
        shadowOf(Looper.getMainLooper()).idle()
        val engine = ShadowSpeechRecognizer.getLatestSpeechRecognizer()
        val shadow = shadowOf(engine)
        val intent = shadow.lastRecognizerIntent
        assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
        shadow.triggerOnError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(Recognizer.OFFLINE_UNAVAILABLE, error)
        assertSame(intent, shadow.lastRecognizerIntent)
    }

    @Test fun replacementUsesNewEngineAndRejectsOldBinderCallback() {
        val engines = mutableListOf<Engine>()
        val recognizer = Recognizer(available = true) { Engine().also { engines += it } }
        val results = mutableListOf<String>()

        recognizer.listen({ text, _ -> results += "first:$text" }, { fail(it) })
        val first = engines.single()
        recognizer.listen({ text, _ -> results += "second:$text" }, { fail(it) })
        val second = engines.last()

        assertTrue(first.cancelled)
        assertTrue(first.destroyed)
        first.result!!("late", 1f)
        second.result!!("current", 1f)

        assertEquals(listOf("second:current"), results)
        assertTrue(second.destroyed)
    }
}
