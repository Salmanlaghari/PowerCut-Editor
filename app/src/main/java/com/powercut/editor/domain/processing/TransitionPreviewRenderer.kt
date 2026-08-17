package com.powercut.editor.domain.processing

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders a short MP4 of the selected transition for the LIVE preview.
 *
 * Media3 1.4.1 (media3-effect / media3-transformer) has no built-in
 * inter-clip transition API, so — exactly as the task's Phase 2 plan allows —
 * the preview is rendered with the real FFmpeg `xfade` chain. Crucially it
 * derives that chain from the SAME [TransitionCatalog] math the export
 * pipeline uses ([TransitionCatalog.cutSpecs] / [TransitionCatalog.xfadeNameFor]
 * / [TransitionCatalog.clampDuration]), so the transition shown on the preview
 * and the transition burned into the exported file can never diverge.
 *
 * The preview takes two short segments of the user's video (one from near the
 * start, one from later, so the two sides are visually distinct), normalises
 * both to a common resolution/fps/timebase exactly like the export does, and
 * joins them with `[v0][v1]xfade=...` at the cut. The result is a small MP4
 * that the editor plays back (looping) the moment a transition is tapped.
 */
class TransitionPreviewRenderer(private val context: Context) {

    companion object {
        private const val TAG = "TransitionPreviewRenderer"

        /** Preview output resolution (small → fast to render). */
        const val PREVIEW_WIDTH = 640
        const val PREVIEW_HEIGHT = 360
        const val PREVIEW_FPS = 30

        /** Seconds of footage on each side of the cut in the preview clip. */
        const val SEGMENT_SEC = 1.2

        /** Minimum source duration needed to extract two distinct segments. */
        const val MIN_SOURCE_SEC = SEGMENT_SEC * 2

        /**
         * Builds the labeled `xfade` filter for a 2-segment preview from the
         * exact same per-cut math as the export path.
         *
         * Returns null when the transition resolves to "none" or a hard cut.
         * Pure function — unit-tested by `TransitionPreviewExportParityTest`.
         *
         * @param segmentSec duration of each preview segment (seconds).
         * @param requestedSec the project's requested transition duration.
         */
        fun buildPreviewFilter(
            transitionId: String?,
            segmentSec: Double = SEGMENT_SEC,
            requestedSec: Double = TransitionCatalog.DEFAULT_DURATION_SEC
        ): String? {
            // Two clips of equal length: the same cutSpecs the multi-clip
            // export computes for two `segmentSec` clips.
            val cuts = TransitionCatalog.cutSpecs(
                transitionId,
                listOf(segmentSec, segmentSec),
                requestedSec
            )
            if (cuts.isEmpty()) return null
            val cut = cuts[0]
            val name = cut.xfadeName ?: return null
            return "[v0][v1]xfade=transition=$name" +
                ":duration=${TransitionCatalog.fmt(cut.durationSec)}" +
                ":offset=${TransitionCatalog.fmt(cut.offsetSec)}[vout]"
        }
    }

    /**
     * Renders a transition preview clip for [transitionId] using [sourcePath].
     *
     * @return the rendered MP4 file, or null when the source is too short,
     *         the transition is "none", or FFmpeg fails. The caller plays the
     *         returned file back on the preview player.
     */
    suspend fun renderPreview(
        sourcePath: String,
        transitionId: String?,
        requestedSec: Double = TransitionCatalog.DEFAULT_DURATION_SEC
    ): File? = withContext(Dispatchers.IO) {
        val resolved = resolveToFile(sourcePath)
        if (resolved == null) {
            Log.e(TAG, "renderPreview: could not resolve source $sourcePath")
            return@withContext null
        }
        val durationMs = mediaDurationMs(resolved)
        if (durationMs == null || durationMs / 1000.0 < MIN_SOURCE_SEC) {
            Log.w(TAG, "renderPreview: source too short for a two-segment preview")
            return@withContext null
        }

        val filter = buildPreviewFilter(transitionId, SEGMENT_SEC, requestedSec)
        if (filter == null) {
            Log.w(TAG, "renderPreview: transition '$transitionId' has no real xfade")
            return@withContext null
        }

        // Two distinct segments: A near the start, B from the middle.
        val startA = 0.0
        val startB = (durationMs / 1000.0 / 2.0).coerceAtLeast(SEGMENT_SEC)
        val out = File(context.cacheDir, "transition_preview_${System.currentTimeMillis()}.mp4")

        val args = arrayListOf(
            "-y", "-loglevel", "error",
            "-ss", TransitionCatalog.fmt(startA), "-t", TransitionCatalog.fmt(SEGMENT_SEC),
            "-i", resolved.absolutePath,
            "-ss", TransitionCatalog.fmt(startB), "-t", TransitionCatalog.fmt(SEGMENT_SEC),
            "-i", resolved.absolutePath,
            "-filter_complex",
            normalizeChain(0) + normalizeChain(1) + filter,
            "-map", "[vout]",
            "-c:v", "libx264", "-preset", "ultrafast", "-crf", "26",
            "-pix_fmt", "yuv420p",
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

    /**
     * Per-segment normalisation identical to the export's multi-clip chain:
     * same scale+pad (letterboxed), same fps, same AVTB timebase and yuv420p
     * format, so `xfade` receives two perfectly uniform inputs.
     */
    private fun normalizeChain(inputIndex: Int): String =
        "[$inputIndex:v]setpts=PTS-STARTPTS," +
            "scale=$PREVIEW_WIDTH:$PREVIEW_HEIGHT:force_original_aspect_ratio=decrease," +
            "pad=$PREVIEW_WIDTH:$PREVIEW_HEIGHT:(ow-iw)/2:(oh-ih)/2:black," +
            "fps=$PREVIEW_FPS,settb=AVTB,format=yuv420p[v$inputIndex];"

    /** Resolves a content:// / file:// / plain path to a real cache file. */
    private fun resolveToFile(sourcePath: String): File? {
        return try {
            when {
                sourcePath.startsWith("content://") -> {
                    val uri = Uri.parse(sourcePath)
                    val input = context.contentResolver.openInputStream(uri)
                        ?: return null
                    val copy = File(context.cacheDir, "transition_src_${System.currentTimeMillis()}.mp4")
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

    private fun mediaDurationMs(file: File): Long? = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val duration = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
        retriever.release()
        duration
    } catch (e: Exception) {
        Log.e(TAG, "mediaDurationMs failed", e)
        null
    }
}
