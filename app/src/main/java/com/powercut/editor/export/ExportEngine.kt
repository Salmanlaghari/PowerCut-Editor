package com.powercut.editor.export

import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.powercut.editor.data.VideoProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class ExportPreset(
    val name: String,
    val w: Int,
    val h: Int,
    val fps: Double,
    val tbr: Long,
    val mbr: Long,
    val vcodec: String,
    val acodec: String,
    val container: String
)

data class ExportConfig(
    val preset: ExportPreset,
    val out: String,
    val hw: Boolean = true,
    val twoPass: Boolean = true,
    val faststart: Boolean = true,
    val removeWatermark: Boolean = false
)

data class ExportProgress(
    val cur: Long,
    val total: Long,
    val speedX: Double,
    val etaSeconds: Int,
    val bytes: Long
)

object ExportEngine {
    private const val TAG = "PowerCut.ExportEngine"

    private val running = AtomicBoolean(false)
    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastProgressAt = AtomicReference(0L)
    private var currentJob: Job? = null

    init {
        try {
            System.loadLibrary("powercut_native")
            Log.i(TAG, "libpowercut_native loaded")
        } catch (t: Throwable) {
            Log.e(TAG, "failed to load libpowercut_native", t)
        }
    }

    fun isAvailable(): Boolean = nativeIsAvailable()

    interface ProgressCallback {
        fun onProgress(percent: Int, fellBackSw: Boolean) {}
        fun onComplete(ok: Boolean, sizeBytes: Long, error: String?, elapsedMs: Long) {}
    }

    data class ExportOutcome(
        val ok: Boolean,
        val outPath: String,
        val sizeBytes: Long,
        val fellBackSw: Boolean,
        val elapsedMs: Long,
        val error: String?
    )

    fun export(
        project: VideoProject,
        config: ExportConfig,
        surface: android.view.Surface? = null,
        callback: ProgressCallback
    ): Job {
        require(!running.get()) { "export already in progress" }
        running.set(true)
        lastProgressAt.set(SystemClock.elapsedRealtime())

        currentJob = scope.launch {
            var fellBackSw = false
            var outcome: ExportOutcome
            try {
                outcome = runExportOnce(project, config, surface, callback)
                if (!outcome.ok && outcome.error?.contains("stall") == true) {
                    Log.w(TAG, "15s watchdog: restarting in SOFTWARE mode")
                    fellBackSw = true
                    lastProgressAt.set(SystemClock.elapsedRealtime())
                    outcome = runExportOnce(project, config.copy(hw = false), surface, callback)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "export threw — caught at Kotlin boundary", t)
                outcome = ExportOutcome(
                    ok = false,
                    outPath = config.out,
                    sizeBytes = 0,
                    fellBackSw = fellBackSw,
                    elapsedMs = 0,
                    error = t.message ?: t::class.java.simpleName
                )
            } finally {
                running.set(false)
                watchdogJob?.cancel()
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                callback.onComplete(outcome.ok, outcome.sizeBytes, outcome.error, outcome.elapsedMs)
            }
        }
        startWatchdog(callback)
        return currentJob!!
    }

    fun cancel() {
        try {
            nativeCancel()
        } catch (t: Throwable) {
            Log.e(TAG, "nativeCancel threw (caught)", t)
        }
        currentJob?.cancel()
        running.set(false)
    }

    private suspend fun runExportOnce(
        project: VideoProject,
        config: ExportConfig,
        surface: android.view.Surface?,
        callback: ProgressCallback
    ): ExportOutcome {
        val t0 = SystemClock.elapsedRealtime()
        val ok: Boolean
        try {
            ok = withContext(Dispatchers.Default) {
                nativeExport(config, project, ProgressRelay(callback), surface)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "nativeExport threw — caught (no crash to UI)", t)
            return ExportOutcome(false, config.out, 0, false,
                SystemClock.elapsedRealtime() - t0, t.message)
        }
        val elapsed = SystemClock.elapsedRealtime() - t0
        val size = if (ok) File(config.out).length() else 0L
        return ExportOutcome(ok, config.out, size, false, elapsed, null)
    }

    private fun startWatchdog(callback: ProgressCallback) {
        watchdogJob = scope.launch {
            while (running.get()) {
                delay(1000)
                val since = SystemClock.elapsedRealtime() - lastProgressAt.get()
                if (since > 15_000) {
                    Log.w(TAG, "15s no-progress watchdog fired")
                    currentJob?.cancel()
                    return@launch
                }
            }
        }
    }

    private class ProgressRelay(private val cb: ProgressCallback) : ProgressCallback {
        override fun onProgress(percent: Int, fellBackSw: Boolean) {
            lastProgressAt.set(SystemClock.elapsedRealtime())
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.Main) {
                cb.onProgress(percent, fellBackSw)
            }
        }
    }

    @JvmStatic
    fun presetTikTok() = ExportPreset("TikTok", 1080, 1920, 30.0, 10000000, 15000000, "h264", "aac", "mp4")
    @JvmStatic
    fun presetReels() = ExportPreset("Reels", 1080, 1920, 30.0, 10000000, 15000000, "h264", "aac", "mp4")
    @JvmStatic
    fun presetShorts() = ExportPreset("Shorts", 1080, 1920, 30.0, 10000000, 15000000, "h264", "aac", "mp4")
    @JvmStatic
    fun presetYt1080() = ExportPreset("YT1080", 1920, 1080, 30.0, 12000000, 18000000, "h264", "aac", "mp4")
    @JvmStatic
    fun presetYt4k() = ExportPreset("YT4K", 3840, 2160, 30.0, 45000000, 70000000, "h264", "aac", "mp4")
    @JvmStatic
    fun presetWhatsApp() = ExportPreset("WhatsApp", 720, 1280, 30.0, 4000000, 6000000, "h264", "aac", "mp4")

    @JvmStatic
    private external fun nativeIsAvailable(): Boolean

    @JvmStatic
    private external fun nativeExport(
        config: ExportConfig,
        project: VideoProject,
        progress: ProgressCallback,
        surface: android.view.Surface?
    ): Boolean

    @JvmStatic
    private external fun nativeCancel()
}
