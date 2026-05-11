package com.focusguard.session

import android.content.Context
import android.content.SharedPreferences

class BlacklistStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getBlacklistedApps(): List<String> {
        return prefs.getStringSet(KEY_BLACKLISTED_PACKAGES, emptySet())
            .orEmpty()
            .filter { it.isNotBlank() }
            .sorted()
    }

    fun replaceBlacklistedApps(packageNames: Collection<String>): List<String> {
        val cleaned = packageNames.cleanedPackageSet()
        prefs.edit().putStringSet(KEY_BLACKLISTED_PACKAGES, cleaned).apply()
        return cleaned.sorted()
    }

    fun addBlacklistedApp(packageName: String): List<String> {
        return replaceBlacklistedApps(getBlacklistedApps() + packageName)
    }

    fun removeBlacklistedApp(packageName: String): List<String> {
        return replaceBlacklistedApps(getBlacklistedApps().filterNot { it == packageName })
    }

    fun isPackageBlocked(packageName: String): Boolean {
        return packageName in getBlacklistedApps()
    }

    private fun Collection<String>.cleanedPackageSet(): Set<String> {
        return map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    companion object {
        private const val PREFS_NAME = "focus_guard_blacklist"
        private const val KEY_BLACKLISTED_PACKAGES = "blacklisted_packages"
    }
}
