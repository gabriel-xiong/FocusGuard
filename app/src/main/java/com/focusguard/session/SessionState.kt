package com.focusguard.session

import com.focusguard.ml.AttentionSignal

// Person 2: Data classes representing session state
data class SessionState(
    val phase: SessionPhase = SessionPhase.Idle,
    val totalFocusSeconds: Int = 0,
    val remainingFocusSeconds: Int = 0,
    val totalRewardSeconds: Int = 0,
    val remainingRewardSeconds: Int = 0,
    val focusedSeconds: Int = 0,
    val distractedSeconds: Int = 0,
    val extensionSeconds: Int = 0,
    val attentionScore: Float = 1f,
    val blacklistedApps: List<String> = emptyList(),
    val distractionReason: DistractionReason = DistractionReason.None,
    val lastSignal: AttentionSignal? = null,
    val lastAttentionInput: AttentionInput? = null,
    val blockedPackageName: String? = null,
    val recentBlockedAttempts: List<BlockedAppAttempt> = emptyList()
) {
    val isActive: Boolean
        get() = phase == SessionPhase.FocusActive || phase == SessionPhase.Paused

    val isRewardActive: Boolean
        get() = phase == SessionPhase.RewardActive

    val initialDurationSeconds: Int
        get() = totalFocusSeconds

    val remainingSeconds: Int
        get() = remainingFocusSeconds

    val distractionCount: Int
        get() = distractedSeconds
}

enum class SessionPhase {
    Idle,
    FocusActive,
    RewardActive,
    Paused,
    Failed,
    Complete
}

enum class DistractionReason {
    None,
    FaceMissing,
    LookingAway,
    EyesClosed,
    BlockedAppOpened,
    LowConfidence
}

data class AttentionInput(
    val faceDetected: Boolean,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val eyeAspectRatio: Float = 0f,
    val faceConfidence: Float = 1f,
    val eyeConfidence: Float = 1f,
    val timestampMs: Long = System.currentTimeMillis()
)

data class BlockedAppAttempt(
    val packageName: String,
    val timestampMs: Long = System.currentTimeMillis()
)
