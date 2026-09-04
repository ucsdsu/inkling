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
    /** The only window of ours that means the child is actually looking at Inkling. */
    const val MAIN_ACTIVITY = "dev.inkling.MainActivity"

    /** How often the service re-evaluates the app that is already in front. */
    const val TICK_MS = 30_000L

    /**
     * True when a window event really is the foreground app changing. The warning overlay is a
     * window in our own package, so posting it raises an event for [self] while the tracked app is
     * still in front; acting on that would close the running span and point lastPkg at us. The
     * overlay reports its widget class, so only [MAIN_ACTIVITY] counts as us coming to the front.
     * Anyone else's window always counts.
     */
    fun isForegroundChange(pkg: String, className: String?, self: String): Boolean =
        pkg != self || className == MAIN_ACTIVITY

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

    /**
     * What the ticker does for the app that is already in front. Same decision as a window event,
     * so a cap reached mid-session fires without the child touching anything. The caller does not
     * open a span for it: the span is already open.
     */
    fun onTick(
        pkg: String, self: String, snap: Snapshot, spans: List<Span>,
        now: Long, zone: Long, minuteOfDay: Int, warnedFor: Set<String>,
    ): Pair<Action, Verdict> = onForeground(pkg, self, snap, spans, now, zone, minuteOfDay, warnedFor)
}
