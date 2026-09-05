package dev.inkling.speech

/**
 * Gives each asynchronous speech operation an identity. Starting a new operation invalidates the
 * old one, and accepting a terminal callback consumes the current operation exactly once.
 */
class CallbackSession {
    private var next = 0L
    private var active: Long? = null

    @Synchronized fun start(): Long = (++next).also { active = it }

    @Synchronized fun cancel() { active = null }

    @Synchronized fun isCurrent(token: Long): Boolean = active == token

    @Synchronized fun accept(token: Long): Boolean {
        if (active != token) return false
        active = null
        return true
    }
}
