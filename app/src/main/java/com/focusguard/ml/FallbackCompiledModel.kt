package com.focusguard.ml

import android.content.Context
import android.util.Log
import com.focusguard.instrumentation.BenchmarkRegistry
import com.focusguard.instrumentation.InferenceBenchmark
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer

/**
 * Small reliability wrapper around LiteRT's CompiledModel API.
 *
 * The hackathon target is Snapdragon NPU acceleration, so each model starts on NPU first. If
 * creation or inference fails, we recreate the model on the next accelerator and retry the same
 * input. Input/output buffers belong to a specific CompiledModel instance, so each fallback step
 * must recreate buffers and ask the caller to write the input again.
 */
internal class FallbackCompiledModel(
    private val context: Context,
    private val assetName: String,
    private val logTag: String
) {
    private val accelerators = listOf(Accelerator.NPU, Accelerator.GPU, Accelerator.CPU)

    @Volatile
    private var runtime: Runtime = createRuntime(startIndex = 0)

    /**
     * Runs inference on the active accelerator.
     *
     * writeInputs is invoked before every attempt, including retries, because fallback swaps to a
     * fresh CompiledModel with fresh TensorBuffers. The returned output buffers are the buffers from
     * the accelerator that actually completed inference.
     */
    fun run(
        benchmark: InferenceBenchmark,
        section: String,
        writeInputs: (List<TensorBuffer>) -> Unit
    ): List<TensorBuffer> {
        while (true) {
            val activeRuntime = runtime
            try {
                writeInputs(activeRuntime.inputBuffers)
                BenchmarkRegistry.trace(benchmark, section) {
                    activeRuntime.model.run(activeRuntime.inputBuffers, activeRuntime.outputBuffers)
                }
                return activeRuntime.outputBuffers
            } catch (e: Exception) {
                val nextIndex = activeRuntime.index + 1
                if (nextIndex >= accelerators.size) {
                    Log.e(logTag, "$assetName failed on all LiteRT accelerators", e)
                    throw e
                }

                Log.w(logTag, "$assetName failed on ${activeRuntime.accelerator}; falling back", e)
                synchronized(this) {
                    if (runtime === activeRuntime) {
                        runtime = createRuntime(nextIndex, e)
                    }
                }
            }
        }
    }

    private fun createRuntime(startIndex: Int, previousFailure: Exception? = null): Runtime {
        var lastFailure = previousFailure
        for (index in startIndex until accelerators.size) {
            val accelerator = accelerators[index]
            try {
                val model = CompiledModel.create(
                    context.assets,
                    assetName,
                    CompiledModel.Options(accelerator)
                )
                Log.i(logTag, "$assetName using LiteRT $accelerator")
                return Runtime(
                    index = index,
                    accelerator = accelerator,
                    model = model,
                    inputBuffers = model.createInputBuffers(),
                    outputBuffers = model.createOutputBuffers()
                )
            } catch (e: Exception) {
                lastFailure = e
                Log.w(logTag, "$assetName could not start on $accelerator", e)
            }
        }

        throw IllegalStateException("$assetName could not start on NPU, GPU, or CPU", lastFailure)
    }

    private data class Runtime(
        val index: Int,
        val accelerator: Accelerator,
        val model: CompiledModel,
        val inputBuffers: List<TensorBuffer>,
        val outputBuffers: List<TensorBuffer>
    )
}
