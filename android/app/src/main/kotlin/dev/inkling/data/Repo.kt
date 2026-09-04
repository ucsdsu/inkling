package dev.inkling.data

import androidx.room.withTransaction
import dev.inkling.core.BookHistory
import dev.inkling.core.Budget
import dev.inkling.core.Span
import java.security.MessageDigest

object Pin {
    /** SHA-256 hex of the PIN. Not salted: a 4-digit PIN has 10,000 values, salt buys nothing against a curious parent. */
    fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
}

class Repo(private val db: InklingDb) {
    suspend fun ensureChild(): Child {
        db.children().first()?.let { return it }
        val id = db.children().insert(Child(name = "Cove", ageYears = 4, createdAt = System.currentTimeMillis()))
        db.settings().upsert(Settings(childId = id))
        return db.children().first()!!
    }

    suspend fun settings(childId: Long): Settings = db.settings().get(childId) ?: Settings(childId).also { db.settings().upsert(it) }
    suspend fun saveSettings(s: Settings) = db.settings().upsert(s)

    suspend fun rules(childId: Long): List<AppRule> = db.rules().list(childId)
    suspend fun upsertRule(r: AppRule) {
        val existing = db.rules().find(r.childId, r.packageName)
        if (existing == null) db.rules().insert(r) else db.rules().update(r.copy(id = existing.id))
    }

    suspend fun openSpan(childId: Long, pkg: String, now: Long) = db.withTransaction {
        closeOpenSpanInternal(now)
        db.usage().insert(UsageEvent(childId = childId, packageName = pkg, startedAt = now, endedAt = null))
        Unit
    }

    /**
     * Opens a span for [pkg] unless it is already the open one. The mid-session cap re-check runs
     * handle() again for the same app every minute; without this each pass would split the session
     * into one-minute spans and lose the seconds in between.
     */
    suspend fun openSpanIfChanged(childId: Long, pkg: String, now: Long) = db.withTransaction {
        val open = db.usage().open()
        if (open.isNotEmpty() && open.all { it.packageName == pkg }) return@withTransaction
        closeOpenSpanInternal(now)
        db.usage().insert(UsageEvent(childId = childId, packageName = pkg, startedAt = now, endedAt = null))
        Unit
    }

    /**
     * Closes every span left open by a crash, a reboot, or an unbound service, capping each at
     * [Budget.MAX_SPAN_MS] past its start. Run at service connect and at app start.
     */
    suspend fun closeStaleSpans(now: Long) = db.withTransaction {
        for (e in db.usage().open()) db.usage().update(e.copy(endedAt = minOf(now, e.startedAt + Budget.MAX_SPAN_MS)))
    }

    suspend fun closeOpenSpan(now: Long) = db.withTransaction { closeOpenSpanInternal(now) }

    /** Caller must already hold the transaction. */
    private suspend fun closeOpenSpanInternal(now: Long) {
        for (e in db.usage().open()) db.usage().update(e.copy(endedAt = now))
    }

    suspend fun spansToday(childId: Long, dayStart: Long): List<Span> =
        db.usage().since(childId, dayStart).map { Span(it.packageName, it.startedAt, it.endedAt) }

    suspend fun addSpike(row: SpikeRow) = db.spikes().insert(row)
    suspend fun spikes(): List<SpikeRow> = db.spikes().all()

    suspend fun logPage(childId: Long, bookId: String, page: Int, mode: String, startedAt: Long, endedAt: Long) {
        db.reads().insert(ReadEvent(childId = childId, bookId = bookId, page = page, startedAt = startedAt, endedAt = endedAt, mode = mode))
    }

    suspend fun logAttempt(a: TutorAttempt) {
        db.attempts().insert(a)
    }

    /**
     * Distinct books whose last page was reached since [dayStart]. Every starter book is
     * [PAGES_PER_BOOK] pages, so the last page index is the same for all of them.
     */
    suspend fun booksFinishedToday(childId: Long, dayStart: Long): Int =
        db.reads().booksFinishedSince(childId, PAGES_PER_BOOK - 1, dayStart)

    /**
     * Shelf history for one book. [finished] counts how many times the last page was logged.
     * Accuracy pools the ten most recent attempts on the book: 1 - missed words / words attempted.
     * Null when he has never read it aloud, which the shelf shows as "try it" rather than a score.
     *
     * Attempts the recognizer was not sure about are dropped first. They flag nothing, so counting
     * them scored three mumbles as a perfect read and promoted him a stage for saying nothing.
     */
    suspend fun history(childId: Long, bookId: String, pageCount: Int): BookHistory {
        val finished = db.reads().timesFinished(childId, bookId, pageCount - 1)
        val recent = db.attempts().recentForBook(childId, bookId)
        val total = recent.sumOf { it.totalWords }
        val missed = recent.sumOf { words(it.missed).size }
        val accuracy = if (total == 0) null else 1f - missed.toFloat() / total
        return BookHistory(bookId = bookId, finished = finished, lastAccuracy = accuracy)
    }

    /** Words he missed in two or more attempts, most missed first. The parent's practice list. */
    suspend fun missedTwice(childId: Long): List<String> =
        db.attempts().all(childId)
            .flatMap { words(it.missed).toSet() }
            .groupingBy { it }.eachCount()
            .filter { it.value >= 2 }
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }

    suspend fun recentAttempts(childId: Long, limit: Int): List<TutorAttempt> = db.attempts().recent(childId, limit)

    /** [TutorAttempt.missed] is space-separated; empty and blank strings mean nothing was missed. */
    private fun words(missed: String): List<String> = missed.split(" ").filter { it.isNotBlank() }

    companion object {
        /** Every book in the starter pack is ten pages. Task 1's BookStoreTest holds this true. */
        const val PAGES_PER_BOOK = 10
    }
}
