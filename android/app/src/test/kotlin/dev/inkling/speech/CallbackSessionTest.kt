package dev.inkling.speech

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallbackSessionTest {
    @Test fun oldAttemptCannotConsumeNewAttempt() {
        val session = CallbackSession()
        val first = session.start()
        session.cancel()
        val second = session.start()

        assertFalse(session.accept(first))
        assertTrue(session.accept(second))
    }

    @Test fun terminalCallbackIsConsumedOnce() {
        val session = CallbackSession()
        val attempt = session.start()

        assertTrue(session.accept(attempt))
        assertFalse(session.accept(attempt))
    }

    @Test fun startingReplacementInvalidatesEarlierAttempt() {
        val session = CallbackSession()
        val first = session.start()
        val second = session.start()

        assertFalse(session.isCurrent(first))
        assertTrue(session.isCurrent(second))
    }
}
