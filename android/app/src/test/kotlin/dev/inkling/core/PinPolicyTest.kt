package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PinPolicyTest {
    @Test fun firstThreeFailuresAreFree() {
        assertEquals(0, PinPolicy.lockoutSeconds(1))
        assertEquals(0, PinPolicy.lockoutSeconds(3))
    }
    @Test fun thenThirtySecondsDoubling() {
        assertEquals(30, PinPolicy.lockoutSeconds(4))
        assertEquals(60, PinPolicy.lockoutSeconds(5))
        assertEquals(120, PinPolicy.lockoutSeconds(6))
    }
    @Test fun capsAtOneHour() { assertEquals(3600, PinPolicy.lockoutSeconds(20)) }
}
