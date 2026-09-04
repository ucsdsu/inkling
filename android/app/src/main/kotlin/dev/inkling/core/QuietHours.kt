package dev.inkling.core

object QuietHours {
    /** All arguments are minutes since local midnight. A range that ends before it starts crosses midnight. */
    fun isQuiet(minuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean {
        if (startMinute == endMinute) return false
        return if (startMinute < endMinute) {
            minuteOfDay in startMinute until endMinute
        } else {
            minuteOfDay >= startMinute || minuteOfDay < endMinute
        }
    }
}
