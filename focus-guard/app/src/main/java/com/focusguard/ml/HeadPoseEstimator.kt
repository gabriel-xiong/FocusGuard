package com.focusguard.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import com.focusguard.instrumentation.BenchmarkRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot

data class PoseResult(
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val eyeAspectRatio: Float = 0f,
    val landmarkScore: Float? = null,
    val eyeLandmarks: EyeLandmarkMetadata? = null
)

class HeadPoseEstimator(private val context: Context) {

    // Landmark Detector: [1, 192, 192, 3] float32 -> [1], [1, 468, 3]
    private val landmarkModel by lazy {
        FallbackCompiledModel(context, "face_landmark_detector.tflite", TAG)
    }

    // Eye Gaze Estimator: [1, 96, 160] float32 (grayscale) -> [1, 2] (pitch, yaw)
    private val gazeModel by lazy {
        FallbackCompiledModel(context, "eyegaze.tflite", TAG)
    }

    suspend fun estimate(faceCrop: FaceCrop): PoseResult = withContext(Dispatchers.Default) {
        val faceBitmap = faceCrop.bitmap

        // --- Step 1: Face Landmark Detection ---
        val landmarkInput = BenchmarkRegistry.trace(BenchmarkRegistry.landmarkPreprocess, "landmark_preprocess") {
            val bitmap = Bitmap.createScaledBitmap(faceBitmap, 192, 192, true)
            val pixels = IntArray(192 * 192)
            bitmap.getPixels(pixels, 0, 192, 0, 0, 192, 192)
            val landmarkFloats = FloatArray(192 * 192 * 3)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                landmarkFloats[i * 3 + 0] = ((pixel shr 16) and 0xFF) / 255f
                landmarkFloats[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255f
                landmarkFloats[i * 3 + 2] = (pixel and 0xFF) / 255f
            }
            LandmarkPreprocessResult(bitmap, landmarkFloats)
        }

        val landmarkOutputBuffers = landmarkModel.run(
            BenchmarkRegistry.landmarkInference,
            "landmark_inference"
        ) { inputBuffers ->
            inputBuffers[0].writeFloat(landmarkInput.floats)
        }

        val landmarkPostprocess = BenchmarkRegistry.trace(BenchmarkRegistry.landmarkPostprocess, "landmark_postprocess") {
            val score = landmarkOutputBuffers[0].readFloat().firstOrNull()
            // Output 2 is landmarks [1, 468, 3]
            val landmarks = landmarkOutputBuffers[1].readFloat()
            val normalizedLandmarks = normalizeLandmarks(landmarks)

            // Landmark indices: 33, 133 (left eye); 362, 263 (right eye)
            // Each landmark has 3 floats (x, y, z)
            val eyeIndices = intArrayOf(33, 133, 159, 145, 362, 263, 386, 374)
            var minX = 1f
            var maxX = 0f
            var minY = 1f
            var maxY = 0f

            for (idx in eyeIndices) {
                val x = normalizedLandmarks[idx * 3]
                val y = normalizedLandmarks[idx * 3 + 1]
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }

            val width = maxX - minX
            val height = maxY - minY
            val paddingX = width * 0.2f
            val paddingY = height * 0.2f

            val eyeRect = RectF(
                (minX - paddingX).coerceIn(0f, 1f) * faceBitmap.width,
                (minY - paddingY).coerceIn(0f, 1f) * faceBitmap.height,
                (maxX + paddingX).coerceIn(0f, 1f) * faceBitmap.width,
                (maxY + paddingY).coerceIn(0f, 1f) * faceBitmap.height
            )
            LandmarkPostprocessResult(
                eyeRect = eyeRect,
                score = score,
                eyeAspectRatio = estimateEyeAspectRatio(normalizedLandmarks),
                eyeLandmarks = EyeLandmarkMetadata(
                    leftInner33 = landmarkPoint(normalizedLandmarks, 33),
                    leftOuter133 = landmarkPoint(normalizedLandmarks, 133),
                    rightInner362 = landmarkPoint(normalizedLandmarks, 362),
                    rightOuter263 = landmarkPoint(normalizedLandmarks, 263)
                )
            )
        }

        val eyeCrop = cropBitmap(faceBitmap, landmarkPostprocess.eyeRect)
        if (eyeCrop.width < 10 || eyeCrop.height < 10) {
            if (eyeCrop != faceBitmap) eyeCrop.recycle()
            if (landmarkInput.bitmap != faceBitmap) landmarkInput.bitmap.recycle()
            return@withContext PoseResult(
                yaw = 0f,
                pitch = 0f,
                roll = 0f,
                eyeAspectRatio = landmarkPostprocess.eyeAspectRatio,
                landmarkScore = landmarkPostprocess.score,
                eyeLandmarks = landmarkPostprocess.eyeLandmarks
            )
        }
        
        val eyeGazeInputFloats = BenchmarkRegistry.trace(BenchmarkRegistry.eyegazePreprocess, "eyegaze_preprocess") {
            prepareGazeInput(eyeCrop) // 96x160 grayscale
        }

        // --- Step 3: Eye Gaze Estimation ---
        val gazeOutputBuffers = gazeModel.run(
            BenchmarkRegistry.eyegazeInference,
            "eyegaze_inference"
        ) { inputBuffers ->
            inputBuffers[0].writeFloat(eyeGazeInputFloats)
        }
        
        val pose = BenchmarkRegistry.trace(BenchmarkRegistry.eyegazePostprocess, "eyegaze_postprocess") {
            // The eyegaze model has 3 outputs: [heatmaps, landmarks, gaze_pitchyaw]
            // gaze_pitchyaw is likely at index 2.
            val gazePitchYaw = gazeOutputBuffers[2].readFloat() // [pitch, yaw] in radians
            val pitchDeg = Math.toDegrees(gazePitchYaw[0].toDouble()).toFloat()
            val yawDeg = Math.toDegrees(gazePitchYaw[1].toDouble()).toFloat()
            PoseResult(
                yaw = yawDeg,
                pitch = pitchDeg,
                roll = 0f,
                eyeAspectRatio = landmarkPostprocess.eyeAspectRatio,
                landmarkScore = landmarkPostprocess.score,
                eyeLandmarks = landmarkPostprocess.eyeLandmarks
            )
        }

        if (landmarkInput.bitmap != faceBitmap) landmarkInput.bitmap.recycle()
        if (eyeCrop != faceBitmap) eyeCrop.recycle()

        pose
    }

    private fun cropBitmap(source: Bitmap, rect: RectF): Bitmap {
        val left = rect.left.toInt().coerceIn(0, source.width - 1)
        val top = rect.top.toInt().coerceIn(0, source.height - 1)
        val width = rect.width().toInt().coerceAtMost(source.width - left)
        val height = rect.height().toInt().coerceAtMost(source.height - top)
        
        return if (width > 0 && height > 0) {
            Bitmap.createBitmap(source, left, top, width, height)
        } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    private fun prepareGazeInput(bitmap: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(bitmap, 160, 96, true)
        val grayscale = Bitmap.createBitmap(160, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(resized, 0f, 0f, paint)

        val pixels = IntArray(160 * 96)
        grayscale.getPixels(pixels, 0, 160, 0, 0, 160, 96)
        val floatValues = FloatArray(160 * 96)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            // Since it's grayscale, R=G=B. Just take one channel.
            val r = (pixel shr 16) and 0xFF
            floatValues[i] = r / 255f
        }
        
        resized.recycle()
        grayscale.recycle()
        return floatValues
    }

    private fun landmarkPoint(landmarks: FloatArray, index: Int): Pair<Float, Float> {
        return landmarks[index * 3] to landmarks[index * 3 + 1]
    }

    private fun normalizeLandmarks(landmarks: FloatArray): FloatArray {
        var maxAbsCoordinate = 0f
        var i = 0
        while (i + 1 < landmarks.size) {
            maxAbsCoordinate = kotlin.math.max(maxAbsCoordinate, abs(landmarks[i]))
            maxAbsCoordinate = kotlin.math.max(maxAbsCoordinate, abs(landmarks[i + 1]))
            i += 3
        }

        if (maxAbsCoordinate <= 2f) return landmarks

        return FloatArray(landmarks.size) { i ->
            if (i % 3 == 2) landmarks[i] else landmarks[i] / LANDMARK_INPUT_SIZE
        }
    }

    private fun estimateEyeAspectRatio(landmarks: FloatArray): Float {
        val left = eyeAspectRatio(landmarks, outer = 33, inner = 133, upper = 159, lower = 145)
        val right = eyeAspectRatio(landmarks, outer = 362, inner = 263, upper = 386, lower = 374)
        return (left + right) / 2f
    }

    private fun eyeAspectRatio(
        landmarks: FloatArray,
        outer: Int,
        inner: Int,
        upper: Int,
        lower: Int
    ): Float {
        val width = distance(landmarks, outer, inner)
        if (width <= 0f) return 0f
        return distance(landmarks, upper, lower) / width
    }

    private fun distance(landmarks: FloatArray, a: Int, b: Int): Float {
        val ax = landmarks[a * 3]
        val ay = landmarks[a * 3 + 1]
        val bx = landmarks[b * 3]
        val by = landmarks[b * 3 + 1]
        return hypot((ax - bx).toDouble(), (ay - by).toDouble()).toFloat()
    }

    private data class LandmarkPostprocessResult(
        val eyeRect: RectF,
        val score: Float?,
        val eyeAspectRatio: Float,
        val eyeLandmarks: EyeLandmarkMetadata
    )

    private data class LandmarkPreprocessResult(
        val bitmap: Bitmap,
        val floats: FloatArray
    )

    private companion object {
        const val TAG = "HeadPoseEstimator"
        const val LANDMARK_INPUT_SIZE = 192f
    }
}
