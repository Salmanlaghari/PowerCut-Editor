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
 * ExportEngine — DEPRECATED STUB
 *
 * The native C++ export engine (JNI bridge) was broken in production builds
 * (stub build, UnsatisfiedLinkError, no actual rendering). The export pipeline
 * now uses [com.powercut.editor.domain.processing.VideoProcessor] exclusively,
 * which provides 300+ FFmpeg filter chains and handles all edits natively.
 *
 * This file is kept only for binary compatibility. All methods return safe
 * no-op values. Do NOT use this class for new code.
 *
 * @deprecated Use [com.powercut.editor.domain.processing.VideoProcessor] directly.
 */
@Deprecated(
    message = "Native export engine removed. Use VideoProcessor for all export operations.",
    replaceWith = ReplaceWith("VideoProcessor", "com.powercut.editor.domain.processing.VideoProcessor")
)
class ExportEngine {

    /** Always returns false — native engine is not available. */
    fun isAvailable(): Boolean = false

    /** No-op — native engine cannot start. */
    fun start(dag: Any, config: ExportConfig): Boolean = false

    /** No-op. */
    fun cancel() {}

    /** Always returns false. */
    fun running(): Boolean = false

    /** No-op. */
    fun destroy() {}

    companion object {
        @JvmStatic fun presetTikTok() = ExportPreset("TikTok", 1080, 1920, 30.0, 10000000, 15000000, "h264", "aac", "mp4")
        @JvmStatic fun presetReels() = ExportPreset("Reels", 1080, 1920, 30.0, 10000000, 15000000, "h264", "aac", "mp4")
        @JvmStatic fun presetShorts() = ExportPreset("Shorts", 1080, 1920, 30.0, 10000000, 15000000, "h264", "aac", "mp4")
        @JvmStatic fun presetYt1080() = ExportPreset("YT1080", 1920, 1080, 30.0, 12000000, 18000000, "h264", "aac", "mp4")
        @JvmStatic fun presetYt4k() = ExportPreset("YT4K", 3840, 2160, 30.0, 45000000, 70000000, "h264", "aac", "mp4")
        @JvmStatic fun presetWhatsApp() = ExportPreset("WhatsApp", 720, 1280, 30.0, 4000000, 6000000, "h264", "aac", "mp4")
    }
}
