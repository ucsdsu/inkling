package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementTest {
    @Test fun sixWordsOneForEachStarterStage() {
        assertEquals(6, Placement.WORDS.size)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), Placement.WORDS.map { it.stageIndex })
    }

    @Test fun allOkStartsAtTheLastStage() {
        assertEquals(5, Placement.stageFor(List(6) { WordOutcome.OK }))
    }

    @Test fun firstMissSetsTheStage() {
        assertEquals(1, Placement.stageFor(listOf(WordOutcome.OK, WordOutcome.MISS, WordOutcome.OK)))
    }

    @Test fun skipOnTheFirstWordStartsAtTheFirstStage() {
        assertEquals(0, Placement.stageFor(listOf(WordOutcome.SKIP)))
    }

    @Test fun unclearCountsAsNotOk() {
        assertEquals(2, Placement.stageFor(listOf(WordOutcome.OK, WordOutcome.OK, WordOutcome.UNCLEAR)))
    }

    @Test fun nothingReadStartsAtTheFirstStage() {
        assertEquals(0, Placement.stageFor(emptyList()))
    }

    @Test fun articulationIsNotAMiss() {
        assertEquals(WordOutcome.OK, Placement.outcome("red", "wed", 0.9f))
    }

    @Test fun aDifferentWordIsAMiss() {
        assertEquals(WordOutcome.MISS, Placement.outcome("pen", "pin", 0.9f))
    }

    @Test fun silenceIsUnclear() {
        assertEquals(WordOutcome.UNCLEAR, Placement.outcome("pen", "", 0.9f))
    }

    @Test fun lowConfidenceIsUnclear() {
        assertEquals(WordOutcome.UNCLEAR, Placement.outcome("pen", "pin", 0.3f))
    }
}
