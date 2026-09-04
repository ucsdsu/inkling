package dev.inkling.service

import dev.inkling.core.BlockDecision
import dev.inkling.core.Budget
import dev.inkling.core.QuietHours
import dev.inkling.core.Rule
import dev.inkling.core.Span
import dev.inkling.core.Verdict

data class Snapshot(val kidsModeOn: Boolean, val rules: List<Rule>, val ceiling: Int, val warning: Int, val quietStart: Int, val quietEnd: Int)

sealed class Action {
    data object None : Action()
    data object SendHome : Action()
    data class Warn(val minutesLeft: Int) : Action()
}

object ServiceState {
    /** Decides what the service does when [pkg] comes to the front. Pure, so it's testable. */
    fun onForeground(
        pkg: String, self: String, snap: Snapshot, spans: List<Span>,
        now: Long, zone: Long, minuteOfDay: Int, warnedFor: Set<String>,
    ): Pair<Action, Verdict> {
        val enabledPkgs = snap.rules.filter { it.enabled }.map { it.packageName }.toSet()
        val used = Budget.usedMinutesToday(spans, pkg, now, zone)
        val usedAll = Budget.usedMinutesTodayAll(spans, enabledPkgs, now, zone)
        val quiet = QuietHours.isQuiet(minuteOfDay, snap.quietStart, snap.quietEnd)
        val verdict = BlockDecision.decide(pkg, self, snap.rules, snap.kidsModeOn, used, usedAll, snap.ceiling, quiet, readPackage = self)
        if (verdict != Verdict.ALLOW) return Action.SendHome to verdict
        val rule = snap.rules.firstOrNull { it.packageName == pkg }
        if (rule != null && rule.dailyCapMinutes > 0 && pkg !in warnedFor) {
            val left = rule.dailyCapMinutes - used
            if (left in 1..snap.warning) return Action.Warn(left) to verdict
        }
        return Action.None to verdict
    }
}
