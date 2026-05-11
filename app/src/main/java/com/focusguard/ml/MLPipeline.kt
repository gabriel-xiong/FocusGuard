package com.focusguard.ml

import android.content.Context
import android.graphics.Bitmap
import com.focusguard.instrumentation.BenchmarkRegistry

/*
 * FocusGuard instrumentation map:
 * - preprocess is measured in MainActivity around ImageProxy.toBitmap(), rotation, and mirroring.
 * - end_to_end wraps MLPipeline.processFrame() for the full per-frame model chain.
 * - face_detect_inference wraps only face_detector compiledModel.run().
 * - face_detect_postprocess wraps face detector output reads, best-anchor decode, bbox creation, and face crop.
 * - landmark_preprocess wraps face crop resize/normalization/input-buffer packing for face_landmark_detector.
 * - landmark_inference wraps only landmark compiledModel.run().
 * - landmark_postprocess wraps landmark output reads and eye landmark/eye-region extraction.
 * - eyegaze_preprocess wraps eye crop resize, grayscale conversion, normalization, and input-buffer packing.
 * - eyegaze_inference wraps only eyegaze compiledModel.run().
 * - eyegaze_postprocess wraps gaze output read and radians-to-degrees conversion.
 *
 * Recording Mode is opt-in from the debug controls on SessionScreen. MainActivity owns the already
 * rotated/mirrored bitmap that is fed into this pipeline, asks QualityCaptureManager whether each
 * frame should be captured, and enqueues only those sampled bitmap copies plus this pipeline's latest
 * QualityFrameMetadata. JPEG encoding, timestamp stamping, sidecar JSON, and disk writes run on the
 * recorder's background thread and never on the inference path.
 */

/**
 * High-level ML coordinator used by MainActivity.
 *
 * The camera layer gives this class an already rotated and mirrored Bitmap. MLPipeline keeps the
 * model-specific details hidden from the rest of the app and returns a single AttentionSignal that
 * the session layer can score. It also stores the latest rich metadata used by Recording Mode.
 */
class MLPipeline(context: Context) {
    private val faceDetector = FaceDetector(context)
    private val headPoseEstimator = HeadPoseEstimator(context)

    var onAttentionSignal: ((AttentionSignal) -> Unit)? = null
    @Volatile var latestQualityMetadata: QualityFrameMetadata? = null
        private set
    var isRunning: Boolean = false
        private set

    fun start() {
        isRunning = true
    }

    fun stop() {
        isRunning = false
    }

    suspend fun processFrame(frameBitmap: Bitmap): AttentionSignal {
        val signal = BenchmarkRegistry.trace(BenchmarkRegistry.endToEnd, "end_to_end") {
            when (val result = faceDetector.detect(frameBitmap)) {
                is FaceDetectionResult.Detected -> {
                    try {
                        // The face crop owns a Bitmap allocation. Keep all downstream estimation
                        // inside try/finally so each processed frame releases its crop promptly.
                        val pose = headPoseEstimator.estimate(result.faceCrop)
                        val faceDetected = result.faceCrop.confidence >= FACE_CONFIDENCE_THRESHOLD

                        AttentionSignal(
                            faceDetected = faceDetected,
                            yaw = pose.yaw,
                            pitch = pose.pitch,
                            roll = pose.roll,
                            eyeAspectRatio = pose.eyeAspectRatio,
                            faceConfidence = result.faceCrop.confidence,
                            eyeConfidence = pose.landmarkScore ?: 0f
                        ).also {
                            latestQualityMetadata = QualityFrameMetadata(
                                faceDetected = faceDetected,
                                faceBBox = result.faceCrop.bounds,
                                faceConfidence = result.faceCrop.confidence,
                                landmarkScore = pose.landmarkScore,
                                eyeLandmarks = pose.eyeLandmarks,
                                rawPitchDeg = pose.pitch,
                                rawYawDeg = pose.yaw
                            )
                        }
                    } finally {
                        result.faceCrop.bitmap.recycle()
                    }
                }

                FaceDetectionResult.NoFace -> AttentionSignal(faceDetected = false).also {
                    // Quality capture still needs a metadata record for no-face frames so offline
                    // labeling can distinguish detector misses from later pose/gaze failures.
                    latestQualityMetadata = QualityFrameMetadata(faceDetected = false)
                }
            }
        }

        onAttentionSignal?.invoke(signal)
        return signal
    }

    fun submitAttentionSignal(signal: AttentionSignal) {
        onAttentionSignal?.invoke(signal)
    }

    companion object {
        const val FACE_CONFIDENCE_THRESHOLD = 0.7f
    }
}

data class QualityFrameMetadata(
    val faceDetected: Boolean,
    val faceBBox: android.graphics.RectF? = null,
    val faceConfidence: Float? = null,
    val landmarkScore: Float? = null,
    val eyeLandmarks: EyeLandmarkMetadata? = null,
    val rawPitchDeg: Float? = null,
    val rawYawDeg: Float? = null
)

data class EyeLandmarkMetadata(
    val leftInner33: Pair<Float, Float>,
    val leftOuter133: Pair<Float, Float>,
    val rightInner362: Pair<Float, Float>,
    val rightOuter263: Pair<Float, Float>
)
