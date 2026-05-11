package com.focusguard.instrumentation

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SystemMetricsSampler {
    private const val MAX_SAMPLES = 600
    private const val SAMPLE_PERIOD_MS = 1_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private val samples = ArrayDeque<SystemMetricSample>()
    private val frameTimesNs = ArrayDeque<Long>()
    private var samplerJob: Job? = null
    private var powerManager: PowerManager? = null
    private var startNs = 0L
    private var framesProcessed = 0L

    fun start(context: Context) {
        synchronized(lock) {
            powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (startNs == 0L) startNs = System.nanoTime()
            if (samplerJob != null) return
            samplerJob = scope.launch {
                while (true) {
                    sample()
                    delay(SAMPLE_PERIOD_MS)
                }
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            samplerJob?.cancel()
            samplerJob = null
        }
    }

    fun recordFrame() {
        val nowNs = System.nanoTime()
        synchronized(lock) {
            framesProcessed += 1
            frameTimesNs.addLast(nowNs)
            trimFramesLocked(nowNs - 10_000_000_000L)
        }
    }

    fun snapshot(): SystemMetricsSnapshot {
        synchronized(lock) {
            val copied = samples.toList()
            val durationSeconds = if (startNs == 0L) {
                0.0
            } else {
                (System.nanoTime() - startNs) / 1_000_000_000.0
            }
            val throttleSeconds = copied.count { it.thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE }
            return SystemMetricsSnapshot(
                samples = copied,
                durationSeconds = durationSeconds,
                framesProcessed = framesProcessed,
                nativePeakMb = copied.maxOfOrNull { it.nativeHeapMb } ?: 0.0,
                javaPeakMb = copied.maxOfOrNull { it.javaHeapMb } ?: 0.0,
                peakThermalStatus = copied.maxOfOrNull { it.thermalStatus } ?: 0,
                timeAtThrottleSeconds = throttleSeconds,
                meanFps = copied.map { it.fps10s }.filter { it > 0.0 }.averageOrZero()
            )
        }
    }

    fun reset() {
        synchronized(lock) {
            samples.clear()
            frameTimesNs.clear()
            framesProcessed = 0L
            startNs = System.nanoTime()
        }
    }

    private fun sample() {
        val runtime = Runtime.getRuntime()
        val javaUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
        val nativeMb = Debug.getNativeHeapAllocatedSize() / (1024.0 * 1024.0)
        val nowNs = System.nanoTime()
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager?.currentThermalStatus ?: 0
        } else {
            0
        }
        synchronized(lock) {
            trimFramesLocked(nowNs - 10_000_000_000L)
            val fps1s = frameTimesNs.count { it >= nowNs - 1_000_000_000L }.toDouble()
            val fps10s = frameTimesNs.count { it >= nowNs - 10_000_000_000L } / 10.0
            if (samples.size >= MAX_SAMPLES) samples.removeFirst()
            samples.addLast(SystemMetricSample(nowNs, nativeMb, javaUsedMb, thermal, fps1s, fps10s))
        }
    }

    private fun trimFramesLocked(cutoffNs: Long) {
        while (frameTimesNs.isNotEmpty() && frameTimesNs.first() < cutoffNs) {
            frameTimesNs.removeFirst()
        }
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
}

data class SystemMetricSample(
    val elapsedRealtimeNs: Long,
    val nativeHeapMb: Double,
    val javaHeapMb: Double,
    val thermalStatus: Int,
    val fps1s: Double,
    val fps10s: Double
)

data class SystemMetricsSnapshot(
    val samples: List<SystemMetricSample>,
    val durationSeconds: Double,
    val framesProcessed: Long,
    val nativePeakMb: Double,
    val javaPeakMb: Double,
    val peakThermalStatus: Int,
    val timeAtThrottleSeconds: Int,
    val meanFps: Double
)

