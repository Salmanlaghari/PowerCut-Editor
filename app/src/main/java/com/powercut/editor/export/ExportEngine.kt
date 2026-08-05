package com.powercut.editor.export

import android.os.Handler
import android.os.Looper

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

/**
 * JNI bridge between Kotlin UI and the native C++ PowerCut Export Engine.
 *
 * Watermark system:
 *   removeWatermark = false  → PowerCut watermark overlay (bottom-right, semi-transparent)
 *   removeWatermark = true   → clean export (user watched rewarded ad)
 *
 * The native library "powercut" is loaded on first instantiation. If the native
 * build is not present (e.g. running on a JVM without NDK artifacts) the external
 * calls are guarded and the engine degrades gracefully so the UI never crashes.
 */
class ExportEngine {
    private var nativeHandle: Long = 0

    init {
        try {
            System.loadLibrary("powercut")
        } catch (e: UnsatisfiedLinkError) {
            // Native lib not available in this build — calls below are no-ops.
        }
    }

    // ---- Native methods (implemented in app/src/main/cpp/native_export.cpp) ----
    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeStart(handle: Long, dag: Any, config: ExportConfig): Boolean
    external fun nativeCancel(handle: Long)
    external fun nativeRunning(handle: Long): Boolean

    /** Progress callback invoked from the native worker thread. */
    var onProgress: ((ExportProgress) -> Unit)? = null

    // PRIORITY 1 FIX: Handler bound to the main looper so progress updates
    // dispatched from the native worker thread are always posted to the UI
    // thread. This prevents the race condition of updating Compose state
    // from a non-UI thread.
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var isExporting = false

    /**
     * PRIORITY 1 FIX: JNI callback invoked from the native worker thread.
     * The C++ progress bridge calls this method via CallVoidMethod after
     * attaching the worker thread to the JVM. We construct an ExportProgress
     * and dispatch it to the main thread via Handler.post so the onProgress
     * lambda (which typically updates Compose state) always runs on the UI
     * thread.
     *
     * Signature: (JJDIJ)V  →  (long cur, long total, double speedX, int etaSeconds, long bytes)
     */
    @Suppress("unused")  // called from JNI (native_export.cpp)
    fun onProgressCallback(cur: Long, total: Long, speedX: Double, etaSeconds: Int, bytes: Long) {
        val progress = ExportProgress(cur, total, speedX, etaSeconds, bytes)
        val cb = onProgress
        if (cb != null) {
            // PRIORITY 1 FIX: post to the main thread to avoid race conditions.
            mainHandler.post { cb(progress) }
        }
    }

    /**
     * Start an export on the native worker thread.
     *
     * @param dag the CURRENT active timeline state — must be the live
     *            [com.powercut.editor.data.VideoProject] instance, NOT a stale
     *            global or empty `Any()`. The JNI bridge reads its fields
     *            (trim, speed, filter, text overlay, crop, rotation, background
     *            music, volumes, etc.) and builds a PowerCutDAG so the native
     *            engine can resolve every timeline edit per-frame, mix all
     *            audio tracks, and hash the DAG for cache invalidation.
     * @param config export configuration. The `removeWatermark` boolean is
     *               mapped directly to the native `remove_watermark` field via
     *               JNI GetBooleanField (Kotlin `Boolean` -> JNI `"Z"`).
     *               `removeWatermark = true`  -> clean export (ad watched).
     *               `removeWatermark = false` -> PowerCut watermark overlay.
     * @return true if the export was started, false if already running or
     *         the native engine is unavailable.
     */
    fun start(dag: Any, config: ExportConfig): Boolean {
        return try {
            if (nativeHandle == 0L) nativeHandle = nativeCreate()
            // PRIORITY 1 FIX: sanitize the output path — replace characters
            // that cause avio_open to fail silently (spaces, special chars).
            val safeConfig = config.copy(out = sanitizePath(config.out))
            val ok = nativeStart(nativeHandle, dag, safeConfig)
            if (ok) isExporting = true
            ok
        } catch (e: UnsatisfiedLinkError) {
            false
        } catch (e: Exception) {
            // PRIORITY 1 FIX: catch any JNI exception to prevent app crash.
            false
        }
    }

    /**
     * PRIORITY 1 FIX: Sanitize the output file path.
     * Replaces spaces and other characters that cause avio_open to fail
     * with underscores. Keeps path separators and valid filename chars.
     */
    private fun sanitizePath(path: String): String {
        if (path.isEmpty()) return path
        val lastSlash = path.lastIndexOf('/')
        val dir = if (lastSlash >= 0) path.substring(0, lastSlash + 1) else ""
        val file = if (lastSlash >= 0) path.substring(lastSlash + 1) else path
        // Replace illegal filename characters with underscores.
        val safeFile = file.replace(" ", "_")
            .replace("\"", "_")
            .replace("'", "_")
            .replace("*", "_")
            .replace("?", "_")
            .replace("<", "_")
            .replace(">", "_")
            .replace("|", "_")
            .replace(":", "_")
            .replace(";", "_")
            .replace("&", "_")
        return dir + safeFile
    }

    /**
     * Cancel a running export and join the worker thread.
     * PRIORITY 1 FIX: Safe to call multiple times — the native cancel()
     * is idempotent (sets the atomic flag and joins the thread).
     */
    fun cancel() {
        try {
            if (nativeHandle != 0L) nativeCancel(nativeHandle)
        } catch (e: UnsatisfiedLinkError) {
            // no-op
        } catch (e: Exception) {
            // PRIORITY 1 FIX: catch any exception to prevent crash on cancel.
        }
        isExporting = false
    }

    /** True while the native export worker is running. */
    fun running(): Boolean = try {
        if (nativeHandle != 0L) nativeRunning(nativeHandle) else false
    } catch (e: UnsatisfiedLinkError) {
        false
    }

    /**
     * Release native resources. Safe to call multiple times.
     * PRIORITY 1 FIX: cancel() is called by the native destructor, but we
     * also cancel() here first so the worker thread is joined before we
     * destroy the handle. This prevents the destructor from blocking.
     */
    fun destroy() {
        try {
            if (nativeHandle != 0L) {
                cancel()  // PRIORITY 1 FIX: join worker thread first
                nativeDestroy(nativeHandle)
                nativeHandle = 0L
            }
        } catch (e: UnsatisfiedLinkError) {
            nativeHandle = 0L
        } catch (e: Exception) {
            nativeHandle = 0L
        }
        isExporting = false
    }

    companion object {
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
    }
}
