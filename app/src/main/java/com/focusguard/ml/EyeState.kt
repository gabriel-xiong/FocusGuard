package com.focusguard.ml

data class EyeState(
    val eyeAspectRatio: Float,
    val isOpen: Boolean = eyeAspectRatio > EYE_CLOSED_THRESHOLD
) {
    companion object {
        const val EYE_CLOSED_THRESHOLD = 0.2f
    }
}
