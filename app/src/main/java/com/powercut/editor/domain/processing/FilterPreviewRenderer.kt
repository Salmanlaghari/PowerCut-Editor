package com.powercut.editor.domain.processing

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.powercut.editor.domain.filter.FilterCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders a short MP4 of the selected filter for the LIVE preview.
 *
 * The GPU preview (Media3 `RgbAdjustment`) can only represent brightness /
 * contrast / saturation / temperature / tint — i.e. plain `eq=` and
 * `colorbalance=` filters. Every other filter in [FilterCatalog] (posterize,
 * edgedetect, emboss, pixelize, colorchannelmixer, solarize, noise, …) silently
 * produced NO visible change on the preview while the same filter WAS baked
 * into the exported file. That divergence is exactly the bug this renderer
 * fixes.
 *
 * Exactly like [TransitionPreviewRenderer] / [TextAnimationPreviewRenderer], the
 * preview is rendered with the REAL FFmpeg `-vf` chain — and it derives that
 * chain from the SAME [FilterCatalog.ffmpeg] source the export pipeline uses
 * ([com.powercut.editor.domain.processing.VideoProcessor.colorGradeChain]
 * delegates to [FilterCatalog.ffmpeg]), so the grade shown live and the grade
 * burned into the exported file can never diverge.
 *
 * The preview takes a short segment of the user's video (default 3 s), applies
 * the filter, and returns a small MP4 the editor plays back (looping) the
 * instant a filter is tapped. "none" resolves to no preview so the raw video
 * keeps playing.
 */
class FilterPreviewRenderer(private val context: Context) {

    companion object {
        private const val TAG = "FilterPreviewRenderer"

        /** Preview output resolution (small → fast to render). */
        const val PREVIEW_WIDTH = 640
        const val PREVIEW_HEIGHT = 360
        const val PREVIEW_FPS = 30

        /** Seconds of footage used for the filter preview clip. */
        const val PREVIEW_SEC = 3.0

/**
     * Returns the real FFmpeg `-vf` chain for a live preview combining the
     * selected [filterId] and [effectId], or null when neither needs a real
     * FFmpeg render (none selected, or only GPU-representable colour effects
     * that the Media3 pipeline already draws live).
     *
     * A complex effect (blur / noise / edges / pixelate / emboss / convolution /
     * geometric distortion) cannot be approximated by a colour matrix, so it is
     * always baked into the FFmpeg preview clip together with the filter. A
     * GPU-representable effect (pure eq/colorbalance/hue/vignette/curves/negate)
     * is intentionally excluded here — the Media3 GPU pipeline applies it live.
     *
     * Pure function — mirrors [com.powercut.editor.domain.filter.FilterCatalog]
     * and [VideoProcessor.EXACT_EFFECT_CHAINS] (the SAME sources the export uses).
     */
    fun buildPreviewFilter(filterId: String?, effectId: String? = null): String? {
        val fid = filterId?.trim()?.lowercase()
        val filterChain = if (fid != null && fid != "none") FilterCatalog.ffmpeg(fid) else ""

        val eid = effectId?.trim()?.lowercase()
        val effectChain = if (eid != null && eid != "none" && !VideoProcessor.isGpuRepresentableEffect(eid)) {
            VideoProcessor.EXACT_EFFECT_CHAINS[eid] ?: ""
        } else ""

        val parts = listOf(filterChain, effectChain).map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.isEmpty()) null else parts.joinToString(",")
    }
    }

    /**
     * Renders a preview clip combining the selected [filterId] and [effectId]
     * using [sourcePath].
     *
     * @return the rendered MP4 file, or null when the source is missing/too
     *         short, nothing needs a real FFmpeg render, or FFmpeg fails. The
     *         caller plays the returned file back on the preview player.
     */
    suspend fun renderPreview(
        sourcePath: String,
        filterId: String?,
        effectId: String? = null,
        requestedSec: Double = PREVIEW_SEC
    ): File? = withContext(Dispatchers.IO) {
        val chain = buildPreviewFilter(filterId, effectId)
        if (chain == null) {
            Log.d(TAG, "renderPreview: filter '$filterId' + effect '$effectId' need no preview")
            return@withContext null
        }

        val resolved = resolveToFile(sourcePath)
        if (resolved == null || !resolved.exists() || resolved.length() == 0L) {
            Log.e(TAG, "renderPreview: source missing or empty: $sourcePath")
            return@withContext null
        }

        // Robust duration probe: MediaMetadataRetriever is tried first, with an
        // FFmpeg fallback. A clip whose duration CANNOT be read must NOT be
        // rejected as "too short" — only a clip we actually measured as
        // degenerate (≤ floor) is skipped. This is what unblocks legitimate
        // short clips (e.g. a 13 s test clip) from the preview blocker.
        val durationMs = PreviewDurationProbe.probe(resolved)
        if (PreviewDurationPolicy.isDegenerate(durationMs)) {
            Log.w(TAG, "renderPreview: source too short for a filter preview")
            return@withContext null
        }
        val effectiveMs = durationMs ?: (requestedSec * 1000).toLong()

        val segSec = requestedSec.coerceAtMost(effectiveMs / 1000.0)
        val out = File(context.cacheDir, "filter_preview_${System.currentTimeMillis()}.mp4")

        val args = arrayListOf(
            "-y", "-loglevel", "error",
            "-ss", fmt(0.0), "-t", fmt(segSec),
            "-i", resolved.absolutePath,
            "-vf",
            "scale=$PREVIEW_WIDTH:$PREVIEW_HEIGHT:force_original_aspect_ratio=decrease," +
                "pad=$PREVIEW_WIDTH:$PREVIEW_HEIGHT:(ow-iw)/2:(oh-ih)/2:black," +
                "fps=$PREVIEW_FPS,settb=AVTB,format=yuv420p,$chain",
            "-c:v", "libx264", "-preset", "ultrafast", "-crf", "26",
            "-pix_fmt", "yuv420p",
            "-an",
            "-movflags", "+faststart",
            out.absolutePath
        )

        val session = try {
            FFmpegKit.executeWithArguments(args.toTypedArray())
        } catch (e: Exception) {
            Log.e(TAG, "renderPreview: FFmpegKit threw", e)
            out.delete()
            return@withContext null
        }

        val ok = ReturnCode.isSuccess(session.returnCode)
        if (!ok) {
            Log.e(
                TAG,
                "renderPreview: ffmpeg failed rc=${session.returnCode} " +
                    (session.failStackTrace ?: "")
            )
            out.delete()
            return@withContext null
        }
        out
    }

    /** Formats a double as FFmpeg time (seconds, 3 dp). */
    private fun fmt(value: Double): String = String.format("%.3f", value)

    /** Resolves a content:// / file:// / plain path to a real cache file. */
    private fun resolveToFile(sourcePath: String): File? {
        return try {
            when {
                sourcePath.startsWith("content://") -> {
                    val uri = Uri.parse(sourcePath)
                    val input = context.contentResolver.openInputStream(uri)
                        ?: return null
                    val copy = File(context.cacheDir, "filter_src_${System.currentTimeMillis()}.mp4")
                    copy.outputStream().use { os -> input.use { it.copyTo(os) } }
                    copy
                }
                sourcePath.startsWith("file://") ->
                    File(Uri.parse(sourcePath).path ?: return null)
                else -> File(sourcePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "resolveToFile failed", e)
            null
        }
    }
}
