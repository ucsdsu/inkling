package dev.inkling.service

import dev.inkling.core.Rule
import dev.inkling.core.Span
import dev.inkling.core.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStateTest {
    private val minute = 60_000L
    private val now = 1_000_000_000_000L
    private val snap = Snapshot(kidsModeOn = true, rules = listOf(Rule("com.chess", true, 30)), ceiling = 60, warning = 2, quietStart = 19 * 60 + 30, quietEnd = 7 * 60)

    @Test fun allowedAppUnderCapDoesNothing() {
        val (a, v) = ServiceState.onForeground("com.chess", "dev.inkling", snap, emptyList(), now, 0, 12 * 60, emptySet())
        assertEquals(Action.None, a); assertEquals(Verdict.ALLOW, v)
    }

    @Test fun notAllowedAppIsSentHome() {
        val (a, v) = ServiceState.onForeground("com.android.settings", "dev.inkling", snap, emptyList(), now, 0, 12 * 60, emptySet())
        assertEquals(Action.SendHome, a); assertEquals(Verdict.BLOCK_NOT_ALLOWED, v)
    }

    @Test fun withinWarningWindowWarnsOnce() {
        val spans = listOf(Span("com.chess", now - 28 * minute, null))
        val (a, _) = ServiceState.onForeground("com.chess", "dev.inkling", snap, spans, now, 0, 12 * 60, emptySet())
        assertEquals(Action.Warn(2), a)
        val (again, _) = ServiceState.onForeground("com.chess", "dev.inkling", snap, spans, now, 0, 12 * 60, setOf("com.chess"))
        assertEquals(Action.None, again)
    }

    @Test fun atCapIsSentHome() {
        val spans = listOf(Span("com.chess", now - 30 * minute, null))
        val (a, v) = ServiceState.onForeground("com.chess", "dev.inkling", snap, spans, now, 0, 12 * 60, setOf("com.chess"))
        assertEquals(Action.SendHome, a); assertEquals(Verdict.BLOCK_CAPPED, v)
    }

    @Test fun kidsModeOffNeverActs() {
        val (a, _) = ServiceState.onForeground("com.android.settings", "dev.inkling", snap.copy(kidsModeOn = false), emptyList(), now, 0, 12 * 60, emptySet())
        assertEquals(Action.None, a)
    }

    @Test
    fun ownWarningOverlayEventIsIgnored() {
        // The overlay is a window in our own package. Handling its event closed the running span
        // and killed the mid-session cap re-check.
        val until = 1_000L + ServiceState.OVERLAY_SUPPRESS_MS
        assertTrue(ServiceState.ignoreEvent("dev.inkling", "dev.inkling", until, 2_000L))
    }

    @Test
    fun otherPackagesAndLateOwnEventsAreNotIgnored() {
        val until = 1_000L + ServiceState.OVERLAY_SUPPRESS_MS
        // A real switch to Inkling after the overlay window has passed still counts.
        assertFalse(ServiceState.ignoreEvent("dev.inkling", "dev.inkling", until, until + 1))
        // Another app is never suppressed.
        assertFalse(ServiceState.ignoreEvent("com.chess", "dev.inkling", until, 2_000L))
    }
}
