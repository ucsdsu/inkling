package dev.inkling.core

enum class Verdict { ALLOW, BLOCK_NOT_ALLOWED, BLOCK_CAPPED, BLOCK_CEILING, BLOCK_QUIET }

object BlockDecision {
    /** Packages that must never be bounced: the shell, keyboards, permission dialogs. */
    val SYSTEM_ALLOWLIST: Set<String> = setOf(
        "android",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.inputmethod.latin",
        "com.android.inputmethod.latin",
        // Boox system overlays. This is a guess from the SDK package name: capture the real Onyx
        // system UI packages on the device with
        //   adb shell dumpsys window | grep mCurrentFocus
        // while the status bar, navigation ball, and notification shade are up, and add them here.
        "com.onyx.android.sdk",
    )

    fun decide(
        packageName: String,
        selfPackage: String,
        rules: List<Rule>,
        kidsModeOn: Boolean,
        usedMinutes: Int,
        usedAllMinutes: Int,
        ceilingMinutes: Int,
        quiet: Boolean,
        readPackage: String,
    ): Verdict {
        if (!kidsModeOn) return Verdict.ALLOW
        if (packageName == selfPackage || packageName == readPackage) return Verdict.ALLOW
        if (packageName in SYSTEM_ALLOWLIST) return Verdict.ALLOW
        val rule = rules.firstOrNull { it.packageName == packageName && it.enabled }
            ?: return Verdict.BLOCK_NOT_ALLOWED
        if (quiet) return Verdict.BLOCK_QUIET
        if (rule.dailyCapMinutes > 0 && usedMinutes >= rule.dailyCapMinutes) return Verdict.BLOCK_CAPPED
        if (ceilingMinutes > 0 && usedAllMinutes >= ceilingMinutes) return Verdict.BLOCK_CEILING
        return Verdict.ALLOW
    }
}
