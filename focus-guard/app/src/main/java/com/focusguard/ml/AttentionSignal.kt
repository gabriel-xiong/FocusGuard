package com.focusguard.ml

data class AttentionSignal(
    val faceDetected: Boolean,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val eyeAspectRatio: Float = 0f,
    val faceConfidence: Float = 0f,
    val eyeConfidence: Float = 0f
)
