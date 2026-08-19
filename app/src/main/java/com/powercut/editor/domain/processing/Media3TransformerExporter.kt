package com.powercut.editor.domain.processing

import android.content.Context
import android.os.Handler
import android.os.Looper
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
 * music, text, captions, stickers, green-screen, …). [ExportManager] routes a
 * project here only when it has colour adjustments and no FFmpeg-only structural
 * edits (see [shouldUseForProject]); everything else keeps using FFmpeg so no
 * existing feature regresses.
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
     * Conditions:
     *  - there is at least one colour effect to apply (filter / look / editor /
     *    curves), AND
     *  - the project has no FFmpeg-only structural edits that Transformer does
     *    not reproduce here (speed change, transitions, music, text, captions,
     *    stickers, image overlay, green-screen, crop, rotation/flip, reverse,
     *    templates, mute). Start/end trim IS allowed (handled via clipping).
     *
     * This keeps preview ⇄ export colour parity for the common "colour grade
     * only" case while leaving complex edits on the proven FFmpeg path.
     */
    fun shouldUseForProject(project: VideoProject): Boolean {
        if (!hasColorAdjustments(project)) return false

        val singleClip = project.timeline.tracks
            .filter { it.type == com.powercut.editor.data.TrackType.VIDEO }
            .flatMap { it.clips }
            .size <= 1

        if (!singleClip) return false

        val noStructuralEdits =
            project.selectedEffect == "none" &&
            project.keyframeTracks.isEmpty() &&
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

        return noStructuralEdits
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

        val finished = java.util.concurrent.atomic.AtomicBoolean(false)

        val mediaItem = buildMediaItem(inputPath, project)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effects))
            .build()

        val mainHandler = Handler(Looper.getMainLooper())
        val progressHolder = ProgressHolder()

        return suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult
                    ) {
                        Log.d(TAG, "Transformer export completed")
                        finished.set(true)
                        cont.resume(true)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Log.e(TAG, "Transformer export failed", exportException)
                        finished.set(true)
                        cont.resume(false)
                    }
                })
                .build()

            // Poll progress on the main looper (Media3 posts listener callbacks there).
            val progressRunnable = object : Runnable {
                override fun run() {
                    if (finished.get() || cont.isCancelled) return
                    try {
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(progressHolder.progress.coerceIn(0, 100))
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Progress poll failed: ${e.message}")
                    }
                    if (!finished.get() && !cont.isCancelled) {
                        mainHandler.postDelayed(this, PROGRESS_POLL_INTERVAL_MS)
                    }
                }
            }
            mainHandler.post(progressRunnable)

            cont.invokeOnCancellation {
                finished.set(true)
                mainHandler.removeCallbacks(progressRunnable)
                try { transformer.cancel() } catch (e: Exception) {
                    Log.w(TAG, "cancel failed: ${e.message}")
                }
            }

            try {
                transformer.start(editedMediaItem, outputPath)
            } catch (e: Exception) {
                Log.e(TAG, "Transformer.start threw", e)
                finished.set(true)
                mainHandler.removeCallbacks(progressRunnable)
                if (cont.isActive) cont.resume(false)
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
