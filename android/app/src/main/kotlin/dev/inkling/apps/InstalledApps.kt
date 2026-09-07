package dev.inkling.apps

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

object InstalledApps {
    /** Decode off the main thread; both app pickers use the same installed icon. */
    fun icon(pm: PackageManager, packageName: String): ImageBitmap? = try {
        pm.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

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
