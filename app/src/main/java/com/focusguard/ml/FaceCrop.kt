package com.focusguard.ml

import android.graphics.Bitmap
import android.graphics.RectF

data class FaceCrop(
    val bitmap: Bitmap,
    val confidence: Float,
    val bounds: RectF? = null
)

sealed class FaceDetectionResult {
    data class Detected(val faceCrop: FaceCrop) : FaceDetectionResult()
    object NoFace : FaceDetectionResult()
}
