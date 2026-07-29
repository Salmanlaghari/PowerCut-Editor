package com.powercut.editor.domain.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.powercut.editor.core.base.Resource
import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.processing.VideoProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoProcessor: VideoProcessor
) {
    private val tag = "ExportManager"

    private val _exportState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val exportState: StateFlow<Resource<String>> = _exportState.asStateFlow()

    fun resetState() {
        _exportState.value = Resource.Idle
    }

    /**
     * Executes the video export according to the current project configuration.
     * Uses ultra fast "Instant Trim" if no scaling, speed change, transitions, audio, or filters are requested.
     * Otherwise, performs fully accelerated transcoding with requested options (resolution, filters).
     */
    suspend fun exportProject(project: VideoProject) {
        _exportState.value = Resource.Loading
        try {
            // Check available storage space before export
            val secureDir = File(context.cacheDir, "PowerCutExports")
            if (!secureDir.exists()) {
                secureDir.mkdirs()
            }

            // Check ACTUAL device storage, not cache dir
            val storageDir = android.os.Environment.getExternalStorageDirectory()
            val availableSpace = storageDir.freeSpace
            val minRequiredSpace = 200 * 1024 * 1024L // 200 MB minimum
            if (availableSpace < minRequiredSpace) {
                val availableMB = availableSpace / (1024 * 1024)
                _exportState.value = Resource.Error(
                    "Storage full! Only ${availableMB}MB available. Free up at least 200MB and try again.",
                    Exception("Insufficient storage: ${availableMB}MB available")
                )
                return
            }

            val tempFileName = "powercut_process_${System.currentTimeMillis()}.mp4"
            val tempOutputFile = File(secureDir, tempFileName)
            val tempOutputPath = tempOutputFile.absolutePath

            // Check if input is audio file
            val isAudioInput = videoProcessor.isAudioFile(project.videoPath)

            val isInstantTrimPossible = !isAudioInput &&
                    !project.isMuted &&
                    project.selectedFilter == "none" &&
                    project.targetResolution == "1080p" &&
                    project.speedFactor == 1.0f &&
                    project.aspectPreset == "16:9" &&
                    project.transitionType == "none" &&
                    !project.hasBackgroundMusic &&
                    project.autoCaptionsLanguage == "off" &&
                    !project.isSilenceRemoverEnabled &&
                    project.rotationDegrees == 0f &&
                    !project.isFlippedHorizontal &&
                    !project.isFlippedVertical &&
                    project.cropPreset == "free" &&
                    project.speedCurve == "constant" &&
                    project.activeTextOverlay == null &&
                    project.stickerType == "none" &&
                    project.activeTemplateId == "none" &&
                    project.visualizerStyle == "none" &&
                    !project.isBeatSyncEnabled &&
                    project.active3DShapeMask == "none"

            val success = if (isInstantTrimPossible) {
                Log.d(tag, "Using ultra-fast Instant Trim (Sab se Tez)")
                videoProcessor.instantTrim(
                    inputPath = project.videoPath,
                    outputPath = tempOutputPath,
                    startMs = project.trimStartMs,
                    endMs = project.trimEndMs
                )
            } else {
                Log.d(tag, "Using transcode pipeline for upscale/filters/speed/audio")
                videoProcessor.processAndExport(
                    inputPath = project.videoPath,
                    outputPath = tempOutputPath,
                    startMs = project.trimStartMs,
                    endMs = project.trimEndMs,
                    resolution = project.targetResolution,
                    filter = project.selectedFilter,
                    isMuted = project.isMuted,
                    speedFactor = project.speedFactor,
                    aspectPreset = project.aspectPreset,
                    transitionType = project.transitionType,
                    backgroundMusicPath = project.backgroundMusicPath,
                    backgroundMusicVolume = project.backgroundMusicVolume,
                    videoVolume = project.videoVolume,
                    autoCaptionsLanguage = project.autoCaptionsLanguage,
                    isSilenceRemoverEnabled = project.isSilenceRemoverEnabled,
                    rotationDegrees = project.rotationDegrees,
                    isFlippedHorizontal = project.isFlippedHorizontal,
                    isFlippedVertical = project.isFlippedVertical,
                    cropPreset = project.cropPreset,
                    speedCurve = project.speedCurve,
                    activeTextOverlay = project.activeTextOverlay,
                    textAnimationType = project.textAnimationType,
                    stickerType = project.stickerType,
                    activeTemplateId = project.activeTemplateId,
                    visualizerStyle = project.visualizerStyle,
                    isBeatSyncEnabled = project.isBeatSyncEnabled,
                    active3DShapeMask = project.active3DShapeMask
                )
            }

            if (success && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                Log.d(tag, "Successfully processed video inside app sandbox: $tempOutputPath")

                val galleryPath = saveToPublicGallery(context, tempOutputFile)

                if (galleryPath != null) {
                    Log.d(tag, "Successfully registered output in system gallery: $galleryPath")
                    _exportState.value = Resource.Success(galleryPath)
                } else {
                    Log.w(tag, "Could not insert in MediaStore, falling back to secure sandbox path")
                    _exportState.value = Resource.Success(tempOutputPath)
                }
            } else {
                Log.e(tag, "Export failed during video processing")
                // Try auto-recovery: retry with lower resolution
                if (project.targetResolution != "1080p") {
                    Log.d(tag, "Retrying export with 1080p resolution...")
                    val retrySuccess = videoProcessor.processAndExport(
                        inputPath = project.videoPath,
                        outputPath = tempOutputPath,
                        startMs = project.trimStartMs,
                        endMs = project.trimEndMs,
                        resolution = "1080p",
                        filter = project.selectedFilter,
                        isMuted = project.isMuted,
                        speedFactor = project.speedFactor,
                        aspectPreset = project.aspectPreset,
                        transitionType = project.transitionType,
                        backgroundMusicPath = project.backgroundMusicPath,
                        backgroundMusicVolume = project.backgroundMusicVolume,
                        videoVolume = project.videoVolume,
                        autoCaptionsLanguage = project.autoCaptionsLanguage,
                        isSilenceRemoverEnabled = project.isSilenceRemoverEnabled,
                        rotationDegrees = project.rotationDegrees,
                        isFlippedHorizontal = project.isFlippedHorizontal,
                        isFlippedVertical = project.isFlippedVertical,
                        cropPreset = project.cropPreset,
                        speedCurve = project.speedCurve,
                        activeTextOverlay = project.activeTextOverlay,
                        textAnimationType = project.textAnimationType,
                        stickerType = project.stickerType,
                        activeTemplateId = project.activeTemplateId,
                        visualizerStyle = project.visualizerStyle,
                        isBeatSyncEnabled = project.isBeatSyncEnabled,
                        active3DShapeMask = project.active3DShapeMask
                    )
                    if (retrySuccess && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                        val galleryPath = saveToPublicGallery(context, tempOutputFile)
                        _exportState.value = Resource.Success(galleryPath ?: tempOutputPath)
                        return
                    }
                }
                _exportState.value = Resource.Error(
                    "Export failed. Your video may be too large or device storage is full. Try: 1) Lower resolution 2) Free up storage 3) Use a shorter clip",
                    Exception("Video processing failed")
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Export failed with exception", e)
            _exportState.value = Resource.Error(e.message ?: "An unknown error occurred during export", e)
        }
    }

    private fun saveToPublicGallery(context: Context, sourceFile: File): String? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "PowerCut_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PowerCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        return try {
            val uri = resolver.insert(collection, contentValues) ?: return null
            resolver.openOutputStream(uri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "Movies/PowerCut/" + sourceFile.name
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val powerCutDir = File(publicDir, "PowerCut")
                if (!powerCutDir.exists()) powerCutDir.mkdirs()
                val targetFile = File(powerCutDir, "PowerCut_${System.currentTimeMillis()}.mp4")
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to insert exported video into system Gallery database", e)
            null
        }
    }
}
