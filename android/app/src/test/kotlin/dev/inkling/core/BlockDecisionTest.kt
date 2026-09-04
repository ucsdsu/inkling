package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockDecisionTest {
    private val self = "dev.inkling"
    private val rules = listOf(
        Rule("com.chess", enabled = true, dailyCapMinutes = 30),
        Rule("com.hangman", enabled = false, dailyCapMinutes = 15),
    )
    private fun decide(pkg: String, kids: Boolean = true, used: Int = 0, usedAll: Int = 0, ceiling: Int = 60, quiet: Boolean = false) =
        BlockDecision.decide(pkg, self, rules, kids, used, usedAll, ceiling, quiet, readPackage = self)

    @Test fun kidsModeOffAllowsEverything() { assertEquals(Verdict.ALLOW, decide("com.android.settings", kids = false)) }
    @Test fun selfIsAllowed() { assertEquals(Verdict.ALLOW, decide(self)) }
    @Test fun systemUiIsAllowed() { assertEquals(Verdict.ALLOW, decide("com.android.systemui")) }
    @Test fun enabledAppUnderCapIsAllowed() { assertEquals(Verdict.ALLOW, decide("com.chess", used = 29)) }
    @Test fun enabledAppAtCapIsBlocked() { assertEquals(Verdict.BLOCK_CAPPED, decide("com.chess", used = 30)) }
    @Test fun disabledAppIsBlocked() { assertEquals(Verdict.BLOCK_NOT_ALLOWED, decide("com.hangman")) }
    @Test fun unknownAppIsBlocked() { assertEquals(Verdict.BLOCK_NOT_ALLOWED, decide("com.android.settings")) }
    @Test fun ceilingBlocksEnabledApp() { assertEquals(Verdict.BLOCK_CEILING, decide("com.chess", used = 5, usedAll = 60)) }
    @Test fun ceilingZeroMeansNoCeiling() { assertEquals(Verdict.ALLOW, decide("com.chess", usedAll = 500, ceiling = 0)) }
    @Test fun quietBlocksEnabledApp() { assertEquals(Verdict.BLOCK_QUIET, decide("com.chess", quiet = true)) }
    @Test fun quietNeverBlocksRead() {
        val v = BlockDecision.decide(self, self, rules, true, 0, 999, 60, quiet = true, readPackage = self)
        assertEquals(Verdict.ALLOW, v)
    }
}
