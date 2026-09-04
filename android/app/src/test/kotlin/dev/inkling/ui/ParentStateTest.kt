package dev.inkling.ui

import dev.inkling.core.Span
import dev.inkling.data.AppRule
import org.junit.Assert.assertEquals
import org.junit.Test

class ParentStateTest {
    private val minute = 60_000L
    private val now = 1_000_000_000_000L
    private val rules = listOf(
        AppRule(childId = 1, packageName = "com.chess", label = "Chess", enabled = true, dailyCapMinutes = 30),
        AppRule(childId = 1, packageName = "com.hangman", label = "Hangman", enabled = true, dailyCapMinutes = 15),
    )

    @Test fun rowsSortedByMinutesDescWithTotal() {
        val spans = listOf(
            Span("com.chess", now - 12 * minute, now),
            Span("com.hangman", now - 40 * minute, now - 20 * minute),
        )
        val (rows, total) = buildToday(rules, spans, now, 0)
        assertEquals(listOf("Hangman" to 20, "Chess" to 12), rows.map { it.label to it.minutes })
        assertEquals(32, total)
    }

    @Test fun zeroMinuteAppsStillListed() {
        val (rows, total) = buildToday(rules, emptyList(), now, 0)
        assertEquals(2, rows.size); assertEquals(0, total)
    }
}
