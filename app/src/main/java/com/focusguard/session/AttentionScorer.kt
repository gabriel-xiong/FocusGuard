package com.focusguard.session

/**
 * Converts one smoothed ML signal into the session score and a human-readable reason.
 *
 * MainActivity already averages raw frame-level predictions into one signal per second. This class
 * is intentionally small and deterministic so the focused-zone thresholds can be validated against
 * recorded data without rerunning the camera pipeline.
 */
class AttentionScorer(
    private val config: AttentionScoringConfig = AttentionScoringConfig()
) {
    fun score(
        input: AttentionInput,
        currentScore: Float,
    ): AttentionScoreResult {
        val reason = distractionReasonFor(input)
        val nextScore = if (reason == DistractionReason.None) {
            currentScore + config.recoveryRate
        } else {
            currentScore - config.decayRate
        }.coerceIn(config.minScore, config.maxScore)

        return AttentionScoreResult(
            attentionScore = nextScore,
            isFocused = reason == DistractionReason.None && nextScore >= config.focusedThreshold,
            distractionReason = reason
        )
    }

    fun distractionReasonFor(input: AttentionInput): DistractionReason {
        // Missing face is handled first because downstream yaw/pitch/eye values are meaningless
        // when the face detector did not produce a confident crop.
        if (!input.faceDetected) return DistractionReason.FaceMissing

        // Low-confidence frames are separated from "looking away" so the UI/debug logs can
        // distinguish model uncertainty from an intentional user movement.
        if (input.faceConfidence < config.minFaceConfidence || input.eyeConfidence < config.minEyeConfidence) {
            return DistractionReason.LowConfidence
        }

        // Tuned from labeled captures after calibration. The range is asymmetric because real
        // focused study movement was asymmetric: downward reading posture and slight leftward
        // glances were common in focused frames, while equivalent movement in the other direction
        // more often meant distraction.
        if (input.yaw !in config.minYaw..config.maxYaw ||
            input.pitch !in config.minPitch..config.maxPitch
        ) {
            return DistractionReason.LookingAway
        }

        // Eye aspect ratio is width-normalized eyelid openness. Values below this threshold are
        // treated as eyes closed even if the head pose still points at the study material.
        if (input.eyeAspectRatio <= config.minEyeAspectRatio) {
            return DistractionReason.EyesClosed
        }
        return DistractionReason.None
    }

    companion object {
        const val MIN_SCORE = 0f
        const val MAX_SCORE = 1f
    }
}

data class AttentionScoringConfig(
    val minScore: Float = AttentionScorer.MIN_SCORE,
    val maxScore: Float = AttentionScorer.MAX_SCORE,
    val focusedThreshold: Float = 0.65f,
    val recoveryRate: Float = 0.075f,
    val decayRate: Float = 0.075f,
    val minYaw: Float = -25f,
    val maxYaw: Float = 20f,
    val minPitch: Float = -15f,
    val maxPitch: Float = 18f,
    val minEyeAspectRatio: Float = 0.15f,
    val minFaceConfidence: Float = 0.5f,
    val minEyeConfidence: Float = 0.5f
)

data class AttentionScoreResult(
    val attentionScore: Float,
    val isFocused: Boolean,
    val distractionReason: DistractionReason
)
