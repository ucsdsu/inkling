package dev.inkling.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReadingRepoTest {
    private lateinit var db: InklingDb
    private lateinit var repo: Repo

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), InklingDb::class.java)
            .allowMainThreadQueries().build()
        repo = Repo(db)
    }
    @After fun tearDown() { db.close() }

    private fun attempt(
        childId: Long,
        missed: String,
        at: Long,
        bookId: String = BOOK,
        totalWords: Int = 8,
        lowConfidence: Boolean = false,
    ) = TutorAttempt(
        childId = childId,
        bookId = bookId,
        page = 0,
        transcript = "the hen is in the pen",
        confidence = 0.9f,
        missed = missed,
        totalWords = totalWords,
        lowConfidence = lowConfidence,
        at = at,
    )

    @Test fun historyIsNullWithoutAttempts() = runBlocking {
        val c = repo.ensureChild()
        repo.logPage(c.id, BOOK, page = 0, mode = "tts", startedAt = 1_000, endedAt = 5_000)
        val h = repo.history(c.id, BOOK, PAGES)
        assertEquals(BOOK, h.bookId)
        assertEquals(0, h.finished)
        assertNull(h.lastAccuracy)
    }

    @Test fun accuracyIsOneMinusMissedShare() = runBlocking {
        val c = repo.ensureChild()
        repo.logAttempt(attempt(c.id, missed = "pen", at = 1_000))
        repo.logAttempt(attempt(c.id, missed = "pen ten hen", at = 2_000))
        assertEquals(0.75f, repo.history(c.id, BOOK, PAGES).lastAccuracy!!, 0.0001f)
    }

    @Test fun unclearAttemptsDoNotCountTowardAccuracy() = runBlocking {
        val c = repo.ensureChild()
        // Three mumbles flagged nothing. Counted, they read as a perfect four-page book.
        repeat(3) { i -> repo.logAttempt(attempt(c.id, missed = "", at = 1_000L + i, lowConfidence = true)) }
        assertNull(repo.history(c.id, BOOK, PAGES).lastAccuracy)
        repo.logAttempt(attempt(c.id, missed = "pen", at = 4_000, totalWords = 4))
        assertEquals(0.75f, repo.history(c.id, BOOK, PAGES).lastAccuracy!!, 0.0001f)
    }

    @Test fun finishedCountsLastPageOnly() = runBlocking {
        val c = repo.ensureChild()
        repo.logPage(c.id, BOOK, page = 3, mode = "self", startedAt = 1_000, endedAt = 2_000)
        repo.logPage(c.id, BOOK, page = PAGES - 1, mode = "self", startedAt = 3_000, endedAt = 4_000)
        repo.logPage(c.id, BOOK, page = PAGES - 1, mode = "tts", startedAt = DAY + 5_000, endedAt = DAY + 6_000)
        assertEquals(2, repo.history(c.id, BOOK, PAGES).finished)
    }

    @Test fun finishedCountsOncePerDay() = runBlocking {
        val c = repo.ensureChild()
        // Paging back and forth over the last page is one reading, not three.
        repo.logPage(c.id, BOOK, page = PAGES - 1, mode = "self", startedAt = 1_000, endedAt = 2_000)
        repo.logPage(c.id, BOOK, page = PAGES - 1, mode = "self", startedAt = 3_000, endedAt = 4_000)
        repo.logPage(c.id, BOOK, page = PAGES - 1, mode = "tts", startedAt = 5_000, endedAt = 6_000)
        assertEquals(1, repo.history(c.id, BOOK, PAGES).finished)
        repo.logPage(c.id, BOOK, page = PAGES - 1, mode = "self", startedAt = DAY + 1_000, endedAt = DAY + 2_000)
        assertEquals(2, repo.history(c.id, BOOK, PAGES).finished)
    }

    @Test fun missedTwiceOrdersByFrequency() = runBlocking {
        val c = repo.ensureChild()
        repo.logAttempt(attempt(c.id, missed = "pen ten hen", at = 1_000))
        repo.logAttempt(attempt(c.id, missed = "pen ten", at = 2_000))
        repo.logAttempt(attempt(c.id, missed = "pen", at = 3_000))
        assertEquals(listOf("pen", "ten"), repo.missedTwice(c.id))
    }

    @Test fun booksFinishedTodayIgnoresYesterday() = runBlocking {
        val c = repo.ensureChild()
        val dayStart = 100_000L
        repo.logPage(c.id, "short-a-cat", page = PAGES - 1, mode = "self", startedAt = dayStart - 10_000, endedAt = dayStart - 9_000)
        repo.logPage(c.id, BOOK, page = PAGES - 1, mode = "self", startedAt = dayStart + 1_000, endedAt = dayStart + 2_000)
        repo.logPage(c.id, BOOK, page = PAGES - 1, mode = "tts", startedAt = dayStart + 3_000, endedAt = dayStart + 4_000)
        repo.logPage(c.id, "short-i-pig", page = 2, mode = "self", startedAt = dayStart + 5_000, endedAt = dayStart + 6_000)
        assertEquals(1, repo.booksFinishedToday(c.id, dayStart))
    }

    private companion object {
        const val BOOK = "short-e-hen"
        const val PAGES = 10
        const val DAY = 86_400_000L
    }
}
