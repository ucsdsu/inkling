package dev.inkling.apps

import android.content.Intent
import android.content.pm.PackageManager

object InstalledApps {
    /** Every app with a launcher icon, except Inkling itself. Sorted by label. */
    fun launchable(pm: PackageManager, selfPackage: String): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != selfPackage }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }
}
