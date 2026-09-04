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

/** Prompt shown before the first of the two entries. */
const val SET_PIN_FIRST = "Choose a 4-digit parent PIN"
/** Prompt shown for the confirming entry. */
const val SET_PIN_CONFIRM = "Enter it again"
/** Shown when the two entries differ. The flow starts over. */
const val SET_PIN_MISMATCH = "Those don't match. Start over."

/** One step out of [SetPinFlow.submit]. */
sealed interface SetPinStep {
    /** The first entry landed. Ask for it again. */
    data class NeedConfirm(val flow: SetPinFlow) : SetPinStep
    /** The two entries differ. [flow] is back at the start. */
    data class Mismatch(val flow: SetPinFlow) : SetPinStep
    /** Both entries matched. [pin] is safe to store. */
    data class Done(val pin: String) : SetPinStep
}

/**
 * Pure state machine for choosing a new PIN. Nothing is stored until the parent types the same
 * 4 digits twice, so a half-finished change cannot leave the device without a PIN.
 *
 * @property first the first entry, null before it is typed
 */
data class SetPinFlow(val first: String? = null) {
    val prompt: String get() = if (first == null) SET_PIN_FIRST else SET_PIN_CONFIRM

    fun submit(pin: String): SetPinStep = when {
        first == null -> SetPinStep.NeedConfirm(SetPinFlow(pin))
        first == pin -> SetPinStep.Done(pin)
        else -> SetPinStep.Mismatch(SetPinFlow(null))
    }
}
