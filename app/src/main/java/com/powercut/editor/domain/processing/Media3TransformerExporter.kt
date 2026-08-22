package com.powercut.editor.domain.processing

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.Effect
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

/** Media3 Transformer exporter for color edits, trimming, and the Crop tool. */
@Singleton
class Media3TransformerExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeline: Media3EffectPipeline
) {
    companion object {
        private const val TAG = "Media3TransformerExporter"
        private const val PROGRESS_POLL_INTERVAL_MS = 200L
    }

    fun shouldUseForProject(project: VideoProject): Boolean {
        val videoClipCount = project.timeline.tracks.filter { it.type == com.powercut.editor.data.TrackType.VIDEO }.flatMap { it.clips }.size
        if (videoClipCount > 1) return false
        if (!hasColorAdjustments(project) && !hasCrop(project)) return false
        return project.selectedEffect == "none" && project.keyframeTracks.isEmpty() && project.speedFactor == 1.0f &&
            project.transitionType == "none" && project.backgroundMusicPath.isNullOrEmpty() && !project.isMuted &&
            project.autoCaptionsLanguage == "off" && project.rotationDegrees == 0f && !project.isFlippedHorizontal &&
            !project.isFlippedVertical && project.activeTextOverlay.isNullOrEmpty() && project.stickerType == "none" &&
            project.activeTemplateId == "none" && project.imageOverlayPath.isNullOrEmpty() && !project.greenScreenEnabled
    }

    private fun hasColorAdjustments(project: VideoProject): Boolean =
        (project.selectedFilter != "none" && project.selectedFilter.isNotBlank()) ||
            (project.activePremiumLook != "none" && project.activePremiumLook.isNotBlank()) ||
            project.imageEditorBrightness != 0f || project.imageEditorContrast != 1f || project.imageEditorSaturation != 1f ||
            project.imageEditorTemperature != 0f || project.imageEditorExposure != 0f || project.colorLift != 0f ||
            project.colorGamma != 0f || project.colorGain != 0f

    private fun hasCrop(project: VideoProject): Boolean = project.cropPreset.isNotBlank() && !project.cropPreset.equals("free", ignoreCase = true)

    suspend fun export(project: VideoProject, inputPath: String, outputPath: String, onProgress: (Int) -> Unit): Boolean {
        if (!File(inputPath).exists()) return false
        val effects = mutableListOf<Effect>()
        effects.addAll(pipeline.buildEffectsFromProject(project))
        Media3CropEffect.forProject(project)?.let(effects::add)
        if (effects.isEmpty()) return false
        File(outputPath).takeIf { it.exists() }?.delete()
        onProgress(0)
        val finished = java.util.concurrent.atomic.AtomicBoolean(false)
        val progressHolder = ProgressHolder()
        val mainHandler = Handler(Looper.getMainLooper())
        val edited = EditedMediaItem.Builder(buildMediaItem(inputPath, project)).setEffects(Effects(emptyList(), effects)).build()
        return suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context).addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) { finished.set(true); cont.resume(true) }
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) { Log.e(TAG, "Transformer export failed", exportException); finished.set(true); cont.resume(false) }
            }).build()
            val poll = object : Runnable {
                override fun run() {
                    if (finished.get() || cont.isCancelled) return
                    if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(progressHolder.progress.coerceIn(0, 100))
                    if (!finished.get() && !cont.isCancelled) mainHandler.postDelayed(this, PROGRESS_POLL_INTERVAL_MS)
                }
            }
            mainHandler.post(poll)
            cont.invokeOnCancellation { finished.set(true); mainHandler.removeCallbacks(poll); runCatching { transformer.cancel() } }
            runCatching { transformer.start(edited, outputPath) }.onFailure {
                finished.set(true); mainHandler.removeCallbacks(poll); if (cont.isActive) cont.resume(false)
            }
        }.also { ok ->
            val output = File(outputPath)
            if (ok && output.exists() && output.length() > 0L) onProgress(100) else if (ok) Log.e(TAG, "Transformer reported success without output")
        }
    }

    private fun buildMediaItem(inputPath: String, project: VideoProject): MediaItem {
        val builder = MediaItem.Builder().setUri(inputPath)
        if (project.trimStartMs > 0L || project.trimEndMs > 0L) builder.setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder().setStartPositionMs(project.trimStartMs).setEndPositionMs(project.trimEndMs).build()
        )
        return builder.build()
    }
}
