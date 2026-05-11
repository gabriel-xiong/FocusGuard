package com.focusguard.instrumentation

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object BenchmarkReporter {
    const val LOGCAT_TAG = "BENCHMARK_REPORT"

    fun dumpReport(context: Context): File {
        val snapshot = SystemMetricsSampler.snapshot()
        val json = JSONObject()
            .put("device_model", Build.MODEL)
            .put("android_version", Build.VERSION.RELEASE)
            .put("timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC)))
            .put("duration_seconds", snapshot.durationSeconds.toLong())
            .put("frames_processed", snapshot.framesProcessed)
            .put("fps_actual_mean", snapshot.meanFps)
            .put("stages", stageStatsJson())
            .put(
                "memory",
                JSONObject()
                    .put("native_peak_mb", snapshot.nativePeakMb)
                    .put("java_peak_mb", snapshot.javaPeakMb)
            )
            .put(
                "thermal",
                JSONObject()
                    .put("peak_status", snapshot.peakThermalStatus)
                    .put("time_at_throttle_seconds", snapshot.timeAtThrottleSeconds)
            )

        val dir = File(context.getExternalFilesDir(null), "benchmarks").apply { mkdirs() }
        val filenameTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val file = File(dir, "baseline_$filenameTimestamp.json")
        file.writeText(json.toString(2))
        Log.i(LOGCAT_TAG, json.toString(2))
        return file
    }

    private fun stageStatsJson(): JSONObject {
        val stages = JSONObject()
        BenchmarkRegistry.all.forEach { benchmark ->
            val stats = benchmark.stats()
            val stage = JSONObject()
            if (stats != null) {
                stage
                    .put("p50_ms", stats.p50Ms)
                    .put("p95_ms", stats.p95Ms)
                    .put("p99_ms", stats.p99Ms)
                    .put("mean_ms", stats.meanMs)
                    .put("samples", stats.sampleCount)
            }
            stages.put(benchmark.tag, stage)
        }
        return stages
    }
}

