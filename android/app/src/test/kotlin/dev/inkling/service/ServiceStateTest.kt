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
    fun tickAtCapSendsHome() {
        // Nothing new happened: the child never left the app, the minutes just ran out. The ticker
        // re-evaluates the app already in front and must reach the same verdict a switch would.
        val spans = listOf(Span("com.chess", now - 30 * minute, null))
        val (a, v) = ServiceState.onTick("com.chess", "dev.inkling", snap, spans, now, 0, 12 * 60, setOf("com.chess"))
        assertEquals(Action.SendHome, a); assertEquals(Verdict.BLOCK_CAPPED, v)
    }

    @Test
    fun tickUnderCapDoesNothing() {
        val spans = listOf(Span("com.chess", now - 5 * minute, null))
        val (a, v) = ServiceState.onTick("com.chess", "dev.inkling", snap, spans, now, 0, 12 * 60, setOf("com.chess"))
        assertEquals(Action.None, a); assertEquals(Verdict.ALLOW, v)
    }

    @Test
    fun ourOwnActivityIsAForegroundChange() {
        assertTrue(ServiceState.isForegroundChange("dev.inkling", "dev.inkling.MainActivity", "dev.inkling"))
    }

    @Test
    fun ourWarningOverlayIsNot() {
        // The overlay is a window in our own package and reports its widget class. Acting on it
        // closed the running span and pointed lastPkg at us.
        assertFalse(ServiceState.isForegroundChange("dev.inkling", "android.widget.TextView", "dev.inkling"))
        assertFalse(ServiceState.isForegroundChange("dev.inkling", null, "dev.inkling"))
    }

    @Test
    fun anotherPackageAlwaysCounts() {
        assertTrue(ServiceState.isForegroundChange("com.chess", "android.widget.TextView", "dev.inkling"))
        assertTrue(ServiceState.isForegroundChange("com.chess", null, "dev.inkling"))
    }

    @Test
    fun mainActivityConstantMatchesTheRealClass() {
        // A rename of MainActivity would otherwise silently stop Inkling being seen as foreground.
        assertEquals(dev.inkling.MainActivity::class.java.name, ServiceState.MAIN_ACTIVITY)
    }
}
