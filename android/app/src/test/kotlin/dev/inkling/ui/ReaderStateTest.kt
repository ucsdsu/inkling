package dev.inkling.ui

import dev.inkling.books.Book
import dev.inkling.core.BookHistory
import dev.inkling.core.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderStateTest {
    private val hen = Book(
        id = "short-e-hen", title = "The Big Red Hen", stage = "cvc-e", target = "e",
        pages = listOf("The hen is in the pen.", "Ten eggs sat in the pen."),
    )
    private val state = ReaderState(book = hen, page = 0)
    private fun chunks(s: ReaderState) = s.chunks.joinToString("|") { (if (it.highlight) "*" else "") + it.text }

    @Test fun exactReadIsGood() {
        val s = onRecognized(state, "The hen is in the pen.", 0.9f)
        assertEquals(TutorPhase.GOOD, s.phase)
        assertNull(s.missedWord)
    }

    @Test fun oneMissCoachesThatWord() {
        val s = onRecognized(state, "the hen is in the pin", 0.9f)
        assertEquals(TutorPhase.COACH, s.phase)
        assertEquals("pen", s.missedWord)
        assertEquals("p|*e|n", chunks(s))
    }

    @Test fun emptyTranscriptIsUnclear() {
        val s = onRecognized(state, "", 0.9f)
        assertEquals(TutorPhase.UNCLEAR, s.phase)
        assertNull(s.missedWord)
        assertEquals(emptyList<Any>(), s.chunks)
    }

    @Test fun lowConfidenceIsUnclear() {
        assertEquals(TutorPhase.UNCLEAR, onRecognized(state, "the hen is in the pen", 0.2f).phase)
    }

    @Test fun noConfidenceScoreIsTrusted() {
        // The engine returns -1 when it scores nothing; that must not read as low confidence.
        assertEquals(TutorPhase.GOOD, onRecognized(state, "the hen is in the pen", -1f).phase)
    }

    @Test fun twoMissesCoachTheFirstOnly() {
        val s = onRecognized(state, "the hem is in the pin", 0.9f)
        assertEquals(TutorPhase.COACH, s.phase)
        assertEquals("hen", s.missedWord)
        assertEquals("h|*e|n", chunks(s))
    }

    @Test fun childArticulationStillCountsAsRead() {
        // "wed" for "red" is a 4-year-old saying r as w, not a decoding miss.
        val red = ReaderState(book = hen.copy(pages = listOf("The hen is red.")), page = 0)
        assertEquals(TutorPhase.GOOD, onRecognized(red, "the hen is wed", 1f).phase)
    }

    @Test fun cancellingTheMicDropsTheResultThatArrivesAnyway() {
        val session = ListenSession()
        var phase = TutorPhase.IDLE
        var logged = 0
        // The same order the view model calls these in.
        fun onResult() {
            if (!session.accept()) return
            phase = TutorPhase.GOOD
            logged++
        }
        session.start()
        session.cancel()
        onResult()
        assertEquals(TutorPhase.IDLE, phase)
        assertEquals(0, logged)
        // The cancel is spent: the next turn at the mic counts.
        session.start()
        onResult()
        assertEquals(TutorPhase.GOOD, phase)
        assertEquals(1, logged)
    }

    @Test fun debugFakesComeFromTheLineOnThePage() {
        val fakes = fakeTranscripts("The hen is in the pen.")
        assertEquals(listOf("The hen is in the pen.", "The hen is in the ben."), fakes)
        assertEquals(TutorPhase.GOOD, onRecognized(state, fakes[0], 1f).phase)
        val coached = onRecognized(state, fakes[1], 1f)
        assertEquals(TutorPhase.COACH, coached.phase)
        assertEquals("pen", coached.missedWord)
    }

    @Test fun theGoodFakeIsTheLineWithRSaidAsW() {
        val red = ReaderState(book = hen.copy(pages = listOf("The hen is red.")), page = 0)
        val fakes = fakeTranscripts("The hen is red.")
        assertEquals(listOf("The hen is wed.", "The hen is bed."), fakes)
        assertEquals(TutorPhase.GOOD, onRecognized(red, fakes[0], 1f).phase)
        assertEquals("red", onRecognized(red, fakes[1], 1f).missedWord)
    }

    @Test fun ttsWinsTheModeForThePage() {
        // He tapped "Read to me" and then tried it himself: he heard the line, so the log says tts.
        assertEquals(ReadMode.TTS, pageMode(usedTts = true, usedSelf = true))
        assertEquals(ReadMode.SELF, pageMode(usedTts = false, usedSelf = true))
        assertEquals(ReadMode.LOOK, pageMode(usedTts = false, usedSelf = false))
    }

    @Test fun reopeningTheOpenBookLeavesThePageAlone() {
        // Rotation re-runs the route's open effect. Reloading it rewound him to page 0.
        assertFalse(shouldOpen("short-e-hen", "short-e-hen"))
        assertTrue(shouldOpen(null, "short-e-hen"))
        assertTrue(shouldOpen("short-a-cat", "short-e-hen"))
    }

    @Test fun freshShelfOffersTheFirstStageOnly() {
        val rows = buildShelf(books, emptyMap())
        assertEquals(listOf(Tag.TRY_IT, Tag.STRETCH, Tag.STRETCH), rows.map { it.tag })
    }

    @Test fun masteringABookMovesTheCurrentStageUp() {
        val h = mapOf("a" to BookHistory("a", 1, 0.96f))
        assertEquals(1, currentStageIndex(books, h))
        assertEquals(listOf(Tag.EASY, Tag.TRY_IT, Tag.STRETCH), buildShelf(books, h).map { it.tag })
    }

    @Test fun aRoughReadIsStretchWhereverItSits() {
        val h = mapOf("a" to BookHistory("a", 0, 0.6f))
        assertEquals(Tag.STRETCH, buildShelf(books, h).first().tag)
    }

    @Test fun subtitleNamesTheStageAndLength() {
        assertEquals("Decodable · short e · 2 pages", buildShelf(books, emptyMap())[1].subtitle)
        assertEquals("Decodable · sh digraph · 2 pages", buildShelf(books, emptyMap())[2].subtitle)
    }

    private val books = listOf(
        Book("a", "A", "cvc-a", "a", listOf("The cat sat.", "The rat ran.")),
        hen,
        Book("sh", "S", "digraph-sh", "sh", listOf("The ship is in the shop.", "Shut the shop.")),
    )
}
