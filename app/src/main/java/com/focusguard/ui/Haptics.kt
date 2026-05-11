package com.focusguard.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.focusguard.state.EarnedItStore

class Haptics(private val view: View) {
    /** Light feedback for buttons, toggles, taps. */
    fun tap() = perform(HapticFeedbackConstants.VIRTUAL_KEY)

    /** Sharp tick for slider step changes and discrete value snaps. */
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    /** Stronger feedback for selection of an item (cards, list rows). */
    fun select() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_END
        else HapticFeedbackConstants.VIRTUAL_KEY
    )

    /** Confirmation feedback for primary actions (start session, finish onboarding). */
    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY
    )

    private fun perform(constant: Int) {
        if (!EarnedItStore.state.value.settings.hapticsEnabled) return
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
