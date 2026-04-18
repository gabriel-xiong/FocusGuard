package com.focusguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.focusguard.session.SessionManager

/**
 * Enforces the app blacklist while a focus session is active.
 *
 * Android does not let regular apps prevent another app from launching directly. The permitted
 * approach here is an AccessibilityService: we observe foreground-window changes, check the package
 * against SessionManager's blacklist, and relaunch EarnedIt when a blocked app appears.
 */
class FocusAccessibilityService : AccessibilityService() {
    private var lastBlockedPackage: String? = null
    private var lastBlockElapsedMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        SessionManager.initialize(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType !in MONITORED_EVENT_TYPES) return

        val foregroundPackage = event.packageName?.toString() ?: return
        // These guards keep the service from blocking the OS, itself, reward time, or repeated
        // events fired by the same package while Android is settling window focus.
        if (foregroundPackage.isBlank() || shouldIgnorePackage(foregroundPackage)) return
        if (SessionManager.isRewardWindowActive()) return
        if (!SessionManager.isPackageBlocked(foregroundPackage)) return
        if (isDuplicateBlock(foregroundPackage)) return

        SessionManager.onBlockedAppOpened(foregroundPackage)
        launchFocusGuard(foregroundPackage)
    }

    override fun onInterrupt() = Unit

    private fun shouldIgnorePackage(packageName: String): Boolean {
        return packageName == this.packageName ||
            packageName in IGNORED_PACKAGES
    }

    private fun isDuplicateBlock(packageName: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val isDuplicate = packageName == lastBlockedPackage &&
            now - lastBlockElapsedMs < BLOCK_DEBOUNCE_MS

        if (!isDuplicate) {
            lastBlockedPackage = packageName
            lastBlockElapsedMs = now
        }
        return isDuplicate
    }

    private fun launchFocusGuard(blockedPackageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        launchIntent.putExtra(EXTRA_BLOCKED_PACKAGE_NAME, blockedPackageName)
        startActivity(launchIntent)
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE_NAME = "com.focusguard.extra.BLOCKED_PACKAGE_NAME"
        private const val BLOCK_DEBOUNCE_MS = 1_500L

        private val MONITORED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED
        )

        private val IGNORED_PACKAGES = setOf(
            "android",
            "com.android.settings",
            "com.android.systemui",
            SessionManager.EMERGENCY_DIALER_PACKAGE
        )
    }
}
