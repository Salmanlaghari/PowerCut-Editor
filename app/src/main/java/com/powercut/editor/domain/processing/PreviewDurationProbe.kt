package com.powercut.editor.domain.processing

import android.media.MediaMetadataRetriever
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File
import java.util.regex.Pattern

/**
 * Robust duration probe for the LIVE preview renderers.
 *
 * Previously each renderer read the source duration with
 * [MediaMetadataRetriever] alone. On a meaningful fraction of devices /
 * `content://` sources that call returns `null`, and the caller then treated
 * the clip as "too short" and refused to render a preview — the systemic bug
 * behind the permanent "video too short" overlay.
 *
 * This probe tries [MediaMetadataRetriever] first and, on failure, falls back
 * to parsing FFmpeg's `Duration:` line from an `ffmpeg -i` probe. Only if both
 * fail does it return `null` (meaning "unknown", which the policy explicitly
 * does NOT treat as too short).
 *
 * @see PreviewDurationPolicy for the rejection decision.
 */
object PreviewDurationProbe {
    private const val TAG = "PreviewDurationProbe"

    private val DURATION_RE = Pattern.compile("Duration:\\s*(\\d+):(\\d+):(\\d+(?:\\.\\d+)?)")

    /**
     * @return source duration in milliseconds, or `null` when it cannot be
     *         determined by either probe.
     */
    fun probe(file: File): Long? {
        val viaRetriever = mediaMetadataDurationMs(file)
        if (viaRetriever != null && viaRetriever > 0L) return viaRetriever

        val viaFfmpeg = ffprobeDurationMs(file)
        if (viaFfmpeg != null && viaFfmpeg > 0L) return viaFfmpeg

        return null
    }

    private fun mediaMetadataDurationMs(file: File): Long? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
            retriever.release()
            duration
        } catch (e: Exception) {
            Log.w(TAG, "mediaMetadataDurationMs failed for ${file.name}", e)
            null
        }
    }

    private fun ffprobeDurationMs(file: File): Long? {
        return try {
            val session = FFmpegKit.executeWithArguments(
                arrayOf("-i", file.absolutePath)
            )
            val output = session.output ?: return null
            val matcher = DURATION_RE.matcher(output)
            if (!matcher.find()) return null
            val hours = matcher.group(1)?.toLongOrNull() ?: 0L
            val minutes = matcher.group(2)?.toLongOrNull() ?: 0L
            val seconds = matcher.group(3)?.toDoubleOrNull() ?: 0.0
            ((hours * 3600L + minutes * 60L) * 1000L + (seconds * 1000.0).toLong())
        } catch (e: Exception) {
            Log.w(TAG, "ffprobeDurationMs failed for ${file.name}", e)
            null
        }
    }
}
