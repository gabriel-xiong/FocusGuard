package com.focusguard.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.focusguard.instrumentation.BenchmarkRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp

class FaceDetector(private val context: Context) {

    private val faceModel by lazy {
        FallbackCompiledModel(context, "face_detector.tflite", TAG)
    }

    // Pre-calculated MediaPipe anchors for the 256x256 face detector. Keeping this local avoids
    // loading extra metadata files and makes the box decode deterministic across devices.
    private val anchors: List<Anchor> = generateAnchors()

    suspend fun detect(frameBitmap: Bitmap): FaceDetectionResult = withContext(Dispatchers.Default) {
        val resizedBitmap = Bitmap.createScaledBitmap(frameBitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        val floatValues = FloatArray(INPUT_SIZE * INPUT_SIZE * 3)
        for (i in intValues.indices) {
            val pixelValue = intValues[i]
            floatValues[i * 3 + 0] = ((pixelValue shr 16) and 0xFF) / 255.0f
            floatValues[i * 3 + 1] = ((pixelValue shr 8) and 0xFF) / 255.0f
            floatValues[i * 3 + 2] = (pixelValue and 0xFF) / 255.0f
        }
        
        val outputBuffers = faceModel.run(
            BenchmarkRegistry.faceDetectInference,
            "face_detect_inference"
        ) { inputBuffers ->
            inputBuffers[0].writeFloat(floatValues)
        }

        if (resizedBitmap != frameBitmap) resizedBitmap.recycle()

        BenchmarkRegistry.trace(BenchmarkRegistry.faceDetectPostprocess, "face_detect_postprocess") {
            // Model Outputs:
            // box_coords_1: [1, 512, 16] - outputBuffers[0]
            // box_coords_2: [1, 384, 16] - outputBuffers[1]
            // box_scores_1: [1, 512, 1]  - outputBuffers[2]
            // box_scores_2: [1, 384, 1]  - outputBuffers[3]
            val coords1 = outputBuffers[0].readFloat() // 512 * 16
            val coords2 = outputBuffers[1].readFloat() // 384 * 16
            val scores1 = outputBuffers[2].readFloat() // 512 * 1
            val scores2 = outputBuffers[3].readFloat() // 384 * 1

            var bestScore = -1f
            var bestIdx = -1
            var useScale1 = true

            for (i in 0 until 512) {
                val score = sigmoid(scores1[i])
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = i
                    useScale1 = true
                }
            }
            for (i in 0 until 384) {
                val score = sigmoid(scores2[i])
                if (score > bestScore) {
                    bestScore = score
                    bestIdx = i
                    useScale1 = false
                }
            }

            if (bestScore >= CONFIDENCE_THRESHOLD) {
                val coords = if (useScale1) coords1 else coords2
                val anchorIdx = if (useScale1) bestIdx else 512 + bestIdx
                val anchor = anchors[anchorIdx]
                val offset = bestIdx * 16

                // MediaPipe decoding: offsets are in pixels relative to INPUT_SIZE
                val cx = (coords[offset] / INPUT_SIZE) + anchor.centerX
                val cy = (coords[offset + 1] / INPUT_SIZE) + anchor.centerY
                val w = coords[offset + 2] / INPUT_SIZE
                val h = coords[offset + 3] / INPUT_SIZE

                val xmin = (cx - w / 2f).coerceIn(0f, 1f)
                val ymin = (cy - h / 2f).coerceIn(0f, 1f)
                val xmax = (cx + w / 2f).coerceIn(0f, 1f)
                val ymax = (cy + h / 2f).coerceIn(0f, 1f)

                val rect = RectF(
                    xmin * frameBitmap.width,
                    ymin * frameBitmap.height,
                    xmax * frameBitmap.width,
                    ymax * frameBitmap.height
                )

                val faceCropBitmap = cropFace(frameBitmap, rect)
                FaceDetectionResult.Detected(FaceCrop(faceCropBitmap, bestScore, rect))
            } else {
                FaceDetectionResult.NoFace
            }
        }
    }

    private fun sigmoid(x: Float): Float = (1.0f / (1.0f + exp(-x.toDouble()))).toFloat()

    private fun generateAnchors(): List<Anchor> {
        val anchors = mutableListOf<Anchor>()
        
        // Layer 1: 16x16 grid, 2 anchors per cell = 512 anchors
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val cx = (x + 0.5f) / 16f
                val cy = (y + 0.5f) / 16f
                repeat(2) { anchors.add(Anchor(cx, cy, 1.0f, 1.0f)) }
            }
        }
        
        // Layer 2: 8x8 grid, 6 anchors per cell = 384 anchors
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val cx = (x + 0.5f) / 8f
                val cy = (y + 0.5f) / 8f
                repeat(6) { anchors.add(Anchor(cx, cy, 1.0f, 1.0f)) }
            }
        }
        
        return anchors // 512 + 384 = 896 total
    }

    private fun cropFace(bitmap: Bitmap, rect: RectF): Bitmap {
        val left = rect.left.toInt().coerceIn(0, bitmap.width - 1)
        val top = rect.top.toInt().coerceIn(0, bitmap.height - 1)
        val width = rect.width().toInt().coerceAtMost(bitmap.width - left)
        val height = rect.height().toInt().coerceAtMost(bitmap.height - top)
        
        return if (width > 0 && height > 0) {
            Bitmap.createBitmap(bitmap, left, top, width, height)
        } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    data class Anchor(val centerX: Float, val centerY: Float, val width: Float, val height: Float)

    companion object {
        private const val TAG = "FaceDetector"
        private const val INPUT_SIZE = 256
        private const val CONFIDENCE_THRESHOLD = 0.7f
    }
}
