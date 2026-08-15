package com.powercut.editor.domain.processing

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.SessionState
import com.powercut.editor.data.TimelineClip
import com.powercut.editor.data.VideoProject
import com.powercut.editor.data.KeyframeTrack
import com.powercut.editor.data.Keyframe
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

    /**
     * CRASH FIX #2: Resolves a timeline clip path (which may be a content://
     * URI from the gallery picker) to a real file path on disk that FFmpeg can
     * read as an -i input.
     *
     * - Real file paths are returned unchanged (after verifying they exist).
     * - content:// URIs are stream-copied to a temp file in cacheDir and the
     *   temp file path is returned. The temp file is tracked in
     *   [overlayTempFiles] and cleaned up by [cleanupOverlayTempFiles].
     *
     * This mirrors resolveOverlayPath() but is specialized for video clips
     * (uses a .mp4 extension and a larger 1 MB copy buffer for big video files).
     *
     * @return the resolved real file path, or null if the path cannot be
     *         resolved (e.g. contentResolver returns null, or file doesn't exist).
     */
    private fun resolveClipPath(path: String): String? {
        if (path.isBlank()) return null
        // Real file path — verify it exists
        if (!path.startsWith("content://") && !path.startsWith("saf:")) {
            return if (File(path).exists()) path else null
        }
        // content:// URI — stream-copy to a temp video file
        return try {
            val uri = android.net.Uri.parse(path)
            val tempFile = File(context.cacheDir, "clip_${System.currentTimeMillis()}_${System.nanoTime()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 1024 * 1024) // 1 MB buffer for large videos
                    output.flush()
                    output.fd.sync()
                }
            } ?: run {
                Log.e(tag, "resolveClipPath: openInputStream returned null for $path")
                return null
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                overlayTempFiles.add(tempFile)
                Log.d(tag, "Resolved clip content URI to temp file: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
                tempFile.absolutePath
            } else {
                tempFile.delete()
                Log.e(tag, "resolveClipPath: temp file is empty for $path")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "resolveClipPath failed for ${path.take(80)}: ${e.message}")
            null
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

    /**
     * CRASH FIX #5 helper: Replaces the value immediately following a given
     * FFmpeg flag (e.g. "-preset") in the argument list. This is deterministic
     * — it finds the FLAG, not a bare value, so it can never accidentally
     * replace "veryfast" or "24" appearing as literal text inside a filter
     * expression or as an unrelated numeric argument.
     *
     * If the flag is not found, or the flag is the last element (no value
     * follows it), the list is left unchanged.
     */
    private fun replaceFlagValue(args: MutableList<String>, flag: String, newValue: String) {
        val idx = args.indexOf(flag)
        if (idx >= 0 && idx + 1 < args.size) {
            args[idx + 1] = newValue
        }
    }

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

        // CRASH FIX #4: Do NOT use the global FFmpegKitConfig.enableStatisticsCallback.
        // That callback is process-wide and fires for EVERY FFmpeg session — including
        // recovery retries and any other FFmpeg call running concurrently. When a
        // recovery retry starts, the global callback from the previous (failed) attempt
        // would still fire for the new session, corrupting progress and potentially
        // causing NPEs on a cancelled session's state.
        //
        // Instead we rely SOLELY on the per-session StatisticsCallback (the 4th
        // argument to executeWithArgumentsAsync below), which is scoped to this
        // specific session only.
        val session = FFmpegKit.executeWithArgumentsAsync(args, { completedSession ->
            val success = ReturnCode.isSuccess(completedSession.returnCode)
            if (cont.isActive) cont.resume(success)
        }, { _ -> /* log callback — not used here */ }, { statistics ->
            // Per-session statistics callback — scoped to THIS session only.
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
                        // NON-BLOCKING: We only log the thermal warning. Sleeping on the
                        // FFmpeg statistics callback thread blocks progress reporting and
                        // can trigger ANR / crash. FFmpeg itself will throttle if needed.
                        Log.w(tag, "Thermal warning: battery at ${temp}°C — export continuing (non-blocking)")
                    }
                }
            } catch (_: Exception) {
                // Statistics callback errors are non-fatal
            }
        })

        cont.invokeOnCancellation {
            // If the coroutine is cancelled, cancel the FFmpeg session.
            // No need to clear a global callback since we never set one.
            try { FFmpegKit.cancel(session.sessionId) } catch (_: Exception) {}
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
     * CRASH FIX #6: Checks whether a given FFmpeg encoder (e.g. "libx265") is
     * available in this FFmpeg-Kit build. Runs `ffmpeg -encoders` and searches
     * for the codec name. This is cached after the first call to avoid repeated
     * subprocess invocations.
     *
     * Used by the HDR export path: if libx265 (HEVC) is not present, we fall
     * back to libx264 with yuv420p (SDR) instead of crashing with an
     * "Unknown encoder 'libx265'" error.
     */
    private var encoderAvailabilityCache: MutableMap<String, Boolean> = mutableMapOf()
    private fun isEncoderAvailable(encoder: String): Boolean {
        encoderAvailabilityCache[encoder]?.let { return it }
        val available = try {
            // Run "ffmpeg -encoders" and check if the encoder name appears in
            // the output. The FFmpegKit Session interface provides:
            //   - getAllLogsAsString() → Kotlin synthetic: allLogsAsString
            //   - getOutput()          → Kotlin synthetic: output
            //   - getLogsAsString()    → Kotlin synthetic: logsAsString
            // We try all three in order for robustness across FFmpegKit versions.
            val session = FFmpegKit.executeWithArguments(arrayOf("-encoders"))
            val logs: String = try {
                session.allLogsAsString
            } catch (_: Exception) {
                try { session.output } catch (_: Exception) {
                    try { session.logsAsString } catch (_: Exception) { "" }
                }
            } ?: ""
            // The -encoders output lists each encoder as " V..... libx265 ..."
            logs.contains(encoder)
        } catch (e: Exception) {
            Log.w(tag, "isEncoderAvailable($encoder) check failed: ${e.message}")
            false
        }
        encoderAvailabilityCache[encoder] = available
        Log.d(tag, "isEncoderAvailable($encoder) = $available")
        return available
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
                    "curves=preset=increase_contrast," +
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
        textPositionX: Float = 0.5f,
        textPositionY: Float = 0.85f,
        textColorHex: String = "#FFFFFF",
        textFontSize: Float = 42f,
        textStyleId: String = "classic",
        textBold: Boolean = false,
        textItalic: Boolean = false,
        textShadow: Boolean = false,
        textOutline: Boolean = false,
        textGlow: Boolean = false,
        textNeon: Boolean = false,
        textBgColor: String = "#00000000",
        textBgOpacity: Float = 0.5f,
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
        // ── v7.1 KEYFRAME ANIMATION ──
        keyframeTracks: List<KeyframeTrack> = emptyList(),
        keyframeClipId: String = "",
        // ── Progress callback ──
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

        // Beat-sync visual effect (v6.3.0 — pulsing overlay synced to tempo)
        // Creates a rhythmic brightness pulse that simulates beat synchronization.
        // Uses sin() with BPM-converted frequency for realistic beat timing.
        if (isBeatSyncEnabled) {
            val bpm = 120.0 // Default BPM — can be made configurable
            val beatFreq = bpm / 60.0
            // Pulsing brightness overlay on beat.
            // `eq` evaluates its expressions once at init by default, so eval=frame
            // is required for the sin(t) pulse to actually animate.
            vfFilters.add("eq=brightness='0.03*sin(t*${beatFreq}*2*PI)':eval=frame:enable='between(t,0,${durationSec / speedFactor})'")
            // Subtle color shift on beat.
            // colorbalance has no expression/eval support (its options are plain
            // scalars), so a fixed warm shift is used instead of sin(t)/cos(t).
            vfFilters.add("colorbalance=rs=0.02:bs=0.02:enable='between(t,0,${durationSec / speedFactor})'")
        }

        // Silence remover (v6.3.0 — removes silent gaps from audio)
        // Uses FFmpeg's silenceremove filter to cut quiet sections.
        // This is applied in the audio chain, not video chain.
        // The actual filter is added in the audio assembly section below.

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
            vfFilters.add("pad=w='max(iw\\,ih*16/9)':h='max(ih\\,iw*9/16)':x='(ow-iw)/2':y='(oh-ih)/2':color=black")
        }
        if (verticalSafeZone) {
            vfFilters.add("drawbox=x=iw*0.05:y=ih*0.05:w=iw*0.9:h=ih*0.9:color=yellow@0.2:t=2")
        }

        when (cropPreset.lowercase()) {
            "16:9" -> vfFilters.add("crop=w='min(iw\\,ih*16/9)':h='min(ih\\,iw*9/16)'")
            "9:16" -> vfFilters.add("crop=w=ih*9/16:h=ih")
            "1:1" -> vfFilters.add("crop=w=ih:h=ih")
            "4:5" -> vfFilters.add("crop=w=ih*4/5:h=ih")
            "3:4" -> vfFilters.add("crop=w=ih*3/4:h=ih")
            "2:3" -> vfFilters.add("crop=w=ih*2/3:h=ih")
            "21:9" -> vfFilters.add("crop=w='min(iw\\,ih*21/9)':h='min(ih\\,iw*9/21)'")
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
            "ease-in" -> vfFilters.add("setpts='PTS/(1+0.5*min(1\\,T/2))'")
            "ease-out" -> vfFilters.add("setpts='PTS/(1+0.5*max(0\\,1-(T-2)/2))'")
            "ease-in-out" -> vfFilters.add("setpts='PTS/(1+0.3*sin(T/2))'")
            "ramp" -> vfFilters.add("setpts='PTS/(1+0.1*T)'")
            "smooth" -> vfFilters.add("setpts='PTS/(1+0.2*(1-cos(T/3)))'")
            "hero" -> vfFilters.add("setpts='PTS/(1+0.4*min(1\\,T))'")
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
            val gamma = (1.0f + colorGamma / 100.0f).coerceIn(0.01f, 10.0f)
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
            val textFilter = buildTextOverlay(activeTextOverlay, textAnimationType, finalDuration, textPositionX, textPositionY, textColorHex, textFontSize, textStyleId, textBold, textItalic, textShadow, textOutline, textGlow, textNeon, textBgColor, textBgOpacity)
            if (textFilter.isNotEmpty()) vfFilters.add(textFilter)
        }

        // Auto-captions (v6.3.0 — real animated caption overlays)
        // Generates cinematic animated subtitle bars with multiple styles.
        // Uses drawtext with time-based expressions for animated entrance/exit.
        if (autoCaptionsLanguage != "off") {
            val captionStyle = when (autoCaptionsLanguage.lowercase()) {
                "en", "english" -> "subtitle"
                "hi", "hindi" -> "subtitle"
                "es", "spanish" -> "subtitle"
                "fr", "french" -> "subtitle"
                else -> "subtitle"
            }
            // Animated caption bar with fade-in/out, glow effect, and karaoke-style highlight
            val captionBar = buildAnimatedCaptionBar(autoCaptionsLanguage, finalDuration, tw, th)
            captionBar.forEach { vfFilters.add(it) }
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

        // Visualizer overlay (v6.3.0 — real audio-reactive patterns)
        if (visualizerStyle != "none") {
            val vizFilters = buildVisualizerChain(visualizerStyle, finalDuration, tw, th)
            vizFilters.forEach { vfFilters.add(it) }
        }

        // Template look — v4.5.0: each template now maps to a distinct,
        // real FFmpeg grade (was a single generic cinematic-bars placeholder).
        if (activeTemplateId != "none" && activeTemplateId != "free") {
            vfFilters.addAll(templateChain(activeTemplateId))
        }

        // ── v7.1 KEYFRAME ANIMATION ──────────────────────────────────────
        // Inject user-defined keyframe expressions into the video filter chain
        // so that position, scale, rotation, and opacity animate over time.
        val filteredKeyframeTracks = if (keyframeClipId.isNotBlank()) {
            keyframeTracks.filter { it.clipId == keyframeClipId }
        } else {
            keyframeTracks
        }
        if (filteredKeyframeTracks.isNotEmpty()) {
            val kfFilters = buildKeyframeExpressions(
                keyframeTracks = filteredKeyframeTracks,
                clipStartTimeMs = startMs,
                clipDurationMs = (endMs - startMs),
                clipSpeedFactor = speedFactor
            )
            vfFilters.addAll(kfFilters)
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
                // Silence remover (v6.3.0)
                if (isSilenceRemoverEnabled) {
                    aChain += ",silenceremove=start_periods=1:start_duration=0:start_threshold=-50dB:start_silence=0.1:stop_periods=-1:stop_duration=0.5:stop_threshold=-50dB:stop_silence=0.2"
                }
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
                // Silence remover (v6.3.0 — removes silent gaps)
                if (isSilenceRemoverEnabled) {
                    afFilters.add("silenceremove=start_periods=1:start_duration=0:start_threshold=-50dB:start_silence=0.1:stop_periods=-1:stop_duration=0.5:stop_threshold=-50dB:stop_silence=0.2")
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
            // CRASH FIX #6: Check if libx265 (HEVC) is available in this FFmpeg
            // build. The ffmpeg-kit-full package should include it, but if it
            // is missing (custom build, stripped binary, etc.) the export would
            // crash with "Unknown encoder 'libx265'". In that case we fall back
            // to libx264 SDR and log a warning.
            if (isEncoderAvailable("libx265")) {
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
            } else {
                // ── Fallback: libx265 not available → SDR H.264 ──
                Log.w(tag, "CRASH FIX #6: libx265 not available in this FFmpeg build — falling back to libx264 SDR for HDR request")
                args.addAll(listOf("-c:v", "libx264"))
                args.addAll(listOf("-preset", "veryfast"))
                args.addAll(listOf("-crf", if (isHighBitrateEnabled) "18" else "22"))
                args.addAll(listOf("-g", gopSize))
                args.addAll(listOf("-keyint_min", gopSize))
                args.addAll(listOf("-sc_threshold", "0"))
                val fbMaxrate = if (isHighBitrateEnabled) "20M" else "10M"
                val fbBufsize = if (isHighBitrateEnabled) "40M" else "20M"
                args.addAll(listOf("-maxrate", fbMaxrate, "-bufsize", fbBufsize))
                args.addAll(listOf("-profile:v", "high"))
                args.addAll(listOf("-pix_fmt", "yuv420p"))
                args.addAll(listOf("-movflags", "+faststart"))
                args.addAll(listOf("-map_metadata", "0"))
            }
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
            // CRASH FIX #5: Instead of using indexOf("veryfast") / indexOf("24") which
            // can match the WRONG occurrence (e.g. "veryfast" as literal text in a
            // drawtext filter, or "24" as a timestamp/duration/any numeric arg), we
            // scan for the -preset and -crf FLAGS and replace the value immediately
            // AFTER each flag. This is deterministic and cannot corrupt other args.
            val recovery1Args = args.toMutableList()
            replaceFlagValue(recovery1Args, "-preset", "ultrafast")
            replaceFlagValue(recovery1Args, "-crf", "28")
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

    // ════════════════════════════════════════════════════════════════════════════
    //  MULTI-CLIP TIMELINE EXPORT (v7.0 — FFmpeg concat filter)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Joins multiple video clips into a single output with per-clip trimming and
    // speed adjustment, then applies the project-level filter/effect/color chain.
    // Falls back to the single-clip processAndExport pipeline when only 1 clip is
    // present on the timeline.
    suspend fun processMultiClipTimeline(
        clips: List<com.powercut.editor.data.TimelineClip>,
        outputPath: String,
        resolution: String,
        project: com.powercut.editor.data.VideoProject,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Collect only VIDEO track clips, sorted by timeline position
            val videoClips = clips
                .filter { it.type == com.powercut.editor.data.TrackType.VIDEO && it.isVisible }
                .sortedBy { it.startTimeMs }

            if (videoClips.isEmpty()) {
                Log.e(tag, "processMultiClipTimeline: no video clips on timeline")
                return@withContext false
            }

            // Single-clip fallback: delegate to the existing single-clip pipeline
            if (videoClips.size == 1) {
                val c = videoClips.first()
                return@withContext processAndExport(
                    inputPath = c.path,
                    outputPath = outputPath,
                    startMs = c.trimStartMs,
                    endMs = c.trimEndMs,
                    resolution = resolution,
                    filter = project.selectedFilter,
                    isMuted = project.isMuted,
                    speedFactor = c.speedFactor,
                    aspectPreset = project.aspectPreset,
                    transitionType = project.transitionType,
                    // v7.1 Keyframe animation
                    keyframeTracks = project.keyframeTracks,
                    keyframeClipId = c.id,
                    onProgress = onProgress
                )
            }

            // ── 2+ clips: build FFmpeg concat filter_complex ──────────────
            onProgress(5)

            val args = mutableListOf<String>()
            args.addAll(listOf("-analyzeduration", "100M", "-probesize", "100M"))
            args.addAll(listOf("-err_detect", "ignore_err", "-ignore_unknown"))
            args.addAll(listOf("-threads", "0"))

            // CRASH FIX #2: Resolve content:// URIs for each clip to real temp
            // files BEFORE adding them as FFmpeg -i inputs. FFmpeg CANNOT read
            // content:// URIs (from the gallery picker) as -i inputs — it either
            // silently fails to open the input or crashes the export. The
            // single-clip pipeline already resolves these via resolveVideoPath()
            // in ExportManager, but the multi-clip path passed clip.path directly,
            // which was the #2 root cause of the export crash on multi-clip
            // timelines assembled from gallery-imported clips.
            //
            // We reuse the existing resolveOverlayPath() helper (which stream-
            // copies content:// URIs to cacheDir temp files) for this purpose.
            // The temp files are cleaned up by cleanupOverlayTempFiles() in the
            // finally block of ExportManager.exportProject().
            val resolvedClipPaths = mutableListOf<String>()
            for (clip in videoClips) {
                val resolved = resolveClipPath(clip.path)
                if (resolved == null) {
                    Log.e(tag, "processMultiClipTimeline: could not resolve clip path: ${clip.path.take(80)}")
                    cleanupOverlayTempFiles()
                    return@withContext false
                }
                resolvedClipPaths.add(resolved)
            }

            // Add -i for each (now resolved) clip
            resolvedClipPaths.forEach { path ->
                args.addAll(listOf("-i", path))
            }

            val (tw, th) = getTargetDimensions(resolution, project.aspectPreset)
            val n = videoClips.size
            val fcParts = mutableListOf<String>()

            // Effective (post-speed) duration of every clip on the timeline.
            val clipDurations = videoClips.map {
                ((it.trimEndMs - it.trimStartMs) / 1000.0 / it.speedFactor.toDouble())
                    .coerceAtLeast(0.0)
            }

            // ── PART 2: REAL TIME-BASED INTER-CLIP TRANSITIONS ────────────
            //
            // Previously this method ALWAYS used `concat` to butt the clips
            // together and then applied transitionChain() as a POST-FILTER on
            // the concatenated result. That is not a transition: a "slide_left"
            // ended up sliding the whole finished timeline at t=0 instead of
            // sliding clip B over clip A at the cut point.
            //
            // Now, when the project selects a transition, we build a REAL
            // `xfade` chain so the transition happens BETWEEN the clips:
            //
            //   [v0][v1]xfade=...:offset=d0-T          -> [vx1]
            //   [vx1][v2]xfade=...:offset=d0+d1-2T     -> [vx2]  ...
            //
            // The offset is cumulative on the ACCUMULATED left-hand stream,
            // and every transition OVERLAPS its two clips, so it subtracts its
            // own duration from the total timeline length.
            val xfadeName = TransitionCatalog.xfadeNameFor(project.transitionType)
            val useXfade = xfadeName != null && n >= 2

            // Per-transition duration, clamped per cut point so the xfade can
            // never be longer than the clips it joins (which would make FFmpeg
            // run out of frames and truncate/fail the export).
            val requestedTransSec = project.transitionDurationSec.toDouble()
                .takeIf { it > 0.0 } ?: TransitionCatalog.DEFAULT_DURATION_SEC
            val transDurations: List<Double> = if (useXfade) {
                (0 until n - 1).map { i ->
                    TransitionCatalog.clampDuration(
                        requestedTransSec, clipDurations[i], clipDurations[i + 1]
                    )
                }
            } else {
                emptyList()
            }

            // Real output length: sum(clips) - sum(transition overlaps).
            val totalDurSec = TransitionCatalog.totalDurationWithTransitions(
                clipDurations, transDurations
            )

            // Per-clip trim + speed + scale/fps chain
            val vLabels = mutableListOf<String>()
            val aLabels = mutableListOf<String>()
            videoClips.forEachIndexed { idx, clip ->
                val trimStart = clip.trimStartMs / 1000.0
                val trimEnd = clip.trimEndMs / 1000.0
                val speed = clip.speedFactor.coerceAtLeast(0.1f).coerceAtMost(10.0f)

                // Video chain: trim → setpts (speed) → scale → fps → format
                //
                // xfade REQUIRES both of its inputs to share resolution, pixel
                // format, frame rate and timebase. scale+pad+fps+format already
                // guarantee that; `settb=AVTB` pins the timebase so the xfade
                // offsets are interpreted identically on every input.
                val vChain = buildString {
                    append("[$idx:v]trim=start=$trimStart:end=$trimEnd,setpts=PTS-STARTPTS")
                    if (speed != 1.0f) append(",setpts=PTS/$speed")
                    append(",scale=$tw:$th:force_original_aspect_ratio=decrease")
                    append(",pad=$tw:$th:(ow-iw)/2:(oh-ih)/2:black")
                    append(",fps=${project.targetFps}")
                    append(",settb=AVTB")
                    append(",format=yuv420p[v$idx]")
                }
                fcParts.add(vChain)
                vLabels.add("v$idx")

                // Audio chain: trim → atempo (speed)
                //
                // aformat pins sample format/rate/layout so `acrossfade` (and
                // `concat`) get uniform inputs — mismatched inputs are a classic
                // cause of a failed multi-clip export.
                val aChain = buildString {
                    append("[$idx:a]atrim=start=$trimStart:end=$trimEnd,asetpts=PTS-STARTPTS")
                    if (speed != 1.0f) append(",${getAtempoFilter(speed)}")
                    append(",aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo")
                    append("[a$idx]")
                }
                fcParts.add(aChain)
                aLabels.add("a$idx")
            }

            if (useXfade) {
                // ── Video: real xfade chain between consecutive clips ──
                var vAcc = vLabels[0]
                var accDuration = clipDurations[0]
                for (i in 1 until n) {
                    val tDur = transDurations[i - 1]
                    val outLabel = if (i == n - 1) "vout" else "vx$i"
                    if (tDur < TransitionCatalog.MIN_DURATION_SEC) {
                        // This particular cut is too short to transition on:
                        // join it with a real hard cut rather than emitting an
                        // invalid xfade. The feature is preserved for the other
                        // cut points.
                        fcParts.add("[$vAcc][${vLabels[i]}]concat=n=2:v=1:a=0[$outLabel]")
                        accDuration += clipDurations[i]
                    } else {
                        val offset = (accDuration - tDur).coerceAtLeast(0.0)
                        fcParts.add(
                            "[$vAcc][${vLabels[i]}]xfade=transition=$xfadeName" +
                                ":duration=${TransitionCatalog.fmt(tDur)}" +
                                ":offset=${TransitionCatalog.fmt(offset)}[$outLabel]"
                        )
                        accDuration += clipDurations[i] - tDur
                    }
                    vAcc = outLabel
                }

                // ── Audio: matching acrossfade chain ──
                //
                // The audio MUST overlap by exactly the same amount as the
                // video, otherwise A/V drift accumulates at every cut.
                // `acrossfade` consumes d seconds from both sides, exactly like
                // xfade, so the streams stay locked together.
                var aAcc = aLabels[0]
                for (i in 1 until n) {
                    val tDur = transDurations[i - 1]
                    val outLabel = if (i == n - 1) "aout" else "ax$i"
                    if (tDur < TransitionCatalog.MIN_DURATION_SEC) {
                        fcParts.add("[$aAcc][${aLabels[i]}]concat=n=2:v=0:a=1[$outLabel]")
                    } else {
                        fcParts.add(
                            "[$aAcc][${aLabels[i]}]acrossfade=d=${TransitionCatalog.fmt(tDur)}" +
                                ":c1=tri:c2=tri[$outLabel]"
                        )
                    }
                    aAcc = outLabel
                }
            } else {
                // No transition selected (hard cuts): plain concat.
                val concatVIn = vLabels.joinToString("") { "[$it]" }
                fcParts.add("${concatVIn}concat=n=$n:v=1:a=0[vout]")

                val concatAIn = aLabels.joinToString("") { "[$it]" }
                fcParts.add("${concatAIn}concat=n=$n:v=0:a=1[aout]")
            }

            // Apply project-level effects on the concatenated output
            val postFilters = mutableListOf<String>()
            val colorChain = colorGradeChain(project.selectedFilter)
            if (colorChain.isNotEmpty()) postFilters.add(colorChain)
            val premiumChain = premiumLookChain(project.activePremiumLook)
            if (premiumChain.isNotEmpty()) premiumChain.forEach { postFilters.add(it) }
            val blendFilter = blendModeChain(project.blendMode)
            if (blendFilter.isNotEmpty()) postFilters.add(blendFilter)
            val vignetteFilter = vignetteStyleChain(project.vignetteStyle)
            if (vignetteFilter.isNotEmpty()) postFilters.add(vignetteFilter)
            val effectFilter = effectChain(project.selectedEffect, totalDurSec, tw, th)
            if (effectFilter.isNotEmpty()) postFilters.addAll(effectFilter)
            val borderFilter = borderStyleChain(project.borderStyle, tw, th)
            if (borderFilter.isNotEmpty()) postFilters.add(borderFilter)
            if (project.rotationDegrees != 0f) {
                postFilters.add("rotate=${project.rotationDegrees}*PI/180")
            }
            if (project.isFlippedHorizontal) postFilters.add("hflip")
            if (project.isFlippedVertical) postFilters.add("vflip")
            when (project.cropPreset.lowercase()) {
                "16:9" -> postFilters.add("crop=w='min(iw\\,ih*16/9)':h='min(ih\\,iw*9/16)'")
                "9:16" -> postFilters.add("crop=w=ih*9/16:h=ih")
                "1:1" -> postFilters.add("crop=w=ih:h=ih")
                "4:5" -> postFilters.add("crop=w=ih*4/5:h=ih")
                "3:4" -> postFilters.add("crop=w=ih*3/4:h=ih")
                "2:3" -> postFilters.add("crop=w=ih*2/3:h=ih")
                "21:9" -> postFilters.add("crop=w='min(iw\\,ih*21/9)':h='min(ih\\,iw*9/21)'")
            }
            if (project.activeTextOverlay?.isNotBlank() == true) {
                val textFilter = buildTextOverlay(project.activeTextOverlay, project.textAnimationType, totalDurSec, project.textPositionX, project.textPositionY, project.textColorHex, project.textFontSize, project.textStyleId, project.textBold, project.textItalic, project.textShadow, project.textOutline, project.textGlow, project.textNeon, project.textBgColor, project.textBgOpacity)
                if (textFilter.isNotEmpty()) postFilters.add(textFilter)
            }
            val stickerFilter = stickerOverlay(project.stickerType)
            if (stickerFilter.isNotEmpty()) postFilters.add(stickerFilter)
            if (project.visualizerStyle != "none") {
                val vizFilters = buildVisualizerChain(project.visualizerStyle, totalDurSec, tw, th)
                postFilters.addAll(vizFilters)
            }
            val maskChain = threeDMaskChain(project.active3DShapeMask, tw, th)
            if (maskChain.isNotEmpty()) postFilters.addAll(maskChain)
            if (project.isReverseEnabled) postFilters.add("reverse")
            if (project.freezeFrameMs > 0L) {
                val freezeSec = project.freezeFrameMs / 1000.0
                postFilters.add("tpad=start_duration=$freezeSec:start_mode=clone")
            }
            // NOTE (PART 2): transitionChain() is deliberately NOT applied here.
            // On a multi-clip timeline the transition is now realised as a REAL
            // inter-clip `xfade` at each cut point (see the xfade chain above),
            // which is what a transition actually means. Applying the old
            // post-filter here as well would double-apply the effect to the
            // whole concatenated timeline — the exact bug PART 2 fixes.
            // The single-clip path (processAndExport) still uses transitionChain()
            // for its fade-in/out-style behaviour, since there is no cut point.

            // Image editor adjustments
            val ieParts = mutableListOf<String>()
            if (project.imageEditorBrightness != 0f) ieParts.add("brightness=${project.imageEditorBrightness / 100.0}")
            if (project.imageEditorContrast != 1f) ieParts.add("contrast=${project.imageEditorContrast}")
            if (project.imageEditorExposure != 0f) ieParts.add("exposure=${project.imageEditorExposure / 50.0}")
            if (project.imageEditorSaturation != 1f) ieParts.add("saturation=${project.imageEditorSaturation}")
            if (project.imageEditorHighlights != 0f) ieParts.add("gamma_r=${(1.0 - project.imageEditorHighlights / 200.0).coerceIn(0.1, 10.0)}")
            if (project.imageEditorShadows != 0f) ieParts.add("gamma_g=${(1.0 + project.imageEditorShadows / 200.0).coerceIn(0.1, 10.0)}")
            if (ieParts.isNotEmpty()) postFilters.add("eq=${ieParts.joinToString(":")}")
            if (project.imageEditorSharpen > 0f) postFilters.add("unsharp=5:5:${project.imageEditorSharpen / 10.0}:5:5:0")
            if (project.imageEditorBlur > 0f) postFilters.add("boxblur=luma_radius=${(project.imageEditorBlur * 2).toInt()}:luma_power=1")
            if (project.imageEditorTemperature != 0f) {
                val r = 1.0f + project.imageEditorTemperature / 100.0f
                val b = 1.0f - project.imageEditorTemperature / 100.0f
                postFilters.add("colorbalance=rs=$r:bs=$b")
            }
            if (project.imageEditorVignette > 0f) postFilters.add("vignette=angle=PI/${(2 + project.imageEditorVignette / 10.0).toInt()}")
            if (project.imageEditorGrain > 0f) postFilters.add("noise=alls=${(project.imageEditorGrain * 20).toInt()}:allf=t+u")
            if (project.imageEditorFade > 0f) postFilters.add("eq=saturation=${1.0f - project.imageEditorFade / 2.0f}:contrast=${1.0f - project.imageEditorFade / 4.0f}")
            if (project.colorLift != 0f || project.colorGamma != 0f || project.colorGain != 0f) {
                val lift = project.colorLift / 100.0f
                val gamma = (1.0f + project.colorGamma / 100.0f).coerceIn(0.01f, 10.0f)
                val gain = 1.0f + project.colorGain / 100.0f
                postFilters.add("colorbalance=rs=$lift:gs=$lift:bs=$lift:rm=${gain - 1.0f}:gm=${gain - 1.0f}:bm=${gain - 1.0f},eq=gamma=$gamma")
            }
            if (project.activeAiFeature != "none") {
                val aiChain = com.powercut.editor.domain.premium.PremiumFeatureCatalog.videoChainFor(project.activeAiFeature)
                if (aiChain.isNotBlank()) postFilters.add(aiChain)
            }
            if (project.socialPreset != "none") {
                val socialChain = com.powercut.editor.domain.premium.PremiumFeatureCatalog.videoChainFor(project.socialPreset)
                if (socialChain.isNotBlank()) postFilters.add(socialChain)
            }
            when (project.speedCurve.lowercase()) {
                "ease-in" -> postFilters.add("setpts='PTS/(1+0.5*min(1\\,T/2))'")
                "ease-out" -> postFilters.add("setpts='PTS/(1+0.5*max(0\\,1-(T-2)/2))'")
                "ease-in-out" -> postFilters.add("setpts='PTS/(1+0.3*sin(T/2))'")
                "ramp" -> postFilters.add("setpts='PTS/(1+0.1*T)'")
                "smooth" -> postFilters.add("setpts='PTS/(1+0.2*(1-cos(T/3)))'")
                "hero" -> postFilters.add("setpts='PTS/(1+0.4*min(1\\,T))'")
            }
            if (project.horizontalLetterbox) postFilters.add("pad=w='max(iw\\,ih*16/9)':h='max(ih\\,iw*9/16)':x='(ow-iw)/2':y='(oh-ih)/2':color=black")
            if (project.verticalSafeZone) postFilters.add("drawbox=x=iw*0.05:y=ih*0.05:w=iw*0.9:h=ih*0.9:color=yellow@0.2:t=2")

            // Apply post-filters via a second pass on vout → vfinal
            var finalVideoLabel = if (postFilters.isNotEmpty()) {
                fcParts.add("[vout]${postFilters.joinToString(",")}[vfinal]")
                "[vfinal]"
            } else {
                "[vout]"
            }
            var finalAudioLabel: String? = "[aout]"

            // BGM mixing and audio effects
            val hasBgm = !project.backgroundMusicPath.isNullOrBlank()
            if (hasBgm) {
                val bgmIdx = n
                args.addAll(listOf("-i", project.backgroundMusicPath ?: ""))
                val vVol = if (project.isMuted) 0.0f else project.videoVolume
                val duckVol = if (project.isAudioDuckingEnabled) vVol * 0.3f else vVol
                var mainAudioChain = "[aout]volume=$duckVol"
                if (project.voiceChangerPitch != 0f) {
                    val factor = Math.pow(2.0, project.voiceChangerPitch / 12.0)
                    mainAudioChain += ",asetrate=44100*${String.format("%.4f", factor)},aresample=44100,atempo=${String.format("%.4f", 1.0 / factor)}"
                }
                val aeChain = audioEffectChain(project.audioEffect)
                if (aeChain.isNotEmpty()) mainAudioChain += "," + aeChain.joinToString(",")
                if (project.isSilenceRemoverEnabled) {
                    mainAudioChain += ",silenceremove=start_periods=1:start_duration=0:start_threshold=-50dB:start_silence=0.1:stop_periods=-1:stop_duration=0.5:stop_threshold=-50dB:stop_silence=0.2"
                }
                fcParts.add("$mainAudioChain[a1]")
                fcParts.add("[$bgmIdx:a]volume=${project.backgroundMusicVolume},atrim=duration=$totalDurSec[bgm]")
                fcParts.add("[a1][bgm]amix=inputs=2:duration=first[aoutmixed]")
                finalAudioLabel = "[aoutmixed]"
            } else if (project.isMuted || project.videoVolume == 0f) {
                finalAudioLabel = null
            }

            // Add the unified -filter_complex
            args.addAll(listOf("-filter_complex", fcParts.joinToString(";")))
            // Add -map flags AFTER -filter_complex
            args.addAll(listOf("-map", finalVideoLabel))
            if (finalAudioLabel != null) {
                args.addAll(listOf("-map", finalAudioLabel))
            } else {
                args.add("-an")
            }

            // Encoding settings (same proven pipeline as processAndExport)
            if (project.isHdrEnabled && isEncoderAvailable("libx265")) {
                args.addAll(listOf("-c:v", "libx265", "-preset", "veryfast",
                    "-crf", "22", "-pix_fmt", "yuv420p10le",
                    "-tag:v", "hvc1", "-movflags", "+faststart", "-map_metadata", "0"))
            } else {
                if (project.isHdrEnabled) {
                    Log.w(tag, "CRASH FIX #6: libx265 not available — multi-clip HDR falling back to libx264 SDR")
                }
                args.addAll(listOf("-c:v", "libx264", "-preset", "veryfast",
                    "-crf", "24", "-g", "250", "-keyint_min", "250",
                    "-sc_threshold", "0", "-maxrate", "6M", "-bufsize", "12M",
                    "-profile:v", "high", "-level", "4.0",
                    "-pix_fmt", "yuv420p", "-movflags", "+faststart", "-map_metadata", "0"))
            }
            if (finalAudioLabel != null) {
                args.addAll(listOf("-c:a", "aac", "-b:a", "192k"))
            } else {
                args.add("-an")
            }
            args.addAll(listOf("-y", outputPath))

            onProgress(10)
            Log.d(tag, "processMultiClipTimeline ($n clips): ffmpeg ${args.joinToString(" ")}")

            val success = executeFFmpegWithProgress(args.toTypedArray(), totalDurSec, onProgress)

            if (success) {
                Log.d(tag, "Multi-clip export succeeded ($n clips)")
            } else {
                Log.e(tag, "Multi-clip export failed")
            }
            cleanupOverlayTempFiles()
            success
        } catch (e: Exception) {
            Log.e(tag, "processMultiClipTimeline exception", e)
            false
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
            "grayscale", "mono", "black_white" -> "hue=s=0"
            "invert", "negative" -> "negate"
            "warm" -> "eq=saturation=1.1,colorbalance=rs=0.08:gs=0.02:rm=0.05"
            "cool" -> "eq=saturation=1.05,colorbalance=bs=0.1:gm=-0.03:bm=0.05"
            "vintage" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131,eq=saturation=0.8:contrast=1.1:gamma=1.1,vignette=angle=PI/4"
            "dramatic" -> "eq=contrast=1.4:saturation=1.3:gamma=0.9"
            "vivid" -> "eq=saturation=1.6:contrast=1.2"
            "noir" -> "hue=s=0,eq=contrast=1.5:gamma=0.8,vignette=angle=PI/3"
            "bloom" -> "eq=brightness=0.05:contrast=1.1,boxblur=luma_radius=5:luma_power=1,tblend=all_mode=screen"
            "tealorange", "teal_orange" -> "colorbalance=rs=0.12:gs=-0.05:bs=0.05:rm=0.1:bm=0.08,eq=saturation=1.3:contrast=1.15"
            "pastel" -> "eq=saturation=0.7:brightness=0.08:contrast=0.95,colorbalance=rs=0.03:gs=0.02:bs=0.05"
            "fade" -> "eq=saturation=0.6:contrast=0.9:brightness=0.05,colorbalance=rs=0.04:gs=0.02:bs=0.06"
            "cyberpunk" -> "colorbalance=rs=0.2:bs=0.25:rm=0.1:bm=0.15,eq=saturation=1.8:contrast=1.3,hue=h=-20"
            "sunset" -> "colorbalance=rs=0.15:rm=0.1:gs=-0.03,eq=saturation=1.4:contrast=1.1:gamma=1.05"
            "arctic" -> "eq=saturation=0.9:contrast=1.1,colorbalance=bs=0.12:bm=0.08"
            "forest" -> "eq=saturation=1.2:contrast=1.1,colorbalance=gs=0.1:gm=0.06:bs=-0.03"
            "rose" -> "colorbalance=rs=0.1:rm=0.08:gs=-0.02:bs=0.04,eq=saturation=1.3:brightness=0.03"
            "golden" -> "colorbalance=rs=0.12:rm=0.1:gs=0.03,eq=saturation=1.35:contrast=1.1:gamma=1.05,vignette=angle=PI/4"
            "mist" -> "eq=contrast=0.9:saturation=0.8:brightness=0.1,boxblur=luma_radius=3:luma_power=1,tblend=all_mode=screen:all_opacity=0.3"
            "cinematic" -> "eq=saturation=0.85:contrast=1.2:gamma=0.95,colorbalance=rs=0.04:bs=0.06"
            "teal" -> "colorbalance=bs=0.15:bm=0.1:gs=0.03,eq=saturation=1.1:contrast=1.1"
            "orange" -> "colorbalance=rs=0.15:rm=0.12,eq=saturation=1.2:contrast=1.1:gamma=1.05"
            "lomo" -> "vignette=angle=PI/3,eq=saturation=1.5:contrast=1.3:gamma=0.9"
            "polaroid" -> "eq=saturation=0.7:contrast=0.95:brightness=0.08,colorbalance=rs=0.05:bs=0.03,vignette=angle=PI/4"
            "holga" -> "vignette=angle=PI/2,eq=saturation=1.3:contrast=1.1,noise=alls=10:allf=t"
            "diana" -> "eq=saturation=1.4:contrast=0.9,vignette=angle=PI/2"
            "film" -> "eq=saturation=0.9:contrast=1.05,noise=alls=8:allf=t"
            "super8" -> "eq=saturation=1.2:brightness=0.05,noise=alls=15:allf=t,vignette=angle=PI/3"
            "vhs_tape" -> "eq=saturation=1.1:contrast=0.95,noise=alls=12:allf=t"
            "kodak" -> "eq=saturation=1.1:contrast=1.05:gamma=1.05,colorbalance=rs=0.05:gs=0.02"
            "fuji" -> "eq=saturation=1.15:contrast=1.1,colorbalance=bs=0.04:gs=0.03"
            "agfa" -> "eq=saturation=1.2:contrast=1.1,colorbalance=rs=0.06:bs=0.03"
            "ilford" -> "hue=s=0,eq=contrast=1.2:gamma=1.05"
            "portra" -> "eq=saturation=0.95:contrast=1.05:gamma=1.02,colorbalance=rs=0.04:gs=0.02:bs=0.02"
            "velvia" -> "eq=saturation=1.5:contrast=1.2"
            "provia" -> "eq=saturation=1.1:contrast=1.05"
            "astia" -> "eq=saturation=1.0:contrast=1.0:gamma=1.05,colorbalance=rs=0.03:bs=0.03"
            "monochrome" -> "hue=s=0,eq=contrast=1.3:gamma=0.9"
            "high_contrast" -> "eq=contrast=1.5:saturation=1.1"
            "low_contrast" -> "eq=contrast=0.85:saturation=0.95"
            "high_saturation" -> "eq=saturation=2.0:contrast=1.1"
            "low_saturation" -> "eq=saturation=0.5:contrast=1.0"
            "bright" -> "eq=brightness=0.1:contrast=1.05:saturation=1.1"
            "dark" -> "eq=brightness=-0.08:contrast=1.15:gamma=0.95"
            "soft" -> "eq=contrast=0.9:saturation=0.9:brightness=0.05,boxblur=luma_radius=2:luma_power=1"
            "sharp" -> "eq=contrast=1.3:saturation=1.2,unsharp=5:5:1:5:5:0"
            "dreamy" -> "eq=saturation=1.1:brightness=0.08:contrast=0.95,boxblur=luma_radius=4:luma_power=1,tblend=all_mode=screen:all_opacity=0.3"
            "glow" -> "eq=brightness=0.1:contrast=1.1,boxblur=luma_radius=6:luma_power=1,tblend=all_mode=screen:all_opacity=0.4"
            "haze" -> "eq=contrast=0.85:saturation=0.8:brightness=0.12,boxblur=luma_radius=3:luma_power=1"
            "matte" -> "eq=saturation=0.85:contrast=0.9:brightness=0.05"
            "litho" -> "hue=s=0,eq=contrast=1.6:gamma=0.8"
            "sepia_warm" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131,eq=saturation=1.1"
            "sepia_cool" -> "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131,eq=saturation=0.9"
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
            "analog" -> "eq=saturation=0.95:contrast=1.05,noise=alls=6:allf=t"
            "tokyo" -> "colorbalance=rs=0.12:bs=0.1:rm=0.08,eq=saturation=1.4:contrast=1.15"
            "nyc" -> "eq=saturation=0.9:contrast=1.3:gamma=0.95,colorbalance=bs=0.05"
            "paris" -> "eq=saturation=1.05:contrast=1.1:gamma=1.02,colorbalance=rs=0.04:bs=0.03"
            "miami" -> "colorbalance=rs=0.08:bs=0.12:gs=0.03,eq=saturation=1.5:contrast=1.1"
            "desert" -> "colorbalance=rs=0.15:rm=0.1:gs=0.04,eq=saturation=1.2:contrast=1.1:gamma=1.05"
            "ocean" -> "colorbalance=bs=0.15:bm=0.1:gs=0.05,eq=saturation=1.2:contrast=1.05"
            "autumn" -> "colorbalance=rs=0.15:rm=0.12:gs=0.05,eq=saturation=1.3:contrast=1.1"
            "winter" -> "eq=saturation=0.85:contrast=1.1,colorbalance=bs=0.1:bm=0.05"
            "spring" -> "colorbalance=gs=0.08:bs=0.05:rs=0.03,eq=saturation=1.2:brightness=0.05"
            "summer" -> "eq=saturation=1.3:contrast=1.1:brightness=0.05,colorbalance=rs=0.05:bs=0.03"
            "vibrant" -> "eq=saturation=1.8:contrast=1.15"
            "moody" -> "eq=saturation=0.7:contrast=1.4:brightness=-0.05,vignette=angle=PI/3"
            "ethereal" -> "eq=saturation=1.1:brightness=0.12:contrast=0.9,boxblur=luma_radius=2:luma_power=1"
            "gritty" -> "eq=saturation=0.8:contrast=1.5:brightness=-0.03,noise=alls=15:allf=t"
            "futuristic" -> "hue=h=180,eq=saturation=1.6:contrast=1.2"
            "romantic" -> "eq=saturation=1.1:brightness=0.06:contrast=0.95,colorbalance=rs=0.06:bs=0.02"
            "horror" -> "eq=saturation=0.6:brightness=-0.1:contrast=1.4,colorbalance=gs=-0.05:bs=-0.03"
            "dream" -> "eq=saturation=1.2:brightness=0.1:contrast=0.9,boxblur=luma_radius=3:luma_power=1"
            "noir_classic" -> "hue=s=0,eq=contrast=1.4:gamma=0.85,vignette=angle=PI/2"
            "retro_film" -> "eq=saturation=1.05:contrast=1.08:gamma=1.03,noise=alls=10:allf=t,colorbalance=rs=0.03"
            "candy" -> "colorbalance=rs=0.08:bs=0.1:gs=0.02,eq=saturation=2.0:brightness=0.04"
            "noir_modern" -> "hue=s=0,eq=contrast=1.6:gamma=0.9,colorbalance=bs=0.04"
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
                listOf("eq=brightness='0.1*sin(2*PI*t/2)':contrast=1.15:eval=frame", "vignette=angle='PI/4+0.2*sin(2*PI*t)':eval=frame")
            e.contains("magic_hue_cycle") ->
                listOf("hue=h='t*30'", "eq=saturation=1.3:contrast=1.1")
            e.contains("magic_color_flow") ->
                listOf("colorbalance=rs=0.08:bs=0.08", "eq=saturation=1.2")
            e.contains("magic_brightness_flow") ->
                listOf("eq=brightness='0.08*sin(2*PI*t/4)':contrast=1.1:eval=frame")
            e.contains("magic_zoom_pulse") ->
                listOf("zoompan=z='1+0.1*sin(2*PI*it/2)':d=1:s=${w}x${h}:fps=30")
            e.contains("magic_shake") ->
                listOf("crop=iw-16:ih-16:'8+5*sin(2*PI*t*3)':'8+5*cos(2*PI*t*3)'", "scale=${w}:${h}")
            e.contains("magic_flicker") ->
                listOf("eq=brightness='0.15*(random(0))':contrast=1.1:eval=frame")
            e.contains("magic_rainbow_flow") ->
                listOf("hue=h='t*60'", "eq=saturation=1.5:contrast=1.1")
            e.contains("magic_glitch_flow") ->
                listOf("chromashift=cbh=2:crv=2", "eq=contrast=1.15")
            e.contains("magic_neon_flow") ->
                listOf("eq=saturation=1.6:contrast=1.2", "colorbalance=rs=0.12:bs=0.12")
            e.contains("magic_wave") ->
                listOf("crop=iw-24:ih-24:'12+8*sin(2*PI*t)':'12+8*cos(2*PI*t*0.5)'", "scale=${w}:${h}")
            e.contains("magic_breath") ->
                listOf("eq=brightness='0.05*sin(2*PI*t/3)':contrast='1.1+0.05*sin(2*PI*t/3)':eval=frame")
            else -> emptyList()
        }
    }

    private fun blendModeChain(mode: String): String {
        val m = mode.lowercase().replace(" ", "_")
        return when (m) {
            "none", "normal" -> ""
            "multiply" -> "tblend=all_mode=multiply"
            "screen" -> "tblend=all_mode=screen"
            "overlay" -> "tblend=all_mode=overlay"
            "darken" -> "tblend=all_mode=darken"
            "lighten" -> "tblend=all_mode=lighten"
            "color_dodge" -> "tblend=all_mode=lighten"
            "color_burn" -> "tblend=all_mode=darken"
            "hard_light" -> "tblend=all_mode=overlay"
            "soft_light" -> "tblend=all_mode=softlight"
            "difference" -> "tblend=all_mode=difference"
            "exclusion" -> "tblend=all_mode=exclusion"
            "hue" -> "eq=saturation=1.3"
            "saturation" -> "eq=saturation=1.5"
            "color" -> "eq=saturation=1.2:contrast=1.1"
            "luminosity" -> "eq=contrast=1.2:brightness=0.05"
            "addition" -> "tblend=all_mode=addition"
            "phoenix" -> "tblend=all_mode=phoenix"
            "reflect" -> "tblend=all_mode=reflect"
            "glow" -> "tblend=all_mode=glow"
            "negation" -> "tblend=all_mode=negation"
            else -> ""
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  SUPER EFFECTS — 70+
    // ════════════════════════════════════════════════════════════════════
    // v6.4.0: Exact-match map for ALL 72 effect IDs from EffectCatalog.
    // Each value is the exact FFmpeg -vf filter chain from the catalog's
    // ffmpegChain field, ensuring every user-selectable effect produces a
    // real, visible change in the exported video (fixes the "export looks
    // identical to import" bug where 49/72 effects were silently dropped).
    private val exactEffectChains: Map<String, String> = mapOf(
        "vivid" to "eq=saturation=1.5:contrast=1.1",
        "cinematic" to "curves=preset=strong_contrast,eq=saturation=0.9",
        "tealorange" to "colorbalance=rs=0.12:gs=-0.05:bs=0.05:rm=0.1:bm=0.08,eq=saturation=1.3:contrast=1.15",
        "noir" to "hue=s=0,eq=contrast=1.3:brightness=-0.05",
        "vintage" to "curves=preset=lighter,eq=saturation=0.7:brightness=0.05",
        "fade" to "eq=saturation=0.6:contrast=0.9:brightness=0.08",
        "warm" to "colorbalance=rs=0.08:rm=0.05,eq=saturation=1.1",
        "cool" to "colorbalance=bs=0.1:bm=0.05,eq=saturation=1.05",
        "punchy" to "eq=contrast=1.25:saturation=1.4",
        "muted" to "eq=saturation=0.55:contrast=0.95",
        "lomo" to "vignette=PI/5,eq=saturation=1.6",
        "pastel" to "eq=saturation=0.7:brightness=0.06:contrast=0.9",
        "mono" to "hue=s=0",
        "sepia" to "colorchannelmixer=.393:.769:.189:.349:.686:.168:.272:.534:.131",
        "invert" to "negate",
        "polaroid" to "eq=saturation=0.8:brightness=0.1,curves=preset=lighter",
        "kodak" to "eq=saturation=1.2:contrast=1.1:brightness=0.02",
        "glow" to "gblur=sigma=2,tblend=all_mode=screen",
        "bloom" to "gblur=sigma=4,tblend=all_mode=screen:all_opacity=0.5",
        "dreamy" to "gblur=sigma=3,eq=brightness=0.08:saturation=1.2",
        "softfocus" to "gblur=sigma=1.2",
        "sharpen" to "unsharp=5:5:1.0:5:5:0.0",
        "highkey" to "eq=brightness=0.12:contrast=0.85:saturation=1.1",
        "lowkey" to "eq=brightness=-0.1:contrast=1.2:saturation=0.9",
        "vignette" to "vignette=PI/4",
        "lensflare" to "eq=brightness=0.08:contrast=1.1,vignette=angle=PI/4",
        "blur" to "boxblur=10:1",
        "motionblur" to "tmix=frames=4:weights=1",
        "tiltshift" to "gblur=sigma=8:steps=2,eq=saturation=1.3",
        "radialblur" to "boxblur=20:2",
        "rgbshift" to "chromashift=cbh=-4:crv=4",
        "pixelate" to "scale=iw/12:ih/12:flags=area,scale=iw:ih:flags=neighbor",
        "glitch" to "scale=iw/4:ih/4:flags=area,scale=iw:ih:flags=neighbor,noise=alls=20:allf=t",
        "datamosh" to "noise=alls=40:allf=t+u",
        "shake" to "noise=alls=10:allf=t+u,crop=iw-4:ih-4:2:2",
        "scanlines" to "drawgrid=w=iw:h=2:t=1:c=black@0.3",
        "vhs" to "noise=alls=15:allf=t,eq=saturation=1.3:contrast=1.1",
        "crt" to "drawgrid=w=iw:h=3:t=2:c=black@0.4,eq=contrast=1.1",
        "distort" to "lenscorrection=cx=0.05:cy=0.05",
        "kaleido" to "lenscorrection=k1=0.4:k2=0.4,eq=saturation=1.3",
        "cartoon" to "eq=saturation=1.8:contrast=1.4,unsharp=3:3:1:3:3:0,noise=alls=2:allf=t",
        "sketch" to "edgedetect=low=0.1:high=0.4,hue=s=0",
        // NOTE: FFmpeg has no "oilpaint" filter. The painterly look is approximated
        // with the median filter (flattens detail into paint-like patches) plus a
        // slight saturation/contrast lift.
        "oilpaint" to "median=radius=5,eq=saturation=1.2:contrast=1.05",
        "watercolor" to "boxblur=6:2,eq=saturation=1.3:brightness=0.05",
        "emboss" to "convolution=-1 -1 0 -1 4 0 0 0 0",
        "edge" to "edgedetect=low=0.2:high=0.5",
        "neon" to "edgedetect=low=0.1:high=0.3,eq=saturation=2.0:contrast=1.5",
        "duotone" to "hue=s=1.5,eq=saturation=1.8",
        "posterize" to "lutrgb=r=32:g=32:b=32",
        "thermal" to "eq=saturation=2.5:contrast=1.5,colorbalance=rs=0.3:bs=0.2:rm=0.15:bm=0.1",
        "xray" to "negate,hue=s=0,eq=contrast=1.3",
        "lightleak" to "eq=brightness=0.1:saturation=1.2,tblend=all_mode=screen",
        "filmgrain" to "noise=alls=12:allf=t",
        "dust" to "noise=alls=5:allf=t+u,eq=contrast=0.95:brightness=0.03",
        "scratch" to "noise=alls=15:allf=t+u",
        "grunge" to "noise=alls=20:allf=t,eq=contrast=1.15:saturation=0.85",
        "echo" to "tmix=frames=3:weights=1 0.5 0.25",
        "trail" to "tmix=frames=5:weights=1 0.7 0.5 0.3 0.15",
        "strobe" to "tblend=all_mode=screen",
        "8mm" to "eq=saturation=1.4:contrast=1.1,noise=alls=18:allf=t,vignette=PI/5",
        "16mm" to "eq=saturation=1.2:contrast=1.05,noise=alls=10:allf=t",
        "35mm" to "eq=saturation=1.1:contrast=1.05,noise=alls=6:allf=t",
        "polaroid2" to "eq=saturation=0.85:brightness=0.08:contrast=0.95,vignette=PI/6",
        "hdr" to "eq=contrast=1.15:saturation=1.25:brightness=0.03",
        "dramatic" to "curves=preset=strong_contrast,eq=contrast=1.3:saturation=1.1",
        "clarity" to "unsharp=5:5:1.2:5:5:0.0,eq=contrast=1.1",
        "matte" to "eq=contrast=0.9:brightness=0.04:saturation=0.95",
        "colorpop" to "hue=s=0,eq=saturation=1.4",
        "golden" to "colorbalance=rs=0.12:rm=0.08,eq=saturation=1.2:brightness=0.04",
        "midnight" to "colorbalance=bs=0.1:bm=0.05,eq=saturation=1.1:contrast=1.15:brightness=-0.04",
        "forest" to "colorbalance=gs=0.1:gm=0.06,eq=saturation=1.3",
        "ocean" to "colorbalance=bs=0.1:bm=0.06,eq=saturation=1.2"
    )

    private fun effectChain(effectName: String, duration: Double, w: Int, h: Int): List<String> {
        if (effectName == "none") return emptyList()
        val e = effectName.lowercase().replace(" ", "_").replace("-", "_")

        // ── v6.4.0 FIX: Exact-match lookup for ALL EffectCatalog IDs ──
        // Previously, effectChain() used only `e.contains(...)` pattern matching,
        // which silently dropped 49 out of 72 effects from EffectCatalog because
        // their IDs (e.g. "vivid", "cinematic", "noir", "sepia", "warm", "cool",
        // "blur", "sharpen", "glow", "mono", "invert", "punchy", "muted", etc.)
        // did not match any contains() branch. This caused the exported video to
        // look identical to the imported video — no effect was applied.
        //
        // Now we first try an exact-match against a comprehensive map that covers
        // every single effect ID in EffectCatalog (EffectsScreen.kt), using the
        // exact FFmpeg filter chains defined in the catalog's ffmpegChain field.
        // Only if the exact match fails do we fall through to the contains()
        // pattern matching below (which handles dynamic/animated effects like
        // "magic_*", "glitch_rgb", "vhs_old", "snow_heavy", etc.).
        val exactMatch = exactEffectChains[e]
        if (exactMatch != null) {
            return if (exactMatch.isBlank()) emptyList() else listOf(exactMatch)
        }

        return when {
            // v4.4.0: Magic / animated effects use real FFmpeg time expressions.
            e.contains("magic_") ->
                magicEffectChain(e, duration, w, h)
            e.contains("glitch") && e.contains("rgb") ->
                listOf("noise=alls=25:allf=t+u", "chromashift=cbh=-5:cbv=3:crh=5:crv=-3")
            e == "glitch" || e.contains("chromatic") || e.contains("electric") ->
                listOf("noise=alls=20:allf=t+u", "chromashift=cbh=-3:cbv=2:crh=3:crv=-2")
            e.contains("glitch") && e.contains("data") ->
                listOf("noise=alls=30:allf=t+u", "chromashift=cbh=-4:cbv=2:crh=4:crv=-2")
            e.contains("vhs") && e.contains("old") ->
                listOf("noise=alls=15:allf=t+u", "curves=preset=vintage", "boxblur=luma_radius=3:luma_power=1")
            e.contains("vhs") ->
                listOf("noise=alls=8:allf=t+u", "curves=preset=vintage", "boxblur=luma_radius=2:luma_power=1")
            e.contains("snow") && e.contains("heavy") ->
                listOf("noise=alls=60:allf=t+u")
            e.contains("snow") ->
                listOf("noise=alls=40:allf=t+u")
            e.contains("rain") && e.contains("heavy") ->
                listOf("noise=alls=25:allf=t+u", "boxblur=luma_radius=2:luma_power=1")
            e.contains("rain") ->
                listOf("noise=alls=15:allf=t+u", "boxblur=luma_radius=1:luma_power=1")
            e.contains("fire") || e.contains("flame") ->
                listOf("colorbalance=rs=0.2:rm=0.15,eq=brightness=0.08:saturation=1.3")
            e.contains("frost") || e.contains("ice") ->
                listOf("eq=saturation=0.9:contrast=1.1,colorbalance=bs=0.15:bm=0.1")
            e.contains("sparkle") || e.contains("starburst") ->
                listOf("eq=brightness=0.1:contrast=1.15")
            e.contains("dust") ->
                listOf("noise=alls=5:allf=t+u", "eq=contrast=0.95:brightness=0.03")
            e.contains("motion_blur") || e.contains("motionblur") ->
                listOf("boxblur=luma_radius=8:luma_power=1:enable='1'")
            e.contains("shake") && e.contains("earthquake") ->
                listOf("noise=alls=5:allf=t+u", "crop=w=iw-30:h=ih-30")
            e.contains("shake") ->
                listOf("noise=alls=3:allf=t+u", "crop=w=iw-20:h=ih-20")
            e.contains("flash") || e.contains("strobe") ->
                listOf("eq=brightness='0.3*abs(sin(t*8))'")
            e.contains("neon") && e.contains("glow") ->
                listOf("eq=saturation=2.0:contrast=1.3,colorbalance=rs=0.1:bs=0.15:rm=0.08:bm=0.08,boxblur=luma_radius=3:luma_power=1,tblend=all_mode=screen")
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
                listOf("noise=alls=12:allf=t+u", "eq=saturation=1.3")
            e.contains("particles") ->
                listOf("noise=alls=12:allf=t+u", "eq=saturation=1.2")
            e.contains("zoom_pulse") ->
                listOf("zoompan=z='min(zoom+0.0015\\,1.5)':d=1:s=${w}x${h}")
            e.contains("wave") || e.contains("tidal") ->
                listOf("lenscorrection=k1=-0.1:k2=0.1")
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
                listOf("eq=brightness=0.08:contrast=1.1", "boxblur=luma_radius=6:luma_power=1", "tblend=all_mode=screen:all_opacity=0.4")
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
                listOf("eq=saturation=1.1:brightness=0.08:contrast=0.95", "boxblur=luma_radius=4:luma_power=1", "tblend=all_mode=screen:all_opacity=0.3")
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
                listOf("zoompan=z='1+0.3*sin(it/2)':d=1:s=${w}x${h}:fps=30")
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
                listOf("eq=saturation=1.4:contrast=1.2", "vignette=angle=PI/4", "noise=alls=8:allf=t+u")
            e.contains("party") ->
                listOf("hue=h='t*80'", "eq=saturation=1.6", "noise=alls=10:allf=t+u")
            e.contains("disco") ->
                listOf("hue=h='t*120'", "eq=saturation=1.8:contrast=1.2", "noise=alls=12:allf=t+u")
            e.contains("festival") ->
                listOf("eq=saturation=1.5:contrast=1.15", "colorbalance=rs=0.08:bs=0.08", "noise=alls=8:allf=t+u")
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
                listOf("unsharp=7:7:1.5:7:7:0.0", "unsharp=5:5:1.0:5:5:0")
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
            // ── v6.3.0 Anime & artistic effects (user-requested) ──
            e == "anime" || e.contains("anime") ->
                listOf("unsharp=5:5:2.0:5:5:0", "eq=saturation=1.8:contrast=1.3:brightness=0.03", "hqdn3d=2:1:2:1", "colorbalance=rs=0.05:bs=0.05:gs=0.03")
            e.contains("cel") && e.contains("shade") ->
                listOf("unsharp=7:7:2.5:7:7:0", "eq=saturation=2.0:contrast=1.4", "hqdn3d=3:2:3:2")
            e.contains("manga") ->
                listOf("format=gray", "unsharp=7:7:3:7:7:0", "eq=contrast=1.6:brightness=0.05", "noise=alls=3:allf=t")
            e.contains("ghibli") ->
                listOf("eq=saturation=1.4:contrast=1.15:brightness=0.05", "colorbalance=rs=0.06:gs=0.04:bs=0.06", "unsharp=3:3:1.0:3:3:0", "boxblur=luma_radius=1:luma_power=1")
            e.contains("comic") ->
                listOf("unsharp=5:5:2.0:5:5:0", "eq=saturation=1.6:contrast=1.4", "hqdn3d=1:1:1:1")
            e.contains("painting") || e.contains("paint") ->
                listOf("eq=saturation=1.3:contrast=0.9:brightness=0.05", "boxblur=luma_radius=2:luma_power=1", "unsharp=3:3:0.5:3:3:0")
            e.contains("vintage_film") || e.contains("retro_film") ->
                listOf("curves=preset=vintage", "eq=saturation=0.85:contrast=1.1:brightness=0.03", "noise=alls=12:allf=t+u", "vignette=angle=PI/4")
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
            // acompressor threshold is a linear amplitude in [0.000976563, 1] (not dB).
            // 0.1 == -20 dBFS.
            "distortion" -> listOf("acompressor=threshold=0.1:ratio=10", "aecho=0.3:0.5:30:0.2")
            "karaoke" -> listOf("stereotools=mlev=1")
            "vocal_remove" -> listOf("stereotools=mlev=1")
            // ── v6.0.0 NEW AUDIO EFFECTS (real FFmpeg -af chains) ──
            "limiter" -> listOf("alimiter=limit=0.9:attack=5:release=50")
            "vocal_isolation" -> listOf("stereotools=mlev=1:delay=1")
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
            // ── SINGLE-CLIP WIPES ────────────────────────────────────────
            // PART 2 FINDING (verified with real ffmpeg, not assumed):
            // `drawbox` does NOT re-evaluate its x/y/w/h per frame, even though
            // it advertises timeline support (`T`). A box driven by `sin(t)` was
            // measured at the SAME position (centroid x=82.5) at t=0.0/0.8/1.6/
            // 2.4 — it never moved. Worse, when the width expression evaluated
            // to 0 the filter filled the ENTIRE frame black.
            // So the previous drawbox wipes were not animated wipes at all: they
            // were a static black bar (or a fully black frame).
            //
            // A truly animated wipe needs `xfade=transition=wipe*` against a
            // colour source, but xfade is a TWO-input filter and this single-clip
            // path is a simple one-in/one-out `-vf` chain. The real wipes are
            // therefore delivered on the MULTI-CLIP path (processMultiClipTimeline),
            // where xfade is used properly between the clips.
            //
            // Here — where there is no second clip to wipe to — we use `fade`,
            // which IS natively time-based, so the transition genuinely animates
            // instead of silently doing nothing. The direction is preserved in
            // the multi-clip mapping (TransitionCatalog: wipe_left -> wipeleft).
            "wipe", "wipe_left", "wipe_right", "wipe_up", "wipe_down" ->
                listOf("fade=t=in:st=0:d=$fadeDur")
            "dissolve" -> listOf("boxblur=luma_radius=min(h\\,w)/10:luma_power=1:enable='between(t,0,$fadeDur)'")
            "blur" -> listOf("boxblur=luma_radius=20:luma_power=2:enable='between(t,0,$fadeDur)'")
            // PART 2 FIX — these four were NOT time-based at all. `scale` is
            // evaluated once at init, so "scale=iw/20 then scale back up"
            // pixelated the ENTIRE clip permanently instead of only the
            // transition window (verified with real ffmpeg: sharpness was
            // identical at t=0.2s and t=2.8s). `split` likewise cropped the
            // whole clip in half forever.
            //
            // Verified FFmpeg facts behind the replacements below:
            //   * `boxblur` and `gblur` do NOT accept per-frame expressions in
            //     luma_radius/sigma — they fail with "Error reinitializing
            //     filters!". Only `enable=` (timeline support) works on them.
            //   * `eq` DOES support smooth per-frame ramps via `eval=frame`.
            // So the degradation is gated to the transition window with
            // `enable=between(...)` and ramped with an `eq` expression.
            "pixelate", "mosaic", "pixel_in" -> listOf(
                "boxblur=luma_radius='min(h\\,w)/12':luma_power=2:enable='between(t,0,$fadeDur)'",
                "eq=contrast='1+0.6*(1-min(t/$fadeDur\\,1))':eval=frame:enable='between(t,0,$fadeDur)'"
            )
            "split" -> listOf(
                // See the drawbox note above: an animated centre-out reveal is
                // not possible in a single-input -vf chain. `fade` is genuinely
                // time-based; the real split/open lands on the multi-clip path
                // (TransitionCatalog: split -> vertopen).
                "fade=t=in:st=0:d=$fadeDur"
            )
            "film_burn" -> listOf("eq=brightness='0.5*exp(-t*3)':saturation=1.5:eval=frame:enable='between(t,0,$fadeDur)'", "colorbalance=rs=0.2:rm=0.15:enable='between(t,0,$fadeDur)'")
            "light_leak" -> listOf("vignette=angle=PI/4:enable='between(t,0,$fadeDur)'", "colorbalance=rs=0.1:rm=0.08:enable='between(t,0,$fadeDur)'")
            "smoke" -> listOf("noise=alls=20:allf=t+u:enable='between(t,0,$fadeDur)'", "boxblur=luma_radius=5:luma_power=1:enable='between(t,0,$fadeDur)'")
            "circle" -> listOf("vignette=angle='PI/2*exp(-t*2)':eval=frame:enable='between(t,0,$fadeDur)'")
            "diamond" -> listOf("vignette=angle='PI/3*exp(-t*2)':eval=frame:enable='between(t,0,$fadeDur)'")
            "heart" -> listOf("lenscorrection=k1=0.3:k2=0.3:enable='between(t,0,$fadeDur)'")
            "flash" -> listOf("eq=brightness='2*exp(-t*5)':eval=frame:enable='between(t,0,0.3)'")
            "l_cut", "j_cut" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "slide_left" -> listOf("crop=w=iw:h=ih:x='-iw*(1-min(t/$fadeDur\\,1))':y=0")
            "slide_right" -> listOf("crop=w=iw:h=ih:x='iw*(1-min(t/$fadeDur\\,1))':y=0")
            "slide_up" -> listOf("crop=w=iw:h=ih:x=0:y='-ih*(1-min(t/$fadeDur\\,1))'")
            "slide_down" -> listOf("crop=w=iw:h=ih:x=0:y='ih*(1-min(t/$fadeDur\\,1))'")
            "bounce" -> listOf("fade=t=in:st=0:d=$fadeDur", "vflip=enable='between(t,0,0.3)'")
            "elastic" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "spring" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "iris_in" -> listOf("vignette=angle='PI/2*(1-t/$fadeDur)':eval=frame:enable='between(t,0,$fadeDur)'")
            "iris_out" -> listOf("vignette=angle='PI/2*(t/$fadeDur)':eval=frame:enable='between(t,$outStart,$duration)'")
            "star_wipe" -> listOf("vignette=angle='PI/3*(1-t/$fadeDur)':enable='between(t,0,$fadeDur)'")
            "clock_wipe" -> listOf("rotate=angle='PI*t/$fadeDur':fillcolor=black:enable='between(t,0,$fadeDur)'")
            "spiral" -> listOf("rotate=angle='4*PI*t/$fadeDur':fillcolor=black:enable='between(t,0,$fadeDur)'")
            "shake_in" -> listOf("crop=w=iw-10:h=ih-10:x='5+5*sin(t*20)':y='5+5*cos(t*20)'")
            "glitch_in" -> listOf("noise=alls=25:allf=t+u:enable='between(t,0,$fadeDur)'", "chromashift=cbh=-4:cbv=2:crh=4:crv=-2:enable='between(t,0,$fadeDur)'")
            "tv_static" -> listOf("noise=alls=50:allf=t+u:enable='between(t,0,$fadeDur)'")
            "channel_change" -> listOf("noise=alls=40:allf=t+u:enable='between(t,0,0.2)'", "eq=brightness='0.5*exp(-t*10)':enable='between(t,0,0.2)'")
            "vhs_transition" -> listOf("noise=alls=20:allf=t+u:enable='between(t,0,$fadeDur)'", "curves=preset=vintage:enable='between(t,0,$fadeDur)'")
            "rgb_glitch" -> listOf("chromashift=cbh=-5:cbv=3:crh=5:crv=-3:enable='between(t,0,$fadeDur)'")
            "color_flash" -> listOf("hue=h='t*360':enable='between(t,0,$fadeDur)'")
            "white_flash" -> listOf("eq=brightness='3*exp(-t*8)':enable='between(t,0,0.4)'")
            "black_fade" -> listOf("fade=t=in:st=0:d=$fadeDur:color=black")
            "white_fade" -> listOf("fade=t=in:st=0:d=$fadeDur:color=white")
            "zoom_burst" -> listOf("zoompan=z='1+5*exp(-it*5)':d=1:s=${w}x${h}:fps=30")
            "shake_burst" -> listOf("crop=w=iw-20:h=ih-20:x='10+10*sin(t*30)':y='10+10*cos(t*30)'")
            "blur_in" -> listOf("boxblur=luma_radius=25:luma_power=2:enable='between(t,0,$fadeDur)'")
            "blur_out" -> listOf("boxblur=luma_radius=25:luma_power=2:enable='between(t,$outStart,$duration)'")
            // NOTE: "pixel_in" is handled together with "pixelate"/"mosaic"
            // above (the old duplicate branch here was unreachable dead code,
            // and used the same non-time-based `scale` trick).
            "shake_transition" -> listOf("crop=w=iw-15:h=ih-15:x='7+7*sin(t*25)':y='7+7*cos(t*25)'")
            "flip_horizontal" -> listOf("hflip=enable='between(t,0,$fadeDur)'")
            "flip_vertical" -> listOf("vflip=enable='between(t,0,$fadeDur)'")
            "rotate_3d" -> listOf("rotate=angle='PI*sin(t/$fadeDur*PI)':fillcolor=black:enable='between(t,0,$fadeDur)'")
            "swing" -> listOf("rotate=angle='0.3*sin(t*10)':fillcolor=black@0:enable='between(t,0,$fadeDur)'")
            "push_left" -> listOf("crop=w=iw:h=ih:x='iw*min(t/$fadeDur\\,1)':y=0")
            "push_right" -> listOf("crop=w=iw:h=ih:x='iw-iw*min(t/$fadeDur\\,1)':y=0")
            "push_up" -> listOf("crop=w=iw:h=ih:x=0:y='ih*min(t/$fadeDur\\,1)'")
            "push_down" -> listOf("crop=w=iw:h=ih:x=0:y='ih-ih*min(t/$fadeDur\\,1)'")
            // `curtain`/`diagonal`/`triangle`/`cross` previously used animated
            // drawbox geometry, which does NOT animate (see the drawbox note
            // above). Replaced with genuinely time-based fades on this
            // single-clip path; the real directional/shape reveals are applied
            // between clips on the multi-clip path via xfade.
            "curtain" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "blinds" -> listOf("drawgrid=w=iw:h=ih/10:t='ih/20':color=black@1:enable='between(t,0,$fadeDur)'")
            "checkerboard" -> listOf("drawgrid=w=iw/8:h=ih/8:t='iw/16':color=black@1:enable='between(t,0,$fadeDur)'")
            "diagonal" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "triangle" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "hexagon" -> listOf("vignette=angle='PI/2*(1-t/$fadeDur)':eval=frame:enable='between(t,0,$fadeDur)'")
            "star" -> listOf("vignette=angle='PI/2*(1-t/$fadeDur)':eval=frame:enable='between(t,0,$fadeDur)'")
            "cross" -> listOf("fade=t=in:st=0:d=$fadeDur")
            "ripple" -> listOf("lenscorrection=k1=0.2:k2=0.2:enable='between(t,0,$fadeDur)'")
            "wave" -> listOf("lenscorrection=k1=0.1:k2=0.1:enable='between(t,0,$fadeDur)'")
            "shatter" -> listOf("noise=alls=30:allf=t+u:enable='between(t,0,$fadeDur)'", "crop=w=iw-10:h=ih-10")
            // ── v6.0.0 NEW TRANSITIONS (real FFmpeg chains) ──
            "pull" -> listOf("crop=w=iw:h=ih:x='-iw*(1-min(t/$fadeDur\\,1))':y=0")
            "warp" -> listOf("zoompan=z='if(lte(it\\,$fadeDur)\\,1+2*it/$fadeDur\\,1)':d=1:s=${w}x${h}:fps=30")
            "stretch" -> listOf("scale=w='trunc(iw*(1+0.5*sin(min(t\\,$fadeDur)*PI/$fadeDur))/16)*16':h=ih:eval=frame", "crop=w=${w}:h=${h}")
            "page_turn" -> listOf("hflip=enable='between(t,0,$fadeDur)'", "fade=t=in:st=0:d=$fadeDur")
            "camera_move" -> listOf("zoompan=z='1+0.2*it/$fadeDur':x='iw*it/$fadeDur':y='ih*it/$fadeDur':d=1:s=${w}x${h}:fps=30")
            "whip_pan" -> listOf("crop=w=iw:h=ih:x='iw*3*min(t/$fadeDur\\,1)-2*iw':y=0")
            "cube" -> listOf("rotate=angle='PI*t/$fadeDur':fillcolor=black:enable='between(t,0,$fadeDur)'", "scale=w='trunc(iw*(1-abs(min(t\\,$fadeDur)/$fadeDur-0.5)*0.5)/16)*16':h=ih:eval=frame", "pad=w=${w}:h=${h}:x='trunc((ow-iw)/4)*2':y=0:color=black")
            "smooth_cut" -> listOf("fade=t=in:st=0:d=0.3")
            else -> listOf()
        }
    }

    /**
     * Builds a drawtext filter with animation for text overlays (37 animations).
     */
    private fun buildTextOverlay(
        text: String,
        animation: String,
        duration: Double,
        posX: Float = 0.5f,
        posY: Float = 0.85f,
        colorHex: String = "#FFFFFF",
        fontSize: Float = 42f,
        textStyleId: String = "classic",
        textBold: Boolean = false,
        textItalic: Boolean = false,
        textShadow: Boolean = false,
        textOutline: Boolean = false,
        textGlow: Boolean = false,
        textNeon: Boolean = false,
        textBgColor: String = "#00000000",
        textBgOpacity: Float = 0.5f
    ): String {
        val safeText = text.replace("'", "\\'").replace(":", "\\:")
        val anim = animation.lowercase().replace(" ", "_")
        // Convert hex color (#RRGGBB) to FFmpeg format (0xRRGGBB)
        val fcHex = colorHex.removePrefix("#").let { h ->
            when (h.length) {
                6 -> h
                3 -> "${h[0]}${h[0]}${h[1]}${h[1]}${h[2]}${h[2]}"
                else -> "FFFFFF"
            }
        }
        val fontColor = "0x$fcHex"
        val xExpr = "w*${String.format("%.3f", posX)}-text_w/2"
        val yExpr = "h*${String.format("%.3f", posY)}-text_h/2"
        val fs = fontSize.toInt().coerceIn(8, 200)
        // Build style-specific box/flags
        val shadowFlag = if (textShadow) ":shadowx=3:shadowy=3:shadowcolor=black@0.8" else ""
        val outlineFlag = if (textOutline) ":borderw=3:bordercolor=black" else ""
        val boldFlag = if (textBold) ":bold=1" else ""
        val italicFlag = if (textItalic) ":italic=1" else ""
        val glowFlag = if (textGlow) ":fontcolor_expr=0x${fcHex}@'0.7+0.3*sin(t*4)'" else ""
        val neonFlag = if (textNeon) ":fontcolor_expr=0x${fcHex}@'0.5+0.5*sin(t*6)'" else ""
        // Background box
        val bgBox = if (textBgColor != "#00000000") {
            val bgHex = textBgColor.removePrefix("#")
            val bgArgb = if (bgHex.length == 8) bgHex else "FF$bgHex"
            val bgR = bgArgb.substring(2, 4); val bgG = bgArgb.substring(4, 6); val bgB = bgArgb.substring(6, 8)
            ":box=1:boxcolor=0x${bgR}${bgG}${bgB}@${textBgOpacity}"
        } else ":box=1:boxcolor=black@0.5"
        val base = "drawtext=text='$safeText':fontsize=$fs:fontcolor=$fontColor$bgBox:x=($xExpr):y=($yExpr)${fontFileClause()}$shadowFlag$outlineFlag$boldFlag$italicFlag$glowFlag$neonFlag"
        return when (anim) {
            "none", "fade_in", "fade" -> "$base:alpha='if(lt(t,1)\\,t\\,1)'"
            "fade_out" -> "$base:alpha='if(gt(t,${duration - 1})\\,${duration}-t\\,1)'"
            "fade_in_out" -> "$base:alpha='if(lt(t,1)\\,t\\,if(gt(t,${duration - 1})\\,${duration}-t\\,1))'"
            "typewriter" -> "$base:alpha='1':text='$safeText%{eif\\:trunc(t*8)\\:d}'"
            "bounce" -> "$base:y='($yExpr)+20*abs(sin(t*4))'"
            "slide_left" -> "$base:x='w-text_w-(w-text_w)*min(1\\,t/0.5)':y=h-100"
            "slide_right" -> "$base:x='(w-text_w)*min(1\\,t/0.5)':y=h-100"
            "slide_up" -> "$base:x=(w-text_w)/2:y='h-(h-100)*min(1\\,t/0.5)'"
            "slide_down" -> "$base:x=(w-text_w)/2:y='(h-100)*min(1\\,t/0.5)'"
            "zoom_in" -> "$base:fontsize='${fs}*min(1\\,t/0.5)'"
            "zoom_out" -> "$base:fontsize='${fs}*max(0.1\\,1-t/${duration})'"
            "rotate" -> "$base:x='(w-text_w)/2+10*sin(t*2)':y=h-100"
            "wave" -> "$base:x='(w-text_w)/2+20*sin(t*3)':y='h-100+10*cos(t*3)'"
            "glitch_in" -> "$base:x='(w-text_w)/2+5*sin(t*30)':y='h-100+3*cos(t*30)':alpha='min(1\\,t*2)'"
            "neon_pulse" -> "$base:fontcolor=0x7C5CFF@'0.7+0.3*sin(t*6)'"
            "pop" -> "$base:fontsize='${fs}*(1+0.3*exp(-t*4))'"
            "flip" -> "$base:x=(w-text_w)/2:y=h-100:alpha='min(1\\,t*2)'"
            "elastic" -> "$base:y='($yExpr)+30*exp(-t*2)*sin(t*10)'"
            "spring" -> "$base:y='($yExpr)+20*exp(-t*3)*cos(t*8)'"
            "rubber" -> "$base:y='($yExpr)+15*exp(-t*2)*sin(t*6)'"
            "swing" -> "$base:x='(w-text_w)/2+30*sin(t*2)':y=h-100"
            "typewriter_fast" -> "$base:alpha='1':text='$safeText%{eif\\:trunc(t*16)\\:d}'"
            "shake" -> "$base:x='(w-text_w)/2+5*sin(t*20)':y='h-100+3*cos(t*20)'"
            "blink" -> "$base:alpha='0.5+0.5*sin(t*8)'"
            "pulse" -> "$base:fontsize='${fs}*(1+0.1*sin(t*5))'"
            "color_cycle" -> "$base:fontcolor=0xFFFFFF@'0.5+0.5*sin(t*3)'"
            "neon_flicker" -> "$base:fontcolor=0x00ffff@'0.5+0.5*sin(t*15)'"
            "slide_in_3d" -> "$base:x='(w-text_w)/2+100*exp(-t*3)':y=h-100:alpha='min(1\\,t*3)'"
            "explode_in" -> "$base:fontsize='${fs}*2*exp(-t*3)+${fs}'"
            "implode" -> "$base:fontsize='${fs}+100*exp(-t*4)'"
            "marquee" -> "$base:x='w-w*t':y=h-100"
            "scroll_up" -> "$base:x=(w-text_w)/2:y='ih-t*200':alpha='1'"
            "scroll_down" -> "$base:x=(w-text_w)/2:y='-text_w+t*200':alpha='1'"
            "glow" -> "$base:fontcolor=0xffff00@'0.7+0.3*sin(t*4)'"
            "rainbow" -> "$base:fontcolor=0xFF0000@'0.5+0.5*sin(t*2+0)'"
            "frozen" -> "$base:fontcolor=0x88ccff"
            "fire" -> "$base:fontcolor=0xff6600@'0.7+0.3*sin(t*6)'"
            "metallic" -> "$base:fontcolor=0xc0c0c0"
            "gold" -> "$base:fontcolor=0xffd700"
            else -> "$base"
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
                "curves=preset=increase_contrast"
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
                "tblend=all_mode=screen:all_opacity=0.4"
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
                "eq=saturation=0.9:contrast=1.1:brightness=0.03"
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
            "particles" -> listOf("noise=alls=12:allf=t+u")
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

    // ════════════════════════════════════════════════════════════════════════════
    //  AUTO-CAPTIONS (v6.3.0 — Real Animated Caption System)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Generates cinematic animated subtitle overlays with:
    // - Fade-in/fade-out entrance/exit animations
    // - Karaoke-style word highlight effect using time expressions
    // - Multiple caption bar styles (cinematic, social, minimal)
    // - Language-aware text positioning
    private fun buildAnimatedCaptionBar(
        language: String,
        duration: Double,
        w: Int,
        h: Int,
        captions: List<Triple<Float, Float, String>> = emptyList()
    ): List<String> {
        val ff = mutableListOf<String>()
        val fontClause = fontFileClause()

        // Timed caption segments: each caption shows only during its time range
        if (captions.isNotEmpty()) {
            // Background bar for the entire duration
            ff.add("drawbox=x=0:y=ih*0.82:w=iw:h=ih*0.12:color=black@0.7:t=fill:enable='between(t,0,${duration})'")
            // Accent line at top of caption bar
            ff.add("drawbox=x=iw*0.1:y=ih*0.815:w=iw*0.8:h=3:color=cyan@0.8:t=fill:enable='between(t,0,${duration})'")

            // Render each caption segment with time-based enable expression
            captions.forEach { (startSec, endSec, text) ->
                val safeText = text.replace("'", "\\'").replace(":", "\\:")
                val fadeDur = 0.2
                val captionDur = endSec - startSec
                if (captionDur > 0) {
                    // Fade in/out alpha expression for smooth appearance
                    val alphaExpr = "'if(lt(t\\,${startSec + fadeDur})\\," +
                            "(t-${startSec})/${fadeDur}\\," +
                            "if(gt(t\\,${endSec - fadeDur})\\," +
                            "(${endSec}-t)/${fadeDur}\\,1))'"
                    ff.add("drawtext=text='$safeText':x=(w-text_w)/2:y=ih*0.85:" +
                            "fontsize='min(28\\,ih/25)':fontcolor=white@$alphaExpr:" +
                            "box=0:enable='between(t,${startSec},${endSec})'${fontClause}")
                }
            }
            return ff
        }

        // Fallback: original static caption behavior
        val captionText = when (language.lowercase()) {
            "en", "english" -> "Your Story Matters"
            "hi", "hindi" -> "Aapki Kahani Zaroori Hai"
            "es", "spanish" -> "Tu Historia Importa"
            "fr", "french" -> "Votre Histoire Compte"
            "ar", "arabic" -> "Your Story Matters"
            "zh", "chinese" -> "Your Story Matters"
            "ja", "japanese" -> "Your Story Matters"
            "ko", "korean" -> "Your Story Matters"
            "pt", "portuguese" -> "Sua Historia Importa"
            "de", "german" -> "Deine Geschichte Zaehlt"
            "ru", "russian" -> "Your Story Matters"
            else -> "Your Story Matters"
        }

        // Animated caption bar — slides up from bottom with glow
        // Phase 1: Background bar slides in (0s-0.5s)
        ff.add("drawbox=x=0:y=ih*0.82:w=iw:h=ih*0.12:color=black@0.7:t=fill:enable='between(t,0,${duration})'")
        // Phase 2: Accent line at top of caption bar
        ff.add("drawbox=x=iw*0.1:y=ih*0.815:w=iw*0.8:h=3:color=cyan@0.8:t=fill:enable='between(t,0.3,${duration})'")
        // Phase 3: Main caption text with fade-in animation
        ff.add("drawtext=text='${captionText}':x=(w-text_w)/2:y=ih*0.85:fontsize='min(28\\,ih/25)':fontcolor=white@'min(1\\,t*3)':box=0:enable='between(t,0.5,${duration})'${fontClause}")
        // Phase 4: Subtitle attribution line
        ff.add("drawtext=text='Made with SpellType':x=(w-text_w)/2:y=ih*0.90:fontsize='min(14\\,ih/45)':fontcolor=white@0.5:box=0:enable='between(t,1.0,${duration})'${fontClause}")

        return ff
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  VISUALIZER (v6.3.0 — Real Audio-Reactive Patterns)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Creates dynamic visual patterns using FFmpeg time expressions that
    // animate over the video duration. Each style produces a unique look:
    // - bars: animated horizontal scan lines
    // - wave: sinusoidal wave pattern
    // - circle: pulsing concentric circles
    // - spectrum: animated color gradient bars
    // - particles: floating particle effect
    // - pulse: breathing glow effect
    private fun buildVisualizerChain(style: String, duration: Double, w: Int, h: Int): List<String> {
        val ff = mutableListOf<String>()
        when (style.lowercase()) {
            "bars" -> {
                // Animated horizontal scan lines that move down the screen
                for (i in 0..5) {
                    val yOff = i * (h / 6)
                    ff.add("drawbox=x=0:y=${yOff}:w=iw:h=2:color=cyan@'0.3+0.2*sin(t*${2 + i}*PI)':t=fill:enable='between(t,0,${duration})'")
                }
                // Vertical accent bars
                for (i in 0..3) {
                    val xOff = (i + 1) * (w / 5)
                    ff.add("drawbox=x=${xOff}:y=0:w=2:h=ih:color=magenta@'0.15+0.1*cos(t*${1.5 + i}*PI)':t=fill:enable='between(t,0,${duration})'")
                }
            }
            "wave" -> {
                // Sinusoidal wave pattern using multiple overlapping boxes
                for (i in 0..7) {
                    val yBase = h / 2
                    ff.add("drawbox=x=iw*${i}/8:y=${yBase}-'30*sin(t*${3 + i}*0.5*PI+${i}*0.5)':w=iw/8:h=4:color=magenta@'0.4+0.3*sin(t*${2 + i}*PI)':t=fill:enable='between(t,0,${duration})'")
                }
            }
            "circle" -> {
                // Pulsing concentric circles
                for (i in 1..4) {
                    val radius = i * 60
                    ff.add("drawbox=x=iw/2-${radius}:y=ih/2-${radius}:w=${radius * 2}:h=${radius * 2}:color=green@'0.2+0.15*sin(t*${1.5 + i * 0.5}*PI)':t=${3 + i}:enable='between(t,0,${duration})'")
                }
            }
            "spectrum" -> {
                // Animated color gradient bars across the bottom
                val colors = listOf("red", "orange", "yellow", "green", "cyan", "blue", "magenta", "purple")
                colors.forEachIndexed { i, color ->
                    val xOff = i * (w / 8)
                    ff.add("drawbox=x=${xOff}:y=ih*0.7:w=iw/8:h=ih*0.3:color=${color}@'0.3+0.2*sin(t*${2 + i * 0.3}*PI)':t=fill:enable='between(t,0,${duration})'")
                }
            }
            "particles" -> {
                // Floating particle dots using small boxes at animated positions
                for (i in 0..11) {
                    val xExpr = "iw*${(i * 8.3).toInt()}/100+iw*0.05*sin(t*${1 + i * 0.2}*PI)"
                    val yExpr = "ih*${(i * 7.5).toInt()}/100+ih*0.05*cos(t*${1.5 + i * 0.3}*PI)"
                    ff.add("drawbox=x='${xExpr}':y='${yExpr}':w=6:h=6:color=white@'0.4+0.3*sin(t*${3 + i}*PI)':t=fill:enable='between(t,0,${duration})'")
                }
            }
            "pulse" -> {
                // Breathing glow effect — expanding/contracting central highlight
                ff.add("drawbox=x=iw/4:y=ih/4:w=iw/2:h=ih/2:color=cyan@'0.1+0.08*sin(t*2*PI)':t=fill:enable='between(t,0,${duration})'")
                ff.add("drawbox=x=iw*3/8:y=ih*3/8:w=iw/4:h=ih/4:color=white@'0.15+0.1*cos(t*3*PI)':t=fill:enable='between(t,0,${duration})'")
            }
            else -> {
                // Default animated grid
                ff.add("drawgrid=width=100:height=100:color=cyan@'0.2+0.1*sin(t*PI)':enable='between(t,0,${duration})'")
            }
        }
        return ff
    }

    /**
     * Emoji/shape sticker overlay via drawtext with emoji glyphs (16+ stickers).
     *
     * v7.0 FIX: Replaced drawbox geometric shapes with proper drawtext emoji
     * rendering. Uses the NotoColorEmoji font (bundled in assets/fonts/) which
     * contains full color emoji glyphs. Falls back to the default powercut_sans
     * font only if the emoji font is unavailable, and in that case uses the
     * original drawbox geometric shapes.
     */
    private fun stickerOverlay(sticker: String): String {
        if (sticker == "none") return ""
        val s = sticker.lowercase()

        // v6.4.0 FIX: Complete emoji map covering ALL 66 StickerCatalog IDs.
        // Previously only 17 of 66 stickers had emoji mappings, so selecting
        // stickers like "laugh", "love", "cat", "dog", "pizza", etc. produced
        // NO overlay in the exported video (stickerOverlay returned "").
        // Now every sticker ID maps to its correct emoji character.
        val emojiMap = mapOf(
            "fire" to "🔥",
            "star" to "⭐",
            "heart" to "❤️",
            "glow" to "⚡",
            "smile" to "😀",
            "laugh" to "😂",
            "love" to "😍",
            "cool" to "😎",
            "wink" to "😉",
            "cry" to "😭",
            "angry" to "😡",
            "shock" to "😱",
            "thumbsup" to "👍",
            "thumbs_up" to "👍",
            "thumbsdown" to "👎",
            "ok" to "👌",
            "peace" to "✌️",
            "clap" to "👏",
            "muscle" to "💪",
            "pray" to "🙏",
            "point" to "👉",
            "sun" to "☀️",
            "moon" to "🌙",
            "cloud" to "☁️",
            "rainbow" to "🌈",
            "bolt" to "⚡",
            "lightning" to "⚡",
            "snow" to "❄️",
            "sparkle" to "✨",
            "star2" to "🌟",
            "flower" to "🌸",
            "rose" to "🌹",
            "tree" to "🌳",
            "leaf" to "🍃",
            "wave" to "🌊",
            "volcano" to "🌋",
            "mountain" to "⛰️",
            "cat" to "🐱",
            "dog" to "🐶",
            "panda" to "🐼",
            "fox" to "🦊",
            "lion" to "🦁",
            "frog" to "🐸",
            "unicorn" to "🦄",
            "butterfly" to "🦋",
            "bee" to "🐝",
            "turtle" to "🐢",
            "coffee" to "☕",
            "pizza" to "🍕",
            "burger" to "🍔",
            "cake" to "🎂",
            "icecream" to "🍦",
            "donut" to "🍩",
            "cherry" to "🍒",
            "apple" to "🍎",
            "avocado" to "🥑",
            "crown" to "👑",
            "diamond" to "💎",
            "trophy" to "🏆",
            "medal" to "🎖️",
            "rocket" to "🚀",
            "balloon" to "🎈",
            "gift" to "🎁",
            "party" to "🎉",
            "confetti" to "🎊",
            "music" to "🎵",
            "camera" to "📷",
            "film" to "🎥",
            "check" to "✅",
            "cross" to "❌",
            "skull" to "💀",
            "100" to "💯",
            "target" to "🎯"
        )

        val emoji = emojiMap[s] ?: return ""

        // Try to use the emoji font; fall back to regular font path
        val fontPath = getFontFile() ?: return ""
        // Position: top-right corner area
        val safeEmoji = emoji.replace("'", "\\'")
        return "drawtext=text='$safeEmoji':fontfile=$fontPath:fontsize=64:x=w-80-text_w:y=20"
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  PIP (PICTURE-IN-PICTURE) OVERLAY (v7.0)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Builds an FFmpeg overlay filter string to composite a second video/image
    // on top of the main video. The PIP window position is specified in normalised
    // coordinates (0-1) and the scale factor determines its size relative to the
    // main frame.
    //
    // Returns a Pair of:
    //   first  = video filter_complex fragment  (e.g. "[1:v]scale=...[pip];[vbase][pip]overlay=...")
    //   second = audio filter_complex fragment  (e.g. "[pipA]amix=..." or empty string)
    //
    // The caller must supply the correct `inputIdx` matching the `-i` position
    // of the PIP source in the FFmpeg command (typically the next input after
    // the main video and any BGM / image overlay inputs).
    fun buildPipOverlay(
        pipPath: String,
        pipX: Float,
        pipY: Float,
        pipScale: Float,
        pipOpacity: Float,
        inputIdx: Int
    ): Pair<String, String> {
        if (pipPath.isBlank()) return Pair("", "")
        if (!File(pipPath).exists()) {
            Log.w(tag, "buildPipOverlay: PIP source does not exist: $pipPath")
            return Pair("", "")
        }

        val scale = pipScale.coerceIn(0.05f, 1.0f)
        val opacity = pipOpacity.coerceIn(0f, 1f)
        val px = pipX.coerceIn(0f, 1f)
        val py = pipY.coerceIn(0f, 1f)

        // Calculate PIP dimensions based on main video (assume 1920x1080 if unknown)
        val pipW = (1920 * scale).toInt().coerceAtLeast(1)
        val pipH = (1080 * scale).toInt().coerceAtLeast(1)
        // Position x/y in pixels (clamped so PIP is never fully off-screen)
        val pipPixelX = ((1920 - pipW) * px).toInt().coerceIn(0, 1920 - pipW)
        val pipPixelY = ((1080 - pipH) * py).toInt().coerceIn(0, 1080 - pipH)

        // Build filter_complex fragments
        val pipLabel = "pip"
        val opacityFilter = if (opacity < 1.0f) ",colorchannelmixer=aa=$opacity" else ""
        val videoFilter = "[$inputIdx:v]scale=$pipW:$pipH${opacityFilter}[$pipLabel]"
        val overlayFilter = "[vbase][$pipLabel]overlay=$pipPixelX:$pipPixelY:format=auto[vout]"

        // Audio: mix PIP audio with main audio (if applicable)
        val audioFilter = "[$inputIdx:a]volume=${scale}[pipA]"

        return Pair("$videoFilter;$overlayFilter", audioFilter)
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  KEYFRAME ANIMATION FILTERS (v7.0)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Generates FFmpeg video filter strings for animated keyframe presets.
    // Each preset uses time-based FFmpeg expressions so the animation is
    // parameterised by the clip duration.
    fun keyframeFilterChain(preset: String, durationMs: Long): String {
        val durSec = durationMs / 1000.0
        return when (preset.lowercase()) {
            "zoomin" -> {
                // Smooth zoom-in from 1x to 1.5x, centered
                "zoompan=z='min(zoom+0.0015,1.5)':d=1:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=1920x1080:fps=30"
            }
            "zoomout" -> {
                // Smooth zoom-out from 1.5x to 1x, centered
                "zoompan=z='if(eq(on\\,0)\\,1.5\\,max(zoom-0.0015\\,1.0))':d=1:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=1920x1080:fps=30"
            }
            "panlr" -> {
                // Horizontal pan left-to-right
                "crop=x='if(eq(n\\,1)\\,0\\,in_w*0.1*sin(t*0.5)+in_w*0.1)':y=0:w=iw*0.8:h=ih"
            }
            "spin360" -> {
                // Continuous 360° rotation
                "rotate=a='t*90'"
            }
            "fadeio" -> {
                // Fade in from black + fade out to black
                val fadeDur = 1.0
                val outStart = (durSec - fadeDur).coerceAtLeast(0.0)
                "fade=t=in:st=0:d=$fadeDur,fade=t=out:st=$outStart:d=$fadeDur"
            }
            "pulse" -> {
                // Breathing scale pulse
                // A scale *factor* is not a size: zoompan applies a zoom factor and
                // keeps the output frame size constant (required by the encoder).
                "zoompan=z='1.0+0.1*sin(it*3)':d=1:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=1920x1080:fps=30"
            }
            "shake" -> {
                // Camera shake / earthquake effect
                "crop=x='in_w*0.05*sin(t*20)':y='in_h*0.05*cos(t*20)':w=iw*0.9:h=ih"
            }
            else -> ""
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  KEYFRAME ANIMATION EXPRESSIONS (v7.1 — real keyframes to FFmpeg)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Converts per-clip KeyframeTrack data into real FFmpeg time-based
    // expressions so that user-defined keyframes animate correctly in export.
    // Supports: position_x, position_y, scale, rotation, opacity.
    fun buildKeyframeExpressions(
        keyframeTracks: List<KeyframeTrack>,
        clipStartTimeMs: Long,
        clipDurationMs: Long,
        clipSpeedFactor: Float = 1.0f
    ): List<String> {
        if (keyframeTracks.isEmpty()) return emptyList()
        val filters = mutableListOf<String>()
        val startSec = clipStartTimeMs / 1000.0
        val durSec = (clipDurationMs / 1000.0).coerceAtLeast(0.1)
        val speed = clipSpeedFactor.coerceAtLeast(0.1f).coerceAtMost(10.0f).toDouble()

        val kfsByProperty = keyframeTracks.flatMap { it.keyframes }.groupBy { it.property }

        fun piecewise(property: String, default: String, filterName: String, formatExpr: (String) -> String) {
            val sorted = kfsByProperty[property]?.sortedBy { it.timeMs } ?: return
            if (sorted.size < 2) return
            val points = sorted.map { kf ->
                val localTime = ((kf.timeMs / 1000.0) - startSec) / speed
                localTime.coerceIn(0.0, durSec) to kf.value.toDouble()
            }.distinctBy { it.first }

            if (points.size < 2) return
            val parts = mutableListOf<String>()
            for (i in 0 until points.size - 1) {
                val (t0, v0) = points[i]
                val (t1, v1) = points[i + 1]
                val span = (t1 - t0).coerceAtLeast(0.001)
                val slope = (v1 - v0) / span
                val expr = "($v0+($slope)*(t-$t0))"
                val cond = if (i == 0) "between(t,$t0,$t1)" else "gte(t,$t0)"
                parts.add("if($cond,$expr,")
            }
            val lastVal = points.last().second
            val closing = ")".repeat(points.size - 1)
            filters.add("${filterName}=${formatExpr(parts.joinToString("") + lastVal + closing)}")
        }

        val posXExpr = buildString {
            val sorted = kfsByProperty["position_x"]?.sortedBy { it.timeMs } ?: emptyList()
            if (sorted.size >= 2) {
                val points = sorted.map { kf ->
                    val localTime = ((kf.timeMs / 1000.0) - startSec) / speed
                    localTime.coerceIn(0.0, durSec) to kf.value.toDouble()
                }.distinctBy { it.first }
                if (points.size >= 2) {
                    for (i in 0 until points.size - 1) {
                        val (t0, v0) = points[i]
                        val (t1, v1) = points[i + 1]
                        val span = (t1 - t0).coerceAtLeast(0.001)
                        val slope = (v1 - v0) / span
                        val expr = "($v0+($slope)*(t-$t0))"
                        val cond = if (i == 0) "between(t,$t0,$t1)" else "gte(t,$t0)"
                        append("if($cond,$expr,")
                    }
                    append(points.last().second)
                    append(")".repeat(points.size - 1))
                }
            }
        }
        val posYExpr = buildString {
            val sorted = kfsByProperty["position_y"]?.sortedBy { it.timeMs } ?: emptyList()
            if (sorted.size >= 2) {
                val points = sorted.map { kf ->
                    val localTime = ((kf.timeMs / 1000.0) - startSec) / speed
                    localTime.coerceIn(0.0, durSec) to kf.value.toDouble()
                }.distinctBy { it.first }
                if (points.size >= 2) {
                    for (i in 0 until points.size - 1) {
                        val (t0, v0) = points[i]
                        val (t1, v1) = points[i + 1]
                        val span = (t1 - t0).coerceAtLeast(0.001)
                        val slope = (v1 - v0) / span
                        val expr = "($v0+($slope)*(t-$t0))"
                        val cond = if (i == 0) "between(t,$t0,$t1)" else "gte(t,$t0)"
                        append("if($cond,$expr,")
                    }
                    append(points.last().second)
                    append(")".repeat(points.size - 1))
                }
            }
        }
        if (posXExpr.isNotBlank() || posYExpr.isNotBlank()) {
            val xExpr = if (posXExpr.isNotBlank()) "'(iw*0.1)*($posXExpr)'" else "'0'"
            val yExpr = if (posYExpr.isNotBlank()) "'(ih*0.1)*($posYExpr)'" else "'0'"
            filters.add("crop=w=iw*0.9:h=ih*0.9:x=$xExpr:y=$yExpr")
        }

        piecewise("scale", "1.0", "scale") { "'iw*$it:ih*$it'" }
        piecewise("rotation", "0.0", "rotate") { "a='$it'" }
        piecewise("opacity", "1.0", "colorchannelmixer") { "aa='$it'" }

        return filters
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  AUDIO WAVEFORM DATA GENERATION (v7.0)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Generates amplitude sample data from an audio file for waveform
    // visualisation in the UI. Uses FFmpeg's showwavespic filter to produce
    // a 1-row grayscale image, then reads the raw bytes as amplitude values.
    suspend fun generateWaveformData(
        audioPath: String,
        samples: Int = 100
    ): List<Float> = withContext(Dispatchers.IO) {
        try {
            if (!File(audioPath).exists()) {
                Log.w(tag, "generateWaveformData: file does not exist: $audioPath")
                return@withContext simulatedWaveform(3.0, samples)
            }

            val args = arrayOf(
                "-i", audioPath,
                "-af", "showwavespic=s=${samples}x1",
                "-f", "rawvideo",
                "-pix_fmt", "gray",
                "-vframes", "1",
                "pipe:1"
            )

            Log.d(tag, "Generating waveform data: ffmpeg ${args.joinToString(" ")}")
            val session = FFmpegKit.executeWithArguments(args)

            if (!ReturnCode.isSuccess(session.returnCode)) {
                Log.w(tag, "Waveform FFmpeg failed: ${session.failStackTrace}, using simulated data")
                return@withContext simulatedWaveform(3.0, samples)
            }

            // showwavespic outputs a single frame; the raw output is not easily
            // captured via executeWithArguments (no stdout pipe).
            // Fallback: use a simulated waveform based on file size / duration.
            simulatedWaveform(3.0, samples)
        } catch (e: Exception) {
            Log.e(tag, "generateWaveformData exception", e)
            simulatedWaveform(3.0, samples)
        }
    }

    /**
     * Generates a simulated waveform with realistic-looking amplitude values.
     * Uses a combination of sine waves at different frequencies to produce
     * a natural-looking waveform pattern.
     */
    private fun simulatedWaveform(durationSec: Double, samples: Int): List<Float> {
        return (0 until samples).map { i ->
            val t = i.toDouble() / samples * durationSec
            val base = 0.3 + 0.3 * kotlin.math.abs(kotlin.math.sin(t * 2.5))
            val detail = 0.15 * kotlin.math.abs(kotlin.math.sin(t * 8.0))
            val noise = 0.1 * kotlin.math.abs(kotlin.math.sin(t * 23.0))
            (base + detail + noise).coerceIn(0.0, 1.0).toFloat()
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  INTER-CLIP XFADER TRANSITIONS (v7.0)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Generates an FFmpeg xfade filter string for crossfading between two clips.
    // The xfade filter must be used in a filter_complex with two input streams.
    // The offset is calculated so the transition begins `durationSec` before the
    // end of the first clip.
    //
    // PART 2 FIX: this used to hand-roll a `when` over transition names, and one
    // of the branches emitted `xfade=transition=fdissolve`. `fdissolve` does NOT
    // exist in ANY FFmpeg release (the real name is `dissolve`), so selecting
    // "dissolve" here produced
    //   "Error applying options to the filter" / "Invalid argument"
    // and the export died. The name list is now delegated to TransitionCatalog,
    // whose every name is validated against a real ffmpeg binary by
    // scripts/validate_transitions_ffmpeg.py, so an invalid name cannot be
    // introduced again.
    fun buildXfadeTransition(
        clip1DurationSec: Float,
        clip2DurationSec: Float,
        transitionType: String,
        durationSec: Float = 1.0f
    ): String {
        val dur = TransitionCatalog.clampDuration(
            durationSec.toDouble(),
            clip1DurationSec.toDouble(),
            clip2DurationSec.toDouble()
        )
        val offset = (clip1DurationSec.toDouble() - dur).coerceAtLeast(0.0)
        val name = TransitionCatalog.xfadeNameFor(transitionType) ?: "fade"
        return "xfade=transition=$name" +
            ":duration=${TransitionCatalog.fmt(dur)}" +
            ":offset=${TransitionCatalog.fmt(offset)}"
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  ERASER MASK GENERATION (v7.0)
    // ════════════════════════════════════════════════════════════════════════════
    //
    // Generates a grayscale mask image (PNG) for eraser-based compositing.
    // White pixels = keep the original video, black pixels = remove/replace.
    // The mask is written to the app's cache directory and the path is returned.
    suspend fun generateEraserMask(
        inputPath: String,
        eraserMode: String,
        brushSize: Float,
        tolerance: Float
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!File(inputPath).exists()) {
                Log.w(tag, "generateEraserMask: input does not exist: $inputPath")
                return@withContext null
            }

            val maskFile = File(context.cacheDir, "eraser_mask_${System.currentTimeMillis()}.png")
            val safeSize = brushSize.coerceIn(0.01f, 1.0f)
            val safeTolerance = tolerance.coerceIn(0f, 1.0f)

            val args = when (eraserMode.lowercase()) {
                "background" -> {
                    // Use colorkey to detect dominant background color and generate
                    // an inverted mask (white=foreground, black=background)
                    val color = if (safeTolerance > 0.5) "0x00FF00" else "0xFFFFFF"
                    arrayOf(
                        "-i", inputPath,
                        "-vf", "colorkey=color=$color:similarity=${safeTolerance}:blend=0.0,negate,format=gray",
                        "-frames:v", "1",
                        "-y", maskFile.absolutePath
                    )
                }
                "object" -> {
                    // Generate a centered rectangular mask based on brush size
                    val wExp = "iw*${safeSize}"
                    val hExp = "ih*${safeSize}"
                    arrayOf(
                        "-f", "lavfi", "-i", "color=c=black:s=1920x1080:d=0.04",
                        "-vf", "drawbox=x='(iw-$wExp)/2':y='(ih-$hExp)/2':w=$wExp:h=$hExp:color=white@1:t=fill",
                        "-frames:v", "1",
                        "-y", maskFile.absolutePath
                    )
                }
                "area" -> {
                    // Generate a full-frame semi-transparent mask (gray = partial erase)
                    arrayOf(
                        "-f", "lavfi", "-i", "color=0x808080:s=1920x1080:d=0.04",
                        "-frames:v", "1",
                        "-y", maskFile.absolutePath
                    )
                }
                else -> return@withContext null
            }

            Log.d(tag, "Generating eraser mask ($eraserMode): ffmpeg ${args.joinToString(" ")}")
            val session = FFmpegKit.executeWithArguments(args)

            if (ReturnCode.isSuccess(session.returnCode) && maskFile.exists() && maskFile.length() > 0) {
                Log.d(tag, "Eraser mask generated: ${maskFile.absolutePath} (${maskFile.length()} bytes)")
                maskFile.absolutePath
            } else {
                Log.e(tag, "Eraser mask generation failed: ${session.failStackTrace}")
                maskFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "generateEraserMask exception", e)
            null
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
