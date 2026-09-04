package dev.inkling.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import dev.inkling.InklingApp
import dev.inkling.MainActivity
import dev.inkling.core.Budget
import dev.inkling.core.Rule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

/**
 * Watches which app is in front. Blocks, logs, caps, warns.
 * Reads no window content: the service XML sets canRetrieveWindowContent=false.
 */
class KidsModeService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastPkg: String? = null
    private val warnedFor = mutableSetOf<String>()
    private var warnedDay: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == lastPkg) return
        lastPkg = pkg
        scope.launch { handle(pkg) }
    }

    private suspend fun handle(pkg: String) {
        val repo = InklingApp.instance.repo
        val child = repo.ensureChild()
        val s = repo.settings(child.id)
        val rules = repo.rules(child.id).map { Rule(it.packageName, it.enabled, it.dailyCapMinutes) }
        val now = System.currentTimeMillis()
        val zone = TimeZone.getDefault().getOffset(now).toLong()
        val dayStart = Budget.startOfDay(now, zone)
        if (dayStart != warnedDay) { warnedFor.clear(); warnedDay = dayStart }
        val cal = Calendar.getInstance()
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        // Log first, so the span that just ended is counted.
        val tracked = rules.any { it.packageName == pkg && it.enabled }
        if (tracked) repo.openSpan(child.id, pkg, now) else repo.closeOpenSpan(now)

        val spans = repo.spansToday(child.id, dayStart)
        val snap = Snapshot(s.kidsModeOn, rules, s.deviceCeilingMinutes, s.warningMinutes, s.quietStartMinute, s.quietEndMinute)
        val (action, _) = ServiceState.onForeground(pkg, packageName, snap, spans, now, zone, minuteOfDay, warnedFor)
        when (action) {
            is Action.SendHome -> withContext(Dispatchers.Main) { sendHome() }
            is Action.Warn -> { warnedFor += pkg; withContext(Dispatchers.Main) { showWarning(action.minutesLeft) } }
            Action.None -> if (tracked) scheduleCapCheck(pkg)
        }
    }

    /** Re-evaluates once a minute while a tracked app stays in front, so caps fire mid-session. */
    private fun scheduleCapCheck(pkg: String) {
        scope.launch {
            delay(60_000)
            if (lastPkg == pkg) { lastPkg = null; handle(pkg) }
        }
    }

    private fun sendHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun showWarning(minutesLeft: Int) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = if (minutesLeft == 1) "1 minute left" else "$minutesLeft minutes left"
            textSize = 28f
            setPadding(48, 32, 48, 32)
            setBackgroundColor(0xFF17160F.toInt())
            setTextColor(0xFFEDEBE6.toInt())
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.OPAQUE,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 80 }
        wm.addView(view, lp)
        scope.launch { delay(3_000); withContext(Dispatchers.Main) { runCatching { wm.removeView(view) } } }
    }

    override fun onInterrupt() {}
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
