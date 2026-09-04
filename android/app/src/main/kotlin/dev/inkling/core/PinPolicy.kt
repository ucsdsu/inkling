package dev.inkling.core

object PinPolicy {
    const val FREE_ATTEMPTS = 3
    private const val BASE_SECONDS = 30
    private const val MAX_SECONDS = 3600

    /** Seconds the PIN pad stays locked after [failures] consecutive wrong entries. */
    fun lockoutSeconds(failures: Int): Int {
        if (failures <= FREE_ATTEMPTS) return 0
        val doublings = failures - FREE_ATTEMPTS - 1
        val seconds = BASE_SECONDS.toLong() shl minOf(doublings, 20)
        return minOf(seconds, MAX_SECONDS.toLong()).toInt()
    }
}
