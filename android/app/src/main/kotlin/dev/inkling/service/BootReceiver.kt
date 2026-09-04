package dev.inkling.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.inkling.MainActivity

/** After a reboot, bring Inkling up so the setup banner is visible if anything got reset. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
