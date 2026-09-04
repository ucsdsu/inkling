package dev.inkling.ui

/** What the PIN pad is for: unlocking with the stored PIN, or choosing a new one. */
enum class PinMode { UNLOCK, SET }

/**
 * Pure. Decides what the PIN pad should do for a given parent state.
 *
 * Returns null while [ParentState] is still loading: the settings row is null both before the
 * first read and when there is genuinely no PIN, and guessing SET there would let a cold start
 * overwrite an existing PIN.
 */
fun pinMode(state: ParentState): PinMode? = when {
    !state.loaded -> null
    state.settings?.pinHash == null -> PinMode.SET
    else -> PinMode.UNLOCK
}
