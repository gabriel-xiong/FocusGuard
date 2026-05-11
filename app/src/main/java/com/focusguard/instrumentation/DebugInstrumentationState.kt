package com.focusguard.instrumentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugInstrumentationState {
    private val _recordingModeEnabled = MutableStateFlow(false)
    private val _stampFramesEnabled = MutableStateFlow(false)

    val recordingModeEnabled: StateFlow<Boolean> = _recordingModeEnabled.asStateFlow()
    val stampFramesEnabled: StateFlow<Boolean> = _stampFramesEnabled.asStateFlow()

    fun setRecordingModeEnabled(enabled: Boolean) {
        _recordingModeEnabled.value = enabled
    }

    fun setStampFramesEnabled(enabled: Boolean) {
        _stampFramesEnabled.value = enabled
    }
}

