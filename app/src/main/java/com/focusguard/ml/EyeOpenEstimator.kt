package com.focusguard.ml

data class EyeResult(val eyeAspectRatio: Float)

class EyeOpenEstimator {
    fun estimate(faceCrop: FaceCrop): EyeResult {
        return EyeResult(0.3f) // stub - eyes open
    }
}
