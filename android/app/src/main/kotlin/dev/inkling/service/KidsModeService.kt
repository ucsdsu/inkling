package dev.inkling.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

/**
 * Watches which app is in front. Blocks, logs, caps, warns.
 * Reads no window content: the service XML sets canRetrieveWindowContent=false.
 */
class KidsModeService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** Window events and the ticker both evaluate. One at a time, and lastPkg/warnedFor are only
     *  ever touched while this is held. */
    private val gate = Mutex()
    private var lastPkg: String? = null
    private val warnedFor = mutableSetOf<String>()
    private var warnedDay: Long = 0
    private var ticker: Job? = null

    /**
     * A span must not run all night. The screen going off ends the session; unlocking clears
     * lastPkg so the first window event after wake re-opens a span instead of being swallowed as
     * "same app as before".
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> scope.launch {
                    gate.withLock {
                        InklingApp.instance.repo.closeOpenSpan(System.currentTimeMillis())
                        lastPkg = null
                    }
                }
                Intent.ACTION_USER_PRESENT -> scope.launch { gate.withLock { lastPkg = null } }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // The service was unbound while an app was in front. That span is stale.
        scope.launch { InklingApp.instance.repo.closeStaleSpans(System.currentTimeMillis()) }
        registerReceiver(
            screenReceiver,
            IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) },
        )
        ticker = scope.launch {
            while (isActive) {
                delay(ServiceState.TICK_MS)
                gate.withLock { lastPkg?.let { evaluate(it, openSpan = false) } }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (!ServiceState.isForegroundChange(pkg, event.className?.toString(), packageName)) return
        scope.launch { onForeground(pkg) }
    }

    private suspend fun onForeground(pkg: String) = gate.withLock {
        if (pkg == lastPkg) return@withLock
        lastPkg = pkg
        evaluate(pkg, openSpan = true)
    }

    /**
     * The one evaluation path. Callers hold [gate]. [openSpan] is false for the ticker: the app
     * never left the front, so its span is already open and must keep running.
     */
    private suspend fun evaluate(pkg: String, openSpan: Boolean) {
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
        if (openSpan) { if (tracked) repo.openSpanIfChanged(child.id, pkg, now) else repo.closeOpenSpan(now) }

        val spans = repo.spansToday(child.id, dayStart)
        val snap = Snapshot(s.kidsModeOn, rules, s.deviceCeilingMinutes, s.warningMinutes, s.quietStartMinute, s.quietEndMinute)
        val (action, _) = if (openSpan) {
            ServiceState.onForeground(pkg, packageName, snap, spans, now, zone, minuteOfDay, warnedFor)
        } else {
            ServiceState.onTick(pkg, packageName, snap, spans, now, zone, minuteOfDay, warnedFor)
        }
        when (action) {
            is Action.SendHome -> withContext(Dispatchers.Main) { sendHome() }
            is Action.Warn -> {
                warnedFor += pkg
                withContext(Dispatchers.Main) { showWarning(action.minutesLeft) }
            }
            Action.None -> {}
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
    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
