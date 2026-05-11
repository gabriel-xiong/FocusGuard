package com.focusguard.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object AccessibilityServiceStatus {
    fun isEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, FocusAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        for (service in splitter) {
            val enabledComponent = ComponentName.unflattenFromString(service)
            if (enabledComponent == expectedComponent) return true
        }
        return false
    }

    fun settingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
