package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingDiffTest {
    private val line = "The big red hen sat in the pen."

    @Test
    fun exactReadIsAllOk() {
        val r = ReadingDiff.score(line, "the big red hen sat in the pen", 0.9f)
        assertTrue(r.words.all { it.second == WordResult.OK })
        assertFalse(r.lowConfidence)
        assertEquals(emptyList<String>(), r.missed)
    }

    @Test
    fun oneSubstitutionIsOneMiss() {
        val r = ReadingDiff.score(line, "the big red hen sat in the pin", 0.9f)
        assertEquals(listOf("pen"), r.missed)
    }

    @Test
    fun skippedWordIsAMiss() {
        val r = ReadingDiff.score(line, "the big hen sat in the pen", 0.9f)
        assertEquals(listOf("red"), r.missed)
    }

    @Test
    fun extraWordsFromSelfCorrectionAreIgnored() {
        val r = ReadingDiff.score(line, "the big red hen hen sat in the pen", 0.9f)
        assertEquals(emptyList<String>(), r.missed)
    }

    @Test
    fun typicalArticulationIsNotAMiss() {
        // r -> w, l -> w, th -> f
        assertEquals(emptyList<String>(), ReadingDiff.score("the red lamp", "the wed wamp", 0.9f).missed)
        assertEquals(emptyList<String>(), ReadingDiff.score("thin", "fin", 0.9f).missed)
    }

    @Test
    fun initialClusterReductionIsNotAMiss() {
        assertEquals(emptyList<String>(), ReadingDiff.score("stop the truck", "top the tuck", 0.9f).missed)
    }

    @Test
    fun reductionIsNotChainedWithSubstitution() {
        // "grill" -> "rill" -> "will" and "free" -> "ree" -> "wee" are different words, not lisps.
        assertEquals(listOf("grill"), ReadingDiff.score("grill", "will", 0.9f).missed)
        assertEquals(listOf("free"), ReadingDiff.score("free", "wee", 0.9f).missed)
    }

    @Test
    fun singleStepPatternsStillPass() {
        // Cluster reduction alone: a documented 4-year-old pattern.
        assertEquals(emptyList<String>(), ReadingDiff.score("black", "back", 0.9f).missed)
        assertEquals(emptyList<String>(), ReadingDiff.score("stop", "top", 0.9f).missed)
        // Substitution alone.
        assertEquals(emptyList<String>(), ReadingDiff.score("red", "wed", 0.9f).missed)
    }

    @Test
    fun lowConfidenceNeverProducesMiss() {
        val r = ReadingDiff.score(line, "the big red hen sat in the pin", 0.3f)
        assertTrue(r.lowConfidence)
        assertTrue(r.words.none { it.second == WordResult.MISS })
        assertTrue(r.words.any { it.second == WordResult.UNSURE })
    }

    @Test
    fun emptyTranscriptIsLowConfidence() {
        val r = ReadingDiff.score(line, "", 0.9f)
        assertTrue(r.lowConfidence)
        assertEquals(emptyList<String>(), r.missed)
    }

    @Test
    fun punctuationAndCaseAreIgnored() {
        val r = ReadingDiff.score("Hop, on Pop!", "hop on pop", 0.9f)
        assertEquals(emptyList<String>(), r.missed)
        assertEquals(listOf("Hop,", "on", "Pop!"), r.words.map { it.first })
    }
}
