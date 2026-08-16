package com.powercut.editor.domain.processing

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.effect.RgbMatrix
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.powercut.editor.data.VideoProject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Media3TransformerExporter — Phase 1, Step C.
 *
 * Exports a video through Media3's [Transformer], applying the **same**
 * [Media3EffectPipeline] effect list the live preview (Step B) applies to
 * ExoPlayer via `setVideoEffects`. Because preview and export both call
 * [Media3EffectPipeline.buildEffectsFromProject] with the same project state,
 * the exported color grade is identical to what the user saw on screen.
 *
 * This class is intentionally scoped to the colour-grade concern: it applies
 * the pipeline's `RgbMatrix` effects plus optional start/end trim clipping. It
 * does NOT reimplement the FFmpeg pipeline's structural edits (speed, transitions,
 * music, text, captions, stickers, green-screen, blur/sharpen, silence
 * removal, watermark, volume, custom resolution/FPS, …). [ExportManager] routes
 * a project here only via [shouldUseForProject], which is deliberately
 * **conservative**: when in doubt it returns `false` so the proven FFmpeg path
 * handles the export. The Transformer path is reserved for the narrowest,
 * safest case — a single-clip, colour-only export with every other setting at
 * its default.
 */
@Singleton
class Media3TransformerExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeline: Media3EffectPipeline
) {

    companion object {
        private const val TAG = "Media3TransformerExporter"
        private const val PROGRESS_POLL_INTERVAL_MS = 200L
    }

    /**
     * Returns `true` when [project] should be exported through the Media3
     * Transformer parity path rather than the FFmpeg pipeline.
     *
     * CONSERVATIVE GATE — the Transformer path only applies the `RgbMatrix`
     * colour grade (+ start/end trim clipping). It silently drops every other
     * feature FFmpeg honours, so it may only be used when the project carries
     * NONE of those features. When in doubt, return `false` and let FFmpeg run.
     *
     * Required conditions (ALL must hold):
     *
     *  1. At least one colour adjustment is present (filter / look / editor /
     *     curves), AND exactly one video clip — Transformer handles a single clip.
     *  2. No FFmpeg-only structural edits: speed factor, transitions, music,
     *     mute, captions, rotation, flip, crop, text overlay, sticker, template,
     *     image overlay, green-screen, reverse.
     *  3. No editor effects Transformer doesn't reproduce: `imageEditorBlur`,
     *     `imageEditorSharpen`, `isSilenceRemoverEnabled`, `speedCurve`,
     *     `selectedEffect`, `freezeFrameMs` — all at their default/inactive values.
     *  4. No custom export geometry: `targetResolution == "1080p"` and
     *     `targetFps == 30` (Transformer keeps source res/FPS; only safe when
     *     the user hasn't requested a custom output resolution/frame rate).
     *  5. No watermark: `watermarkPath == null` (no custom watermark) AND
     *     `isProTier` (the bundled PowerCut watermark is disabled for Pro —
     *     free-tier exports carry it, which Transformer cannot reproduce).
     *  6. Audio volume unchanged: `videoVolume == 1.0f` (Transformer applies no
     *     volume change; FFmpeg honours a non-default `videoVolume`).
     */
    fun shouldUseForProject(project: VideoProject): Boolean {
        // (1) Colour adjustments present, single clip.
        if (!hasColorAdjustments(project)) return false
        val videoClips = project.timeline.tracks
            .filter { it.type == com.powercut.editor.data.TrackType.VIDEO }
            .flatMap { it.clips }
        if (videoClips.size != 1) return false

        // (2) No FFmpeg-only structural edits the Transformer skips.
        val noStructuralEdits =
            project.speedFactor == 1.0f &&
                project.transitionType == "none" &&
                project.backgroundMusicPath.isNullOrEmpty() &&
                !project.isMuted &&
                project.autoCaptionsLanguage == "off" &&
                project.rotationDegrees == 0f &&
                !project.isFlippedHorizontal &&
                !project.isFlippedVertical &&
                project.cropPreset == "free" &&
                project.activeTextOverlay.isNullOrEmpty() &&
                project.stickerType == "none" &&
                project.activeTemplateId == "none" &&
                project.imageOverlayPath.isNullOrEmpty() &&
                !project.greenScreenEnabled &&
                !project.isReverseEnabled
        if (!noStructuralEdits) return false

        // (3) No editor effects the Transformer doesn't reproduce.
        val noEditorEffects =
            project.imageEditorBlur == 0f &&
                project.imageEditorSharpen == 0f &&
                !project.isSilenceRemoverEnabled &&
                project.speedCurve == "constant" &&
                project.selectedEffect == "none" &&
                project.freezeFrameMs == 0L
        if (!noEditorEffects) return false

        // (4) No custom export resolution / frame rate.
        val defaultExportGeometry =
            project.targetResolution == "1080p" &&
                project.targetFps == 30
        if (!defaultExportGeometry) return false

        // (5) No watermark (custom or bundled).
        val noWatermark = project.watermarkPath == null && project.isProTier
        if (!noWatermark) return false

        // (6) Audio volume at default.
        if (project.videoVolume != 1.0f) return false

        return true
    }

    /** True when the project carries any colour-grade state the pipeline maps. */
    private fun hasColorAdjustments(project: VideoProject): Boolean {
        if (project.selectedFilter != "none" && project.selectedFilter.isNotBlank()) return true
        if (project.activePremiumLook != "none" && project.activePremiumLook.isNotBlank()) return true
        if (project.imageEditorBrightness != 0f) return true
        if (project.imageEditorContrast != 1f) return true
        if (project.imageEditorSaturation != 1f) return true
        if (project.imageEditorTemperature != 0f) return true
        if (project.imageEditorExposure != 0f) return true
        if (project.colorLift != 0f || project.colorGamma != 0f || project.colorGain != 0f) return true
        return false
    }

    /**
     * Runs the Media3 Transformer export.
     *
     * @param project   the project whose colour state drives the effect list
     *                  (same [Media3EffectPipeline.buildEffectsFromProject] call
     *                  the preview uses).
     * @param inputPath absolute path to the source media file (must be a real
     *                  file path, not a content:// URI — ExportManager resolves
     *                  those to a temp file first).
     * @param outputPath absolute path of the MP4 to write.
     * @param onProgress invoked with 0..100 as the encode progresses.
     * @return `true` if the export completed and [outputPath] exists & non-empty.
     */
    suspend fun export(
        project: VideoProject,
        inputPath: String,
        outputPath: String,
        onProgress: (Int) -> Unit
    ): Boolean {
        // Same call the live preview makes — this is the parity guarantee.
        val effects: List<RgbMatrix> = pipeline.buildEffectsFromProject(project)
        if (effects.isEmpty()) {
            Log.w(TAG, "No Media3 effects to apply; nothing to export through Transformer")
            return false
        }
        if (!File(inputPath).exists()) {
            Log.e(TAG, "Input file does not exist: $inputPath")
            return false
        }
        // Remove a stale output so a failed prior run can't masquerade as success.
        File(outputPath).takeIf { it.exists() }?.delete()

        onProgress(0)

        // EditedMediaItem construction holds no thread affinity — build it here.
        val mediaItem = buildMediaItem(inputPath, project)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effects))
            .build()

        val finished = AtomicBoolean(false)

        // ════════════════════════════════════════════════════════════════════════
        //  Thread confinement (Qodo fix): Media3's Transformer requires that
        //  build(), start(), getProgress(), cancel(), and all Listener callbacks
        //  run on ONE Looper. The previous implementation built/started the
        //  Transformer on a Dispatchers.Default worker thread (no Looper) while
        //  polling progress on the main Looper — crossing loopers and violating
        //  that requirement.
        //
        //  FIX: own a dedicated HandlerThread, explicitly bind the Transformer
        //  to its Looper via Transformer.Builder.setLooper(...), and route
        //  build / start / progress-poll / cancel through a Handler on that
        //  same Looper. Listener callbacks fire on that Looper; resume() is
        //  thread-safe so resuming the coroutine from there is fine. The
        //  HandlerThread is quit once the export finishes or is cancelled.
        // ════════════════════════════════════════════════════════════════════════
        val handlerThread = HandlerThread("Media3TransformerExport").also { it.start() }
        val exportLooper = handlerThread.looper
        val exportHandler = Handler(exportLooper)
        val progressHolder = ProgressHolder()

        return suspendCancellableCoroutine { cont ->
            // Holders so the cancellation handler (registered outside the Looper)
            // can reach the Transformer to cancel it ON the export Looper.
            var transformer: Transformer? = null
            var progressRunnable: Runnable? = null

            // Stops progress polling and quits the HandlerThread. Called only from
            // the export Looper (Listener callbacks / start-catch), so it is safe
            // to touch transformer/progressRunnable there.
            fun shutdownFromExportLooper() {
                progressRunnable?.let { exportHandler.removeCallbacks(it) }
                handlerThread.quit()
            }

            // Resumes the coroutine exactly once: the first of onCompleted /
            // onError / start-catch to win the CAS proceeds; everyone else (incl.
            // cancellation, which sets `finished` first) is a no-op.
            fun completeOnce(value: Boolean) {
                if (finished.compareAndSet(false, true)) {
                    shutdownFromExportLooper()
                    if (cont.isActive) cont.resume(value)
                }
            }

            exportHandler.post {
                val t = Transformer.Builder(context)
                    .setLooper(exportLooper)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult
                        ) {
                            Log.d(TAG, "Transformer export completed")
                            completeOnce(true)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            Log.e(TAG, "Transformer export failed", exportException)
                            completeOnce(false)
                        }
                    })
                    .build()
                transformer = t

                val pr = object : Runnable {
                    override fun run() {
                        if (finished.get() || cont.isCancelled) return
                        try {
                            val state = t.getProgress(progressHolder)
                            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress(progressHolder.progress.coerceIn(0, 100))
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Progress poll failed: ${e.message}")
                        }
                        if (!finished.get() && !cont.isCancelled) {
                            exportHandler.postDelayed(this, PROGRESS_POLL_INTERVAL_MS)
                        }
                    }
                }
                progressRunnable = pr

                try {
                    t.start(editedMediaItem, outputPath)
                    // Poll progress on the same Looper the Transformer is bound to.
                    exportHandler.post(pr)
                } catch (e: Exception) {
                    Log.e(TAG, "Transformer.start threw", e)
                    completeOnce(false)
                }
            }

            cont.invokeOnCancellation {
                // Mark finished so polling + Listener callbacks stop, then cancel
                // the Transformer ON its own Looper (never off-Looper).
                finished.set(true)
                exportHandler.post {
                    try { transformer?.cancel() } catch (e: Exception) {
                        Log.w(TAG, "cancel failed: ${e.message}")
                    }
                    progressRunnable?.let { exportHandler.removeCallbacks(it) }
                    handlerThread.quit()
                }
            }
        }.also { ok ->
            // Best-effort final progress + ensure the output is real before trusting it.
            if (ok) {
                val out = File(outputPath)
                if (!out.exists() || out.length() == 0L) {
                    Log.e(TAG, "Transformer reported success but output is missing/empty: $outputPath")
                } else {
                    onProgress(100)
                }
            }
        }
    }

    /** Builds the source [MediaItem], adding start/end trim as a clipping config when present. */
    private fun buildMediaItem(inputPath: String, project: VideoProject): MediaItem {
        val builder = MediaItem.Builder().setUri(inputPath)
        val startMs = project.trimStartMs
        val endMs = project.trimEndMs
        if (startMs > 0L || endMs > 0L) {
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()
            builder.setClippingConfiguration(clipping)
        }
        return builder.build()
    }
}
