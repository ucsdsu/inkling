package dev.inkling.spike

import dev.inkling.data.SpikeRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpikeRunTest {
    @Test fun rapidRepeatedVerdictStartsOneSave() {
        val guard = SpikeSaveGuard()
        assertTrue(guard.begin(7))
        assertFalse(guard.begin(7))
        guard.succeeded(7)
        assertFalse(guard.begin(7))
        assertTrue(guard.begin(8))
    }

    @Test fun failedSaveCanRetrySameResult() {
        val guard = SpikeSaveGuard()
        assertTrue(guard.begin(7))
        guard.failed(7)
        assertTrue(guard.begin(7))
    }

    @Test fun noCorrectReadsIsInsufficientData() {
        val summary = summarizeSpike(listOf(row("wrong", "cat"), row("unclear", "")))
        assertEquals(0, summary.correctReads)
        assertNull(summary.falseFlagRate)
        assertNull(summary.passesGate)
        assertFalse(summary.hasGateSample)
    }

    @Test fun falseFlagRateExcludesWrongAndUnclearRows() {
        val rows = buildList {
            repeat(2) { add(row("correct", "cat")) }
            repeat(18) { add(row("correct", "")) }
            add(row("wrong", "dog"))
            add(row("unclear", "hen"))
        }
        val summary = summarizeSpike(rows)
        assertEquals(20, summary.correctReads)
        assertEquals(2, summary.falselyFlagged)
        assertEquals(0.10f, summary.falseFlagRate!!, 0.0001f)
        assertTrue(summary.hasGateSample)
        assertEquals(true, summary.passesGate)
    }

    @Test fun oneCorrectAndNineteenWrongCannotClaimGatePass() {
        val rows = listOf(row("correct", "")) + List(19) { row("wrong", "") }
        val summary = summarizeSpike(rows)
        assertEquals(1, summary.correctReads)
        assertEquals(0f, summary.falseFlagRate!!, 0.0001f)
        assertNull(summary.passesGate)
    }

    @Test fun threeOfTwentyIsOverGateThreshold() {
        val summary = summarizeSpike(List(3) { row("correct", "cat") } + List(17) { row("correct", "") })
        assertEquals(0.15f, summary.falseFlagRate!!, 0.0001f)
        assertEquals(false, summary.passesGate)
    }

    private fun row(verdict: String, flagged: String) = SpikeRow(
        expected = "The cat sat.", transcript = "the cat sat", confidence = 1f,
        flagged = flagged, verdict = verdict, at = 1,
    )
}
