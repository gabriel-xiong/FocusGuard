package com.focusguard.session

import android.content.Context
import com.focusguard.ml.AttentionSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Person 2: Manages focus session lifecycle
object SessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scorer = AttentionScorer()
    private val _state = MutableStateFlow(SessionState())
    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()
    private var countdownJob: Job? = null
    private var blacklistStore: BlacklistStore? = null
    private var focusedElapsedSeconds = 0f
    private var distractedElapsedSeconds = 0f

    val stateFlow: StateFlow<SessionState> = _state.asStateFlow()
    val currentState: SessionState
        get() = _state.value

    fun initialize(context: Context) {
        if (blacklistStore != null) return
        blacklistStore = BlacklistStore(context)
        _state.update { it.copy(blacklistedApps = blacklistStore?.getBlacklistedApps().orEmpty()) }
    }

    fun startSession(
        durationSeconds: Int,
        blacklistedApps: List<String> = _state.value.blacklistedApps,
        rewardDurationSeconds: Int = DEFAULT_REWARD_SECONDS
    ) {
        countdownJob?.cancel()
        focusedElapsedSeconds = 0f
        distractedElapsedSeconds = 0f
        val savedBlacklist = persistBlacklist(blacklistedApps)

        _state.value = SessionState(
            phase = SessionPhase.FocusActive,
            totalFocusSeconds = durationSeconds.coerceAtLeast(0),
            remainingFocusSeconds = durationSeconds.coerceAtLeast(0),
            totalRewardSeconds = rewardDurationSeconds.coerceAtLeast(0),
            remainingRewardSeconds = 0,
            blacklistedApps = savedBlacklist,
            attentionScore = AttentionScorer.MAX_SCORE
        )

        countdownJob = scope.launch {
            while (_state.value.phase == SessionPhase.FocusActive ||
                _state.value.phase == SessionPhase.RewardActive ||
                _state.value.phase == SessionPhase.Paused
            ) {
                delay(ONE_SECOND_MS)
                advanceTimer()
            }
        }
    }

    fun stopSession() {
        countdownJob?.cancel()
        countdownJob = null
        focusedElapsedSeconds = 0f
        distractedElapsedSeconds = 0f
        _state.update {
            it.copy(
                phase = SessionPhase.Idle,
                remainingRewardSeconds = 0,
                blockedPackageName = null,
                distractionReason = DistractionReason.None
            )
        }
    }

    fun pauseSession() {
        if (_state.value.phase != SessionPhase.FocusActive) return
        _state.update { it.copy(phase = SessionPhase.Paused) }
    }

    fun resumeSession() {
        if (_state.value.phase != SessionPhase.Paused) return
        _state.update { it.copy(phase = SessionPhase.FocusActive) }
    }

    fun failSession(reason: DistractionReason = _state.value.distractionReason) {
        countdownJob?.cancel()
        countdownJob = null
        _state.update { it.copy(phase = SessionPhase.Failed, distractionReason = reason) }
    }

    fun completeSession() {
        countdownJob?.cancel()
        countdownJob = null
        _state.update {
            it.copy(
                phase = SessionPhase.Complete,
                remainingFocusSeconds = 0,
                remainingRewardSeconds = 0,
                blockedPackageName = null
            )
        }
    }

    fun resetSession() {
        countdownJob?.cancel()
        countdownJob = null
        focusedElapsedSeconds = 0f
        distractedElapsedSeconds = 0f
        val blacklist = _state.value.blacklistedApps
        _state.value = SessionState(blacklistedApps = blacklist)
    }

    fun updateBlacklistedApps(packageNames: List<String>) {
        _state.update { it.copy(blacklistedApps = persistBlacklist(packageNames)) }
    }

    fun addBlacklistedApp(packageName: String) {
        val updated = blacklistStore?.addBlacklistedApp(packageName)
            ?: (_state.value.blacklistedApps + packageName).cleanedPackages()
        _state.update { it.copy(blacklistedApps = updated) }
    }

    fun removeBlacklistedApp(packageName: String) {
        val updated = blacklistStore?.removeBlacklistedApp(packageName)
            ?: _state.value.blacklistedApps.filterNot { it == packageName }
        _state.update { it.copy(blacklistedApps = updated) }
    }

    fun setCalibrating(calibrating: Boolean) {
        _isCalibrating.value = calibrating
    }

    fun onAttentionSignal(signal: AttentionSignal, frameDeltaSeconds: Float = DEFAULT_FRAME_DELTA_SECONDS) {
        onAttentionInput(signal.toAttentionInput(), signal, frameDeltaSeconds)
    }

    fun onAttentionInput(
        input: AttentionInput,
        sourceSignal: AttentionSignal? = null,
        frameDeltaSeconds: Float = DEFAULT_FRAME_DELTA_SECONDS
    ) {
        val state = _state.value
        if (state.phase != SessionPhase.FocusActive) return

        val result = scorer.score(input, state.attentionScore)
        focusedElapsedSeconds = if (result.isFocused) {
            focusedElapsedSeconds + frameDeltaSeconds
        } else {
            0f
        }
        distractedElapsedSeconds = if (result.isFocused) {
            0f
        } else {
            distractedElapsedSeconds + frameDeltaSeconds
        }

        val focusedWholeSeconds = if (focusedElapsedSeconds >= 1f) {
            focusedElapsedSeconds -= 1f
            1
        } else {
            0
        }
        val distractedWholeSeconds = if (distractedElapsedSeconds >= 1f) {
            distractedElapsedSeconds -= 1f
            1
        } else {
            0
        }

        _state.update { current ->
            current.copy(
                remainingFocusSeconds = current.remainingFocusSeconds,
                focusedSeconds = current.focusedSeconds + focusedWholeSeconds,
                distractedSeconds = current.distractedSeconds + distractedWholeSeconds,
                attentionScore = result.attentionScore,
                distractionReason = result.distractionReason,
                lastSignal = sourceSignal,
                lastAttentionInput = input
            )
        }
    }

    fun onBlockedAppOpened(packageName: String) {
        _state.update { state ->
            state.copy(
                distractionReason = DistractionReason.BlockedAppOpened,
                blockedPackageName = packageName,
                recentBlockedAttempts = (
                    listOf(BlockedAppAttempt(packageName)) + state.recentBlockedAttempts
                ).take(MAX_RECENT_BLOCKED_ATTEMPTS)
            )
        }
    }

    fun clearBlockedApp() {
        _state.update { it.copy(blockedPackageName = null) }
    }

    fun isPackageBlocked(packageName: String): Boolean {
        return isBlockingEnabled() &&
            packageName != EMERGENCY_DIALER_PACKAGE &&
            packageName in _state.value.blacklistedApps
    }

    fun isBlockingEnabled(): Boolean {
        return _state.value.phase == SessionPhase.FocusActive
    }

    fun isRewardWindowActive(): Boolean {
        return _state.value.phase == SessionPhase.RewardActive
    }

    private fun advanceTimer() {
        _state.update { state ->
            when (state.phase) {
                SessionPhase.FocusActive -> {
                    if (_isCalibrating.value) return@update state // freeze timer during calibration
                    val isFocused = _state.value.distractionReason == DistractionReason.None
                    val nextRemaining = if (isFocused) {
                        (state.remainingFocusSeconds - 1).coerceAtLeast(0)
                    } else {
                        state.remainingFocusSeconds // timer paused, penalty handled by onAttentionInput
                    }
                    if (nextRemaining == 0) {
                        state.copy(
                            phase = SessionPhase.RewardActive,
                            remainingFocusSeconds = 0,
                            remainingRewardSeconds = state.totalRewardSeconds,
                            distractionReason = DistractionReason.None,
                            blockedPackageName = null
                        )
                    } else {
                        state.copy(remainingFocusSeconds = nextRemaining)
                    }
                }

                SessionPhase.RewardActive -> {
                    val nextRemaining = (state.remainingRewardSeconds - 1).coerceAtLeast(0)
                    if (nextRemaining == 0) {
                        state.copy(
                            phase = SessionPhase.Complete,
                            remainingRewardSeconds = 0,
                            blockedPackageName = null
                        )
                    } else {
                        state.copy(remainingRewardSeconds = nextRemaining)
                    }
                }

                else -> state
            }
        }
    }

    private fun persistBlacklist(packageNames: Collection<String>): List<String> {
        return blacklistStore?.replaceBlacklistedApps(packageNames) ?: packageNames.cleanedPackages()
    }

    private fun Collection<String>.cleanedPackages(): List<String> {
        return map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    private fun AttentionSignal.toAttentionInput(): AttentionInput {
        return AttentionInput(
            faceDetected = faceDetected,
            yaw = yaw,
            pitch = pitch,
            roll = roll,
            eyeAspectRatio = eyeAspectRatio,
            faceConfidence = faceConfidence,
            eyeConfidence = eyeConfidence
        )
    }

    private const val ONE_SECOND_MS = 1_000L
    private const val DEFAULT_FRAME_DELTA_SECONDS = 1f / 15f
    private const val DISTRACTION_TIME_PENALTY_SECONDS = 3
    private const val DEFAULT_REWARD_SECONDS = 5 * 60
    private const val MAX_EXTENSION_SECONDS = 10 * 60
    private const val MAX_RECENT_BLOCKED_ATTEMPTS = 10
    const val EMERGENCY_DIALER_PACKAGE = "com.android.dialer"
}
