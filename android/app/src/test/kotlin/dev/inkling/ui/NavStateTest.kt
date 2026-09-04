package dev.inkling.ui

import dev.inkling.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavStateTest {
    @Test fun unloadedStateHasNoPinMode() {
        assertNull(pinMode(ParentState()))
        // A settings row that arrived without the loaded flag is still not trustworthy.
        assertNull(pinMode(ParentState(settings = Settings(childId = 1))))
    }

    @Test fun loadedWithoutHashSetsPin() {
        assertEquals(PinMode.SET, pinMode(ParentState(settings = Settings(childId = 1), loaded = true)))
    }

    @Test fun loadedWithHashUnlocks() {
        val s = Settings(childId = 1, pinHash = "abc")
        assertEquals(PinMode.UNLOCK, pinMode(ParentState(settings = s, loaded = true)))
    }
}
