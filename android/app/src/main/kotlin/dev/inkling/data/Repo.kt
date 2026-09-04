package dev.inkling.data

import androidx.room.withTransaction
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
}
