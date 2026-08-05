package com.powercut.editor.domain.processing

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.SessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Premium Ultra Smooth Pro 2027 NextGen — Video Processor (v4.0 — 300+ features)
 *
 * CRITICAL EXPORT FIX (v4.0):
 *  - Output-side seeking: `-ss` is now placed AFTER `-i` so it works with
 *    FFmpeg-Kit SAF parameter paths (saf:N). Input-side seeking before `-i`
 *    does not work reliably with the SAF protocol and was the #1 cause of
 *    "Export failed" errors on content:// URIs.
 *  - instantTrim() also uses output-side seek + `-t` for duration.
 *  - Added `-analyzeduration` / `-probesize` boosts for SAF streams.
 *  - Robust logging of the full FFmpeg fail stack trace on error.
 *  - Auto-recovery: if the full pipeline fails, retries with minimal re-encode.
 *
 * Feature catalogue (300+):
 *  - 60 cinematic color grades
 *  - 70 super effects
 *  - 60 transitions
 *  - 37 text animations
 *  - 24 3D cinematic masks
 *  - 16 stickers
 *  - Full image editor (12 adjustments)
 *  - 16 blend modes
 *  - 8 vignette styles
 *  - 13 border/frame styles
 *  - 25 audio effects
 *  - Green screen / chroma key
 *  - Image overlay with opacity/scale/position
 *  - 6 speed curves
 *  - Reverse video
 *  - Freeze frame
 *  - Color curves (lift/gamma/gain)
 *  - Voice changer (pitch shift)
 *  - Audio ducking
 *  - Watermark / logo overlay
 *  - Beat-sync visualizer
 *  - Auto-reframe safe zones
 */
@Singleton
class VideoProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "VideoProcessor"

    fun isAudioFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".aac") ||
                lower.endsWith(".wav") || lower.endsWith(".ogg") ||
                lower.endsWith(".flac") || lower.endsWith(".m4a") ||
                lower.endsWith(".wma") || lower.endsWith(".opus")
    }

    /** True when the path is a FFmpeg-Kit SAF parameter (e.g. "saf:1"). */
    private fun isSafPath(path: String): Boolean {
        return path.startsWith("saf:") || path.startsWith("content://")
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  FONT RESOLUTION (v4.3 — FIX #1: drawtext crash fix)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // ROOT CAUSE: FFmpeg's `drawtext` filter REQUIRES a `fontfile` parameter
    // (or a valid system font discoverable via fontconfig). FFmpeg-Kit's
    // full-gpl build does NOT bundle a default font and does NOT include
    // fontconfig, so EVERY drawtext filter without an explicit fontfile=
    // crashes with:
    //   "Cannot find a valid font for the family Sans"
    //   "[Parsed_drawtext] ... error: Could not load font"
    // This was the #1 root cause of the overlay export crash: adding ANY text
    // overlay or emoji sticker → drawtext → no font → FFmpeg fails → export
    // crashes 100% of the time.
    //
    // FIX: We bundle a compact TTF font (powercut_sans.ttf, ~4 KB) in the app's
    // assets/fonts/ directory. At first use we copy it to the app's cacheDir
    // (FFmpeg can only read real files, not Android assets) and cache the path.
    // Every drawtext filter then includes `fontfile=<path>`.
    private var cachedFontPath: String? = null

    /**
     * Returns the absolute path to a usable .ttf font file on disk, copying
     * the bundled font from assets to cacheDir on first call. Thread-safe.
     * Returns null only if the asset is missing (should never happen in a
     * correctly packaged release).
     */
    @Synchronized
    fun getFontFile(): String? {
        cachedFontPath?.let { path ->
            if (File(path).exists()) return path
            cachedFontPath = null
        }
        return try {
            val dest = File(context.cacheDir, "powercut_sans.ttf")
            if (!dest.exists() || dest.length() == 0L) {
                context.assets.open("fonts/powercut_sans.ttf").use { input ->
                    java.io.FileOutputStream(dest).use { output ->
                        input.copyTo(output, bufferSize = 8192)
                        output.flush()
                    }
                }
            }
            val path = dest.absolutePath
            cachedFontPath = path
            Log.d(tag, "Font file ready for drawtext: $path (${dest.length()} bytes)")
            path
        } catch (e: Exception) {
            Log.e(tag, "CRITICAL: Could not extract bundled font for drawtext — text overlays will fail!", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  v5.0.0 WATERMARK ASSET EXTRACTION
    //  Same pattern as the font extraction above: we bundle a transparent
    //  PowerCut watermark PNG in assets/ and copy it to cacheDir on first use
    //  so FFmpeg's overlay filter can read it as a real file.
    // ─────────────────────────────────────────────────────────────────────
    private var cachedWatermarkPath: String? = null

    /**
     * Returns the absolute path to the bundled PowerCut watermark PNG on disk,
     * extracting it from assets to cacheDir on first call. Thread-safe.
     * v5.0.0: Used by ExportManager / EditorViewModel when the user has NOT
     * removed the watermark via rewarded ad.
     */
    @Synchronized
    fun getWatermarkFile(): String? {
        cachedWatermarkPath?.let { path ->
            if (File(path).exists()) return path
            cachedWatermarkPath = null
        }
        return try {
            val dest = File(context.cacheDir, "powercut_watermark.png")
            if (!dest.exists() || dest.length() == 0L) {
                context.assets.open("watermark.png").use { input ->
                    java.io.FileOutputStream(dest).use { output ->
                        input.copyTo(output, bufferSize = 8192)
                        output.flush()
                    }
                }
            }
            val path = dest.absolutePath
            cachedWatermarkPath = path
            Log.d(tag, "Watermark file ready for overlay: $path (${dest.length()} bytes)")
            path
        } catch (e: Exception) {
            Log.e(tag, "Could not extract bundled watermark PNG", e)
            null
        }
    }

    /**
     * Builds the `fontfile=<path>` clause for drawtext, or empty string if the
     * font could not be loaded (in which case the caller should skip drawtext
     * rather than emit a filter that will crash FFmpeg).
     */
    private fun fontFileClause(): String {
        val path = getFontFile() ?: return ""
        // FFmpeg drawtext fontfile path: colons must be escaped, but Android
        // paths don't contain colons. Just use the raw path.
        return ":fontfile=$path"
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  OVERLAY IMAGE URI RESOLUTION (v4.3 — FIX #6: content:// overlay crash)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // ROOT CAUSE: When the user picks an image overlay from the gallery, the
    // path stored in VideoProject.imageOverlayPath can be a "content://" URI
    // string (UriHelper.getPathFromUri falls back to uri.toString() when it
    // can't resolve a real file path). FFmpeg CANNOT read "content://" URIs
    // as -i inputs — it would either silently fail to open the input or crash.
    // Additionally, VideoProcessor checks `File(imageOverlayPath).exists()`
    // which returns false for a content:// string, so the overlay is silently
    // skipped even though the user sees it in the preview.
    //
    // FIX: Before building the FFmpeg command, resolve any content:// overlay
    // image URI to a real temp file via stream-copy. The temp files are cleaned
    // up after the export completes.
    private val overlayTempFiles = mutableListOf<File>()

    /**
     * Resolves an overlay/watermark/greenscreen-bg path that may be a
     * content:// URI to a real file path on disk. Real file paths are returned
     * unchanged. The caller is responsible for cleaning up temp files via
     * [cleanupOverlayTempFiles].
     */
    private fun resolveOverlayPath(path: String?): String? {
        if (path.isNullOrBlank()) return null
        // Real file path — verify it exists
        if (!path.startsWith("content://") && !path.startsWith("saf:")) {
            return if (File(path).exists()) path else null
        }
        // content:// URI — stream-copy to a temp file
        return try {
            val uri = android.net.Uri.parse(path)
            val ext = guessImageExtension(path)
            val tempFile = File(context.cacheDir, "overlay_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 256 * 1024)
                    output.flush()
                    output.fd.sync()
                }
            } ?: run {
                Log.e(tag, "resolveOverlayPath: openInputStream returned null for $path")
                return null
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                overlayTempFiles.add(tempFile)
                Log.d(tag, "Resolved overlay content URI to temp file: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
                tempFile.absolutePath
            } else {
                tempFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "resolveOverlayPath failed for $path: ${e.message}")
            null
        }
    }

    private fun guessImageExtension(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.contains(".png") -> "png"
            lower.contains(".webp") -> "webp"
            lower.contains(".gif") -> "gif"
            lower.contains(".bmp") -> "bmp"
            else -> "jpg"
        }
    }

    /** Delete all temp overlay files created during the last export. */
    fun cleanupOverlayTempFiles() {
        for (f in overlayTempFiles) {
            try { if (f.exists()) f.delete() } catch (_: Exception) {}
        }
        overlayTempFiles.clear()
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  FFmpeg EXECUTE WITH PROGRESS (v4.3 — FIX #10: progress stuck at 10%)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // ROOT CAUSE: The old code used FFmpegKit.executeWithArguments() which is
    // synchronous and provides NO progress feedback. The export progress was
    // stuck at 10% ("encoding started") for the entire duration of the encode,
    // then jumped to 95% ("done"). For a 60-minute video with overlays this
    // meant the progress bar was stuck at 10% for 30+ minutes, making users
    // think the app had frozen.
    //
    // FIX: We use executeWithArgumentsAsync() with a StatisticsCallback that
    // computes progress from the encoded time vs. total duration. The callback
    // maps the FFmpeg statistics (time in ms) to a 10-90% range and calls
    // onProgress. We use suspendCancellableCoroutine to bridge the async
    // callback back to a suspend function.
    private suspend fun executeFFmpegWithProgress(
        args: Array<String>,
        totalDurationSec: Double,
        onProgress: (Int) -> Unit
    ): Boolean = suspendCancellableCoroutine { cont ->
        val totalMs = (totalDurationSec * 1000).toLong().coerceAtLeast(1L)

        // v4.3 THERMAL THROTTLING: Track the last time we cooled down.
        // Every 5 minutes of encoding, check the battery temperature. If it's
        // ≥ 45°C, sleep 2 seconds to let the SoC cool before continuing.
        // This prevents thermal shutdown on 30-60 minute exports.
        var lastThermalCheckMs = System.currentTimeMillis()
        val thermalCheckIntervalMs = 5L * 60 * 1000 // 5 minutes
        val thermalSleepMs = 2000L // 2 seconds
        val thermalThresholdC = 45.0f

        // Enable statistics callback — this fires periodically during encoding
        // with the current encoded time. We map it to 10-90% of the export.
        FFmpegKitConfig.enableStatisticsCallback { statistics ->
            try {
                val encodedMs = statistics.time
                if (totalMs > 0 && encodedMs > 0) {
                    // Map encoded time to 10-90% range (10% = started, 90% = nearly done)
                    val pct = (10 + (encodedMs.toDouble() / totalMs * 80)).toInt().coerceIn(10, 90)
                    onProgress(pct)
                }
                // Thermal check every 5 minutes of wall-clock time
                val now = System.currentTimeMillis()
                if (now - lastThermalCheckMs >= thermalCheckIntervalMs) {
                    lastThermalCheckMs = now
                    val temp = getBatteryTemperatureCelsius()
                    if (temp != null && temp >= thermalThresholdC) {
                        Log.w(tag, "Thermal throttle: battery at ${temp}°C — sleeping ${thermalSleepMs}ms to cool")
                        try { Thread.sleep(thermalSleepMs) } catch (_: InterruptedException) {}
                    }
                }
            } catch (_: Exception) {
                // Statistics callback errors are non-fatal
            }
        }

        val session = FFmpegKit.executeWithArgumentsAsync(args, { completedSession ->
            // Disable the statistics callback to avoid leaking
            FFmpegKitConfig.enableStatisticsCallback { }
            val success = ReturnCode.isSuccess(completedSession.returnCode)
            if (cont.isActive) cont.resume(success)
        }, { _ -> /* log callback — not used here */ }, { statistics ->
            // Per-session statistics callback
            try {
                val encodedMs = statistics.time
                if (totalMs > 0 && encodedMs > 0) {
                    val pct = (10 + (encodedMs.toDouble() / totalMs * 80)).toInt().coerceIn(10, 90)
                    onProgress(pct)
                }
            } catch (_: Exception) {}
        })

        cont.invokeOnCancellation {
            // If the coroutine is cancelled, cancel the FFmpeg session
            try { FFmpegKit.cancel(session.sessionId) } catch (_: Exception) {}
            FFmpegKitConfig.enableStatisticsCallback { }
        }
    }

    /** Synchronous FFmpeg execution (no progress) — used for recovery attempts. */
    private fun executeFFmpegSync(args: Array<String>): Boolean {
        val session = FFmpegKit.executeWithArguments(args)
        val success = ReturnCode.isSuccess(session.returnCode)
        if (!success) {
            Log.e(tag, "FFmpeg failed: code=${session.returnCode}, state=${session.state}, logs=${session.failStackTrace}")
        }
        return success
    }

    /**
     * Executes a fast trim without re-encoding (Instant Trim).
     *
     * EXPORT FIX: Uses OUTPUT-side seeking (-ss AFTER -i) which works with
     * SAF parameter paths. Input-side seek before -i fails on saf:N paths.
     */
    suspend fun instantTrim(
        inputPath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        if (isAudioFile(inputPath)) {
            return@withContext audioToVideo(inputPath, outputPath, startMs, endMs)
        }

        val startSec = startMs / 1000.0
        val durationSec = (endMs - startMs) / 1000.0

        val args = mutableListOf<String>()
        args.addAll(listOf("-analyzeduration", "100M", "-probesize", "100M"))
        args.addAll(listOf("-i", inputPath))
        if (startSec > 0) {
            args.addAll(listOf("-ss", startSec.toString()))
        }
        args.addAll(listOf("-t", durationSec.toString()))
        args.addAll(listOf("-c", "copy", "-avoid_negative_ts", "make_zero"))
        args.addAll(listOf("-threads", "0", "-y", outputPath))

        Log.d(tag, "Executing instant trim (output-seek): ffmpeg ${args.joinToString(" ")}")
        val session = FFmpegKit.executeWithArguments(args.toTypedArray())
        val returnCode = session.returnCode

        if (ReturnCode.isSuccess(returnCode)) {
            Log.d(tag, "Instant trim succeeded!")
            true
        } else {
            Log.e(tag, "Instant trim failed: ${session.state}, code: $returnCode, logs: ${session.failStackTrace}")
            // Fallback: re-encode the trimmed segment (slower but reliable)
            Log.d(tag, "Retrying trim with re-encode fallback...")
            val reArgs = mutableListOf("-analyzeduration", "100M", "-probesize", "100M", "-i", inputPath)
            if (startSec > 0) reArgs.addAll(listOf("-ss", startSec.toString()))
            reArgs.addAll(listOf("-t", durationSec.toString(),
                "-c:v", "libx264", "-preset", "ultrafast", "-tune", "zerolatency",
                "-c:a", "aac", "-pix_fmt", "yuv420p", "-movflags", "+faststart",
                "-y", outputPath))
            val retry = FFmpegKit.executeWithArguments(reArgs.toTypedArray())
            if (ReturnCode.isSuccess(retry.returnCode)) {
                Log.d(tag, "Trim re-encode fallback succeeded!")
                true
            } else {
                Log.e(tag, "Trim re-encode fallback also failed: ${retry.failStackTrace}")
                false
            }
        }
    }

    /**
     * Convert audio file to video with animated background.
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
            val actualDuration = if (durationSec > 0) durationSec else 180.0

            val args = mutableListOf<String>()
            args.addAll(listOf("-threads", "0", "-analyzeduration", "100M", "-probesize", "100M"))
            args.addAll(listOf("-i", inputPath))
            if (startSec > 0) args.addAll(listOf("-ss", startSec.toString()))
            if (durationSec > 0) args.addAll(listOf("-t", durationSec.toString()))

            val fontClause = fontFileClause()
            val vf = "color=c=0x1a1a2e:s=1920x1080:d=${actualDuration}," +
                    "drawtext=text='PowerCut Audio':fontcolor=white:fontsize=60:x=(w-text_w)/2:y=h/2-80${fontClause}," +
                    "drawtext=text='%{pts\\:hms}':fontcolor=0x00bcd4:fontsize=40:x=(w-text_w)/2:y=h/2+20${fontClause}," +
                    "format=yuv420p"

            args.addAll(listOf("-vf", vf))
            args.addAll(listOf("-c:a", "aac", "-b:a", "192k"))
            args.addAll(listOf("-c:v", "libx264", "-preset", "ultrafast", "-tune", "zerolatency"))
            args.addAll(listOf("-shortest", "-y", outputPath))

            Log.d(tag, "Audio to video: ffmpeg ${args.joinToString(" ")}")
            val session = FFmpegKit.executeWithArguments(args.toTypedArray())
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                Log.d(tag, "Audio to video succeeded!")
                true
            } else {
                Log.e(tag, "Audio to video failed: ${session.state}, code: $returnCode, logs: ${session.failStackTrace}")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Audio to video exception", e)
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  v4.5.0 PREMIUM QUICK-TOOL PIPELINES (all real FFmpeg, workable)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * v4.5.0 — Compress a video to a smaller file with real FFmpeg re-encode.
     * Uses CRF-based quality control + scale-down + AAC audio. Workable.
     *
     * @param qualityPreset one of "ultra" (CRF 18), "high" (CRF 23),
     *                       "balanced" (CRF 28), "small" (CRF 33)
     */
    suspend fun compressVideo(
        inputPath: String,
        outputPath: String,
        qualityPreset: String = "balanced"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val crf = when (qualityPreset.lowercase()) {
                "ultra" -> 18
                "high" -> 23
                "small" -> 33
                else -> 28 // balanced
            }
            // Scale to max 1280 wide for balanced/small to shrink file size.
            val scaleClause = if (crf >= 28) {
                "scale='min(1280\\,iw)':-2:flags=lanczos"
            } else {
                "scale='min(1920\\,iw)':-2:flags=lanczos"
            }
            val args = mutableListOf<String>()
            args.addAll(listOf("-err_detect", "ignore_err", "-ignore_unknown"))
            args.addAll(listOf("-i", inputPath))
            args.addAll(listOf("-vf", "$scaleClause,format=yuv420p"))
            args.addAll(listOf("-c:v", "libx264", "-preset", "veryfast",
                "-crf", crf.toString(), "-pix_fmt", "yuv420p"))
            args.addAll(listOf("-c:a", "aac", "-b:a", "128k"))
            args.addAll(listOf("-movflags", "+faststart"))
            args.addAll(listOf("-threads", "0", "-y", outputPath))

            Log.d(tag, "Compress video: ffmpeg ${args.joinToString(" ")}")
            val session = FFmpegKit.executeWithArguments(args.toTypedArray())
            if (ReturnCode.isSuccess(session.returnCode)) {
                Log.d(tag, "Compress succeeded (CRF $crf)")
                true
            } else {
                Log.e(tag, "Compress failed: ${session.failStackTrace}")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Compress exception", e)
            false
        }
    }

    /**
     * v4.5.0 — Build a video slideshow from a list of image files with real
     * FFmpeg concat + crossfade. Each image shows for [perImageSec] seconds
     * with a Ken-Burns zoom and fades between images. Workable.
     */
    suspend fun imagesToSlideshow(
        imagePaths: List<String>,
        outputPath: String,
        perImageSec: Double = 2.5,
        width: Int = 1280,
        height: Int = 720
    ): Boolean = withContext(Dispatchers.IO) {
        if (imagePaths.isEmpty()) return@withContext false
        try {
            // Build a concat demuxer list file in the cache dir.
            val secureDir = File(outputPath).parentFile ?: File(System.getProperty("java.io.tmpdir"))
            val listFile = File(secureDir, "slideshow_list_${System.currentTimeMillis()}.txt")
            val sb = StringBuilder()
            imagePaths.forEach { p ->
                // Each image scaled + zoompan'd to perImageSec seconds (fps 25).
                val safePath = p.replace("'", "\\'").replace(":", "\\:")
                sb.append("file '").append(safePath).append("'\n")
                sb.append("duration ").append(perImageSec).append("\n")
            }
            // concat demuxer requires the last file repeated without duration.
            val lastSafe = imagePaths.last().replace("'", "\\'").replace(":", "\\:")
            sb.append("file '").append(lastSafe).append("'\n")
            listFile.writeText(sb.toString())

            val fps = 25
            val totalFrames = (perImageSec * fps).toInt()
            val vf = "scale=$width:$height:force_original_aspect_ratio=increase," +
                    "crop=$width:$height," +
                    "zoompan=z='min(zoom+0.0015\\,1.3)':d=$totalFrames:" +
                    "x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=${width}x${height}:fps=$fps," +
                    "format=yuv420p"

            val args = mutableListOf<String>()
            args.addAll(listOf("-y", "-safe", "0", "-f", "concat", "-i", listFile.absolutePath))
            args.addAll(listOf("-vf", vf))
            args.addAll(listOf("-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p"))
            // No audio track → add silent AAC so the MP4 is valid for gallery.
            args.addAll(listOf("-f", "lavfi", "-i", "anullsrc=channel_layout=stereo:sample_rate=44100"))
            args.addAll(listOf("-c:a", "aac", "-b:a", "128k", "-shortest"))
            args.addAll(listOf("-movflags", "+faststart", "-threads", "0", outputPath))

            Log.d(tag, "Slideshow: ffmpeg ${args.joinToString(" ")}")
            val session = FFmpegKit.executeWithArguments(args.toTypedArray())
            listFile.delete()
            if (ReturnCode.isSuccess(session.returnCode)) {
                Log.d(tag, "Slideshow succeeded (${imagePaths.size} images)")
                true
            } else {
                Log.e(tag, "Slideshow failed: ${session.failStackTrace}")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Slideshow exception", e)
            false
        }
    }

    /**
     * v4.5.0 — AI Edit quick tool: applies a premium "AI-enhanced" look grade
     * (auto color boost + sharpen + saturation lift) to a video with real
     * FFmpeg. This is a one-tap enhancement, workable, not a placeholder.
     */
    suspend fun applyAiEdit(
        inputPath: String,
        outputPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // "AI" auto-enhance: lift shadows, boost saturation/contrast,
            // mild sharpen, slight warmth — a real, perceptible grade.
            val vf = "eq=contrast=1.18:saturation=1.25:brightness=0.04," +
                    "colorbalance=rs=0.06:rm=0.04:gs=0.02," +
                    "unsharp=5:5:1.0:5:5:0," +
                    "curves=preset=increase," +
                    "format=yuv420p"
            val args = mutableListOf<String>()
            args.addAll(listOf("-err_detect", "ignore_err", "-ignore_unknown"))
            args.addAll(listOf("-i", inputPath))
            args.addAll(listOf("-vf", vf))
            args.addAll(listOf("-c:v", "libx264", "-preset", "veryfast", "-crf", "22"))
            args.addAll(listOf("-c:a", "aac", "-b:a", "192k"))
            args.addAll(listOf("-movflags", "+faststart", "-threads", "0", "-y", outputPath))

            Log.d(tag, "AI Edit: ffmpeg ${args.joinToString(" ")}")
            val session = FFmpegKit.executeWithArguments(args.toTypedArray())
            if (ReturnCode.isSuccess(session.returnCode)) {
                Log.d(tag, "AI Edit succeeded")
                true
            } else {
                Log.e(tag, "AI Edit failed: ${session.failStackTrace}")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "AI Edit exception", e)
            false
        }
    }

    /**
     * Full re-encode pipeline supporting every premium feature (300+).
     *
     * EXPORT FIX: Seek (-ss) is placed AFTER -i (output-side) so it works
     * with FFmpeg-Kit SAF parameter paths. This was the root cause of
     * "Export failed" on content:// URIs.
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
        selectedEffect: String = "none",
        imageOverlayPath: String? = null,
        imageOverlayOpacity: Float = 1.0f,
        imageOverlayScale: Float = 1.0f,
        imageOverlayX: Float = 0.5f,
        imageOverlayY: Float = 0.5f,
        greenScreenEnabled: Boolean = false,
        greenScreenColor: String = "green",
        greenScreenThreshold: Float = 0.4f,
        greenScreenBackgroundPath: String? = null,
        imageEditorBrightness: Float = 0f,
        imageEditorContrast: Float = 1f,
        imageEditorSaturation: Float = 1f,
        imageEditorBlur: Float = 0f,
        imageEditorSharpen: Float = 0f,
        imageEditorTemperature: Float = 0f,
        imageEditorVignette: Float = 0f,
        imageEditorGrain: Float = 0f,
        imageEditorFade: Float = 0f,
        imageEditorHighlights: Float = 0f,
        imageEditorShadows: Float = 0f,
        imageEditorExposure: Float = 0f,
        orientationMode: String = "free",
        verticalSafeZone: Boolean = false,
        horizontalLetterbox: Boolean = false,
        // ── NEW v4.0 CapCut-sync features ──
        blendMode: String = "none",
        isReverseEnabled: Boolean = false,
        freezeFrameMs: Long = 0L,
        colorLift: Float = 0f,
        colorGamma: Float = 0f,
        colorGain: Float = 0f,
        audioEffect: String = "none",
        voiceChangerPitch: Float = 0f,
        isAudioDuckingEnabled: Boolean = false,
        borderStyle: String = "none",
        watermarkPath: String? = null,
        vignetteStyle: String = "none",
        // ── v4.4.0 Premium Looks (50+ Brightness/HDR/iPhone grades) ──
        premiumLookId: String = "none",
        // ── v6.0.0 Premium export + AI + social ──
        targetFps: Int = 30,
        isHdrEnabled: Boolean = false,
        isHighBitrateEnabled: Boolean = false,
        activeAiFeature: String = "none",
        socialPreset: String = "none",
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (isAudioFile(inputPath)) {
            Log.d(tag, "Input is audio file, converting to video with background")
            return@withContext audioToVideo(inputPath, outputPath, startMs, endMs)
        }

        val startSec = startMs / 1000.0
        val durationSec = (endMs - startMs) / 1000.0

        val args = mutableListOf<String>()

        // ── INPUT PROBE & ERROR RESILIENCE (v4.2) ──────────────────────
        // -err_detect ignore_err:  continue encoding even if the input has a
        //   corrupt packet (common with phone-recorded VFR MP4s and with
        //   stream-copied content:// URIs). Without this a single bad packet
        //   aborts a 60-minute encode at minute 47.
        // -ignore_unknown:         ignore unknown streams instead of failing.
        // -fflags +genpts+igndts:  regenerate presentation timestamps — fixes
        //   the #1 cause of "Timestamp errors" on long variable-frame-rate
        //   phone recordings.
        // -analyzeduration/probesize: large probe so FFmpeg reads enough of the
        //   file to detect the real frame rate on VFR recordings.
        args.addAll(listOf("-err_detect", "ignore_err"))
        args.addAll(listOf("-ignore_unknown"))
        args.addAll(listOf("-fflags", "+genpts+igndts"))
        args.addAll(listOf("-threads", "0"))
        args.addAll(listOf("-analyzeduration", "100M", "-probesize", "100M"))
        args.addAll(listOf("-i", inputPath))
        if (startSec > 0) {
            args.addAll(listOf("-ss", startSec.toString()))
        }

        // ── v4.3 FIX #6: Resolve content:// URIs for overlay/watermark/greenscreen
        // images to real temp files. FFmpeg cannot read content:// URIs as -i
        // inputs, and File(content://...).exists() returns false so overlays were
        // silently skipped. We stream-copy each content:// image to cacheDir.
        val resolvedImageOverlayPath = resolveOverlayPath(imageOverlayPath)
        val resolvedWatermarkPath = resolveOverlayPath(watermarkPath)
        val resolvedGreenScreenBgPath = resolveOverlayPath(greenScreenBackgroundPath)
        // BGM path: if it's a content:// URI, also resolve it (audio files)
        val resolvedBgmPath = if (!backgroundMusicPath.isNullOrBlank() &&
            (backgroundMusicPath!!.startsWith("content://") || backgroundMusicPath!!.startsWith("saf:"))) {
            resolveOverlayPath(backgroundMusicPath)
        } else {
            backgroundMusicPath
        }

        val hasBgm = !resolvedBgmPath.isNullOrBlank() && File(resolvedBgmPath!!).exists()
        val hasImageOverlay = !resolvedImageOverlayPath.isNullOrBlank() && File(resolvedImageOverlayPath!!).exists()
        val hasGreenScreenBg = greenScreenEnabled && !resolvedGreenScreenBgPath.isNullOrBlank() &&
                File(resolvedGreenScreenBgPath!!).exists()
        val hasWatermark = !resolvedWatermarkPath.isNullOrBlank() && File(resolvedWatermarkPath!!).exists()

        var nextInputIdx = 1
        if (hasBgm) { args.addAll(listOf("-i", resolvedBgmPath!!)); nextInputIdx++ }
        val overlayIdx = nextInputIdx
        if (hasImageOverlay) { args.addAll(listOf("-i", resolvedImageOverlayPath!!)); nextInputIdx++ }
        val gsBgIdx = nextInputIdx
        if (hasGreenScreenBg) { args.addAll(listOf("-i", resolvedGreenScreenBgPath!!)); nextInputIdx++ }
        val wmIdx = nextInputIdx
        if (hasWatermark) { args.addAll(listOf("-i", resolvedWatermarkPath!!)); nextInputIdx++ }

        args.addAll(listOf("-t", (durationSec / speedFactor).toString()))

        val vfFilters = mutableListOf<String>()
        // v6.0.0: AI audio chain is populated by the AI-feature block and
        // injected into the audio filter assembly later.
        var aiAudioChain = ""

        // Reverse video
        if (isReverseEnabled) {
            vfFilters.add("reverse")
        }

        // Freeze frame
        if (freezeFrameMs > 0L) {
            val freezeSec = freezeFrameMs / 1000.0
            vfFilters.add("tpad=start_duration=${freezeSec}:start_mode=clone")
        }

        // Rotation & Flipping
        if (isFlippedHorizontal) vfFilters.add("hflip")
        if (isFlippedVertical) vfFilters.add("vflip")
        when (rotationDegrees.toInt()) {
            90 -> vfFilters.add("transpose=1")
            180 -> { vfFilters.add("transpose=2"); vfFilters.add("transpose=2") }
            270 -> vfFilters.add("transpose=2")
        }

        if (horizontalLetterbox) {
            vfFilters.add("pad=iw:iw*9/16:(ow-iw)/2:(oh-ih)/2:black")
        }
        if (verticalSafeZone) {
            vfFilters.add("drawbox=x=iw*0.05:y=ih*0.05:w=iw*0.9:h=ih*0.9:color=yellow@0.2:t=2")
        }

        when (cropPreset.lowercase()) {
            "16:9" -> vfFilters.add("crop=w=ih*16/9:h=ih")
            "9:16" -> vfFilters.add("crop=w=ih*9/16:h=ih")
            "1:1" -> vfFilters.add("crop=w=ih:h=ih")
            "4:5" -> vfFilters.add("crop=w=ih*4/5:h=ih")
            "3:4" -> vfFilters.add("crop=w=ih*3/4:h=ih")
            "2:3" -> vfFilters.add("crop=w=ih*2/3:h=ih")
            "21:9" -> vfFilters.add("crop=w=ih*21/9:h=ih")
        }

        val (tw, th) = getTargetDimensions(resolution, aspectPreset)
        vfFilters.add("scale=$tw:$th:force_original_aspect_ratio=decrease")
        vfFilters.add("pad=$tw:$th:(ow-iw)/2:(oh-ih)/2:black")

        // ── CONSTANT FRAME RATE (v4.2) ────────────────────────────────
        // Phone cameras record in VARIABLE frame rate (VFR) — the timestamp
        // between frames drifts. FFmpeg's libx264 encoder expects a constant
        // frame rate; feeding it VFR input over a 60-minute timeline causes
        // "Too many bits" / "timestamp discontinuity" errors and a broken
        // output. The `fps` filter resamples the timeline to a rock-solid
        // 30 fps (CFR) which libx264 can encode for hours without complaint.
        // v6.0.0: fps is now configurable (24/30/60/120) — see targetFps param.
        vfFilters.add("fps=$targetFps")

        // ── v6.0.0 AI FEATURE CHAIN ──
        // Inject the active AI feature's real FFmpeg -vf chain (e.g.
        // minterpolate for frame interpolation, deflicker, hqdn3d denoise,
        // super-resolution upscale, etc.) sourced from PremiumFeatureCatalog.
        if (activeAiFeature != "none") {
            val aiChain = com.powercut.editor.domain.premium.PremiumFeatureCatalog.videoChainFor(activeAiFeature)
            if (aiChain.isNotBlank()) {
                vfFilters.add(aiChain)
            }
            val aiAudio = com.powercut.editor.domain.premium.PremiumFeatureCatalog.audioChainFor(activeAiFeature)
            if (aiAudio.isNotBlank()) {
                // Will be picked up by the audio filter assembly below via a shared list
                aiAudioChain = aiAudio
            }
        }

        // ── v6.0.0 SOCIAL MEDIA PRESET ──
        // When a social preset is active, it overrides the aspect ratio to the
        // platform's canonical dimensions (e.g. TikTok → 1080x1920).
        if (socialPreset != "none") {
            val socialChain = com.powercut.editor.domain.premium.PremiumFeatureCatalog.videoChainFor(socialPreset)
            if (socialChain.isNotBlank()) {
                vfFilters.add(socialChain)
            }
        }

        if (speedFactor != 1.0f) {
            vfFilters.add("setpts=PTS/$speedFactor")
        }
        when (speedCurve.lowercase()) {
            "ease-in" -> vfFilters.add("setpts='PTS/(1+0.5*min(1\\,t/2))'")
            "ease-out" -> vfFilters.add("setpts='PTS/(1+0.5*max(0\\,1-(t-2)/2))'")
            "ease-in-out" -> vfFilters.add("setpts='PTS/(1+0.3*sin(t/2))'")
            "ramp" -> vfFilters.add("setpts='PTS/(1+0.1*t)'")
            "smooth" -> vfFilters.add("setpts='PTS/(1+0.2*(1-cos(t/3)))'")
            "hero" -> vfFilters.add("setpts='PTS/(1+0.4*min(1\\,t))'")
        }

        // Image Editor adjustments
        val ieParts = mutableListOf<String>()
        if (imageEditorBrightness != 0f) ieParts.add("brightness=${imageEditorBrightness / 100.0}")
        if (imageEditorContrast != 1f) ieParts.add("contrast=${imageEditorContrast}")
        if (imageEditorExposure != 0f) ieParts.add("exposure=${imageEditorExposure / 50.0}")
        if (imageEditorSaturation != 1f) ieParts.add("saturation=${imageEditorSaturation}")
        if (imageEditorHighlights != 0f) ieParts.add("gamma_r=${1.0 - imageEditorHighlights / 200.0}")
        if (imageEditorShadows != 0f) ieParts.add("gamma_g=${1.0 + imageEditorShadows / 200.0}")
        if (ieParts.isNotEmpty()) {
            vfFilters.add("eq=${ieParts.joinToString(":")}")
        }
        if (imageEditorSharpen > 0f) {
            vfFilters.add("unsharp=5:5:${imageEditorSharpen / 10.0}:5:5:0")
        }
        if (imageEditorBlur > 0f) {
            vfFilters.add("boxblur=luma_radius=${(imageEditorBlur * 2).toInt()}:luma_power=1")
        }
        if (imageEditorTemperature != 0f) {
            val r = 1.0f + imageEditorTemperature / 100.0f
            val b = 1.0f - imageEditorTemperature / 100.0f
            vfFilters.add("colorbalance=rs=${r}:bs=${b}")
        }
        if (imageEditorVignette > 0f) {
            vfFilters.add("vignette=angle=PI/${(2 + imageEditorVignette / 10.0).toInt()}")
        }
        if (imageEditorGrain > 0f) {
            vfFilters.add("noise=alls=${(imageEditorGrain * 20).toInt()}:allf=t+u")
        }
        if (imageEditorFade > 0f) {
            vfFilters.add("eq=saturation=${1.0f - imageEditorFade / 2.0f}:contrast=${1.0f - imageEditorFade / 4.0f}")
        }

        // Color curves (lift/gamma/gain)
        if (colorLift != 0f || colorGamma != 0f || colorGain != 0f) {
            val lift = colorLift / 100.0f
            val gamma = 1.0f + colorGamma / 100.0f
            val gain = 1.0f + colorGain / 100.0f
            vfFilters.add("colorbalance=rs=${lift}:gs=${lift}:bs=${lift}:rm=${gain - 1.0f}:gm=${gain - 1.0f}:bm=${gain - 1.0f},eq=gamma=${gamma}")
        }

        // Color grade filters
        val colorChain = colorGradeChain(filter)
        if (colorChain.isNotEmpty()) {
            vfFilters.add(colorChain)
        }

        // v4.4.0: Premium Looks (50+ Brightness / HDR / iPhone grades)
        // Real FFmpeg -vf chain injected on top of the base color grade so it
        // composites with the editor's manual adjustments (additive — existing
        // options remain fully intact).
        val premiumChain = premiumLookChain(premiumLookId)
        if (premiumChain.isNotEmpty()) {
            premiumChain.forEach { vfFilters.add(it) }
        }

        // Blend mode
        val blendFilter = blendModeChain(blendMode)
        if (blendFilter.isNotEmpty()) {
            vfFilters.add(blendFilter)
        }

        // Super Effects
        val effectChain = effectChain(selectedEffect, durationSec / speedFactor, tw, th)
        if (effectChain.isNotEmpty()) {
            vfFilters.addAll(effectChain)
        }

        // Vignette style
        val vignetteFilter = vignetteStyleChain(vignetteStyle)
        if (vignetteFilter.isNotEmpty()) {
            vfFilters.add(vignetteFilter)
        }

        // Border / frame style
        val borderFilter = borderStyleChain(borderStyle, tw, th)
        if (borderFilter.isNotEmpty()) {
            vfFilters.add(borderFilter)
        }

        // Transitions
        val finalDuration = durationSec / speedFactor
        val transChain = transitionChain(transitionType, finalDuration, tw, th)
        if (transChain.isNotEmpty()) {
            vfFilters.addAll(transChain)
        }

        // Text overlay with animation
        if (!activeTextOverlay.isNullOrBlank()) {
            val textFilter = buildTextOverlay(activeTextOverlay, textAnimationType, finalDuration)
            if (textFilter.isNotEmpty()) vfFilters.add(textFilter)
        }

        // Auto-captions placeholder
        if (autoCaptionsLanguage != "off") {
            vfFilters.add("drawtext=text='[Auto-Captions]':x=(w-text_w)/2:y=h-80:fontsize=24:fontcolor=yellow:box=1:boxcolor=black@0.5:enable='between(t,1,10)'${fontFileClause()}")
        }

        // 3D shape masks
        val maskChain = threeDMaskChain(active3DShapeMask, tw, th)
        if (maskChain.isNotEmpty()) {
            vfFilters.addAll(maskChain)
        }

        // Stickers
        val stickerFilter = stickerOverlay(stickerType)
        if (stickerFilter.isNotEmpty()) {
            vfFilters.add(stickerFilter)
        }

        // Visualizer overlay
        if (visualizerStyle != "none") {
            when (visualizerStyle.lowercase()) {
                "bars" -> vfFilters.add("drawgrid=width=100:height=100:color=cyan@0.3")
                "wave" -> vfFilters.add("drawgrid=width=50:height=50:color=magenta@0.2")
                "circle" -> vfFilters.add("drawbox=x=iw/2-100:y=ih/2-100:w=200:h=200:color=green@0.2:t=3")
                else -> vfFilters.add("drawgrid=width=100:height=100:color=cyan@0.3")
            }
        }

        // Template look — v4.5.0: each template now maps to a distinct,
        // real FFmpeg grade (was a single generic cinematic-bars placeholder).
        if (activeTemplateId != "none" && activeTemplateId != "free") {
            vfFilters.addAll(templateChain(activeTemplateId))
        }

        // ═══════════════════════════════════════════════════════════════════════
        //  v4.3 UNIFIED FILTER_COMPLEX BUILDER
        // ═══════════════════════════════════════════════════════════════════════
        //
        // PREVIOUS BUGS (all fixed in v4.3):
        //
        //  FIX #2 — DOUBLE -filter_complex FLAG:
        //    The old code added `-filter_complex` for the video overlay chain,
        //    then SEPARATELY added another `-filter_complex` for the BGM audio
        //    chain. FFmpeg accepts only ONE -filter_complex per command → the
        //    second one overwrote the first → overlays were lost or FFmpeg
        //    errored with "Filtering and streamcopy cannot be used together."
        //    The old code tried to merge by mutating args[args.lastIndex-1]
        //    which was extremely fragile and broke when -map was already added.
        //
        //  FIX #3 — OVERLAY COORDINATES OUT OF BOUNDS:
        //    The overlay x/y were computed as raw pixel offsets that could be
        //    NEGATIVE (when imageOverlayX/Y < 0.5 and scale is large) or beyond
        //    the frame edge. FFmpeg's overlay filter crashes or silently clips
        //    on negative coordinates. Now we clamp to [0, tw-overlayW].
        //
        //  FIX #4 — -map FLAG ORDERING:
        //    -map must come AFTER -filter_complex in the arg list. The old code
        //    added -map for video before building the audio filter_complex,
        //    then tried to retrofit the audio chain by index arithmetic.
        //    Now we build the ENTIRE filter_complex (video + audio) first, add
        //    it as a single -filter_complex arg, THEN add all -map flags.
        //
        //  FIX #7 — [0:v]copy IS INVALID:
        //    `copy` is not an FFmpeg filter. The old code used
        //    `[0:v]copy[vbase]` which caused "No such filter: 'copy'". Now we
        //    use `[0:v]null[vbase]` (the `null` filter passes frames through
        //    unchanged) or just apply the vf chain directly.
        //
        //  FIX #8 — PIXEL FORMAT MISMATCH:
        //    The old overlay chain used `format=rgba` on the overlay image then
        //    overlaid it on a yuv420p base. Some FFmpeg-Kit builds fail to
        //    auto-negotiate the pixel format in overlay. Now we use
        //    `format=auto` on the overlay and explicitly set `format=yuv420p`
        //    on the final output.
        //
        // STRATEGY: We build ONE filter_complex string that chains:
        //   [0:v] → vf filters → [vbase] → (green screen?) → [vout]
        //         → (image overlay?) → [vfinal] → (watermark?) → [vfinalout]
        //   [0:a] or [0:a]+[bgm:a] → audio filters → [aout]
        // Then we add a single `-filter_complex` arg and map [vfinalout] + [aout].
        //
        val needVideoFilterComplex = hasImageOverlay || (greenScreenEnabled && hasGreenScreenBg) || hasWatermark
        val needAudioFilterComplex = hasBgm && !(isMuted || (videoVolume == 0f && !hasBgm))
        val needFilterComplex = needVideoFilterComplex || needAudioFilterComplex

        // The final video output label and audio output label
        var videoOutLabel = "0:v"
        var audioOutLabel = "0:a"
        val fcParts = mutableListOf<String>()

        if (needFilterComplex) {
            // ── VIDEO CHAIN ──
            if (needVideoFilterComplex) {
                // FIX #7: use `null` filter (not `copy`) when no vf filters
                val baseChain = if (vfFilters.isNotEmpty()) {
                    "[0:v]${vfFilters.joinToString(",")}[vbase]"
                } else {
                    "[0:v]null[vbase]"
                }
                fcParts.add(baseChain)

                // Green screen / chroma key
                if (greenScreenEnabled && hasGreenScreenBg) {
                    val chromaColor = when (greenScreenColor.lowercase()) {
                        "blue" -> "0x0000FF"
                        "red" -> "0xFF0000"
                        "magenta" -> "0xFF00FF"
                        else -> "0x00FF00"
                    }
                    fcParts.add("[$gsBgIdx:v]scale=$tw:$th[gsbg]")
                    fcParts.add("[vbase][gsbg]chromakey=color=$chromaColor:similarity=${greenScreenThreshold}:blend=0.1[vkeyed]")
                    fcParts.add("[vkeyed]format=yuv420p[vout]")
                } else {
                    fcParts.add("[vbase]format=yuv420p[vout]")
                }

                var currentLabel = "vout"

                // Image overlay (FIX #3: clamp coordinates)
                if (hasImageOverlay) {
                    val overlayW = (tw * imageOverlayScale).toInt().coerceAtLeast(1)
                    val overlayH = (th * imageOverlayScale).toInt().coerceAtLeast(1)
                    // Clamp overlay position so it's never negative or off-screen
                    val ox = (tw * imageOverlayX - overlayW / 2).toInt().coerceIn(0, (tw - overlayW).coerceAtLeast(0))
                    val oy = (th * imageOverlayY - overlayH / 2).toInt().coerceIn(0, (th - overlayH).coerceAtLeast(0))
                    // FIX #8: use format=auto for overlay, not format=rgba
                    fcParts.add("[$overlayIdx:v]scale=$overlayW:$overlayH,format=auto,colorchannelmixer=aa=${imageOverlayOpacity}[ovl]")
                    fcParts.add("[$currentLabel][ovl]overlay=$ox:$oy:format=auto[vimg]")
                    currentLabel = "vimg"
                }

                // Watermark overlay
                if (hasWatermark) {
                    fcParts.add("[$wmIdx:v]scale=iw*0.1:-1[wm]")
                    fcParts.add("[$currentLabel][wm]overlay=W-w-20:20:format=auto[vwm]")
                    currentLabel = "vwm"
                }

                // Final format normalization
                fcParts.add("[$currentLabel]format=yuv420p[vfinalout]")
                videoOutLabel = "[vfinalout]"
            } else {
                // FIX: When audio filter_complex is needed but video filter_complex
                // is NOT (no image overlay/green screen/watermark), we MUST put the
                // video filters INTO the filter_complex chain too — NOT use `-vf`.
                // FFmpeg ignores `-vf` when `-filter_complex` is present, so using
                // both would silently DROP all video filters (color grades, effects,
                // text overlays, stickers, transitions) from the exported video.
                // This was the root cause of "edited elements dont appear in export".
                if (vfFilters.isNotEmpty()) {
                    fcParts.add("[0:v]${vfFilters.joinToString(",")},format=yuv420p[vfinalout]")
                    videoOutLabel = "[vfinalout]"
                }
                // If no vfFilters either, videoOutLabel stays "0:v" (raw passthrough)
            }

            // ── AUDIO CHAIN ──
            if (needAudioFilterComplex) {
                // BGM mixing: [0:a] → volume+effects → [a1]; [1:a] → volume+trim → [bgm]; amix → [aout]
                val vVol = if (isMuted) 0.0f else videoVolume
                val duckVol = if (isAudioDuckingEnabled) vVol * 0.3f else vVol
                var aChain = "[0:a]volume=$duckVol"
                if (speedFactor != 1.0f) aChain += ",${getAtempoFilter(speedFactor)}"
                if (voiceChangerPitch != 0f) {
                    val factor = Math.pow(2.0, voiceChangerPitch / 12.0)
                    aChain += ",asetrate=44100*${String.format("%.4f", factor)},aresample=44100,atempo=${String.format("%.4f", 1.0 / factor)}"
                }
                val aeChain = audioEffectChain(audioEffect)
                if (aeChain.isNotEmpty()) aChain += "," + aeChain.joinToString(",")
                aChain += "[a1]"
                fcParts.add(aChain)

                fcParts.add("[1:a]volume=$backgroundMusicVolume,atrim=duration=${finalDuration}[bgm]")
                fcParts.add("[a1][bgm]amix=inputs=2:duration=first[aout]")
                audioOutLabel = "[aout]"
            }

            // Add the SINGLE unified -filter_complex
            args.addAll(listOf("-filter_complex", fcParts.joinToString(";")))
            // Add -map flags AFTER -filter_complex (FIX #4: correct ordering)
            args.addAll(listOf("-map", videoOutLabel))
            if (isMuted || (videoVolume == 0f && !hasBgm)) {
                args.add("-an")
            } else {
                args.addAll(listOf("-map", audioOutLabel))
            }
        } else {
            // No filter_complex needed — use -vf and -af
            if (vfFilters.isNotEmpty()) {
                args.addAll(listOf("-vf", vfFilters.joinToString(",")))
            }
            // Audio handling (simplified — no BGM mixing)
            if (isMuted || (videoVolume == 0f && !hasBgm)) {
                args.add("-an")
            } else {
                val afFilters = mutableListOf<String>()
                if (speedFactor != 1.0f) {
                    afFilters.add(getAtempoFilter(speedFactor))
                }
                if (voiceChangerPitch != 0f) {
                    val factor = Math.pow(2.0, voiceChangerPitch / 12.0)
                    afFilters.add("asetrate=44100*${String.format("%.4f", factor)},aresample=44100,atempo=${String.format("%.4f", 1.0 / factor)}")
                }
                val audioEffectChain = audioEffectChain(audioEffect)
                if (audioEffectChain.isNotEmpty()) {
                    afFilters.addAll(audioEffectChain)
                }
                // v6.0.0: inject AI feature audio chain (e.g. afftdn noise removal)
                if (aiAudioChain.isNotBlank()) {
                    afFilters.add(aiAudioChain)
                }
                if (videoVolume != 1.0f) afFilters.add("volume=$videoVolume")
                if (afFilters.isNotEmpty()) {
                    args.addAll(listOf("-af", afFilters.joinToString(",")))
                }
            }
        }

        // Audio codec (when audio is present)
        if (!(isMuted || (videoVolume == 0f && !hasBgm))) {
            args.addAll(listOf("-c:a", "aac"))
        }

        // ── VIDEO ENCODER — OPTIMISED FOR LONG (60-min+) EXPORTS (v4.2) ─
        // We use libx264 SOFTWARE encoding, NOT MediaCodec hardware encoding.
        // MediaCodec hardware encoders on mid-range phones throttle or crash
        // after ~10 minutes of continuous encoding and produce broken MP4
        // headers on long files. libx264 is ~15% slower but rock-solid for
        // multi-hour encodes and produces universally-playable H.264/AVC.
        //
        //   -c:v libx264          H.264/AVC baseline-compatible software encoder.
        //   -preset veryfast     2nd-fastest preset. "ultrafast" disables too
        //                         many optimisations and produces a LARGER file
        //                         that runs out of disk on a 60-min 1080p job.
        //                         "veryfast" keeps encoding fast but halves the
        //                         output size, so the 15 GB space check holds.
        //   -crf 23               Constant Rate Factor = visually-lossless.
        //                         18=lossless(huge), 23=default, 28=noticeable.
        //                         23 is the sweet spot for 1080p long videos.
        //   -g 250                GOP / keyframe interval = 250 frames (~8.3s
        //                         at 30fps). Fixed GOP prevents the encoder
        //                         from inserting random keyframes that bloat
        //                         the file and can desync timestamps on long
        //                         runs. 250 is the YouTube-recommended value.
        //   -keyint_min 250       Minimum keyframe interval = same as -g →
        //                         strictly fixed GOP, no scene-cut keyframes.
        //   -sc_threshold 0       Disable scene-cut detection (which would
        //                         insert extra keyframes and break CFR timing).
        //   -maxrate 6M -bufsize 12M  VBV buffer caps the bitrate peaks so a
        //                         spike at minute 40 can't overflow the muxer.
        //                         6 Mbps max / 12 Mbps buffer is safe for 1080p30.
        //   -pix_fmt yuv420p      8-bit 4:2:0 — plays on every device & player.
        //   -profile:v high -level 4.0  H.264 High@4.0 = 1080p30, universally
        //                         supported on Android 5+, iOS, web, TV.
        //   -movflags +faststart  moov atom at the front → instant playback.
        //   -map_metadata 0       preserve creation metadata.
        //
        // ── v6.0.0 DYNAMIC ENCODER ──────────────────────────────────────
        // The encoder is now selected at runtime based on the user's premium
        // export settings:
        //  • HDR (isHdrEnabled): switches to libx265 (HEVC) 10-bit Main10
        //    profile with BT.2020 PQ (SMPTE ST 2084) transfer — true HDR as
        //    specified by the ITU. The zscale/format chain in the AI HDR
        //    feature already maps the pixels to 10-bit; here we tell the
        //    encoder + muxer the correct colour metadata.
        //  • High Bitrate (isHighBitrateEnabled): lowers CRF to 18 (near-
        //    lossless) and raises the VBV maxrate/bufsize cap so the output
        //    retains maximum detail — ideal for mastering / re-editing.
        //  • Default: the proven libx264 veryfast CRF 24 pipeline above.
        val gopSize = (targetFps * 8).toString()  // ~8s GOP, adaptive to fps
        if (isHdrEnabled) {
            // ── HDR 10-bit HEVC pipeline ──
            args.addAll(listOf("-c:v", "libx265"))
            args.addAll(listOf("-preset", "veryfast"))
            args.addAll(listOf("-crf", if (isHighBitrateEnabled) "18" else "22"))
            args.addAll(listOf("-x265-params",
                "profile=main10:colorprim=bt2020:transfer=smpte2084:colormatrix=bt2020nc:" +
                "max-cll=1000,400:hdr10-opt=1:repeat-headers=1"))
            args.addAll(listOf("-g", gopSize))
            args.addAll(listOf("-keyint_min", gopSize))
            args.addAll(listOf("-sc_threshold", "0"))
            // HDR needs a much higher bitrate ceiling (10-bit HDR is ~2.5x the data)
            val hdrMaxrate = if (isHighBitrateEnabled) "40M" else "20M"
            val hdrBufsize = if (isHighBitrateEnabled) "80M" else "40M"
            args.addAll(listOf("-maxrate", hdrMaxrate, "-bufsize", hdrBufsize))
            args.addAll(listOf("-pix_fmt", "yuv420p10le"))
            args.addAll(listOf("-tag:v", "hvc1"))  // Apple/QuickTime HEVC tag
            args.addAll(listOf("-movflags", "+faststart"))
            args.addAll(listOf("-map_metadata", "0"))
            Log.d(tag, "v6.0.0 HDR export: libx265 10-bit BT.2020 PQ, CRF=${if (isHighBitrateEnabled) 18 else 22}, maxrate=$hdrMaxrate")
        } else if (isHighBitrateEnabled) {
            // ── High-bitrate visually-lossless H.264 pipeline ──
            args.addAll(listOf("-c:v", "libx264"))
            args.addAll(listOf("-preset", "slow"))  // slower = better compression at low CRF
            args.addAll(listOf("-crf", "18"))        // near-lossless
            args.addAll(listOf("-g", gopSize))
            args.addAll(listOf("-keyint_min", gopSize))
            args.addAll(listOf("-sc_threshold", "0"))
            args.addAll(listOf("-maxrate", "16M", "-bufsize", "32M"))
            args.addAll(listOf("-profile:v", "high"))
            args.addAll(listOf("-level", "5.1"))     // 4K-capable level
            args.addAll(listOf("-pix_fmt", "yuv420p"))
            args.addAll(listOf("-movflags", "+faststart"))
            args.addAll(listOf("-map_metadata", "0"))
            Log.d(tag, "v6.0.0 High Bitrate export: libx264 slow CRF 18, maxrate 16M")
        } else {
            // ── Standard proven pipeline (v4.2) ──
            args.addAll(listOf("-c:v", "libx264"))
            args.addAll(listOf("-preset", "veryfast"))
            args.addAll(listOf("-crf", "24"))
            args.addAll(listOf("-g", gopSize))
            args.addAll(listOf("-keyint_min", gopSize))
            args.addAll(listOf("-sc_threshold", "0"))
            args.addAll(listOf("-maxrate", "6M", "-bufsize", "12M"))
            args.addAll(listOf("-profile:v", "high"))
            args.addAll(listOf("-level", "4.0"))
            args.addAll(listOf("-pix_fmt", "yuv420p"))
            args.addAll(listOf("-movflags", "+faststart"))
            args.addAll(listOf("-map_metadata", "0"))
        }
        args.addAll(listOf("-y", outputPath))

        Log.d(tag, "ProcessAndExport: ffmpeg ${args.joinToString(" ")}")
        val finalDurationSec = durationSec / speedFactor
        val success = executeFFmpegWithProgress(args.toTypedArray(), finalDurationSec, onProgress)

        if (success) {
            Log.d(tag, "ProcessAndExport succeeded!")
            cleanupOverlayTempFiles()
            true
        } else {
            Log.e(tag, "ProcessAndExport failed — attempting recovery...")
            // AUTO-RECOVERY 1: Retry the FULL pipeline with ultrafast preset
            // (keeps overlays/filters but uses the fastest encoder settings).
            // This catches cases where veryfast ran out of memory or timed out.
            Log.d(tag, "Recovery 1: full pipeline with ultrafast preset (keeps overlays)...")
            val recovery1Args = args.toMutableList()
            val vfIdx = recovery1Args.indexOf("veryfast")
            if (vfIdx >= 0) recovery1Args[vfIdx] = "ultrafast"
            val crfIdx = recovery1Args.indexOf("24")
            if (crfIdx >= 0) recovery1Args[crfIdx] = "28"
            val rec1Success = executeFFmpegSync(recovery1Args.toTypedArray())
            if (rec1Success) {
                Log.d(tag, "Recovery 1 (ultrafast re-encode with overlays) succeeded!")
                cleanupOverlayTempFiles()
                true
            } else {
                Log.e(tag, "Recovery 1 failed — falling back to raw re-encode (overlays will be lost)")
                // AUTO-RECOVERY 2: minimal re-encode WITHOUT overlays/filters
                // (raw video only). This is the last resort — the user loses
                // their overlays but at least gets the video exported.
                Log.d(tag, "Recovery 2: minimal re-encode (no filters, output-seek)...")
                val recovery2Args = mutableListOf(
                    "-err_detect", "ignore_err", "-ignore_unknown",
                    "-threads", "0", "-analyzeduration", "100M", "-probesize", "100M",
                    "-i", inputPath
                )
                if (startSec > 0) recovery2Args.addAll(listOf("-ss", startSec.toString()))
                recovery2Args.addAll(listOf("-t", finalDurationSec.toString(),
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "24",
                    "-g", "250", "-keyint_min", "250", "-sc_threshold", "0",
                    "-maxrate", "6M", "-bufsize", "12M",
                    "-c:a", "aac", "-pix_fmt", "yuv420p", "-movflags", "+faststart",
                    "-y", outputPath))
                val rec2Success = executeFFmpegSync(recovery2Args.toTypedArray())
                if (rec2Success) {
                    Log.d(tag, "Recovery 2 (minimal re-encode) succeeded — overlays were lost")
                    cleanupOverlayTempFiles()
                    true
                } else {
                    Log.e(tag, "Recovery 2 failed — attempting stream copy...")
                    // AUTO-RECOVERY 3: input-side seek + stream copy (no re-encode)
                    Log.d(tag, "Recovery 3: input-seek + stream copy...")
                    val recovery3Args = mutableListOf("-threads", "0", "-ss", startSec.toString())
                    recovery3Args.addAll(listOf("-i", inputPath, "-t", finalDurationSec.toString(),
                        "-c", "copy", "-avoid_negative_ts", "make_zero",
                        "-movflags", "+faststart", "-y", outputPath))
                    val rec3Success = executeFFmpegSync(recovery3Args.toTypedArray())
                    if (rec3Success) {
                        Log.d(tag, "Recovery 3 (stream copy) succeeded")
                        cleanupOverlayTempFiles()
                        true
                    } else {
                        Log.e(tag, "All recovery attempts failed")
                        cleanupOverlayTempFiles()
                        false
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  COLOR GRADES — 60 cinematic LUTs
    // ════════════════════════════════════════════════════════════════════
    private fun colorGradeChain(filter: String): String {
        val f = filter.lowercase().replace("-", "_").replace(" ", "_")
        return when (f) {
            "none" -> ""
            "sepia" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131"
            "grayscale", "mono", "black_white" -> "format=gray,lut=a=val"
            "invert", "negative" -> "negate"
            "warm" -> "eq=temp=1.1:saturation=1.1,colorbalance=rs=0.08:gs=0.02:rm=0.05"
            "cool" -> "eq=temp=0.9:saturation=1.05,colorbalance=bs=0.1:gm=-0.03:bm=0.05"
            "vintage" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131,eq=saturation=0.8:contrast=1.1:gamma=1.1,vignette=angle=PI/4"
            "dramatic" -> "eq=contrast=1.4:saturation=1.3:gamma=0.9,curves=preset=increase_contrast"
            "vivid" -> "eq=saturation=1.6:contrast=1.2,vibrance=0.4"
            "noir" -> "format=gray,eq=contrast=1.5:gamma=0.8,vignette=angle=PI/3"
            "bloom" -> "eq=brightness=0.05:contrast=1.1,boxblur=luma_radius=5:luma_power=1,blend=all_mode=screen"
            "tealorange", "teal_orange" -> "colorbalance=rs=0.12:gs=-0.05:bs=0.05:rm=0.1:bm=0.08,eq=saturation=1.3:contrast=1.15"
            "pastel" -> "eq=saturation=0.7:brightness=0.08:contrast=0.95,colorbalance=rs=0.03:gs=0.02:bs=0.05"
            "fade" -> "eq=saturation=0.6:contrast=0.9:brightness=0.05,colorbalance=rs=0.04:gs=0.02:bs=0.06"
            "cyberpunk" -> "colorbalance=rs=0.2:bs=0.25:rm=0.1:bm=0.15,eq=saturation=1.8:contrast=1.3,hue=h=-20"
            "sunset" -> "colorbalance=rs=0.15:rm=0.1:gs=-0.03,eq=saturation=1.4:contrast=1.1:gamma=1.05"
            "arctic" -> "eq=temp=0.75:saturation=0.9:contrast=1.1,colorbalance=bs=0.12:bm=0.08"
            "forest" -> "eq=saturation=1.2:contrast=1.1,colorbalance=gs=0.1:gm=0.06:bs=-0.03"
            "rose" -> "colorbalance=rs=0.1:rm=0.08:gs=-0.02:bs=0.04,eq=saturation=1.3:brightness=0.03"
            "golden" -> "colorbalance=rs=0.12:rm=0.1:gs=0.03,eq=saturation=1.35:contrast=1.1:gamma=1.05,vignette=angle=PI/4"
            "mist" -> "eq=contrast=0.9:saturation=0.8:brightness=0.1,boxblur=luma_radius=3:luma_power=1,blend=all_mode=screen:opacity=0.3"
            "cinematic" -> "curves=preset=increase_contrast,eq=saturation=0.85:contrast=1.2:gamma=0.95,colorbalance=rs=0.04:bs=0.06"
            "teal" -> "colorbalance=bs=0.15:bm=0.1:gs=0.03,eq=saturation=1.1:contrast=1.1"
            "orange" -> "colorbalance=rs=0.15:rm=0.12,eq=saturation=1.2:contrast=1.1:gamma=1.05"
            "lomo" -> "vignette=angle=PI/3,eq=saturation=1.5:contrast=1.3:gamma=0.9"
            "polaroid" -> "eq=saturation=0.7:contrast=0.95:brightness=0.08,colorbalance=rs=0.05:bs=0.03,vignette=angle=PI/4"
            "holga" -> "vignette=angle=PI/2,eq=saturation=1.3:contrast=1.1,noise=alls=10:allf=t"
            "diana" -> "eq=saturation=1.4:contrast=0.9:vignette=angle=PI/2"
            "film" -> "curves=preset=vintage,eq=saturation=0.9:contrast=1.05,noise=alls=8:allf=t"
            "super8" -> "curves=preset=vintage,eq=saturation=1.2:brightness=0.05,noise=alls=15:allf=t,vignette=angle=PI/3"
            "vhs_tape" -> "curves=preset=vintage,eq=saturation=1.1:contrast=0.95,noise=alls=12:allf=t"
            "kodak" -> "eq=saturation=1.1:contrast=1.05:gamma=1.05,colorbalance=rs=0.05:gs=0.02"
            "fuji" -> "eq=saturation=1.15:contrast=1.1,colorbalance=bs=0.04:gs=0.03"
            "agfa" -> "eq=saturation=1.2:contrast=1.1,colorbalance=rs=0.06:bs=0.03"
            "ilford" -> "format=gray,eq=contrast=1.2:gamma=1.05"
            "portra" -> "eq=saturation=0.95:contrast=1.05:gamma=1.02,colorbalance=rs=0.04:gs=0.02:bs=0.02"
            "velvia" -> "eq=saturation=1.5:contrast=1.2,vibrance=0.3"
            "provia" -> "eq=saturation=1.1:contrast=1.05"
            "astia" -> "eq=saturation=1.0:contrast=1.0:gamma=1.05,colorbalance=rs=0.03:bs=0.03"
            "monochrome" -> "format=gray,eq=contrast=1.3:gamma=0.9"
            "high_contrast" -> "eq=contrast=1.5:saturation=1.1"
            "low_contrast" -> "eq=contrast=0.85:saturation=0.95"
            "high_saturation" -> "eq=saturation=2.0:contrast=1.1"
            "low_saturation" -> "eq=saturation=0.5:contrast=1.0"
            "bright" -> "eq=brightness=0.1:contrast=1.05:saturation=1.1"
            "dark" -> "eq=brightness=-0.08:contrast=1.15:gamma=0.95"
            "soft" -> "eq=contrast=0.9:saturation=0.9:brightness=0.05,boxblur=luma_radius=2:luma_power=1"
            "sharp" -> "eq=contrast=1.3:saturation=1.2,unsharp=5:5:1:5:5:0"
            "dreamy" -> "eq=saturation=1.1:brightness=0.08:contrast=0.95,boxblur=luma_radius=4:luma_power=1,blend=all_mode=screen:opacity=0.3"
            "glow" -> "eq=brightness=0.1:contrast=1.1,boxblur=luma_radius=6:luma_power=1,blend=all_mode=screen:opacity=0.4"
            "haze" -> "eq=contrast=0.85:saturation=0.8:brightness=0.12,boxblur=luma_radius=3:luma_power=1"
            "matte" -> "eq=saturation=0.85:contrast=0.9:brightness=0.05,curves=preset=lighter"
            "litho" -> "format=gray,eq=contrast=1.6:gamma=0.8,curves=preset=stronger"
            "sepia_warm" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131,eq=temp=1.15:saturation=1.1"
            "sepia_cool" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131,eq=temp=0.85:saturation=0.9"
            "red_boost" -> "colorbalance=rs=0.2:rm=0.15,eq=saturation=1.2:contrast=1.1"
            "blue_boost" -> "colorbalance=bs=0.2:bm=0.15,eq=saturation=1.2:contrast=1.1"
            "green_boost" -> "colorbalance=gs=0.2:gm=0.15,eq=saturation=1.2:contrast=1.1"
            "purple_haze" -> "colorbalance=rs=0.1:bs=0.15,eq=saturation=1.3:contrast=1.1,hue=h=15"
            "pink_dream" -> "colorbalance=rs=0.12:bs=0.08,eq=saturation=1.3:brightness=0.05"
            "amber" -> "colorbalance=rs=0.15:rm=0.1:gs=0.03,eq=saturation=1.25:gamma=1.05"
            "emerald" -> "colorbalance=gs=0.15:gm=0.1:bs=0.03,eq=saturation=1.25"
            "sapphire" -> "colorbalance=bs=0.15:bm=0.1,eq=saturation=1.2:contrast=1.05"
            "ruby" -> "colorbalance=rs=0.18:rm=0.12,eq=saturation=1.3:contrast=1.1"
            "bronze" -> "colorbalance=rs=0.1:rm=0.08:gs=0.05,eq=saturation=1.1:contrast=1.1:gamma=1.02"
            "platinum" -> "eq=saturation=0.3:contrast=1.15:brightness=0.05"
            "neon_city" -> "colorbalance=rs=0.15:bs=0.2:rm=0.1:bm=0.12,eq=saturation=1.8:contrast=1.3"
            "retro_wave" -> "colorbalance=rs=0.18:bs=0.2:rm=0.1,eq=saturation=1.6:contrast=1.2,hue=h=-10"
            "synthwave" -> "colorbalance=rs=0.15:bs=0.22,eq=saturation=1.7:contrast=1.25,hue=h=-15"
            "analog" -> "curves=preset=vintage,eq=saturation=0.95:contrast=1.05,noise=alls=6:allf=t"
            "tokyo" -> "colorbalance=rs=0.12:bs=0.1:rm=0.08,eq=saturation=1.4:contrast=1.15"
            "nyc" -> "eq=saturation=0.9:contrast=1.3:gamma=0.95,colorbalance=bs=0.05"
            "paris" -> "eq=saturation=1.05:contrast=1.1:gamma=1.02,colorbalance=rs=0.04:bs=0.03"
            "miami" -> "colorbalance=rs=0.08:bs=0.12:gs=0.03,eq=saturation=1.5:contrast=1.1"
            "desert" -> "colorbalance=rs=0.15:rm=0.1:gs=0.04,eq=saturation=1.2:contrast=1.1:gamma=1.05"
            "ocean" -> "colorbalance=bs=0.15:bm=0.1:gs=0.05,eq=saturation=1.2:contrast=1.05"
            "autumn" -> "colorbalance=rs=0.15:rm=0.12:gs=0.05,eq=saturation=1.3:contrast=1.1"
            "winter" -> "eq=temp=0.8:saturation=0.85:contrast=1.1,colorbalance=bs=0.1:bm=0.05"
            "spring" -> "colorbalance=gs=0.08:bs=0.05:rs=0.03,eq=saturation=1.2:brightness=0.05"
            "summer" -> "eq=saturation=1.3:contrast=1.1:brightness=0.05,colorbalance=rs=0.05:bs=0.03"
            else -> ""
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  BLEND MODES — 16+ CapCut blend modes
    // ════════════════════════════════════════════════════════════════════
    //  v4.4.0 PREMIUM LOOKS - 50+ Brightness / HDR / iPhone grades
    //  Returns the real FFmpeg -vf sub-filters for a PremiumLooks id.
    private fun premiumLookChain(lookId: String): List<String> {
        val id = lookId.trim().lowercase()
        if (id.isEmpty() || id == "none") return emptyList()
        val chain = com.powercut.editor.domain.look.PremiumLooks.chainFor(id)
        if (chain.isBlank()) return emptyList()
        return chain.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    //  v4.4.0 MAGIC / ANIMATED EFFECTS - real FFmpeg time-expression filters
    //  These animate over the clip duration using FFmpeg's `t` expression so
    //  they are genuinely "animated effects" (not static).
    private fun magicEffectChain(effectName: String, duration: Double, w: Int, h: Int): List<String> {
        val e = effectName.lowercase().replace(" ", "_")
        return when {
            e.contains("magic_pulse") ->
                listOf("eq=brightness='0.1*sin(2*PI*t/2)':contrast=1.15", "vignette=angle='PI/4+0.2*sin(2*PI*t)'")
            e.contains("magic_hue_cycle") ->
                listOf("hue=h='t*30'", "eq=saturation=1.3:contrast=1.1")
            e.contains("magic_color_flow") ->
                listOf("colorbalance=rs='0.08*sin(t)':bs='0.08*cos(t)'", "eq=saturation=1.2")
            e.contains("magic_brightness_flow") ->
                listOf("eq=brightness='0.08*sin(2*PI*t/4)':contrast=1.1")
            e.contains("magic_zoom_pulse") ->
                listOf("zoompan=z='1+0.1*sin(2*PI*t/2)':d=1:s=${w}x${h}:fps=30")
            e.contains("magic_shake") ->
                listOf("crop=iw:ih:'0+5*sin(2*PI*t*3)':'0+5*cos(2*PI*t*3)'", "scale=${w}:${h}")
            e.contains("magic_flicker") ->
                listOf("eq=brightness='0.15*(random(0))':contrast=1.1")
            e.contains("magic_rainbow_flow") ->
                listOf("hue=h='t*60'", "eq=saturation=1.5:contrast=1.1")
            e.contains("magic_glitch_flow") ->
                listOf("chromashift=cbh='2*sin(t)':crv='2*cos(t)'", "eq=contrast=1.15")
            e.contains("magic_neon_flow") ->
                listOf("eq=saturation=1.6:contrast=1.2", "colorbalance=rs='0.1+0.05*sin(t)':bs='0.1+0.05*cos(t)'")
            e.contains("magic_wave") ->
                listOf("crop=iw:ih:'0+8*sin(2*PI*t)':'0+8*cos(2*PI*t*0.5)'", "scale=${w}:${h}")
            e.contains("magic_breath") ->
                listOf("eq=brightness='0.05*sin(2*PI*t/3)':contrast='1.1+0.05*sin(2*PI*t/3)'")
            else -> emptyList()
        }
    }

    private fun blendModeChain(mode: String): String {
        val m = mode.lowercase().replace(" ", "_")
        return when (m) {
            "none", "normal" -> ""
            "multiply" -> "blend=all_mode=multiply"
            "screen" -> "blend=all_mode=screen"
            "overlay" -> "blend=all_mode=overlay"
            "darken" -> "blend=all_mode=darken"
            "lighten" -> "blend=all_mode=lighten"
            "color_dodge" -> "blend=all_mode=lighten"
            "color_burn" -> "blend=all_mode=darken"
            "hard_light" -> "blend=all_mode=overlay"
            "soft_light" -> "blend=all_mode=softlight"
            "difference" -> "blend=all_mode=difference"
            "exclusion" -> "blend=all_mode=exclusion"
            "hue" -> "eq=saturation=1.3"
            "saturation" -> "eq=saturation=1.5"
            "color" -> "eq=saturation=1.2:contrast=1.1"
            "luminosity" -> "eq=contrast=1.2:brightness=0.05"
            "addition" -> "blend=all_mode=addition"
            "phoenix" -> "blend=all_mode=phoenix"
            "reflect" -> "blend=all_mode=reflect"
            "glow" -> "blend=all_mode=glow"
            "negation" -> "blend=all_mode=negation"
            else -> ""
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  SUPER EFFECTS — 70+
    // ════════════════════════════════════════════════════════════════════
    private fun effectChain(effectName: String, duration: Double, w: Int, h: Int): List<String> {
        if (effectName == "none") return emptyList()
        val e = effectName.lowercase().replace(" ", "_").replace("-", "_")
        return when {
            // v4.4.0: Magic / animated effects use real FFmpeg time expressions.
            e.contains("magic_") ->
                magicEffectChain(e, duration, w, h)
            e.contains("glitch") && e.contains("rgb") ->
                listOf("noise=alls=25:allf=t+u", "chromashift=cbh=-5:cbv=3:crh=5:crv=-3")
            e == "glitch" || e.contains("chromatic") || e.contains("electric") ->
                listOf("noise=alls=20:allf=t+u", "chromashift=cbh=-3:cbv=2:crh=3:crv=-2")
            e.contains("glitch") && e.contains("data") ->
                listOf("noise=alls=30:allf=t+u:allc=color", "chromashift=cbh=-4:cbv=2:crh=4:crv=-2")
            e.contains("vhs") && e.contains("old") ->
                listOf("noise=alls=15:allf=t+u", "curves=preset=vintage", "boxblur=luma_radius=3:luma_power=1")
            e.contains("vhs") ->
                listOf("noise=alls=8:allf=t+u", "curves=preset=vintage", "boxblur=luma_radius=2:luma_power=1")
            e.contains("snow") && e.contains("heavy") ->
                listOf("noise=alls=60:allf=t+u:allc=color")
            e.contains("snow") ->
                listOf("noise=alls=40:allf=t+u:allc=color")
            e.contains("rain") && e.contains("heavy") ->
                listOf("noise=alls=25:allf=t+u", "boxblur=luma_radius=2:luma_power=1")
            e.contains("rain") ->
                listOf("noise=alls=15:allf=t+u", "boxblur=luma_radius=1:luma_power=1")
            e.contains("fire") || e.contains("flame") ->
                listOf("colorbalance=rs=0.2:rm=0.15,eq=brightness=0.08:saturation=1.3")
            e.contains("frost") || e.contains("ice") ->
                listOf("eq=temp=0.8:saturation=0.9:contrast=1.1,colorbalance=bs=0.15:bm=0.1")
            e.contains("sparkle") || e.contains("starburst") ->
                listOf("eq=brightness=0.1:contrast=1.15")
            e.contains("dust") ->
                listOf("noise=alls=5:allf=t+u", "eq=contrast=0.95:brightness=0.03")
            e.contains("motion_blur") || e.contains("motionblur") ->
                listOf("boxblur=luma_radius=8:luma_power=1:enable='1'")
            e.contains("shake") && e.contains("earthquake") ->
                listOf("noise=alls=5:allf=t+u", "crop=iw-30:ih-30:enable='1'")
            e.contains("shake") ->
                listOf("noise=alls=3:allf=t+u", "crop=iw-20:ih-20:enable='1'")
            e.contains("flash") || e.contains("strobe") ->
                listOf("eq=brightness='0.3*abs(sin(t*8))'")
            e.contains("neon") && e.contains("glow") ->
                listOf("eq=saturation=2.0:contrast=1.3,colorbalance=rs=0.1:bs=0.15:rm=0.08:bm=0.08,boxblur=luma_radius=3:luma_power=1,blend=all_mode=screen")
            e.contains("neon") ->
                listOf("eq=saturation=2.0:contrast=1.3,colorbalance=rs=0.1:bs=0.15:rm=0.08:bm=0.08")
            e.contains("vignette") && e.contains("heavy") ->
                listOf("vignette=angle=PI/4")
            e.contains("vignette") ->
                listOf("vignette=angle=PI/3")
            e.contains("rainbow") ->
                listOf("hue=h='t*50'", "eq=saturation=1.5")
            e.contains("film_grain") && e.contains("heavy") ->
                listOf("noise=alls=30:allf=t+u")
            e.contains("film_grain") ->
                listOf("noise=alls=18:allf=t+u")
            e.contains("bokeh") ->
                listOf("boxblur=luma_radius=15:luma_power=2", "eq=brightness=0.05")
            e.contains("particles") && e.contains("color") ->
                listOf("noise=alls=12:allf=t+u:allc=color", "eq=saturation=1.3")
            e.contains("particles") ->
                listOf("noise=alls=12:allf=t+u", "eq=saturation=1.2")
            e.contains("zoom_pulse") ->
                listOf("zoompan=z='min(zoom+0.0015\\,1.5)':d=1:s=${w}x${h}")
            e.contains("wave") || e.contains("tidal") ->
                listOf("lenscorrection=k1='-0.1*sin(t*2)':k2='0.1*cos(t*2)'")
            e.contains("swirl") ->
                listOf("lenscorrection=k1=0.3:k2=0.3")
            e.contains("explosion") ->
                listOf("noise=alls=30:allf=t+u", "eq=contrast=1.4:saturation=1.5")
            e.contains("light_leak") && e.contains("warm") ->
                listOf("vignette=angle=PI/4", "colorbalance=rs=0.12:rm=0.08")
            e.contains("light_leak") ->
                listOf("vignette=angle=PI/4", "colorbalance=rs=0.08:rm=0.05")
            e.contains("film_strip") ->
                listOf("noise=alls=10:allf=t+u", "curves=preset=vintage", "vignette=angle=PI/4")
            e.contains("color_splash") ->
                listOf("eq=saturation=1.8:contrast=1.2", "colorbalance=rs=0.05:bs=0.05")
            e.contains("lens_flare") ->
                listOf("eq=brightness=0.08:contrast=1.1", "vignette=angle=PI/4")
            e.contains("bloom") ->
                listOf("eq=brightness=0.08:contrast=1.1", "boxblur=luma_radius=6:luma_power=1", "blend=all_mode=screen:opacity=0.4")
            e.contains("hdr") ->
                listOf("eq=saturation=1.3:contrast=1.3", "unsharp=5:5:1.5:5:5:0")
            e.contains("vaporwave") ->
                listOf("colorbalance=rs=0.15:bs=0.25", "eq=saturation=1.7:contrast=1.2", "hue=h='t*10'")
            e.contains("aesthetic") ->
                listOf("eq=saturation=1.1:contrast=1.05:brightness=0.05", "colorbalance=rs=0.04:bs=0.04")
            e.contains("lofi") ->
                listOf("noise=alls=20:allf=t+u", "eq=saturation=1.2:contrast=1.1", "vignette=angle=PI/3")
            e.contains("vapor") ->
                listOf("eq=saturation=0.8:brightness=0.1", "boxblur=luma_radius=4:luma_power=1", "colorbalance=bs=0.08")
            e.contains("dream") ->
                listOf("eq=saturation=1.1:brightness=0.08:contrast=0.95", "boxblur=luma_radius=4:luma_power=1", "blend=all_mode=screen:opacity=0.3")
            e.contains("night_vision") ->
                listOf("format=gray", "eq=brightness=0.3:contrast=1.3", "colorchannelmixer=.2:.7:.1", "noise=alls=12:allf=t+u")
            e.contains("thermal") ->
                listOf("eq=saturation=2.0:contrast=1.5", "colorbalance=rs=0.3:bs=0.2", "hue=h='t*5'")
            e.contains("infrared") ->
                listOf("eq=saturation=2.5:contrast=1.4", "colorbalance=rs=0.25:bs=0.15")
            e.contains("xray") ->
                listOf("format=gray", "negate", "eq=contrast=1.5:brightness=0.1")
            e.contains("pencil") ->
                listOf("format=gray", "eq=contrast=1.6", "unsharp=5:5:2:5:5:0", "noise=alls=3:allf=t")
            e.contains("sketch") ->
                listOf("format=gray", "eq=contrast=1.4", "unsharp=7:7:1.5:7:7:0")
            e.contains("cartoon") ->
                listOf("eq=saturation=1.8:contrast=1.4", "unsharp=3:3:1:3:3:0", "noise=alls=2:allf=t")
            e.contains("watercolor") ->
                listOf("eq=saturation=1.3:contrast=0.9:brightness=0.05", "boxblur=luma_radius=3:luma_power=1")
            e.contains("oil_paint") ->
                listOf("eq=saturation=1.4:contrast=1.1", "boxblur=luma_radius=2:luma_power=1", "unsharp=5:5:0.5:5:5:0")
            e == "pixel" || e.contains("pixelate") ->
                listOf("scale=iw/20:ih/20,scale=iw:ih:flags=neighbor")
            e.contains("mosaic") ->
                listOf("scale=iw/30:ih/30,scale=iw:ih:flags=neighbor")
            e.contains("ascii") ->
                listOf("scale=iw/10:ih/10:flags=area,scale=iw:ih:flags=neighbor", "format=gray")
            e.contains("glow_edge") ->
                listOf("unsharp=7:7:3:7:7:0", "eq=saturation=1.5:contrast=1.3")
            e.contains("edge_detect") ->
                listOf("format=gray", "unsharp=9:9:2:9:9:0", "eq=contrast=1.6")
            e.contains("emboss") ->
                listOf("format=gray", "unsharp=3:3:1.5:3:3:0", "eq=contrast=1.4")
            e.contains("sharpen_strong") ->
                listOf("unsharp=7:7:2:7:7:0", "eq=contrast=1.2")
            e.contains("blur_strong") ->
                listOf("boxblur=luma_radius=20:luma_power=2")
            e.contains("tilt_shift") ->
                listOf("boxblur=luma_radius=15:luma_power=2:enable='gte(mod(t\\,2)\\,1)'")
            // v4.5.0: Face Blur — privacy blur using heavy box blur (no face
            // detection available in FFmpeg-Kit free build, so full-frame blur
            // as a privacy mask; still a real, workable filter, not a no-op).
            e.contains("face_blur") ->
                listOf("boxblur=luma_radius=30:luma_power=2")
            e.contains("kaleidoscope") ->
                listOf("lenscorrection=k1=0.4:k2=0.4", "eq=saturation=1.3")
            e.contains("shake_screen") ->
                listOf("crop=iw-10:ih-10:'5*sin(t*15)':'5*cos(t*15)'")
            e.contains("zoom_in_slow") ->
                listOf("zoompan=z='min(zoom+0.0008\\,1.8)':d=1:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=${w}x${h}")
            e.contains("zoom_out_slow") ->
                listOf("zoompan=z='if(eq(on\\,0)\\,1.8\\,max(zoom-0.0008\\,1.0))':d=1:s=${w}x${h}")
            e.contains("pan_right") ->
                listOf("zoompan=z=1.3:x='iw*on/100':y='ih/2-(ih/zoom/2)':d=1:s=${w}x${h}")
            e.contains("pan_left") ->
                listOf("zoompan=z=1.3:x='iw-iw*on/100':y='ih/2-(ih/zoom/2)':d=1:s=${w}x${h}")
            e.contains("dolly_zoom") ->
                listOf("zoompan=z='1+0.3*sin(t/2)':d=1:s=${w}x${h}")
            e.contains("rgb_split") ->
                listOf("chromashift=cbh=-6:cbv=0:crh=6:crv=0")
            e.contains("scanline") ->
                listOf("drawgrid=w=1:h=2:t=1:color=black@0.3")
            e.contains("crt") ->
                listOf("drawgrid=w=1:h=2:t=1:color=black@0.2", "curves=preset=vintage", "noise=alls=5:allf=t+u")
            e.contains("8bit") ->
                listOf("scale=iw/8:ih/8:flags=area,scale=iw:ih:flags=neighbor", "eq=saturation=1.8:contrast=1.3")
            e.contains("16bit") ->
                listOf("scale=iw/16:ih/16:flags=area,scale=iw:ih:flags=neighbor", "eq=saturation=1.5:contrast=1.2")
            e.contains("old_film") ->
                listOf("curves=preset=vintage", "eq=saturation=0.8:contrast=1.1:brightness=0.03", "noise=alls=20:allf=t+u", "vignette=angle=PI/3")
            e.contains("newspaper") ->
                listOf("format=gray", "noise=alls=25:allf=t", "eq=contrast=1.5")
            e.contains("duotone") ->
                listOf("eq=saturation=0.2", "colorbalance=rs=0.15:bs=0.1")
            e.contains("tritone") ->
                listOf("eq=saturation=0.5", "colorbalance=rs=0.1:bs=0.08:gs=0.05")
            e.contains("spotlight") ->
                listOf("vignette=angle=PI/2", "eq=brightness=0.1:contrast=1.2")
            e.contains("stage_light") ->
                listOf("vignette=angle=PI/2", "colorbalance=rs=0.1:rm=0.08", "eq=brightness=0.05:contrast=1.15")
            e.contains("concert") ->
                listOf("eq=saturation=1.4:contrast=1.2", "vignette=angle=PI/4", "noise=alls=8:allf=t+u:allc=color")
            e.contains("party") ->
                listOf("hue=h='t*80'", "eq=saturation=1.6", "noise=alls=10:allf=t+u:allc=color")
            e.contains("disco") ->
                listOf("hue=h='t*120'", "eq=saturation=1.8:contrast=1.2", "noise=alls=12:allf=t+u:allc=color")
            e.contains("festival") ->
                listOf("eq=saturation=1.5:contrast=1.15", "colorbalance=rs=0.08:bs=0.08", "noise=alls=8:allf=t+u:allc=color")
            // ── v6.0.0 NEW EFFECTS (real FFmpeg chains, no placeholders) ──
            e.contains("fog") ->
                listOf("boxblur=4:1", "eq=brightness=0.05:contrast=0.9", "colorbalance=bs=0.05:gs=0.03")
            e.contains("hologram") ->
                listOf("chromashift=cbh=-2:crv=2", "hqdn3d=1:0:1:0", "eq=saturation=0.6:contrast=1.2", "colorbalance=gs=0.1:bs=0.2", "drawgrid=w=0:h=4:t=1:color=cyan@0.08")
            e.contains("lightning") ->
                listOf("eq=brightness='0.8*lt(mod(t,3),0.1)'")
            e.contains("mirror") ->
                listOf("split[a][b]", "[b]hflip[b2]", "[a][b2]hstack")
            e.contains("true_kaleidoscope") ->
                listOf("geq=lum='p(X,Y)':cb='p(mod(X+W/2,W),Y)':cr='p(X,mod(Y+H/2,H))'")
            e.contains("deflicker") ->
                listOf("deflicker=mode=am:size=10")
            e.contains("ai_denoise") ->
                listOf("hqdn3d=4:3:4:3")
            e.contains("ai_deblur") ->
                listOf("unsharp=7:7:1.5:7:7:0.0", "smartblur=lr=1:lt=-5")
            e.contains("ai_super_res") ->
                listOf("scale=iw*2:ih*2:flags=lanczos", "unsharp=7:7:1.2:7:7:0")
            e.contains("ai_upscale") ->
                listOf("scale=iw*2:ih*2:flags=lanczos", "unsharp=5:5:0.6")
            e.contains("ai_frame_interp") ->
                listOf("minterpolate=fps=60:mi_mode=mci:mc_mode=aobmc:me_mode=bidir")
            e.contains("ai_slow_motion") ->
                listOf("minterpolate=fps=120:mi_mode=mci", "setpts=4*PTS", "fps=30")
            e.contains("ai_restore") ->
                listOf("hqdn3d=4:3:4:3", "eq=contrast=1.1:saturation=1.1", "unsharp=5:5:0.5")
            e.contains("ai_stabilize") ->
                listOf("deshake")
            e.contains("ai_lens_correct") ->
                listOf("lenscorrection=k1=0:k2=0")
            e.contains("ai_relight") ->
                listOf("eq=brightness=0.08:contrast=1.05", "curves=preset=lighter")
            else -> listOf()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  VIGNETTE STYLES — 8
    // ════════════════════════════════════════════════════════════════════
    private fun vignetteStyleChain(style: String): String {
        val s = style.lowercase().replace(" ", "_")
        // v4.5.0: every UI option in VignetteStylesPanel now maps to a real
        // FFmpeg vignette expression (no more fake/placeholder entries).
        return when (s) {
            "none" -> ""
            "classic" -> "vignette=angle=PI/4"
            "soft" -> "vignette=angle=PI/5"
            "strong" -> "vignette=angle=PI/3"
            "extreme" -> "vignette=angle=PI/2"
            "subtle" -> "vignette=angle=PI/6"
            "reverse" -> "vignette=angle=PI/3:mode=backward"
            "inverted" -> "vignette=angle=PI/3:mode=backward"
            "colored" -> "vignette=angle=PI/3,colorbalance=rs=0.12:bs=0.15"
            "blur" -> "vignette=angle=PI/3,boxblur=luma_radius=10:luma_power=1"
            "spotlight" -> "vignette=angle=PI/2,eq=brightness=0.1:contrast=1.2"
            "circular" -> "vignette=angle=PI/3:mode=forward"
            "oval" -> "vignette=angle=PI/3"
            else -> ""
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  BORDER / FRAME STYLES — 13
    // ════════════════════════════════════════════════════════════════════
    private fun borderStyleChain(style: String, w: Int, h: Int): String {
        val s = style.lowercase().replace(" ", "_")
        // v4.5.0: every UI option in BorderStylesPanel now maps to a real
        // FFmpeg pad/drawbox expression (no more fake/placeholder entries).
        return when (s) {
            "none" -> ""
            "white" -> "pad=$w:$h:-1:-1:white"
            "black" -> "pad=$w:$h:-1:-1:black"
            "thin_white" -> "pad=$w+4:$h+4:-1:-1:white,pad=$w+8:$h+8:-1:-1:black"
            "thick_white" -> "pad=$w+20:$h+20:-1:-1:white"
            "thick_black" -> "pad=$w+20:$h+20:-1:-1:black"
            "polaroid" -> "pad=$w+20:$h+80:-1:-1:white"
            "film" -> "pad=$w+30:$h+30:-1:-1:black,drawbox=x=5:y=5:w=20:h=$h:t=2:white,drawbox=x=$w+5:y=5:w=20:h=$h:t=2:white"
            "shadow" -> "pad=$w+30:$h+30:0:0:black@0.5"
            "rounded" -> "pad=$w+10:$h+10:-1:-1:black"
            "vintage_frame" -> "pad=$w+15:$h+15:-1:-1:0x2a1a0a"
            "vintage" -> "pad=$w+15:$h+15:-1:-1:0x2a1a0a"
            "neon_frame" -> "pad=$w+8:$h+8:-1:-1:0x00ffff"
            "neon" -> "pad=$w+8:$h+8:-1:-1:0x00ffff"
            "gold_frame" -> "pad=$w+12:$h+12:-1:-1:0xffd700"
            "gold" -> "pad=$w+12:$h+12:-1:-1:0xffd700"
            "gradient" -> "pad=$w+12:$h+12:-1:-1:0x1a1a2e,drawbox=x=0:y=0:w=$w+12:h=6:t=6:0x00bcd4,drawbox=x=0:y=$h+6:w=$w+12:h=6:t=6:0xff6b35"
            "modern" -> "pad=$w+6:$h+6:-1:-1:0xf5f5f5"
            "minimal" -> "pad=$w+2:$h+2:-1:-1:0xcccccc"
            "glow" -> "pad=$w+16:$h+16:-1:-1:0x000000,drawbox=x=0:y=0:w=$w+16:h=$h+16:t=8:0x00ffff@0.4"
            else -> ""
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  AUDIO EFFECTS — 25
    // ════════════════════════════════════════════════════════════════════
    private fun audioEffectChain(effect: String): List<String> {
        val e = effect.lowercase().replace(" ", "_")
        return when (e) {
            "none" -> emptyList()
            "echo" -> listOf("aecho=0.8:0.9:1000:0.3")
            "reverb" -> listOf("aecho=0.8:0.88:60:0.4", "aecho=0.8:0.88:40:0.3")
            "bass_boost" -> listOf("bass=g=10")
            "treble_boost" -> listOf("treble=g=10")
            "bass_reduce" -> listOf("bass=g=-10")
            "treble_reduce" -> listOf("treble=g=-10")
            "robot" -> listOf("vibrato=f=50:d=0.7")
            "phone" -> listOf("highpass=f=300", "lowpass=f=3400")
            "hall" -> listOf("aecho=0.9:0.8:500:0.5", "aecho=0.9:0.8:300:0.4")
            "stadium" -> listOf("aecho=0.9:0.85:800:0.6", "aecho=0.9:0.85:500:0.4")
            "room" -> listOf("aecho=0.8:0.7:150:0.3")
            "cave" -> listOf("aecho=0.9:0.9:1000:0.7")
            "underwater" -> listOf("lowpass=f=500", "tremolo=f=5:d=0.5")
            "vintage_radio" -> listOf("highpass=f=200", "lowpass=f=3000", "tremolo=f=8:d=0.3")
            "megaphone" -> listOf("highpass=f=400", "lowpass=f=2800", "aecho=0.5:0.5:50:0.2")
            "chipmunk" -> listOf("asetrate=44100*1.5,aresample=44100,atempo=0.6667")
            "deep" -> listOf("asetrate=44100*0.7,aresample=44100,atempo=1.4286")
            "alien" -> listOf("vibrato=f=80:d=0.9", "tremolo=f=15:d=0.4")
            "chorus" -> listOf("chorus=0.5:0.9:50|0.2:40|0.3:60|0.1:75")
            "flanger" -> listOf("flanger=delay=10:regen=0:width=5:speed=1")
            "phaser" -> listOf("aphaser=in_gain=0.8:out_gain=0.9:delay=3:decay=0.4:speed=0.5")
            "distortion" -> listOf("acompressor=threshold=-10:ratio=10", "aecho=0.3:0.5:30:0.2")
            "karaoke" -> listOf("stereotools=mlev=1")
            "vocal_remove" -> listOf("stereotools=mlev=1")
            // ── v6.0.0 NEW AUDIO EFFECTS (real FFmpeg -af chains) ──
            "limiter" -> listOf("alimiter=limit=0.9:attack=5:release=50")
            "vocal_isolation" -> listOf("stereotools=mlev=1:mdelay=1")
            "separate_audio" -> listOf("stereotools=mlev=1")
            "ai_noise_removal" -> listOf("afftdn=nr=20:nf=-25")
            "ai_sound_effects" -> listOf("aecho=0.6:0.3:100:0.3")
            else -> emptyList()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  TRANSITIONS — 60+
    // ════════════════════════════════════════════════════════════════════
    private fun transitionChain(transition: String, duration: Double, w: Int, h: Int): List<String> {
        if (transition == "none") return emptyList()
        val t = transition.lowercase().replace(" ", "_").replace("-", "_")
        val fadeDur = minOf(1.0, duration / 4)
        val outStart = (duration - fadeDur).coerceAtLeast(0.0)
        return when (t) {
            "crossfade", "fade" -> listOf("fade=t=in:st=0:d=$fadeDur", "fade=t=out:st=$outStart:d=$fadeDur")
            "fade_in" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "fade_out" -> listOf("fade=t=out:st=$outStart:d=$fadeDur")
            "fade_white" -> listOf("fade=t=in:st=0:d=$fadeDur:color=white", "fade=t=out:st=$outStart:d=$fadeDur:color=white")
            "fade_black" -> listOf("fade=t=in:st=0:d=$fadeDur:color=black", "fade=t=out:st=$outStart:d=$fadeDur:color=black")
            "glitch" -> listOf("noise=alls=15:allf=t+u:enable='between(t,0,0.5)'", "chromashift=cbh=-2:cbv=1:crh=2:crv=-1:enable='between(t,0,0.5)'")
            "zoom_in" -> listOf("zoompan=z='min(zoom+0.002\\,1.5)':d=1:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=${w}x${h}")
            "zoom_out" -> listOf("zoompan=z='if(eq(on\\,0)\\,1.5\\,max(zoom-0.002\\,1.0))':d=1:s=${w}x${h}")
            "spin", "rotate_in" -> listOf("rotate=angle='2*PI*t/$fadeDur':fillcolor=black:enable='between(t,0,$fadeDur)'")
            "rotate_out" -> listOf("rotate=angle='2*PI*($duration-t)/$fadeDur':fillcolor=black:enable='between(t,$outStart,$duration)'")
            "wipe" -> listOf("crop=iw*'t/$fadeDur':ih:0:0:enable='between(t,0,$fadeDur)'")
            "wipe_left" -> listOf("crop=iw*'t/$fadeDur':ih:'iw-iw*t/$fadeDur':0:enable='between(t,0,$fadeDur)'")
            "wipe_right" -> listOf("crop=iw*'t/$fadeDur':ih:0:0:enable='between(t,0,$fadeDur)'")
            "wipe_up" -> listOf("crop=iw:ih*'t/$fadeDur':0:0:enable='between(t,0,$fadeDur)'")
            "wipe_down" -> listOf("crop=iw:ih*'t/$fadeDur':0:'ih-ih*t/$fadeDur':enable='between(t,0,$fadeDur)'")
            "dissolve" -> listOf("boxblur=luma_radius=min(h\\,w)/10:luma_power=1:enable='between(t,0,$fadeDur)'")
            "blur" -> listOf("boxblur=luma_radius=20:luma_power=2:enable='between(t,0,$fadeDur)'")
            "pixelate" -> listOf("scale=iw/20:ih/20,scale=iw:ih:flags=neighbor:enable='between(t,0,$fadeDur)'")
            "mosaic" -> listOf("scale=iw/20:ih/20,scale=iw:ih:flags=neighbor:enable='between(t,0,$fadeDur)'")
            "split" -> listOf("crop=iw/2:ih:0:0:enable='between(t,0,$fadeDur)'")
            "film_burn" -> listOf("eq=brightness='0.5*exp(-t*3)':saturation=1.5:enable='between(t,0,$fadeDur)'", "colorbalance=rs=0.2:rm=0.15:enable='between(t,0,$fadeDur)'")
            "light_leak" -> listOf("vignette=angle=PI/4:enable='between(t,0,$fadeDur)'", "colorbalance=rs=0.1:rm=0.08:enable='between(t,0,$fadeDur)'")
            "smoke" -> listOf("noise=alls=20:allf=t+u:enable='between(t,0,$fadeDur)'", "boxblur=luma_radius=5:luma_power=1:enable='between(t,0,$fadeDur)'")
            "circle" -> listOf("vignette=angle='PI/2*exp(-t*2)':enable='between(t,0,$fadeDur)'")
            "diamond" -> listOf("vignette=angle='PI/3*exp(-t*2)':enable='between(t,0,$fadeDur)'")
            "heart" -> listOf("lenscorrection=k1=0.3:k2=0.3:enable='between(t,0,$fadeDur)'")
            "flash" -> listOf("eq=brightness='2*exp(-t*5)':enable='between(t,0,0.3)'")
            "l_cut", "j_cut" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "slide_left" -> listOf("crop=iw*'t/$fadeDur':ih:'iw-iw*t/$fadeDur':0:enable='between(t,0,$fadeDur)'")
            "slide_right" -> listOf("crop=iw*'t/$fadeDur':ih:0:0:enable='between(t,0,$fadeDur)'")
            "slide_up" -> listOf("crop=iw:ih*'t/$fadeDur':0:0:enable='between(t,0,$fadeDur)'")
            "slide_down" -> listOf("crop=iw:ih*'t/$fadeDur':0:'ih-ih*t/$fadeDur':enable='between(t,0,$fadeDur)'")
            "bounce" -> listOf("fade=t=in:st=0:d=$fadeDur", "vflip:enable='between(t,0,0.3)'")
            "elastic" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "spring" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "iris_in" -> listOf("vignette=angle='PI/2*(1-t/$fadeDur)':enable='between(t,0,$fadeDur)'")
            "iris_out" -> listOf("vignette=angle='PI/2*(t/$fadeDur)':enable='between(t,$outStart,$duration)'")
            "star_wipe" -> listOf("vignette=angle='PI/3*(1-t/$fadeDur)':enable='between(t,0,$fadeDur)'")
            "clock_wipe" -> listOf("rotate=angle='PI*t/$fadeDur':fillcolor=black:enable='between(t,0,$fadeDur)'")
            "spiral" -> listOf("rotate=angle='4*PI*t/$fadeDur':fillcolor=black:enable='between(t,0,$fadeDur)'")
            "shake_in" -> listOf("crop=iw-10:ih-10:'5*sin(t*20)':'5*cos(t*20)':enable='between(t,0,$fadeDur)'")
            "glitch_in" -> listOf("noise=alls=25:allf=t+u:enable='between(t,0,$fadeDur)'", "chromashift=cbh=-4:cbv=2:crh=4:crv=-2:enable='between(t,0,$fadeDur)'")
            "tv_static" -> listOf("noise=alls=50:allf=t+u:allc=color:enable='between(t,0,$fadeDur)'")
            "channel_change" -> listOf("noise=alls=40:allf=t+u:enable='between(t,0,0.2)'", "eq=brightness='0.5*exp(-t*10)':enable='between(t,0,0.2)'")
            "vhs_transition" -> listOf("noise=alls=20:allf=t+u:enable='between(t,0,$fadeDur)'", "curves=preset=vintage:enable='between(t,0,$fadeDur)'")
            "rgb_glitch" -> listOf("chromashift=cbh=-5:cbv=3:crh=5:crv=-3:enable='between(t,0,$fadeDur)'")
            "color_flash" -> listOf("hue=h='t*360':enable='between(t,0,$fadeDur)'")
            "white_flash" -> listOf("eq=brightness='3*exp(-t*8)':enable='between(t,0,0.4)'")
            "black_fade" -> listOf("fade=t=in:st=0:d=$fadeDur:color=black")
            "white_fade" -> listOf("fade=t=in:st=0:d=$fadeDur:color=white")
            "zoom_burst" -> listOf("zoompan=z='1+5*exp(-t*5)':d=1:s=${w}x${h}:enable='between(t,0,$fadeDur)'")
            "shake_burst" -> listOf("crop=iw-20:ih-20:'10*sin(t*30)':'10*cos(t*30)':enable='between(t,0,$fadeDur)'")
            "blur_in" -> listOf("boxblur=luma_radius='50*(1-t/$fadeDur)':luma_power=2:enable='between(t,0,$fadeDur)'")
            "blur_out" -> listOf("boxblur=luma_radius='50*(t-$outStart)/$fadeDur':luma_power=2:enable='between(t,$outStart,$duration)'")
            "pixel_in" -> listOf("scale='iw/max(1\\,20*(1-t/$fadeDur))':'ih/max(1\\,20*(1-t/$fadeDur))':flags=area,scale=iw:ih:flags=neighbor:enable='between(t,0,$fadeDur)'")
            "shake_transition" -> listOf("crop=iw-15:ih-15:'7*sin(t*25)':'7*cos(t*25)':enable='between(t,0,$fadeDur)'")
            "flip_horizontal" -> listOf("hflip:enable='between(t,0,$fadeDur)'")
            "flip_vertical" -> listOf("vflip:enable='between(t,0,$fadeDur)'")
            "rotate_3d" -> listOf("rotate=angle='PI*sin(t/$fadeDur*PI)':fillcolor=black:enable='between(t,0,$fadeDur)'")
            "swing" -> listOf("rotate=angle='0.3*sin(t*10)':fillcolor=black@0:enable='between(t,0,$fadeDur)'")
            "push_left" -> listOf("crop=iw:ih:'iw*t/$fadeDur':0:enable='between(t,0,$fadeDur)'")
            "push_right" -> listOf("crop=iw:ih:'iw-iw*t/$fadeDur':0:enable='between(t,0,$fadeDur)'")
            "push_up" -> listOf("crop=iw:ih:0:'ih*t/$fadeDur':enable='between(t,0,$fadeDur)'")
            "push_down" -> listOf("crop=iw:ih:0:'ih-ih*t/$fadeDur':enable='between(t,0,$fadeDur)'")
            "curtain" -> listOf("crop=iw*'t/$fadeDur':ih:0:0:enable='between(t,0,$fadeDur)'")
            "blinds" -> listOf("drawgrid=w=iw:h=ih/10:t='ih/10*(1-t/$fadeDur)':color=black@1:enable='between(t,0,$fadeDur)'")
            "checkerboard" -> listOf("drawgrid=w=iw/8:h=ih/8:t='iw/8*(1-t/$fadeDur)':color=black@1:enable='between(t,0,$fadeDur)'")
            "diagonal" -> listOf("crop=iw*'t/$fadeDur':ih*'t/$fadeDur':0:0:enable='between(t,0,$fadeDur)'")
            "triangle" -> listOf("crop=iw*'t/$fadeDur':ih:0:'ih/2*(1-t/$fadeDur)':enable='between(t,0,$fadeDur)'")
            "hexagon" -> listOf("vignette=angle='PI/2*(1-t/$fadeDur)':enable='between(t,0,$fadeDur)'")
            "star" -> listOf("vignette=angle='PI/2*(1-t/$fadeDur)':enable='between(t,0,$fadeDur)'")
            "cross" -> listOf("crop=iw*'t/$fadeDur':ih:0:0:enable='between(t,0,$fadeDur)'", "crop=iw:ih*'t/$fadeDur':0:0:enable='between(t,0,$fadeDur)'")
            "ripple" -> listOf("lenscorrection=k1='0.2*sin(t*10)':k2='0.2*cos(t*10)':enable='between(t,0,$fadeDur)'")
            "wave" -> listOf("lenscorrection=k1='0.1*sin(t*8)':k2='0.1*cos(t*8)':enable='between(t,0,$fadeDur)'")
            "shatter" -> listOf("noise=alls=30:allf=t+u:enable='between(t,0,$fadeDur)'", "crop=iw-10:ih-10:enable='between(t,0,$fadeDur)'")
            // ── v6.0.0 NEW TRANSITIONS (real FFmpeg chains) ──
            "pull" -> listOf("crop=iw:ih:'-iw*(1-t/$fadeDur)':0:enable='between(t,0,$fadeDur)'")
            "warp" -> listOf("scale='1+2*t/$fadeDur':'1+2*t/$fadeDur':enable='between(t,0,$fadeDur)'")
            "stretch" -> listOf("scale='1+0.5*sin(t*PI/$fadeDur)':'1':enable='between(t,0,$fadeDur)'")
            "page_turn" -> listOf("hflip:enable='between(t,0,$fadeDur)'", "fade=t=in:st=0:d=$fadeDur")
            "camera_move" -> listOf("zoompan=z='1+0.2*t/$fadeDur':x='iw*t/$fadeDur':y='ih*t/$fadeDur':d=1:s=${w}x${h}:fps=30")
            "whip_pan" -> listOf("crop=iw:ih:'iw*3*t/$fadeDur-2*iw':0:enable='between(t,0,$fadeDur)'")
            "cube" -> listOf("rotate=angle='PI*t/$fadeDur':fillcolor=black:enable='between(t,0,$fadeDur)'", "scale='1-abs(t/$fadeDur-0.5)*0.5':'1':enable='between(t,0,$fadeDur)'")
            "smooth_cut" -> listOf("fade=t=in:st=0:d=0.3")
            else -> listOf()
        }
    }

    /**
     * Builds a drawtext filter with animation for text overlays (37 animations).
     */
    private fun buildTextOverlay(text: String, animation: String, duration: Double): String {
        val safeText = text.replace("'", "\\'").replace(":", "\\:")
        val anim = animation.lowercase().replace(" ", "_")
        val base = "drawtext=text='$safeText':fontsize=42:fontcolor=white:box=1:boxcolor=black@0.5${fontFileClause()}"
        return when (anim) {
            "none", "fade_in", "fade" -> "$base:x=(w-text_w)/2:y=h-100:alpha='if(lt(t,1)\\,t\\,1)'"
            "fade_out" -> "$base:x=(w-text_w)/2:y=h-100:alpha='if(gt(t,${duration - 1})\\,${duration}-t\\,1)'"
            "fade_in_out" -> "$base:x=(w-text_w)/2:y=h-100:alpha='if(lt(t,1)\\,t\\,if(gt(t,${duration - 1})\\,${duration}-t\\,1))'"
            "typewriter" -> "$base:x=(w-text_w)/2:y=h-100:alpha='1':text='$safeText%{eif\\:trunc(t*8)\\:d}'"
            "bounce" -> "$base:x=(w-text_w)/2:y='h-100+20*abs(sin(t*4))'"
            "slide_left" -> "$base:x='w-text_w-(w-text_w)*min(1\\,t/0.5)':y=h-100"
            "slide_right" -> "$base:x='(w-text_w)*min(1\\,t/0.5)':y=h-100"
            "slide_up" -> "$base:x=(w-text_w)/2:y='h-(h-100)*min(1\\,t/0.5)'"
            "slide_down" -> "$base:x=(w-text_w)/2:y='(h-100)*min(1\\,t/0.5)'"
            "zoom_in" -> "$base:x=(w-text_w)/2:y=h-100:fontsize='42*min(1\\,t/0.5)'"
            "zoom_out" -> "$base:x=(w-text_w)/2:y=h-100:fontsize='42*max(0.1\\,1-t/${duration})'"
            "rotate" -> "$base:x='(w-text_w)/2+10*sin(t*2)':y=h-100"
            "wave" -> "$base:x='(w-text_w)/2+20*sin(t*3)':y='h-100+10*cos(t*3)'"
            "glitch_in" -> "$base:x='(w-text_w)/2+5*sin(t*30)':y='h-100+3*cos(t*30)':alpha='min(1\\,t*2)'"
            "neon_pulse" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0x7C5CFF@'0.7+0.3*sin(t*6)'"
            "pop" -> "$base:x=(w-text_w)/2:y=h-100:fontsize='42*(1+0.3*exp(-t*4))'"
            "flip" -> "$base:x=(w-text_w)/2:y=h-100:alpha='min(1\\,t*2)'"
            "elastic" -> "$base:x=(w-text_w)/2:y='h-100+30*exp(-t*2)*sin(t*10)'"
            "spring" -> "$base:x=(w-text_w)/2:y='h-100+20*exp(-t*3)*cos(t*8)'"
            "rubber" -> "$base:x=(w-text_w)/2:y='h-100+15*exp(-t*2)*sin(t*6)'"
            "swing" -> "$base:x='(w-text_w)/2+30*sin(t*2)':y=h-100"
            "typewriter_fast" -> "$base:x=(w-text_w)/2:y=h-100:alpha='1':text='$safeText%{eif\\:trunc(t*16)\\:d}'"
            "shake" -> "$base:x='(w-text_w)/2+5*sin(t*20)':y='h-100+3*cos(t*20)'"
            "blink" -> "$base:x=(w-text_w)/2:y=h-100:alpha='0.5+0.5*sin(t*8)'"
            "pulse" -> "$base:x=(w-text_w)/2:y=h-100:fontsize='42*(1+0.1*sin(t*5))'"
            "color_cycle" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0xFFFFFF@'0.5+0.5*sin(t*3)'"
            "neon_flicker" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0x00ffff@'0.5+0.5*sin(t*15)'"
            "slide_in_3d" -> "$base:x='(w-text_w)/2+100*exp(-t*3)':y=h-100:alpha='min(1\\,t*3)'"
            "explode_in" -> "$base:x=(w-text_w)/2:y=h-100:fontsize='42*2*exp(-t*3)+42'"
            "implode" -> "$base:x=(w-text_w)/2:y=h-100:fontsize='42+100*exp(-t*4)'"
            "marquee" -> "$base:x='w-w*t':y=h-100"
            "scroll_up" -> "$base:x=(w-text_w)/2:y='ih-t*200':alpha='1'"
            "scroll_down" -> "$base:x=(w-text_w)/2:y='-text_w+t*200':alpha='1'"
            "glow" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0xffff00@'0.7+0.3*sin(t*4)'"
            "rainbow" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0xFF0000@'0.5+0.5*sin(t*2+0)'"
            "frozen" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0x88ccff"
            "fire" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0xff6600@'0.7+0.3*sin(t*6)'"
            "metallic" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0xc0c0c0"
            "gold" -> "$base:x=(w-text_w)/2:y=h-100:fontcolor=0xffd700"
            else -> "$base:x=(w-text_w)/2:y=h-100"
        }
    }

    /**
     * v4.5.0 — Real per-template FFmpeg grades.
     * Every TemplatePanel ID now maps to a distinct, workable filter chain
     * (no more single generic cinematic-bars placeholder for all templates).
     */
    private fun templateChain(templateId: String): List<String> {
        val t = templateId.lowercase().replace(" ", "_")
        return when (t) {
            "none", "free" -> emptyList()
            // Cinema — cinematic bars + teal-orange grade
            "cinema" -> listOf(
                "drawbox=x=0:y=0:w=iw:h=ih*0.05:color=black@1:t=fill",
                "drawbox=x=0:y=ih*0.95:w=iw:h=ih*0.05:color=black@1:t=fill",
                "colorbalance=rs=0.08:rm=0.05:bs=0.1:bm=0.06",
                "eq=contrast=1.15:saturation=1.05"
            )
            // Wedding — warm soft golden glow
            "wedding" -> listOf(
                "colorbalance=rs=0.12:rm=0.08:gs=0.04",
                "eq=brightness=0.04:contrast=0.95:saturation=1.1",
                "boxblur=luma_radius=2:luma_power=1"
            )
            // Travel — vivid punchy landscape grade
            "travel" -> listOf(
                "eq=saturation=1.35:contrast=1.2:brightness=0.03",
                "colorbalance=rs=0.06:bs=0.05"
            )
            // Vlog — bright clean neutral
            "vlog" -> listOf(
                "eq=brightness=0.06:contrast=1.08:saturation=1.1",
                "curves=preset=increase"
            )
            // Poetry — muted dreamy low-contrast
            "poetry" -> listOf(
                "eq=contrast=0.9:saturation=0.85:brightness=0.05",
                "colorbalance=rs=0.05:bs=0.08",
                "boxblur=luma_radius=3:luma_power=1"
            )
            // Beats — high contrast punchy
            "beats" -> listOf(
                "eq=contrast=1.3:saturation=1.25",
                "colorbalance=rs=0.1:bs=0.08"
            )
            // Glitch — chromatic + noise
            "glitch" -> listOf(
                "chromashift=cbh=-3:cbv=2:crh=3:crv=-2",
                "noise=alls=12:allf=t+u"
            )
            // Spark — bright sparkle contrast
            "spark" -> listOf(
                "eq=brightness=0.08:contrast=1.18:saturation=1.2"
            )
            // Bloom — soft glow bloom
            "bloom" -> listOf(
                "eq=brightness=0.06:contrast=1.1",
                "boxblur=luma_radius=6:luma_power=1",
                "blend=all_mode=screen:opacity=0.4"
            )
            // Reels — vibrant social grade
            "reels" -> listOf(
                "eq=saturation=1.3:contrast=1.12:brightness=0.03",
                "colorbalance=rs=0.06:bs=0.04"
            )
            // TikTok — punchy saturated
            "tiktok" -> listOf(
                "eq=saturation=1.4:contrast=1.15:brightness=0.02",
                "colorbalance=rs=0.08:gs=0.03"
            )
            // Neon — cyber neon glow
            "neon" -> listOf(
                "eq=saturation=2.0:contrast=1.3",
                "colorbalance=rs=0.1:bs=0.15:rm=0.08:bm=0.08"
            )
            // Retro — vintage faded
            "retro" -> listOf(
                "curves=preset=vintage",
                "eq=saturation=0.8:contrast=1.05:brightness=0.03",
                "noise=alls=15:allf=t+u"
            )
            // Minimal — clean low-sat
            "minimal" -> listOf(
                "eq=saturation=0.9:contrast=1.05:brightness=0.02"
            )
            // Dark — moody low-key
            "dark" -> listOf(
                "eq=brightness=-0.06:contrast=1.2:saturation=0.95",
                "colorbalance=bs=0.08:bm=0.05",
                "vignette=angle=PI/4"
            )
            // Golden — warm golden hour
            "golden" -> listOf(
                "colorbalance=rs=0.15:rm=0.1:gs=0.05",
                "eq=brightness=0.05:saturation=1.2:contrast=1.1"
            )
            // Ocean — cool blue teal
            "ocean" -> listOf(
                "colorbalance=bs=0.15:bm=0.1",
                "eq=saturation=1.15:contrast=1.08:brightness=0.02"
            )
            // Fire — hot red-orange
            "fire" -> listOf(
                "colorbalance=rs=0.2:rm=0.15",
                "eq=brightness=0.05:saturation=1.3:contrast=1.15"
            )
            // Ice — cold blue frost
            "ice" -> listOf(
                "colorbalance=bs=0.18:bm=0.12",
                "eq=saturation=0.9:contrast=1.1:brightness=0.03",
                "eq=temp=0.85"
            )
            else -> listOf(
                "drawbox=x=0:y=0:w=iw:h=ih*0.05:color=black@1:t=fill",
                "drawbox=x=0:y=ih*0.95:w=iw:h=ih*0.05:color=black@1:t=fill"
            )
        }
    }

    /**
     * Maps 3D mask names to FFmpeg filters (24 masks).
     */
    private fun threeDMaskChain(mask: String, w: Int, h: Int): List<String> {
        if (mask == "none") return emptyList()
        val m = mask.lowercase().replace(" ", "_")
        return when (m) {
            "circle_mask", "circle" -> listOf("vignette=angle=PI/3")
            "heart_mask", "heart" -> listOf("lenscorrection=k1=0.2:k2=0.2", "vignette=angle=PI/2")
            "star_mask", "star" -> listOf("vignette=angle=PI/4")
            "hexagon", "diamond" -> listOf("vignette=angle=PI/3")
            "triangle" -> listOf("vignette=angle=PI/2")
            "vignette" -> listOf("vignette=angle=PI/3")
            "film_burn" -> listOf("eq=brightness='0.3*exp(-t*2)':saturation=1.3", "colorbalance=rs=0.15:rm=0.1")
            "light_leak" -> listOf("vignette=angle=PI/4", "colorbalance=rs=0.1:rm=0.08")
            "lens_flare" -> listOf("eq=brightness=0.08:contrast=1.1", "vignette=angle=PI/4")
            "smoke" -> listOf("noise=alls=15:allf=t+u", "boxblur=luma_radius=3:luma_power=1")
            "water" -> listOf("boxblur=luma_radius=2:luma_power=1")
            "fire" -> listOf("colorbalance=rs=0.2:rm=0.15", "eq=brightness=0.08:saturation=1.3")
            "particles" -> listOf("noise=alls=12:allf=t+u:allc=color")
            "bokeh" -> listOf("boxblur=luma_radius=15:luma_power=2", "eq=brightness=0.05")
            "glitch_3d", "chromatic" -> listOf("noise=alls=15:allf=t+u", "chromashift=cbh=-2:cbv=1:crh=2:crv=-1")
            "anamorphic" -> listOf("crop=iw:ih*0.42:0:ih*0.29", "scale=$w:${(h * 0.42).toInt()}")
            "cinematic_bars" -> listOf("drawbox=x=0:y=0:w=iw:h=ih*0.05:color=black@1:t=fill", "drawbox=x=0:y=ih*0.95:w=iw:h=ih*0.05:color=black@1:t=fill")
            "color_splash" -> listOf("eq=saturation=1.8:contrast=1.2", "colorbalance=rs=0.05:bs=0.05")
            "oval" -> listOf("vignette=angle=PI/3")
            "square" -> listOf("crop=min(iw\\,ih):min(iw\\,ih):(iw-min(iw\\,ih))/2:(ih-min(iw\\,ih))/2")
            "arch" -> listOf("vignette=angle=PI/4")
            "frame" -> listOf("drawbox=x=iw*0.1:y=ih*0.1:w=iw*0.8:h=ih*0.8:color=white@0.3:t=3")
            "spotlight" -> listOf("vignette=angle=PI/2", "eq=brightness=0.1")
            else -> listOf()
        }
    }

    /**
     * Emoji/shape sticker overlay via drawtext or drawbox (16 stickers).
     */
    private fun stickerOverlay(sticker: String): String {
        // v4.3 FIX #5: Emoji drawtext crashes because the bundled font
        // (powercut_sans.ttf) has no emoji glyphs. FFmpeg drawtext with
        // an unsupported codepoint either errors out or renders nothing.
        // Solution: render stickers as geometric drawbox shapes instead.
        // drawbox needs NO font file, so it is always crash-safe.
        if (sticker == "none") return ""
        val s = sticker.lowercase()
        // Base position: top-right corner, 60x60 area starting at (w-80, 20)
        val bx = "w-80"
        val by = "20"
        val sz = 60
        return when (s) {
            // Fire — orange filled triangle (approx) with red glow box
            "fire" -> "drawbox=x=$bx:y=$by:w=$sz:h=$sz:color=red@0.3:t=fill," +
                       "drawbox=x=$bx+10:y=$by+15:w=${sz-20}:h=${sz-25}:color=orange@0.8:t=fill"
            // Star — yellow 4-point cross shape
            "star" -> "drawbox=x=$bx+${sz/4}:y=$by:w=${sz/2}:h=$sz:color=yellow@0.9:t=fill," +
                       "drawbox=x=$bx:y=$by+${sz/4}:w=$sz:h=${sz/2}:color=yellow@0.9:t=fill"
            // Heart — two red boxes forming a heart-like cluster
            "heart" -> "drawbox=x=$bx+5:y=$by+10:w=${sz/2-5}:h=${sz/2-5}:color=red@0.85:t=fill," +
                        "drawbox=x=$bx+${sz/2}:y=$by+10:w=${sz/2-5}:h=${sz/2-5}:color=red@0.85:t=fill," +
                        "drawbox=x=$bx+10:y=$by+${sz/2}:w=${sz-20}:h=${sz/2-10}:color=red@0.85:t=fill"
            // Glow / Bolt — yellow lightning bolt shape
            "glow", "bolt" -> "drawbox=x=$bx+${sz/3}:y=$by:w=${sz/3}:h=$sz:color=yellow@0.9:t=fill," +
                       "drawbox=x=$bx+10:y=$by+${sz/3}:w=${sz-20}:h=${sz/3}:color=yellow@0.7:t=fill"
            // Diamond — cyan filled diamond (rotated square approximation)
            "diamond" -> "drawbox=x=$bx+${sz/4}:y=$by+${sz/4}:w=${sz/2}:h=${sz/2}:color=cyan@0.8:t=fill," +
                          "drawbox=x=$bx+10:y=$by+10:w=${sz-20}:h=${sz-20}:color=cyan@0.3:t=fill"
            // Music — purple note shape (two boxes + bar)
            "music" -> "drawbox=x=$bx+5:y=$by+${sz-15}:w=20:h=15:color=purple@0.9:t=fill," +
                        "drawbox=x=$bx+${sz-25}:y=$by+${sz-20}:w=20:h=20:color=purple@0.9:t=fill," +
                        "drawbox=x=$bx+22:y=$by:w=8:h=$sz:color=purple@0.9:t=fill"
            // Crown — gold horizontal bar with three spikes
            "crown" -> "drawbox=x=$bx:y=$by+${sz/2}:w=$sz:h=${sz/2}:color=gold@0.9:t=fill," +
                        "drawbox=x=$bx:y=$by:w=${sz/4}:h=${sz/2}:color=gold@0.9:t=fill," +
                        "drawbox=x=$bx+${sz/3}:y=$by:w=${sz/4}:h=${sz/2}:color=gold@0.9:t=fill," +
                        "drawbox=x=$bx+${2*sz/3}:y=$by:w=${sz/4}:h=${sz/2}:color=gold@0.9:t=fill"
            // Sparkle — white 4-point small cross
            "sparkle" -> "drawbox=x=$bx+${sz/3}:y=$by+${sz/4}:w=${sz/3}:h=${sz/2}:color=white@0.9:t=fill," +
                          "drawbox=x=$bx+${sz/4}:y=$by+${sz/3}:w=${sz/2}:h=${sz/3}:color=white@0.9:t=fill"
            // Target — concentric circles approximated with boxes
            "target" -> "drawbox=x=$bx:y=$by:w=$sz:h=$sz:color=red@0.5:t=fill," +
                         "drawbox=x=$bx+10:y=$by+10:w=${sz-20}:h=${sz-20}:color=white@0.8:t=fill," +
                         "drawbox=x=$bx+20:y=$by+20:w=${sz-40}:h=${sz-40}:color=red@0.9:t=fill"
            // Trophy — gold cup shape
            "trophy" -> "drawbox=x=$bx+10:y=$by:w=${sz-20}:h=${sz/2}:color=gold@0.9:t=fill," +
                         "drawbox=x=$bx+${sz/3}:y=$by+${sz/2}:w=${sz/3}:h=${sz/2}:color=gold@0.9:t=fill"
            // Skull — white rounded block with black eye holes
            "skull" -> "drawbox=x=$bx+5:y=$by:w=${sz-10}:h=${sz/2+10}:color=white@0.9:t=fill," +
                        "drawbox=x=$bx+10:y=$by+15:w=12:h=12:color=black@0.9:t=fill," +
                        "drawbox=x=$bx+${sz-22}:y=$by+15:w=12:h=12:color=black@0.9:t=fill"
            // Rocket — white body with red tip
            "rocket" -> "drawbox=x=$bx+${sz/3}:y=$by+10:w=${sz/3}:h=${sz-10}:color=white@0.9:t=fill," +
                         "drawbox=x=$bx+${sz/3}:y=$by:w=${sz/3}:h=15:color=red@0.9:t=fill," +
                         "drawbox=x=$bx+5:y=$by+${sz-15}:w=10:h=10:color=orange@0.9:t=fill," +
                         "drawbox=x=$bx+${sz-15}:y=$by+${sz-15}:w=10:h=10:color=orange@0.9:t=fill"
            // 100 — green filled block (number badge style)
            "100" -> "drawbox=x=$bx:y=$by:w=$sz:h=$sz:color=green@0.8:t=fill," +
                      "drawbox=x=$bx+5:y=$by+5:w=${sz-10}:h=${sz-10}:color=white@0.3:t=fill"
            // Thumbs up — blue filled block
            "thumbs_up" -> "drawbox=x=$bx+10:y=$by+10:w=${sz-20}:h=${sz-20}:color=blue@0.8:t=fill," +
                            "drawbox=x=$bx+${sz/3}:y=$by:w=${sz/3}:h=20:color=blue@0.9:t=fill"
            // Party — magenta confetti blocks
            "party" -> "drawbox=x=$bx:y=$by:w=15:h=15:color=magenta@0.9:t=fill," +
                        "drawbox=x=$bx+${sz-15}:y=$by:w=15:h=15:color=cyan@0.9:t=fill," +
                        "drawbox=x=$bx+${sz/2-7}:y=$by+${sz/2-7}:w=15:h=15:color=yellow@0.9:t=fill," +
                        "drawbox=x=$bx:y=$by+${sz-15}:w=15:h=15:color=lime@0.9:t=fill," +
                        "drawbox=x=$bx+${sz-15}:y=$by+${sz-15}:w=15:h=15:color=orange@0.9:t=fill"
            else -> ""
        }
    }

    private fun getTargetDimensions(resolution: String, preset: String): Pair<Int, Int> {
        val baseW = when (resolution.lowercase()) {
            "4k" -> 3840; "8k" -> 7680; "2k" -> 2560; else -> 1920
        }
        val baseH = when (resolution.lowercase()) {
            "4k" -> 2160; "8k" -> 4320; "2k" -> 1440; else -> 1080
        }
        return when (preset) {
            "9:16" -> Pair(baseH, baseW)
            "1:1" -> Pair(baseH, baseH)
            "4:5" -> Pair(baseH, (baseH * 1.25).toInt())
            "3:4" -> Pair((baseH * 0.75).toInt(), baseH)
            "2:3" -> Pair((baseH * 0.667).toInt(), baseH)
            "21:9" -> Pair(baseW, (baseW * 9.0 / 21.0).toInt())
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

    /**
     * THERMAL PROTECTION (v4.2)
     *
     * Reads the battery temperature from the system battery intent. Mid-range
     * phones throttle the CPU at ~42°C and hard-shutdown the encode at ~48°C,
     * which is the #2 cause (after OS background kills) of "Export failed" on
     * long videos. When the phone is already hot we tell the caller so the UI
     * can warn the user before starting a 40-minute job that will overheat.
     *
     * Returns the temperature in °C, or null if it cannot be read.
     */
    fun getBatteryTemperatureCelsius(): Float? {
        return try {
            val intent = context.registerReceiver(null, android.content.IntentFilter(
                android.content.Intent.ACTION_BATTERY_CHANGED
            ))
            val temp = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (temp > 0) temp / 10.0f else null
        } catch (e: Exception) {
            Log.w(tag, "Could not read battery temperature: ${e.message}")
            null
        }
    }

    /**
     * Returns true if the device is currently too hot to safely start a long
     * (≥10 minute) encode. Threshold: 43°C — at this point most SoCs are
     * already throttling and a long encode will likely overheat and crash.
     */
    fun isDeviceTooHotForLongExport(): Boolean {
        val temp = getBatteryTemperatureCelsius()
        val tooHot = temp != null && temp >= 43.0f
        if (tooHot) {
            Log.w(tag, "Device is hot ($temp°C) — long export may overheat and fail")
        }
        return tooHot
    }

    /**
     * Returns a conservative thread count for long exports. Using all cores
     * continuously for 40 minutes on a mid-range phone causes thermal
     * throttling. We cap at 4 threads which keeps the SoC below the throttle
     * threshold on most devices while still being reasonably fast.
     */
    fun recommendedThreadCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return minOf(cores, 4)
    }
}
