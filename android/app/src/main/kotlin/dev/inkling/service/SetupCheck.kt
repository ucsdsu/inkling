package dev.inkling.service

import android.content.Context
import android.content.Intent
import android.provider.Settings

object SetupCheck {
    const val NOT_HOME = "Inkling is not the Home app"
    const val SERVICE_OFF = "Accessibility service is off"

    fun problems(context: Context): List<String> {
        val out = mutableListOf<String>()
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val home = context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
        if (home != context.packageName) out += NOT_HOME
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        if (!enabled.contains("${context.packageName}/")) out += SERVICE_OFF
        return out
    }

    /** The settings page that fixes [problem]. */
    fun fixIntent(problem: String): Intent = when (problem) {
        NOT_HOME -> Intent(Settings.ACTION_HOME_SETTINGS)
        else -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** The stock launcher, for "Open Boox home". Falls back to the first non-Inkling HOME app. */
    fun stockHomeIntent(context: Context): Intent? {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val other = context.packageManager.queryIntentActivities(homeIntent, 0)
            .firstOrNull { it.activityInfo.packageName != context.packageName } ?: return null
        return Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setClassName(other.activityInfo.packageName, other.activityInfo.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
