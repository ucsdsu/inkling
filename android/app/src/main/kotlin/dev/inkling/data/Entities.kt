package dev.inkling.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ageYears: Int,
    val createdAt: Long,
    /** Comma-separated interest tags from onboarding. Stored now, used by phase 2 book generation. */
    val interests: String = "",
    /** Stage index the placement read put him at. The shelf never drops below it. */
    val startStage: Int = 0,
)

/**
 * The one row of device-wide state. Several children can live here; exactly one is active, and a
 * null [activeChildId] means nobody has been onboarded yet.
 */
@Entity
data class DeviceState(
    @PrimaryKey val id: Int = 1,
    val activeChildId: Long?,
)

@Entity(indices = [Index(value = ["childId", "packageName"], unique = true)])
data class AppRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val dailyCapMinutes: Int,
)

@Entity(indices = [Index("childId"), Index("startedAt")])
data class UsageEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val packageName: String,
    val startedAt: Long,
    val endedAt: Long?,
)

@Entity
data class Settings(
    @PrimaryKey val childId: Long,
    val kidsModeOn: Boolean = false,
    val deviceCeilingMinutes: Int = 60,
    val warningMinutes: Int = 2,
    val quietStartMinute: Int = 19 * 60 + 30,
    val quietEndMinute: Int = 7 * 60,
    val pinHash: String? = null,
    val pinFailures: Int = 0,
    val lockoutUntil: Long = 0,
)

@Entity
data class SpikeRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expected: String,
    val transcript: String,
    val confidence: Float,
    val flagged: String,
    val verdict: String,
    val at: Long,
)

@Entity(indices = [Index("childId"), Index("bookId"), Index("startedAt")])
data class ReadEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val bookId: String,
    val page: Int,
    val startedAt: Long,
    val endedAt: Long?,
    /** "tts" (read to me), "self" (I'll read), or "look" (page turned without either). */
    val mode: String,
)

@Entity(indices = [Index("childId"), Index("bookId"), Index("at")])
data class TutorAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val bookId: String,
    val page: Int,
    val transcript: String,
    val confidence: Float,
    /** Space-separated words the child missed on this line. Empty when the line was clean. */
    val missed: String,
    /** Words on the line, so accuracy is missed share of what was actually attempted. */
    val totalWords: Int,
    val lowConfidence: Boolean,
    val at: Long,
)
