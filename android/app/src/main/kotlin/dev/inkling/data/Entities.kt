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
