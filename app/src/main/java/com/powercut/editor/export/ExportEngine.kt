package com.powercut.editor.export

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

    /**
     * Start an export on the native worker thread.
     * Returns true if the export was started, false if already running or
     * the native engine is unavailable.
     */
    fun start(dag: Any, config: ExportConfig): Boolean {
        return try {
            if (nativeHandle == 0L) nativeHandle = nativeCreate()
            nativeStart(nativeHandle, dag, config)
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    /** Cancel a running export and join the worker thread. */
    fun cancel() {
        try {
            if (nativeHandle != 0L) nativeCancel(nativeHandle)
        } catch (e: UnsatisfiedLinkError) {
            // no-op
        }
    }

    /** True while the native export worker is running. */
    fun running(): Boolean = try {
        if (nativeHandle != 0L) nativeRunning(nativeHandle) else false
    } catch (e: UnsatisfiedLinkError) {
        false
    }

    /** Release native resources. Safe to call multiple times. */
    fun destroy() {
        try {
            if (nativeHandle != 0L) {
                nativeDestroy(nativeHandle)
                nativeHandle = 0L
            }
        } catch (e: UnsatisfiedLinkError) {
            nativeHandle = 0L
        }
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
