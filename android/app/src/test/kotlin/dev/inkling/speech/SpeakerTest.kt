package dev.inkling.speech

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeakerTest {
    @Test fun offlineEnglishVoiceWinsOverNetworkDefault() {
        val networkUs = VoiceDescriptor("default-network", Locale.US, networkRequired = true)
        val offlineGb = VoiceDescriptor("english-offline", Locale.UK, networkRequired = false)
        assertEquals(offlineGb, chooseOfflineEnglishVoice(listOf(networkUs, offlineGb)))
    }

    @Test fun offlineUsVoiceWinsOverOtherOfflineEnglishVoice() {
        val offlineGb = VoiceDescriptor("a-gb", Locale.UK, networkRequired = false)
        val offlineUs = VoiceDescriptor("z-us", Locale.US, networkRequired = false)
        assertEquals(offlineUs, chooseOfflineEnglishVoice(listOf(offlineGb, offlineUs)))
    }

    @Test fun networkOnlyOrNonEnglishVoicesAreUnavailable() {
        val network = VoiceDescriptor("network", Locale.US, networkRequired = true)
        val offlineFrench = VoiceDescriptor("french", Locale.FRANCE, networkRequired = false)
        assertNull(chooseOfflineEnglishVoice(listOf(network, offlineFrench)))
    }
}
