package dev.inkling.core

/** A foreground stint of one app. [endedAt] is null while the app is still in front. */
data class Span(val packageName: String, val startedAt: Long, val endedAt: Long?)

/** Per-app rule. [dailyCapMinutes] of 0 means no cap. */
data class Rule(val packageName: String, val enabled: Boolean, val dailyCapMinutes: Int)

object Budget {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MINUTE_MS = 60L * 1000

    /**
     * Longest an OPEN span can count for. A span left open by a crash, a reboot, or an unbound
     * accessibility service would otherwise charge the child for every hour since. A closed span
     * has a real end time written by the service, so it is trusted however long it ran.
     */
    const val MAX_SPAN_MS = 2 * 60 * 60 * 1000L

    /** Local midnight before [now], as epoch millis, for a fixed zone offset. */
    fun startOfDay(now: Long, zoneOffsetMillis: Long): Long {
        val local = now + zoneOffsetMillis
        val localMidnight = local - Math.floorMod(local, DAY_MS)
        return localMidnight - zoneOffsetMillis
    }

    fun usedMinutesToday(spans: List<Span>, packageName: String, now: Long, zoneOffsetMillis: Long): Int =
        usedMinutesTodayAll(spans, setOf(packageName), now, zoneOffsetMillis)

    fun usedMinutesTodayAll(spans: List<Span>, packages: Set<String>, now: Long, zoneOffsetMillis: Long): Int {
        val dayStart = startOfDay(now, zoneOffsetMillis)
        var total = 0L
        for (s in spans) {
            if (s.packageName !in packages) continue
            val start = maxOf(s.startedAt, dayStart)
            val end = if (s.endedAt != null) minOf(s.endedAt, now) else minOf(now, s.startedAt + MAX_SPAN_MS)
            if (end > start) total += end - start
        }
        return (total / MINUTE_MS).toInt()
    }
}
