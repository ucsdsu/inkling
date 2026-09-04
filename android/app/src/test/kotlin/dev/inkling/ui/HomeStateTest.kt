package dev.inkling.ui

import dev.inkling.core.Span
import dev.inkling.data.AppRule
import dev.inkling.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {
    private val minute = 60_000L
    private val now = 1_000_000_000_000L
    private val rules = listOf(
        AppRule(childId = 1, packageName = "com.chess", label = "Chess", enabled = true, dailyCapMinutes = 30),
        AppRule(childId = 1, packageName = "com.off", label = "Off", enabled = false, dailyCapMinutes = 0),
        AppRule(childId = 1, packageName = "com.hoopla", label = "Hoopla", enabled = true, dailyCapMinutes = 0),
    )
    private val settings = Settings(childId = 1, kidsModeOn = true, deviceCeilingMinutes = 60)

    @Test fun onlyEnabledAppsBecomeTiles() {
        val s = buildHomeState("Cove", rules, emptyList(), settings, now, 0, 12 * 60)
        assertEquals(listOf("Chess", "Hoopla"), s.tiles.map { it.label })
    }

    @Test fun fractionReflectsUsage() {
        val spans = listOf(Span("com.chess", now - 15 * minute, now))
        val s = buildHomeState("Cove", rules, spans, settings, now, 0, 12 * 60)
        assertEquals(0.5f, s.tiles.first { it.label == "Chess" }.fraction, 0.01f)
        assertFalse(s.tiles.first { it.label == "Chess" }.done)
    }

    @Test fun cappedAppIsDone() {
        val spans = listOf(Span("com.chess", now - 30 * minute, now))
        val s = buildHomeState("Cove", rules, spans, settings, now, 0, 12 * 60)
        assertTrue(s.tiles.first { it.label == "Chess" }.done)
    }

    @Test fun noCapAppShowsZeroFractionAndNeverDone() {
        val spans = listOf(Span("com.hoopla", now - 500 * minute, now))
        val s = buildHomeState("Cove", rules, spans, settings.copy(deviceCeilingMinutes = 0), now, 0, 12 * 60)
        val t = s.tiles.first { it.label == "Hoopla" }
        assertEquals(0f, t.fraction, 0.01f); assertFalse(t.done)
    }

    @Test fun quietHoursMarkEverythingDone() {
        val s = buildHomeState("Cove", rules, emptyList(), settings, now, 0, 21 * 60)
        assertTrue(s.tiles.all { it.done })
    }

    @Test fun bookCountReadsAsEnglish() {
        assertEquals("0 books today.", booksTodayLine(0))
        assertEquals("1 book today.", booksTodayLine(1))
        assertEquals("3 books today.", booksTodayLine(3))
    }
}
