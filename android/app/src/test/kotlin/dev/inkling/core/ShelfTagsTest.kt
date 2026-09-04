package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfTagsTest {
    @Test fun neverOpenedAtCurrentStageIsTryIt() { assertEquals(Tag.TRY_IT, ShelfTags.tag(null, 1, 1)) }
    @Test fun aboveCurrentStageIsStretch() { assertEquals(Tag.STRETCH, ShelfTags.tag(null, 3, 1)) }
    @Test fun belowCurrentStageIsEasy() { assertEquals(Tag.EASY, ShelfTags.tag(null, 0, 2)) }
    @Test fun finishedWithHighAccuracyIsEasy() { assertEquals(Tag.EASY, ShelfTags.tag(BookHistory("x", 1, 0.96f), 1, 1)) }
    @Test fun openedButNotMasteredIsJustRight() { assertEquals(Tag.JUST_RIGHT, ShelfTags.tag(BookHistory("x", 0, 0.8f), 1, 1)) }
    @Test fun lowAccuracyIsStretch() { assertEquals(Tag.STRETCH, ShelfTags.tag(BookHistory("x", 0, 0.6f), 1, 1)) }
}
