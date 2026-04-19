package com.focusguard.instrumentation

class InferenceBenchmark(val tag: String) {
    private val latenciesNs = ArrayDeque<Long>()
    private val maxSamples = 2000
    private var startNs = 0L
    private val lock = Any()

    fun start() {
        startNs = System.nanoTime()
    }

    fun stop() {
        val elapsed = System.nanoTime() - startNs
        synchronized(lock) {
            if (latenciesNs.size >= maxSamples) latenciesNs.removeFirst()
            latenciesNs.addLast(elapsed)
        }
    }

    fun stats(): Stats? {
        synchronized(lock) {
            if (latenciesNs.size < 30) return null
            val sorted = latenciesNs.sorted()
            return Stats(
                tag = tag,
                sampleCount = sorted.size,
                p50Ms = sorted[sorted.size / 2] / 1_000_000.0,
                p95Ms = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)] / 1_000_000.0,
                p99Ms = sorted[(sorted.size * 0.99).toInt().coerceAtMost(sorted.lastIndex)] / 1_000_000.0,
                meanMs = sorted.average() / 1_000_000.0,
                minMs = sorted.first() / 1_000_000.0,
                maxMs = sorted.last() / 1_000_000.0
            )
        }
    }

    fun reset() {
        synchronized(lock) { latenciesNs.clear() }
    }
}

data class Stats(
    val tag: String,
    val sampleCount: Int,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val meanMs: Double,
    val minMs: Double,
    val maxMs: Double
)

