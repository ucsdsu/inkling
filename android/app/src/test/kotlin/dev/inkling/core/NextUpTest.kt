package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextUpTest {
    private fun entry(id: String, title: String, label: String, tag: Tag, acc: Float? = null) =
        ShelfEntry(bookId = id, title = title, stageLabel = label, tag = tag, lastAccuracy = acc)

    @Test fun freshShelfSuggestsTheTryItAndOneStretch() {
        val rows = listOf(
            entry("cat", "Cat", "short a", Tag.TRY_IT),
            entry("hen", "Hen", "short e", Tag.STRETCH),
            entry("pig", "Pig", "short i", Tag.STRETCH),
        )
        val out = NextUp.recommend(rows, emptyList())
        assertEquals(2, out.size)
        assertEquals("Cat", out[0].title)
        assertEquals("cat", out[0].bookId)
        assertEquals("Introduces short a. One new pattern is the right step.", out[0].why)
        assertEquals("Hen", out[1].title)
        assertEquals("Too many new patterns for now. Keep it as a 'read to me' book.", out[1].why)
    }

    @Test fun aJustRightUnderNinetyGetsASecondPass() {
        val rows = listOf(
            entry("cat", "Cat", "short a", Tag.TRY_IT),
            entry("hen", "Hen", "short e", Tag.JUST_RIGHT, 0.88f),
            entry("pig", "Pig", "short i", Tag.STRETCH),
        )
        val out = NextUp.recommend(rows, listOf("pen", "ten"))
        assertEquals(3, out.size)
        assertEquals("Hen", out[1].title)
        assertEquals(
            "Hen was 88% last time. A second pass usually pushes it past 90. Words to watch: pen, ten.",
            out[1].why,
        )
        assertEquals("Pig", out[2].title)
    }

    @Test fun aJustRightAtOrAboveNinetyIsNotSuggested() {
        val rows = listOf(entry("hen", "Hen", "short e", Tag.JUST_RIGHT, 0.93f))
        assertTrue(NextUp.recommend(rows, emptyList()).isEmpty())
    }

    @Test fun everythingEasySuggestsNothing() {
        val rows = listOf(
            entry("cat", "Cat", "short a", Tag.EASY, 0.99f),
            entry("hen", "Hen", "short e", Tag.EASY, 0.97f),
        )
        assertEquals(emptyList<Recommendation>(), NextUp.recommend(rows, emptyList()))
    }

    @Test fun missedTwiceOnlyLandsOnTheSecondPassItem() {
        val rows = listOf(entry("cat", "Cat", "short a", Tag.TRY_IT), entry("pig", "Pig", "short i", Tag.STRETCH))
        val out = NextUp.recommend(rows, listOf("pen", "ten"))
        assertEquals(2, out.size)
        assertTrue(out.none { it.why.contains("Words to watch") })
    }
}
