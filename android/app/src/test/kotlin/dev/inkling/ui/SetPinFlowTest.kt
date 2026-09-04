package dev.inkling.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetPinFlowTest {
    @Test fun firstEntryAsksForConfirmation() {
        val step = SetPinFlow().submit("1234")
        assertTrue(step is SetPinStep.NeedConfirm)
        assertEquals(SET_PIN_CONFIRM, (step as SetPinStep.NeedConfirm).flow.prompt)
    }

    @Test fun matchingSecondEntryFinishes() {
        val after = (SetPinFlow().submit("1234") as SetPinStep.NeedConfirm).flow
        assertEquals(SetPinStep.Done("1234"), after.submit("1234"))
    }

    @Test fun mismatchStartsOverAndSetsNothing() {
        val after = (SetPinFlow().submit("1234") as SetPinStep.NeedConfirm).flow
        val step = after.submit("9999")
        assertTrue(step is SetPinStep.Mismatch)
        assertEquals(SetPinFlow(), (step as SetPinStep.Mismatch).flow)
        assertEquals(SET_PIN_FIRST, step.flow.prompt)
    }

    @Test fun oneEntryAloneNeverProducesAPin() {
        // The whole point of F2: a single entry, or an abandoned change, stores nothing.
        assertTrue(SetPinFlow().submit("1234") !is SetPinStep.Done)
    }
}
