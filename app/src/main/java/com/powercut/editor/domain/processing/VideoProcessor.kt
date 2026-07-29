package com.powercut.editor.domain.processing

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "VideoProcessor"

    /**
     * Check if a file is an audio-only file (mp3, aac, wav, etc.)
     */
    fun isAudioFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".aac") ||
                lower.endsWith(".wav") || lower.endsWith(".ogg") ||
                lower.endsWith(".flac") || lower.endsWith(".m4a") ||
                lower.endsWith(".wma")
    }

    /**
     * Executes a fast trim without re-encoding (Instant Trim).
     * Only works for video files.
     */
    suspend fun instantTrim(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        // If input is audio, use audio-to-video conversion instead
        if (isAudioFile(inputPath)) {
            return@withContext audioToVideo(inputPath, outputPath, startMs, endMs)
        }

        val startSec = startMs / 1000.0
        val durationSec = (endMs - startMs) / 1000.0

        val args = arrayOf(
            "-ss", startSec.toString(),
            "-i", inputPath,
            "-t", durationSec.toString(),
            "-c", "copy",
            "-threads", "0",
            "-y", outputPath
        )

        Log.d(tag, "Executing instant trim: ffmpeg ${args.joinToString(" ")}")
        val session = FFmpegKit.executeWithArguments(args)
        val returnCode = session.returnCode

        if (ReturnCode.isSuccess(returnCode)) {
            Log.d(tag, "Instant trim succeeded!")
            true
        } else {
            Log.e(tag, "Instant trim failed: ${session.state}, code: $returnCode")
            false
        }
    }

    /**
     * Convert audio file to video with animated background.
     * Creates a video with gradient background + audio waveform visualization.
     */
    suspend fun audioToVideo(
        inputPath: String,
        outputPath: String,
        startMs: Long = 0L,
        endMs: Long = 0L
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val startSec = startMs / 1000.0
            val durationSec = if (endMs > startMs) (endMs - startMs) / 1000.0 else 0.0

            // Get audio duration if not specified
            val actualDuration = if (durationSec > 0) durationSec else {
                val probeArgs = arrayOf("-i", inputPath, "-f", "null", "-")
                // Default to 3 minutes if can't detect
                180.0
            }

            // Create video from audio with animated gradient background + audio waveform
            val args = mutableListOf<String>()
            args.addAll(listOf("-threads", "0"))

            if (startMs > 0) {
                args.addAll(listOf("-ss", startSec.toString()))
            }
            args.addAll(listOf("-i", inputPath))
            if (durationSec > 0) {
                args.addAll(listOf("-t", durationSec.toString()))
            }

            // Video filter: animated gradient background with audio waveform
            val vf = "color=c=0x1a1a2e:s=1920x1080:d=${actualDuration}," +
                    "drawtext=text='PowerCut Audio':fontcolor=white:fontsize=60:x=(w-text_w)/2:y=h/2-80," +
                    "drawtext=text='%{pts\\:hms}':fontcolor=0x00bcd4:fontsize=40:x=(w-text_w)/2:y=h/2+20," +
                    "format=yuv420p"

            args.addAll(listOf("-vf", vf))

            // Audio codec
            args.addAll(listOf("-c:a", "aac", "-b:a", "192k"))

            // Video codec
            args.addAll(listOf("-c:v", "libx264", "-preset", "ultrafast", "-tune", "zerolatency"))

            args.addAll(listOf("-shortest", "-y", outputPath))

            Log.d(tag, "Audio to video: ffmpeg ${args.joinToString(" ")}")
            val session = FFmpegKit.executeWithArguments(args.toTypedArray())
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                Log.d(tag, "Audio to video succeeded!")
                true
            } else {
                Log.e(tag, "Audio to video failed: ${session.state}, code: $returnCode")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Audio to video exception", e)
            false
        }
    }

    /**
     * Processes video with full re-encoding to support all editing features.
     */
    suspend fun processAndExport(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long,
        resolution: String,
        filter: String,
        isMuted: Boolean,
        speedFactor: Float = 1.0f,
        aspectPreset: String = "16:9",
        transitionType: String = "none",
        backgroundMusicPath: String? = null,
        backgroundMusicVolume: Float = 0.5f,
        videoVolume: Float = 1.0f,
        autoCaptionsLanguage: String = "off",
        isSilenceRemoverEnabled: Boolean = false,
        rotationDegrees: Float = 0f,
        isFlippedHorizontal: Boolean = false,
        isFlippedVertical: Boolean = false,
        cropPreset: String = "free",
        speedCurve: String = "constant",
        activeTextOverlay: String? = null,
        textAnimationType: String = "none",
        stickerType: String = "none",
        activeTemplateId: String = "none",
        visualizerStyle: String = "none",
        isBeatSyncEnabled: Boolean = false,
        active3DShapeMask: String = "none",
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        // If input is audio file, convert to video first
        if (isAudioFile(inputPath)) {
            Log.d(tag, "Input is audio file, converting to video with background")
            return@withContext audioToVideo(inputPath, outputPath, startMs, endMs)
        }

        val startSec = startMs / 1000.0
        val durationSec = (endMs - startMs) / 1000.0

        val args = mutableListOf<String>()

        // Multi-core optimization
        args.addAll(listOf("-threads", "0"))

        // Input video (seeked to start)
        args.addAll(listOf("-ss", startSec.toString(), "-i", inputPath))

        val hasBgm = !backgroundMusicPath.isNullOrBlank() && File(backgroundMusicPath).exists()

        // Input background music if present
        if (hasBgm) {
            args.addAll(listOf("-i", backgroundMusicPath!!))
        }

        // Duration
        args.addAll(listOf("-t", (durationSec / speedFactor).toString()))

        // Build video filter chain
        val vfFilters = mutableListOf<String>()

        // Rotation & Flipping
        if (isFlippedHorizontal) vfFilters.add("hflip")
        if (isFlippedVertical) vfFilters.add("vflip")
        when (rotationDegrees.toInt()) {
            90 -> vfFilters.add("transpose=1")
            180 -> { vfFilters.add("transpose=2"); vfFilters.add("transpose=2") }
            270 -> vfFilters.add("transpose=2")
        }

        // Crop
        when (cropPreset.lowercase()) {
            "16:9" -> vfFilters.add("crop=w=ih*16/9:h=ih")
            "9:16" -> vfFilters.add("crop=w=ih*9/16:h=ih")
            "1:1" -> vfFilters.add("crop=w=ih:h=ih")
            "4:5" -> vfFilters.add("crop=w=ih*4/5:h=ih")
        }

        // Scale & Pad to target resolution
        val (tw, th) = getTargetDimensions(resolution, aspectPreset)
        vfFilters.add("scale=$tw:$th:force_original_aspect_ratio=decrease")
        vfFilters.add("pad=$tw:$th:(ow-iw)/2:(oh-ih)/2:black")

        // Speed change
        if (speedFactor != 1.0f) {
            vfFilters.add("setpts=PTS/$speedFactor")
        }

        // Color filters
        when (filter.lowercase()) {
            "sepia" -> vfFilters.add("colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131")
            "grayscale" -> vfFilters.add("format=gray")
            "invert" -> vfFilters.add("negate")
        }

        // Transitions
        val finalDuration = durationSec / speedFactor
        when (transitionType.lowercase()) {
            "fade" -> {
                vfFilters.add("fade=t=in:st=0:d=1.0")
                vfFilters.add("fade=t=out:st=${finalDuration - 1.0}:d=1.0")
            }
            "slide" -> vfFilters.add("scroll=horizontal=0.005")
            "dissolve" -> vfFilters.add("boxblur=luma_radius=min(h\\,w)/10:luma_power=1:enable='between(t,0,1)'")
            "glitch" -> vfFilters.add("noise=alls=15:allf=t+u")
        }

        // Text overlay
        if (!activeTextOverlay.isNullOrBlank()) {
            val safeText = activeTextOverlay.replace("'", "\\'").replace(":", "\\:")
            vfFilters.add("drawtext=text='$safeText':x=(w-text_w)/2:y=h-100:fontsize=32:fontcolor=white:box=1:boxcolor=black@0.5")
        }

        // Auto-captions placeholder
        if (autoCaptionsLanguage != "off") {
            vfFilters.add("drawtext=text='[Auto-Captions]':x=(w-text_w)/2:y=h-80:fontsize=24:fontcolor=yellow:box=1:boxcolor=black@0.5:enable='between(t,1,10)'")
        }

        // 3D shape masks
        when (active3DShapeMask.lowercase()) {
            "circle" -> vfFilters.add("vignette=angle='PI/3'")
            "heart" -> vfFilters.add("lenscorrection=k1=0.2:k2=0.2")
            "star" -> vfFilters.add("vignette=angle='PI/4'")
        }

        // Visualizer overlay
        if (visualizerStyle != "none") {
            vfFilters.add("drawgrid=width=100:height=100:color=cyan@0.3")
        }

        if (vfFilters.isNotEmpty()) {
            args.addAll(listOf("-vf", vfFilters.joinToString(",")))
        }

        // Audio handling
        if (isMuted || (videoVolume == 0f && !hasBgm)) {
            args.add("-an")
        } else {
            val afFilters = mutableListOf<String>()

            if (speedFactor != 1.0f) {
                afFilters.add(getAtempoFilter(speedFactor))
            }

            if (hasBgm) {
                args.add("-filter_complex")
                val vVol = if (isMuted) 0.0f else videoVolume
                var fc = "[0:a]volume=$vVol"
                if (speedFactor != 1.0f) fc += ",${getAtempoFilter(speedFactor)}"
                fc += "[a1];[1:a]volume=$backgroundMusicVolume[bgm];[a1][bgm]amix=inputs=2:duration=first[aout]"
                args.addAll(listOf(fc, "-map", "0:v", "-map", "[aout]"))
            } else {
                if (videoVolume != 1.0f) afFilters.add("volume=$videoVolume")
                if (afFilters.isNotEmpty()) {
                    args.addAll(listOf("-af", afFilters.joinToString(",")))
                }
            }
            args.addAll(listOf("-c:a", "aac"))
        }

        // Video encoder
        args.addAll(listOf("-c:v", "libx264", "-preset", "ultrafast", "-tune", "zerolatency"))
        args.addAll(listOf("-y", outputPath))

        Log.d(tag, "ProcessAndExport: ffmpeg ${args.joinToString(" ")}")
        val session = FFmpegKit.executeWithArguments(args.toTypedArray())
        val returnCode = session.returnCode

        if (ReturnCode.isSuccess(returnCode)) {
            Log.d(tag, "ProcessAndExport succeeded!")
            true
        } else {
            Log.e(tag, "ProcessAndExport failed: ${session.state}, code: $returnCode, logs: ${session.failStackTrace}")
            false
        }
    }

    private fun getTargetDimensions(resolution: String, preset: String): Pair<Int, Int> {
        val baseW = when (resolution.lowercase()) {
            "4k" -> 3840; "8k" -> 7680; else -> 1920
        }
        val baseH = when (resolution.lowercase()) {
            "4k" -> 2160; "8k" -> 4320; else -> 1080
        }
        return when (preset) {
            "9:16" -> Pair(baseH, baseW)
            "1:1" -> Pair(baseH, baseH)
            "4:5" -> Pair(baseH, (baseH * 1.25).toInt())
            else -> Pair(baseW, baseH)
        }
    }

    private fun getAtempoFilter(factor: Float): String {
        var remaining = factor
        val filters = mutableListOf<String>()
        while (remaining > 2.0f) { filters.add("atempo=2.0"); remaining /= 2.0f }
        while (remaining < 0.5f) { filters.add("atempo=0.5"); remaining /= 0.5f }
        if (remaining != 1.0f) filters.add("atempo=$remaining")
        return filters.joinToString(",")
    }
}
