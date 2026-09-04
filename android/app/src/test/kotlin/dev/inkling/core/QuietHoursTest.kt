package dev.inkling.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    private val h = 60
    @Test fun insideSameDayRange() { assertTrue(QuietHours.isQuiet(13 * h, 12 * h, 14 * h)) }
    @Test fun outsideSameDayRange() { assertFalse(QuietHours.isQuiet(15 * h, 12 * h, 14 * h)) }
    @Test fun crossesMidnightLateEvening() { assertTrue(QuietHours.isQuiet(20 * h, 19 * h + 30, 7 * h)) }
    @Test fun crossesMidnightEarlyMorning() { assertTrue(QuietHours.isQuiet(6 * h, 19 * h + 30, 7 * h)) }
    @Test fun crossesMidnightDaytimeIsNotQuiet() { assertFalse(QuietHours.isQuiet(12 * h, 19 * h + 30, 7 * h)) }
    @Test fun startEqualsEndMeansNeverQuiet() { assertFalse(QuietHours.isQuiet(12 * h, 8 * h, 8 * h)) }
}
