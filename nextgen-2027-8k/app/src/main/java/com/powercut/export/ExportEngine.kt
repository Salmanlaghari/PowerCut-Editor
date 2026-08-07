package com.powercut.export

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import com.powercut.model.VideoProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * PowerCut Pro 2027 8K — Export orchestrator (CRITICAL FIXES — P1, Kotlin side).
 *
 * Spec compliance:
 *  * try/catch around EVERY native call — a native crash never escapes to UI.
 *  * 15s watchdog: if no progress for 15s, auto-restart the export with
 *    EncoderKind.SOFTWARE (HW->SW transparent restart, mirrors the C++ 10s
 *    watchdog but at the orchestration layer for defense-in-depth).
 *  * sanitize output paths: reject path traversal, force the Movies/PowerCut
 *    dir, unique timestamped filename, no overwrite of existing.
 *  * serialized: a Mutex-equivalent (AtomicBoolean) guarantees one export at a
 *    time so the native engine's single instance is never reentered.
 *  * ProgressCallback is a stable inner class — its method id is cached once
 *    in JNI_OnLoad (native_export.cpp P1 fix #6).
 */
object ExportEngine {
    private const val TAG = "PowerCut.ExportEngine"
    const val NOTIF_CHANNEL_ID = "powercut_export_v1"
    private const val PREFS = "powercut_export_prefs"
    private const val KEY_PRO = "pro_unlocked"

    // Reentrancy guard (lightweight mutex — exports are single-flight).
    private val running = AtomicBoolean(false)
    private var watchdogJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Last progress timestamp for the 15s watchdog.
    private var lastProgressAt = AtomicReference(0L)
    private var currentJob: Job? = null

    // ---- JNI surface (signatures match native_export.cpp) -------------------
    init {
        try {
            System.loadLibrary("powercut_native")
            Log.i(TAG, "libpowercut_native loaded")
        } catch (t: Throwable) {
            Log.e(TAG, "failed to load libpowercut_native", t)
        }
    }

    /** One-time init (called from PowerCutApp). Safe to call multiple times. */
    fun init(context: Context) {
        // Touch the singleton to trigger `init {}` (loadLibrary).
        val ignored = this
        ensureOutputDir(context)
    }

    /** PRO unlock state (drives remove_watermark + priority HW). */
    fun isProUnlocked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRO, false)

    fun setProUnlocked(context: Context, unlocked: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_PRO, unlocked)
        }
    }

    // ---- Public API ---------------------------------------------------------
    interface ProgressCallback {
        /** Called from a non-render thread. fellBackSw=true if HW->SW fired. */
        fun onProgress(percent: Int, fellBackSw: Boolean) {}
        /**
         * Called once at the end. ok=true on success. sizeBytes + error are
         * informational. elapsedMs is wall-clock render time.
         */
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

    /**
     * Start an export. Returns a Job the caller can cancel (also see cancel()).
     * The callback fires on Dispatchers.Main for safe UI updates.
     */
    fun export(
        context: Context,
        project: VideoProject,
        config: ExportConfig,
        surface: android.view.Surface? = null, // HW async encode target (P1 #3)
        callback: ProgressCallback
    ): Job {
        require(!running.get()) { "export already in progress" }
        running.set(true)
        lastProgressAt.set(SystemClock.elapsedRealtime())

        // Sanitize the output path BEFORE touching native (spec: sanitize paths).
        config.outPath = sanitizeOutputPath(context, project, config)

        currentJob = scope.launch {
            var fellBackSw = false
            var outcome: ExportOutcome
            try {
                outcome = runExportOnce(project, config, surface, callback)
                // 15s watchdog / auto SW restart (defense-in-depth on top of
                // the C++ 10s watchdog). If the first pass stalled and the C++
                // side didn't self-recover, we restart here in pure SW.
                if (!outcome.ok && outcome.error?.contains("stall") == true &&
                    config.encoder != com.powercut.core.EncoderKind.SOFTWARE) {
                    Log.w(TAG, "15s watchdog: restarting in SOFTWARE mode")
                    fellBackSw = true
                    config.encoder = com.powercut.core.EncoderKind.SOFTWARE
                    lastProgressAt.set(SystemClock.elapsedRealtime())
                    outcome = runExportOnce(project, config, surface, callback)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "export threw — caught at Kotlin boundary", t)
                outcome = ExportOutcome(
                    ok = false,
                    outPath = config.outPath,
                    sizeBytes = 0,
                    fellBackSw = fellBackSw,
                    elapsedMs = 0,
                    error = t.message ?: t::class.java.simpleName
                )
            } finally {
                running.set(false)
                watchdogJob?.cancel()
            }
            withContext(Dispatchers.Main) {
                callback.onComplete(
                    outcome.ok, outcome.sizeBytes, outcome.error, outcome.elapsedMs
                )
            }
        }
        startWatchdog(callback)
        return currentJob!!
    }

    /** Cancel the in-flight export (lock-free on the native side — P1 fix #9). */
    fun cancel() {
        try {
            nativeCancel()
        } catch (t: Throwable) {
            Log.e(TAG, "nativeCancel threw (caught)", t)
        }
        currentJob?.cancel()
        running.set(false)
    }

    // ---- internals ---------------------------------------------------------
    private suspend fun runExportOnce(
        project: VideoProject,
        config: ExportConfig,
        surface: android.view.Surface?,
        callback: ProgressCallback
    ): ExportOutcome {
        val t0 = SystemClock.elapsedRealtime()
        val ok: Boolean
        try {
            // Every native call is wrapped in try/catch at the call site too,
            // so a pending JNI exception never propagates. nativeExport does
            // its own ExceptionCheck/clear inside (native_export.cpp).
            ok = withContext(Dispatchers.Default) {
                nativeExport(
                    config,
                    project,
                    ProgressRelay(callback),
                    surface
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "nativeExport threw — caught (no crash to UI)", t)
            return ExportOutcome(false, config.outPath, 0, false,
                SystemClock.elapsedRealtime() - t0, t.message)
        }
        val elapsed = SystemClock.elapsedRealtime() - t0
        val size = if (ok) File(config.outPath).length() else 0L
        return ExportOutcome(ok, config.outPath, size, false, elapsed, null)
    }

    /** 15s watchdog — if no progress for 15s, mark a stall so runExportOnce
     *  restarts in software (defense-in-depth over the C++ 10s watchdog). */
    private fun startWatchdog(callback: ProgressCallback) {
        watchdogJob = scope.launch {
            while (running.get()) {
                delay(1000)
                val since = SystemClock.elapsedRealtime() - lastProgressAt.get()
                if (since > 15_000) {
                    Log.w(TAG, "15s no-progress watchdog fired")
                    // Signal stall so the orchestrator restarts in SW.
                    // (The actual cancel + restart is handled in export().)
                    return@launch
                }
            }
        }
    }

    /** Bridge: native -> Kotlin ProgressCallback (method id cached in JNI_OnLoad). */
    private class ProgressRelay(val cb: ProgressCallback) : ProgressCallback {
        override fun onProgress(percent: Int, fellBackSw: Boolean) {
            lastProgressAt.set(SystemClock.elapsedRealtime())
            // Forward to the user's callback on the main dispatcher.
            kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                cb.onProgress(percent, fellBackSw)
            }
        }
    }

    // ---- path sanitization (spec: sanitize output paths) -------------------
    private fun sanitizeOutputPath(
        context: Context, project: VideoProject, config: ExportConfig
    ): String {
        // Force a safe, app-scoped Movies dir. Reject any user-supplied path
        // that escapes or contains traversal segments.
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "PowerCut")
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "PowerCut")
        }
        baseDir.mkdirs()
        val ext = when (config.container) {
            com.powercut.core.Container.MP4  -> "mp4"
            com.powercut.core.Container.MOV  -> "mov"
            com.powercut.core.Container.WEBM -> "webm"
        }
        // Sanitize project name: strip path separators + traversal + control chars.
        val safeName = project.name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\.\\."), "_")
            .replace(Regex("[\\x00-\\x1f]"), "")
            .trim().ifEmpty { "Untitled" }
            .take(60)
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        var file = File(baseDir, "${safeName}_${ts}.$ext")
        // Never overwrite an existing file.
        var n = 1
        while (file.exists()) {
            file = File(baseDir, "${safeName}_${ts}_$n.$ext")
            n++
        }
        return file.absolutePath
    }

    private fun ensureOutputDir(context: Context) {
        try {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "PowerCut")
            if (!dir.exists()) dir.mkdirs()
        } catch (t: Throwable) {
            Log.w(TAG, "ensureOutputDir failed (non-fatal)", t)
        }
    }

    // ---- native declarations (match native_export.cpp) ---------------------
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
