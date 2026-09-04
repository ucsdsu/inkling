package dev.inkling.ui

import dev.inkling.books.Book
import dev.inkling.core.BookHistory
import dev.inkling.core.NextUp
import dev.inkling.core.Span
import dev.inkling.core.Tag
import dev.inkling.data.AppRule
import dev.inkling.data.TutorAttempt
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

    @Test fun csvFieldDoublesEmbeddedQuotes() {
        assertEquals("\"he said \"\"hop\"\"\"", csvField("he said \"hop\""))
    }

    @Test fun csvFieldQuotesPlainAndCommaBearingValues() {
        assertEquals("\"hop on pop\"", csvField("hop on pop"))
        assertEquals("\"red, hen\"", csvField("red, hen"))
    }

    private val hen = Book("short-e-hen", "The Big Red Hen", "cvc-e", "e", List(10) { "The hen is red." })
    private val fox = Book("short-o-fox", "The Fox on the Log", "cvc-o", "o", List(10) { "The fox is on a log." })
    private val day = 1_700_000_000_000L
    private fun attempt(at: Long, missed: String, total: Int, low: Boolean = false) =
        TutorAttempt(childId = 1, bookId = "short-e-hen", page = 0, transcript = "x", confidence = 0.9f, missed = missed, totalWords = total, lowConfidence = low, at = at)

    @Test fun readingListsEveryBookWithItsHistory() {
        val r = buildReading(
            listOf(hen, fox), mapOf("short-e-hen" to BookHistory("short-e-hen", 2, 0.91f)),
            emptyList(), emptyList(), day,
        )
        assertEquals(listOf("finished×2 · 91%", "not started"), r.books.map(::readingRowValue))
    }

    @Test fun todaysAccuracyIgnoresYesterdayAndUnclearLines() {
        val r = buildReading(
            listOf(hen), emptyMap(), listOf("red"),
            listOf(
                attempt(day - 1, "red red red", 4),          // yesterday
                attempt(day + 1, "red", 4),                   // today: 1 of 4 missed
                attempt(day + 2, "", 4),                      // today: clean
                attempt(day + 3, "", 4, low = true),          // today: never scored
            ),
            day,
        )
        assertEquals(2, r.attemptsToday)
        assertEquals(0.875f, r.accuracyToday!!, 0.001f)
        assertEquals(listOf("red"), r.missedTwice)
    }

    @Test fun noAttemptsMeansNoAverage() {
        assertEquals(null, buildReading(listOf(hen), emptyMap(), emptyList(), emptyList(), day).accuracyToday)
    }

    @Test fun nextUpMapsShelfRowsToEntries() {
        val books = listOf(hen, fox)
        val histories = mapOf("short-e-hen" to BookHistory("short-e-hen", 1, 0.88f))
        val entries = toShelfEntries(buildShelf(books, histories), histories)
        assertEquals(listOf("short-e-hen", "short-o-fox"), entries.map { it.bookId })
        assertEquals(listOf("The Big Red Hen", "The Fox on the Log"), entries.map { it.title })
        assertEquals(listOf("short e", "short o"), entries.map { it.stageLabel })
        assertEquals(listOf(Tag.JUST_RIGHT, Tag.STRETCH), entries.map { it.tag })
        assertEquals(listOf(0.88f, null), entries.map { it.lastAccuracy })

        // The mapping is only useful if NextUp can read it: the 88% book is the second-pass item.
        val out = NextUp.recommend(entries, listOf("pen"))
        assertEquals(listOf("The Big Red Hen", "The Fox on the Log"), out.map { it.title })
        assertEquals(
            "The Big Red Hen was 88% last time. A second pass usually pushes it past 90. Words to watch: pen.",
            out[0].why,
        )
    }
}
