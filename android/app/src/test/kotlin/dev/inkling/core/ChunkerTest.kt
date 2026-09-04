package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ChunkerTest {
    private fun s(chunks: List<Chunk>) = chunks.joinToString("|") { (if (it.highlight) "*" else "") + it.text }

    @Test fun cvcSplitsPerLetterAndHighlightsTarget() { assertEquals("p|*e|n", s(Chunker.chunk("pen", "e"))) }
    @Test fun digraphStaysTogether() { assertEquals("*sh|i|p", s(Chunker.chunk("ship", "sh"))); assertEquals("f|i|*sh", s(Chunker.chunk("fish", "sh"))) }
    @Test fun otherDigraphsStayTogetherUnhighlighted() { assertEquals("th|*i|n", s(Chunker.chunk("thin", "i"))); assertEquals("b|*a|ck", s(Chunker.chunk("back", "a"))) }
    @Test fun punctuationAndCaseAreStripped() { assertEquals("*h|e|n", s(Chunker.chunk("Hen.", "h"))) }
}
