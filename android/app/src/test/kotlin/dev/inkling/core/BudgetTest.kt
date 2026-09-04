package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetTest {
    private val day = 24L * 60 * 60 * 1000
    private val minute = 60L * 1000
    private val noon = 3 * day + 12 * 60 * minute   // some day at 12:00 UTC
    private val zone = 0L

    @Test
    fun startOfDayIsMidnightInZone() {
        assertEquals(3 * day, Budget.startOfDay(noon, zone))
        // UTC-7: local midnight is 07:00 UTC
        assertEquals(3 * day + 7 * 60 * minute, Budget.startOfDay(noon, -7 * 60 * minute))
    }

    @Test
    fun sumsOnlyTodayAndOnlyThatPackage() {
        val spans = listOf(
            Span("chess", noon - 30 * minute, noon - 10 * minute),   // 20 min today
            Span("chess", noon - day - 30 * minute, noon - day),     // yesterday, ignored
            Span("hangman", noon - 5 * minute, noon),                // other app
        )
        assertEquals(20, Budget.usedMinutesToday(spans, "chess", noon, zone))
    }

    @Test
    fun openSpanCountsUpToNow() {
        val spans = listOf(Span("chess", noon - 7 * minute, null))
        assertEquals(7, Budget.usedMinutesToday(spans, "chess", noon, zone))
    }

    @Test
    fun spanCrossingMidnightCountsOnlyTodayPart() {
        val midnight = 3 * day
        val spans = listOf(Span("chess", midnight - 10 * minute, midnight + 5 * minute))
        assertEquals(5, Budget.usedMinutesToday(spans, "chess", noon, zone))
    }

    @Test
    fun allPackagesSum() {
        val spans = listOf(
            Span("chess", noon - 30 * minute, noon - 10 * minute),
            Span("hangman", noon - 5 * minute, noon),
            Span("read", noon - 50 * minute, noon - 40 * minute),
        )
        assertEquals(25, Budget.usedMinutesTodayAll(spans, setOf("chess", "hangman"), noon, zone))
    }
}
