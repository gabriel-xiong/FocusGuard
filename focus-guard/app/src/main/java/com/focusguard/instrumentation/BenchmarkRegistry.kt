package com.focusguard.instrumentation

import android.os.Trace

object BenchmarkRegistry {
    val preprocess = InferenceBenchmark("preprocess")
    val faceDetectInference = InferenceBenchmark("face_detect_inference")
    val faceDetectPostprocess = InferenceBenchmark("face_detect_postprocess")
    val landmarkPreprocess = InferenceBenchmark("landmark_preprocess")
    val landmarkInference = InferenceBenchmark("landmark_inference")
    val landmarkPostprocess = InferenceBenchmark("landmark_postprocess")
    val eyegazePreprocess = InferenceBenchmark("eyegaze_preprocess")
    val eyegazeInference = InferenceBenchmark("eyegaze_inference")
    val eyegazePostprocess = InferenceBenchmark("eyegaze_postprocess")
    val endToEnd = InferenceBenchmark("end_to_end")

    val all: List<InferenceBenchmark> = listOf(
        preprocess,
        faceDetectInference,
        faceDetectPostprocess,
        landmarkPreprocess,
        landmarkInference,
        landmarkPostprocess,
        eyegazePreprocess,
        eyegazeInference,
        eyegazePostprocess,
        endToEnd
    )

    inline fun <T> trace(benchmark: InferenceBenchmark, section: String, block: () -> T): T {
        Trace.beginSection(section)
        benchmark.start()
        return try {
            block()
        } finally {
            benchmark.stop()
            Trace.endSection()
        }
    }
}

