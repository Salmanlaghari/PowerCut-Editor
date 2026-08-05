package com.powercut.export

import com.powercut.core.Resolution
import com.powercut.core.FrameRate
import com.powercut.core.Container
import com.powercut.core.EncoderKind

/**
 * Export configuration — the kept-working contract between Kotlin and the
 * native export engine. Field names + types are referenced by JNI field ids
 * cached in JNI_OnLoad (see native_export.cpp). DO NOT rename without
 * updating the C++ side.
 */
data class ExportConfig(
    var resolution: Resolution = Resolution.P1080,
    var fps: FrameRate = FrameRate.FPS30,
    var container: Container = Container.MP4,
    var encoder: EncoderKind = EncoderKind.AUTO,
    var videoBitrate: Long = 12_000_000L,
    var audioBitrate: Int = 192_000,
    var audioChannels: Int = 2,
    var audioSampleRate: Int = 48_000,
    var removeWatermark: Boolean = false, // PRO unlock
    var priorityHw: Boolean = false,      // PRO priority HW queue
    var outPath: String = ""
) {
    /** Estimate the output file size in bytes (heuristic, shown live in UI). */
    fun estimateSizeBytes(durationSec: Double): Long {
        val video = (videoBitrate / 8.0) * durationSec
        val audio = (audioBitrate / 8.0) * durationSec
        val containerOverhead = (video + audio) * 0.02
        return (video + audio + containerOverhead).toLong()
    }

    /** Human-readable resolution label, e.g. "1080p", "4K", "8K". */
    fun resolutionLabel(): String = when (resolution) {
        Resolution.P480  -> "480p"
        Resolution.P720  -> "720p"
        Resolution.P1080 -> "1080p"
        Resolution.P2K   -> "2K"
        Resolution.P4K   -> "4K"
        Resolution.P8K   -> "8K"
    }
}
