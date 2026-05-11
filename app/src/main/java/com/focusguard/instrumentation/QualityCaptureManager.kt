package com.focusguard.instrumentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.Log
import com.focusguard.ml.EyeLandmarkMetadata
import com.focusguard.ml.QualityFrameMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object QualityCaptureManager {
    private const val CAPTURE_INTERVAL = 5
    private const val EXPECTED_CAPTURE_FPS = 3.0
    private const val QUEUE_CAPACITY = 8
    private const val JPEG_QUALITY = 92
    private const val TAG = "QUALITY_CAPTURE"
    private val lock = Any()
    private var workerThread: Thread? = null
    private var queue: LinkedBlockingQueue<CaptureJob>? = null
    private var session: CaptureSession? = null
    private var totalFramesProcessed = 0L
    private var capturedFrames = 0L
    private var droppedFrames = 0L
    private var sequence = 0L

    val currentSessionId: String?
        get() = synchronized(lock) { session?.sessionId }

    fun start(
        context: Context,
        calibrationCompleted: Boolean,
        baselinePitchDeg: Float?,
        baselineYawDeg: Float?
    ) {
        synchronized(lock) {
            if (session != null) return
            val appContext = context.applicationContext
            val sessionId = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val root = File(appContext.getExternalFilesDir(null), "quality_capture/$sessionId")
            val framesDir = File(root, "frames").apply { mkdirs() }
            val sidecarsDir = File(root, "sidecars").apply { mkdirs() }
            root.mkdirs()
            val startMs = System.currentTimeMillis()
            session = CaptureSession(sessionId, root, framesDir, sidecarsDir, startMs, appContext)
            totalFramesProcessed = 0L
            capturedFrames = 0L
            droppedFrames = 0L
            sequence = 0L
            writeManifest(appContext, root, sessionId, calibrationCompleted, baselinePitchDeg, baselineYawDeg)
            val activeQueue = LinkedBlockingQueue<CaptureJob>(QUEUE_CAPACITY)
            queue = activeQueue
            workerThread = Thread({ drainQueue(activeQueue) }, "QualityCaptureWriter").apply { start() }
        }
    }

    fun stop() {
        val endingSession: CaptureSession?
        val endingQueue: LinkedBlockingQueue<CaptureJob>?
        synchronized(lock) {
            endingSession = session
            endingQueue = queue
            session = null
            queue = null
        }
        endingQueue?.offer(CaptureJob.Stop)
        workerThread?.join(1_000L)
        workerThread = null
        endingSession?.let { writeSessionEnd(it) }
    }

    fun recordFrameAvailable(): Boolean {
        synchronized(lock) {
            if (session == null) return false
            totalFramesProcessed += 1
            return totalFramesProcessed % CAPTURE_INTERVAL == 0L
        }
    }

    fun enqueue(
        sourceBitmap: Bitmap,
        metadata: QualityFrameMetadata?,
        isCalibrationFrame: Boolean,
        baselinePitchDeg: Float?,
        baselineYawDeg: Float?,
        inFocusedZone: Boolean,
        sessionScore: Float,
        stampFrames: Boolean
    ) {
        val captureJob = synchronized(lock) {
            val active = session ?: return
            val nextSequence = sequence++
            val elapsedMs = System.currentTimeMillis() - active.startMs
            CaptureJob.Frame(
                session = active,
                bitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, false),
                frameId = "%06d".format(nextSequence),
                elapsedMs = elapsedMs,
                timestampMs = System.currentTimeMillis(),
                metadata = metadata,
                isCalibrationFrame = isCalibrationFrame,
                baselinePitchDeg = baselinePitchDeg,
                baselineYawDeg = baselineYawDeg,
                baselineSubtractedPitchDeg = (metadata?.rawPitchDeg ?: 0f) - (baselinePitchDeg ?: 0f),
                baselineSubtractedYawDeg = (metadata?.rawYawDeg ?: 0f) - (baselineYawDeg ?: 0f),
                inFocusedZone = inFocusedZone,
                sessionScore = sessionScore,
                stampFrames = stampFrames
            )
        }
        val accepted = queue?.offer(captureJob) == true
        synchronized(lock) {
            if (accepted) {
                capturedFrames += 1
            } else {
                droppedFrames += 1
                captureJob.bitmap.recycle()
                Log.w(TAG, "Dropped quality capture frame; writer queue is full. dropped=$droppedFrames")
            }
        }
    }

    private fun drainQueue(activeQueue: LinkedBlockingQueue<CaptureJob>) {
        while (true) {
            val job = activeQueue.poll(500L, TimeUnit.MILLISECONDS) ?: continue
            when (job) {
                is CaptureJob.Frame -> writeFrame(job)
                CaptureJob.Stop -> return
            }
        }
    }

    private fun writeFrame(job: CaptureJob.Frame) {
        try {
            val baseName = "${job.frameId}_t${"%07d".format(job.elapsedMs)}"
            val bitmapToSave = if (job.stampFrames) stampedBitmap(job) else job.bitmap
            File(job.session.framesDir, "$baseName.jpg").outputStream().use { stream ->
                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
            if (bitmapToSave !== job.bitmap) bitmapToSave.recycle()
            File(job.session.sidecarsDir, "$baseName.json").writeText(sidecarJson(job).toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write quality capture frame", e)
        } finally {
            job.bitmap.recycle()
        }
    }

    private fun stampedBitmap(job: CaptureJob.Frame): Bitmap {
        val copy = job.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val density = job.session.context.resources.displayMetrics.scaledDensity
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f * density
        }
        val bgPaint = Paint().apply { color = Color.argb(150, 0, 0, 0) }
        val label = "t=${formatElapsed(job.elapsedMs)}  f=${job.frameId}  yaw=${"%.1f".format(job.baselineSubtractedYawDeg)} deg pitch=${"%.1f".format(job.baselineSubtractedPitchDeg)} deg  ${if (job.inFocusedZone) "FOCUSED" else "NOT FOCUSED"}"
        val padding = 6f * density
        val x = 8f * density
        val y = copy.height - 12f * density
        val width = paint.measureText(label) + padding * 2
        val height = paint.textSize + padding * 2
        canvas.drawRect(x - padding, y - paint.textSize - padding, x - padding + width, y - paint.textSize - padding + height, bgPaint)
        canvas.drawText(label, x, y, paint)
        return copy
    }

    private fun sidecarJson(job: CaptureJob.Frame): JSONObject {
        val metadata = job.metadata
        return JSONObject()
            .put("frame_id", job.frameId)
            .put("session_id", job.session.sessionId)
            .put("timestamp_ms", job.timestampMs)
            .put("elapsed_ms_since_session_start", job.elapsedMs)
            .put("is_calibration_frame", job.isCalibrationFrame)
            .put("face_detected", metadata?.faceDetected ?: false)
            .put("face_bbox_xywh", bboxJson(metadata?.faceBBox))
            .put("face_confidence", metadata?.faceConfidence ?: JSONObject.NULL)
            .put("landmark_score", metadata?.landmarkScore ?: JSONObject.NULL)
            .put("eye_landmarks", eyeLandmarksJson(metadata?.eyeLandmarks))
            .put("raw_pitch_deg", metadata?.rawPitchDeg ?: JSONObject.NULL)
            .put("raw_yaw_deg", metadata?.rawYawDeg ?: JSONObject.NULL)
            .put("baseline_pitch_deg", job.baselinePitchDeg ?: JSONObject.NULL)
            .put("baseline_yaw_deg", job.baselineYawDeg ?: JSONObject.NULL)
            .put("baseline_subtracted_pitch_deg", job.baselineSubtractedPitchDeg)
            .put("baseline_subtracted_yaw_deg", job.baselineSubtractedYawDeg)
            .put("in_focused_zone", job.inFocusedZone)
            .put("session_score", job.sessionScore)
            .put("label", "")
    }

    private fun bboxJson(rect: RectF?): JSONArray {
        if (rect == null) return JSONArray()
        return JSONArray()
            .put(rect.left.toInt())
            .put(rect.top.toInt())
            .put(rect.width().toInt())
            .put(rect.height().toInt())
    }

    private fun eyeLandmarksJson(landmarks: EyeLandmarkMetadata?): JSONObject {
        fun point(pair: Pair<Float, Float>?) = if (pair == null) JSONArray() else JSONArray().put(pair.first).put(pair.second)
        return JSONObject()
            .put("left_inner_33", point(landmarks?.leftInner33))
            .put("left_outer_133", point(landmarks?.leftOuter133))
            .put("right_inner_362", point(landmarks?.rightInner362))
            .put("right_outer_263", point(landmarks?.rightOuter263))
    }

    private fun writeManifest(
        context: Context,
        root: File,
        sessionId: String,
        calibrationCompleted: Boolean,
        baselinePitchDeg: Float?,
        baselineYawDeg: Float?
    ) {
        val appVersion = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "unknown"
        }.getOrDefault("unknown")
        val manifest = JSONObject()
            .put("session_id", sessionId)
            .put("device_model", Build.MODEL)
            .put("android_version", Build.VERSION.RELEASE)
            .put("app_version", appVersion)
            .put(
                "model_files",
                JSONObject()
                    .put("face_detector", modelJson(context, "face_detector.tflite"))
                    .put("landmark", modelJson(context, "face_landmark_detector.tflite"))
                    .put("eyegaze", modelJson(context, "eyegaze.tflite"))
            )
            .put("accelerator", "NPU")
            .put("calibration_completed", calibrationCompleted)
            .put("calibration_baseline_pitch_deg", baselinePitchDeg ?: JSONObject.NULL)
            .put("calibration_baseline_yaw_deg", baselineYawDeg ?: JSONObject.NULL)
            .put("focused_zone_yaw_min", -25.0)
            .put("focused_zone_yaw_max", 20.0)
            .put("focused_zone_pitch_min", -15.0)
            .put("focused_zone_pitch_max", 18.0)
            .put("frame_capture_interval", CAPTURE_INTERVAL)
            .put("expected_capture_fps", EXPECTED_CAPTURE_FPS)
            .put("writer_queue_capacity", QUEUE_CAPACITY)
        File(root, "manifest.json").writeText(manifest.toString(2))
    }

    private fun writeSessionEnd(ended: CaptureSession) {
        val durationMs = System.currentTimeMillis() - ended.startMs
        val summary = synchronized(lock) {
            JSONObject()
                .put("session_id", ended.sessionId)
                .put("duration_ms", durationMs)
                .put("total_frames_processed", totalFramesProcessed)
                .put("frames_captured", capturedFrames)
                .put("frames_dropped", droppedFrames)
                .put("actual_capture_fps", if (durationMs > 0L) capturedFrames / (durationMs / 1000.0) else 0.0)
        }
        val manifestFile = File(ended.root, "manifest.json")
        runCatching {
            val manifest = JSONObject(manifestFile.readText())
                .put("frames_dropped", summary.getLong("frames_dropped"))
            manifestFile.writeText(manifest.toString(2))
        }
        File(ended.root, "session_end.json").writeText(summary.toString(2))
    }

    private fun modelJson(context: Context, filename: String): JSONObject {
        return JSONObject()
            .put("filename", filename)
            .put("sha256", sha256Asset(context, filename))
    }

    private fun sha256Asset(context: Context, filename: String): String {
        val outFile = File(context.cacheDir, "asset_hash_$filename")
        context.assets.open(filename).use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(outFile).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        outFile.delete()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun formatElapsed(elapsedMs: Long): String {
        val minutes = elapsedMs / 60_000L
        val seconds = (elapsedMs % 60_000L) / 1_000L
        val millis = elapsedMs % 1_000L
        return "%02d:%02d.%03d".format(minutes, seconds, millis)
    }

    data class CaptureState(
        val yawFocused: Boolean,
        val pitchFocused: Boolean
    ) {
        val inFocusedZone: Boolean = yawFocused && pitchFocused
    }

    fun focusedZoneFor(yaw: Float, pitch: Float): Boolean {
        // Tuned from 6-min labeled session (88.4% agreement, 86.9% focused recall).
        return yaw in -25.0f..20.0f && pitch in -15.0f..18.0f
    }

    private data class CaptureSession(
        val sessionId: String,
        val root: File,
        val framesDir: File,
        val sidecarsDir: File,
        val startMs: Long,
        val context: Context
    )

    private sealed class CaptureJob {
        data object Stop : CaptureJob()
        data class Frame(
            val session: CaptureSession,
            val bitmap: Bitmap,
            val frameId: String,
            val elapsedMs: Long,
            val timestampMs: Long,
            val metadata: QualityFrameMetadata?,
            val isCalibrationFrame: Boolean,
            val baselinePitchDeg: Float?,
            val baselineYawDeg: Float?,
            val baselineSubtractedPitchDeg: Float,
            val baselineSubtractedYawDeg: Float,
            val inFocusedZone: Boolean,
            val sessionScore: Float,
            val stampFrames: Boolean
        ) : CaptureJob()
    }
}
