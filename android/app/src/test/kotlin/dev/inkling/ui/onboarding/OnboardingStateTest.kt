package dev.inkling.ui.onboarding

import dev.inkling.core.Placement
import dev.inkling.core.WordOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStateTest {
    @Test fun blankNameCannotContinue() {
        assertFalse(canContinue(OnboardingState()))
        assertFalse(canContinue(OnboardingState(name = "   ")))
        assertTrue(canContinue(OnboardingState(name = "Cove")))
    }

    @Test fun okAdvancesToTheNextWord() {
        val s = onWordResult(OnboardingState(listening = true), WordOutcome.OK)
        assertEquals(1, s.wordIndex)
        assertEquals(listOf(WordOutcome.OK), s.outcomes)
        assertEquals(WordOutcome.OK, s.lastOutcome)
        assertFalse(s.listening)
        assertFalse(s.retried)
        assertNull(s.stage)
    }

    @Test fun firstUnclearStaysOnTheWord() {
        val s = onWordResult(OnboardingState(), WordOutcome.UNCLEAR)
        assertEquals(0, s.wordIndex)
        assertTrue(s.outcomes.isEmpty())
        assertTrue(s.retried)
        assertEquals(WordOutcome.UNCLEAR, s.lastOutcome)
    }

    @Test fun secondUnclearAppendsAndAdvances() {
        val once = onWordResult(OnboardingState(), WordOutcome.UNCLEAR)
        val twice = onWordResult(once, WordOutcome.UNCLEAR)
        assertEquals(1, twice.wordIndex)
        assertEquals(listOf(WordOutcome.UNCLEAR), twice.outcomes)
        assertFalse("the retry is spent on the word it was given to", twice.retried)
    }

    @Test fun retryIsPerWordNotPerRead() {
        val first = onWordResult(OnboardingState(), WordOutcome.UNCLEAR)
        val second = onWordResult(first, WordOutcome.OK)
        assertFalse(second.retried)
        assertEquals(1, second.wordIndex)
    }

    @Test fun sixOutcomesSetTheStage() {
        var s = OnboardingState()
        repeat(Placement.WORDS.size) { s = onWordResult(s, WordOutcome.OK) }
        assertEquals(6, s.outcomes.size)
        assertEquals(5, s.stage)
    }

    @Test fun skippingEverythingStartsAtTheFirstStage() {
        var s = OnboardingState()
        repeat(Placement.WORDS.size) { s = onWordResult(s, WordOutcome.SKIP) }
        assertEquals(0, s.stage)
    }

    @Test fun theResultLineNeverSaysWrong() {
        assertNull(resultLine(OnboardingState()))
        assertEquals("Got it", resultLine(OnboardingState(lastOutcome = WordOutcome.OK)))
        assertEquals("We'll start there", resultLine(OnboardingState(lastOutcome = WordOutcome.MISS)))
        assertEquals("One more try", resultLine(OnboardingState(lastOutcome = WordOutcome.UNCLEAR, retried = true)))
        // The retry is used up, so a second unclear reads like a skip, not a failure.
        assertEquals("We'll start there", resultLine(OnboardingState(lastOutcome = WordOutcome.UNCLEAR)))
    }

    @Test fun recapNamesReadAndSkippedWordsAndNothingElse() {
        val outcomes = listOf(WordOutcome.OK, WordOutcome.OK, WordOutcome.SKIP, WordOutcome.MISS)
        assertEquals("Read cat, pen. Skipped pig.", recapLine(outcomes))
    }

    @Test fun recapDropsTheHalfItHasNothingFor() {
        assertEquals("Read cat.", recapLine(listOf(WordOutcome.OK)))
        assertEquals("Skipped cat.", recapLine(listOf(WordOutcome.SKIP)))
        assertEquals("", recapLine(listOf(WordOutcome.MISS)))
    }

    @Test fun interestsAreTheTenFromTheProfileScreen() {
        assertEquals(10, INTERESTS.size)
        assertEquals("dinosaurs", INTERESTS.first())
    }
}
