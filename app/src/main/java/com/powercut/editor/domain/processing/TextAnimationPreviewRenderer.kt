package com.powercut.editor.domain.processing

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders a short MP4 of the selected text animation for the LIVE preview.
 *
 * PART 3 (Animations) mirrors the Phase 2 transition preview: Media3's
 * Compose preview cannot render FFmpeg's drawtext animation, so the live
 * preview renders the EXACT `drawtext` chain the export burns in — both sides
 * call [TextAnimationCatalog.buildDrawtextFilters] with the same arguments —
 * and plays the resulting MP4 back the moment an animation is tapped. Because
 * [buildPreviewFilter] and [com.powercut.editor.domain.processing.VideoProcessor.buildTextOverlay]
 * are the same pure function, the animation the user sees on the preview and
 * the animation in the exported file can never diverge
 * (`TextAnimationPreviewExportParityTest` pins this).
 *
 * The preview takes a short segment of the user's video, normalises it to a
 * common resolution/fps/timebase exactly like the export does, overlays the
 * animated text via the catalog chain, and encodes a small MP4 the editor
 * plays back (looping).
 */
class TextAnimationPreviewRenderer(private val context: Context) {

    companion object {
        private const val TAG = "TextAnimationPreviewRenderer"

        /** Preview output resolution (small → fast to render). */
        const val PREVIEW_WIDTH = 640
        const val PREVIEW_HEIGHT = 360
        const val PREVIEW_FPS = 30

        /** Seconds of footage shown in the preview clip. */
        const val PREVIEW_SEC = 2.4

        /**
         * Builds the `drawtext` chain for a 2.4s preview clip using the SAME
         * pure function the export pipeline uses ([TextAnimationCatalog.buildDrawtextFilters]).
         *
         * Returns "" when the animation resolves to "none" or the text is blank
         * (the caller then keeps showing the raw video). Pure function —
         * unit-tested by `TextAnimationPreviewExportParityTest`.
         */
        fun buildPreviewFilter(
            animationId: String?,
            text: String,
            durationSec: Double = PREVIEW_SEC,
            fontSize: Float = 42f
        ): String {
            val anim = TextAnimationCatalog.normalize(animationId)
            if (TextAnimationCatalog.isNone(anim) || text.isBlank()) return ""
            return TextAnimationCatalog.buildDrawtextFilters(
                text = text,
                animation = anim,
                duration = durationSec,
                posX = 0.5f,
                posY = 0.85f,
                colorHex = "#FFFFFF",
                fontSize = fontSize
            )
        }
    }

    /**
     * Renders a text-animation preview clip for [animationId] using [sourcePath].
     *
     * @return the rendered MP4 file, or null when the source is too short, the
     *         animation is "none", the text is blank, or FFmpeg fails. The
     *         caller plays the returned file back on the preview player.
     */
    suspend fun renderPreview(
        sourcePath: String,
        animationId: String?,
        text: String,
        fontSize: Float = 42f
    ): File? = withContext(Dispatchers.IO) {
        val resolved = resolveToFile(sourcePath)
        if (resolved == null) {
            Log.e(TAG, "renderPreview: could not resolve source $sourcePath")
            return@withContext null
        }
        // Robust duration probe: MediaMetadataRetriever tried first, FFmpeg
        // fallback. Only a clip we *measured* as degenerate (≤ floor) is
        // skipped; an unreadable duration no longer blocks legitimate short
        // clips (e.g. a 13 s test clip) from previewing.
        val durationMs = PreviewDurationProbe.probe(resolved)
        if (PreviewDurationPolicy.isDegenerate(durationMs)) {
            Log.w(TAG, "renderPreview: source too short for an animation preview")
            return@withContext null
        }
        val effectiveMs = durationMs ?: (PREVIEW_SEC * 1000).toLong()
        val anim = TextAnimationCatalog.normalize(animationId)
        if (TextAnimationCatalog.isNone(anim) || text.isBlank()) {
            Log.w(TAG, "renderPreview: animation '$anim' or text is empty")
            return@withContext null
        }
        val fontPath = ensureFontFile()
        if (fontPath == null) {
            Log.e(TAG, "renderPreview: bundled font unavailable — drawtext would fail")
            return@withContext null
        }

        val drawtext = TextAnimationCatalog.buildDrawtextFilters(
            text = text,
            animation = anim,
            duration = PREVIEW_SEC,
            posX = 0.5f,
            posY = 0.85f,
            colorHex = "#FFFFFF",
            fontSize = fontSize,
            fontFileClause = ":fontfile=$fontPath"
        )
        if (drawtext.isBlank()) {
            Log.w(TAG, "renderPreview: catalog produced no drawtext chain for '$anim'")
            return@withContext null
        }

        val out = File(context.cacheDir, "text_anim_preview_${System.currentTimeMillis()}.mp4")
        // Normalise the source the same way the export does, then overlay the
        // animated text — the drawtext chain is literally the export's chain.
        val normalize = "scale=$PREVIEW_WIDTH:$PREVIEW_HEIGHT:force_original_aspect_ratio=decrease," +
            "pad=$PREVIEW_WIDTH:$PREVIEW_HEIGHT:(ow-iw)/2:(oh-ih)/2:black," +
            "fps=$PREVIEW_FPS,settb=AVTB,format=yuv420p"

        val args = arrayListOf(
            "-y", "-loglevel", "error",
            "-ss", "0", "-t", TextAnimationCatalog.fmt(PREVIEW_SEC),
            "-i", resolved.absolutePath,
            "-vf", "$normalize,$drawtext",
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

    /** Copies the bundled font to cacheDir (same asset the export uses). */
    private fun ensureFontFile(): String? = try {
        val dest = File(context.cacheDir, "powercut_sans.ttf")
        if (!dest.exists() || dest.length() == 0L) {
            context.assets.open("fonts/powercut_sans.ttf").use { input ->
                java.io.FileOutputStream(dest).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                    output.flush()
                }
            }
        }
        if (dest.exists()) dest.absolutePath else null
    } catch (e: Exception) {
        Log.e(TAG, "ensureFontFile failed", e)
        null
    }

    /** Resolves a content:// / file:// / plain path to a real cache file. */
    private fun resolveToFile(sourcePath: String): File? {
        return try {
            when {
                sourcePath.startsWith("content://") -> {
                    val uri = Uri.parse(sourcePath)
                    val input = context.contentResolver.openInputStream(uri)
                        ?: return null
                    val copy = File(context.cacheDir, "text_anim_src_${System.currentTimeMillis()}.mp4")
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
