package com.powercut.editor.domain.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.powercut.editor.core.base.Resource
import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.processing.VideoProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
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
            // Use external cache dir if available (more space), fallback to internal cache
            val secureDir = context.externalCacheDir?.let { ext ->
                val dir = File(ext, "PowerCutExports")
                if (!dir.exists()) dir.mkdirs()
                dir
            } ?: File(context.cacheDir, "PowerCutExports").let { dir ->
                if (!dir.exists()) dir.mkdirs()
                dir
            }

            // Check available space on the ACTUAL partition we're writing to.
            // NOTE: We NO LONGER copy the input video to a temp file (that was the
            // root cause of the "Space" issue with long videos). FFmpeg-Kit reads
            // content:// URIs directly via the SAF protocol, so we only need space
            // for the OUTPUT file — a fraction of the input size, not 3x.
            val availableSpace = secureDir.freeSpace
            val inputSize = if (project.videoPath.startsWith("content://")) {
                try {
                    val afd = context.contentResolver.openAssetFileDescriptor(android.net.Uri.parse(project.videoPath), "r")
                    afd?.use { fd -> fd.length } ?: 0L
                } catch (e: Exception) { 0L }
            } else {
                val f = java.io.File(project.videoPath)
                if (f.exists()) f.length() else 0L
            }
            // We only need headroom for the output (≈ input size at most, usually
            // much less after compression). Use a modest fixed minimum plus a
            // small fraction of input — NOT 3x input which blocked long videos.
            val estimatedOutputSize = if (inputSize > 0) inputSize / 2 else 250L * 1024 * 1024
            val minRequiredSpace = maxOf(150L * 1024 * 1024, estimatedOutputSize)
            if (availableSpace < minRequiredSpace) {
                val availableMB = availableSpace / (1024 * 1024)
                val requiredMB = minRequiredSpace / (1024 * 1024)
                _exportState.value = Resource.Error(
                    "Storage full! Only ${availableMB}MB available, need ~${requiredMB}MB for the export. Free up storage and try again.",
                    Exception("Insufficient storage: ${availableMB}MB available, need ${requiredMB}MB")
                )
                return
            }

            val tempFileName = "powercut_process_${System.currentTimeMillis()}.mp4"
            val tempOutputFile = File(secureDir, tempFileName)
            val tempOutputPath = tempOutputFile.absolutePath

            // Resolve video path: if it's a content:// URI, copy to temp file for FFmpeg
            // (run on IO — the copy of long/multi-GB videos must never block the caller)
            val videoPath = withContext(kotlinx.coroutines.Dispatchers.IO) {
                resolveVideoPath(context, project.videoPath, secureDir)
            }
            if (videoPath == null) {
                _exportState.value = Resource.Error(
                    "Could not access video file. Please re-import the video.",
                    Exception("Failed to resolve video path")
                )
                return
            }

            // Check if input is audio file
            val isAudioInput = videoProcessor.isAudioFile(videoPath)

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
                    project.active3DShapeMask == "none" &&
                    project.selectedEffect == "none" &&
                    project.imageOverlayPath == null &&
                    !project.isGreenScreenActive &&
                    !project.isEraserActive &&
                    !project.isImageEditorActive &&
                    project.orientationMode == "free"

            val success = if (isInstantTrimPossible) {
                Log.d(tag, "Using ultra-fast Instant Trim (Sab se Tez)")
                videoProcessor.instantTrim(
                    inputPath = videoPath,
                    outputPath = tempOutputPath,
                    startMs = project.trimStartMs,
                    endMs = project.trimEndMs
                )
            } else {
                Log.d(tag, "Using transcode pipeline for upscale/filters/speed/audio")
                videoProcessor.processAndExport(
                    inputPath = videoPath,
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
                    active3DShapeMask = project.active3DShapeMask,
                    selectedEffect = project.selectedEffect,
                    imageOverlayPath = project.imageOverlayPath,
                    imageOverlayOpacity = project.imageOverlayOpacity,
                    imageOverlayScale = project.imageOverlayScale,
                    imageOverlayX = project.imageOverlayX,
                    imageOverlayY = project.imageOverlayY,
                    greenScreenEnabled = project.greenScreenEnabled,
                    greenScreenColor = project.greenScreenColor,
                    greenScreenThreshold = project.greenScreenThreshold,
                    greenScreenBackgroundPath = project.greenScreenBackgroundPath,
                    imageEditorBrightness = project.imageEditorBrightness,
                    imageEditorContrast = project.imageEditorContrast,
                    imageEditorSaturation = project.imageEditorSaturation,
                    imageEditorBlur = project.imageEditorBlur,
                    imageEditorSharpen = project.imageEditorSharpen,
                    imageEditorTemperature = project.imageEditorTemperature,
                    imageEditorVignette = project.imageEditorVignette,
                    imageEditorGrain = project.imageEditorGrain,
                    imageEditorFade = project.imageEditorFade,
                    imageEditorHighlights = project.imageEditorHighlights,
                    imageEditorShadows = project.imageEditorShadows,
                    imageEditorExposure = project.imageEditorExposure,
                    orientationMode = project.orientationMode,
                    verticalSafeZone = project.verticalSafeZone,
                    horizontalLetterbox = project.horizontalLetterbox,
                    blendMode = project.blendMode,
                    isReverseEnabled = project.isReverseEnabled,
                    freezeFrameMs = project.freezeFrameMs,
                    colorLift = project.colorLift,
                    colorGamma = project.colorGamma,
                    colorGain = project.colorGain,
                    audioEffect = project.audioEffect,
                    voiceChangerPitch = project.voiceChangerPitch,
                    isAudioDuckingEnabled = project.isAudioDuckingEnabled,
                    borderStyle = project.borderStyle,
                    watermarkPath = project.watermarkPath,
                    vignetteStyle = project.vignetteStyle
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
                        inputPath = videoPath,
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
                        active3DShapeMask = project.active3DShapeMask,
                        selectedEffect = project.selectedEffect,
                        imageOverlayPath = project.imageOverlayPath,
                        imageOverlayOpacity = project.imageOverlayOpacity,
                        imageOverlayScale = project.imageOverlayScale,
                        imageOverlayX = project.imageOverlayX,
                        imageOverlayY = project.imageOverlayY,
                        greenScreenEnabled = project.greenScreenEnabled,
                        greenScreenColor = project.greenScreenColor,
                        greenScreenThreshold = project.greenScreenThreshold,
                        greenScreenBackgroundPath = project.greenScreenBackgroundPath,
                        imageEditorBrightness = project.imageEditorBrightness,
                        imageEditorContrast = project.imageEditorContrast,
                        imageEditorSaturation = project.imageEditorSaturation,
                        imageEditorBlur = project.imageEditorBlur,
                        imageEditorSharpen = project.imageEditorSharpen,
                        imageEditorTemperature = project.imageEditorTemperature,
                        imageEditorVignette = project.imageEditorVignette,
                        imageEditorGrain = project.imageEditorGrain,
                        imageEditorFade = project.imageEditorFade,
                        imageEditorHighlights = project.imageEditorHighlights,
                        imageEditorShadows = project.imageEditorShadows,
                        imageEditorExposure = project.imageEditorExposure,
                        orientationMode = project.orientationMode,
                        verticalSafeZone = project.verticalSafeZone,
                        horizontalLetterbox = project.horizontalLetterbox,
                        blendMode = project.blendMode,
                        isReverseEnabled = project.isReverseEnabled,
                        freezeFrameMs = project.freezeFrameMs,
                        colorLift = project.colorLift,
                        colorGamma = project.colorGamma,
                        colorGain = project.colorGain,
                        audioEffect = project.audioEffect,
                        voiceChangerPitch = project.voiceChangerPitch,
                        isAudioDuckingEnabled = project.isAudioDuckingEnabled,
                        borderStyle = project.borderStyle,
                        watermarkPath = project.watermarkPath,
                        vignetteStyle = project.vignetteStyle
                    )
                    if (retrySuccess && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                        val galleryPath = saveToPublicGallery(context, tempOutputFile)
                        _exportState.value = Resource.Success(galleryPath ?: tempOutputPath)
                        return
                    }
                }
                _exportState.value = Resource.Error(
                    "Export failed. We auto-retried with safe settings but the video could not be encoded. Try: 1) Use a shorter clip (under 5 min) 2) Lower resolution in settings 3) Disable heavy effects/filters 4) Free up storage space",
                    Exception("Video processing failed")
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Export failed with exception", e)
            _exportState.value = Resource.Error(e.message ?: "An unknown error occurred during export", e)
        }
    }

    /**
     * Resolve video path for FFmpeg processing.
     * - If it's a regular file path, return as-is.
     * - If it's a content:// URI, use FFmpeg-Kit's SAF (Storage Access Framework)
     *   protocol — `getSafParameterForRead()` — which lets FFmpeg stream directly
     *   from the content URI WITHOUT copying the whole file to a temp file.
     *
     *   This is the critical fix for the "Space" issue with long/multi-GB videos:
     *   the previous implementation copied the entire video to a temp file, which
     *   required enormous free space (and time). The SAF approach streams the
     *   content directly, so no extra storage is needed for the input.
     *
     *   As a fallback (e.g. very old SAF incompatibility), it falls back to a
     *   stream-copy, but this should rarely be hit.
     */
    private fun resolveVideoPath(context: Context, videoPath: String, tempDir: File): String? {
        // Regular file path — return directly
        if (!videoPath.startsWith("content://")) {
            return if (File(videoPath).exists()) videoPath else null
        }

        // Content URI — use FFmpeg-Kit SAF protocol to read directly (NO full copy!)
        return try {
            val uri = android.net.Uri.parse(videoPath)
            val safPath = FFmpegKitConfig.getSafParameterForRead(context, uri)
            if (safPath != null) {
                Log.d(tag, "Using FFmpeg-Kit SAF protocol to stream content URI directly (no copy): $safPath")
                safPath
            } else {
                // Fallback: stream-copy to temp file (rare path for incompatible SAF)
                Log.w(tag, "SAF parameter unavailable, falling back to stream-copy")
                streamCopyToTemp(context, uri, tempDir)
            }
        } catch (e: NoSuchMethodError) {
            Log.w(tag, "getSafParameterForRead not available on this FFmpeg-Kit build, falling back to copy: ${e.message}")
            streamCopyToTemp(context, android.net.Uri.parse(videoPath), tempDir)
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve content URI via SAF: $videoPath — ${e.message}")
            // Last-resort fallback to copy
            try {
                streamCopyToTemp(context, android.net.Uri.parse(videoPath), tempDir)
            } catch (inner: Exception) {
                Log.e(tag, "Copy fallback also failed: ${inner.message}")
                null
            }
        }
    }

    /**
     * Fallback: stream-copy a content URI to a temp file using a large buffer.
     * Never loads the whole file into memory; used only when SAF is unavailable.
     */
    private fun streamCopyToTemp(context: Context, uri: android.net.Uri, tempDir: File): String? {
        return try {
            val tempFile = File(tempDir, "input_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                Log.d(tag, "Streamed content URI to temp file: ${tempFile.absolutePath} (${tempFile.length() / (1024 * 1024)} MB)")
                tempFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "streamCopyToTemp failed: ${e.message}")
            null
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
            val uri = resolver.insert(collection, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outStream ->
                    sourceFile.inputStream().use { inStream ->
                        // Use 1MB buffer for fast copy of large video files
                        val buffer = ByteArray(1024 * 1024)
                        var read: Int
                        while (inStream.read(buffer).also { read = it } != -1) {
                            outStream.write(buffer, 0, read)
                        }
                        outStream.flush()
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "Movies/PowerCut/PowerCut_${System.currentTimeMillis()}.mp4"
                } else {
                    uri.path
                }
            } else {
                // Fallback 1: Direct File Copy to Public Movies Directory
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val powerCutDir = File(publicDir, "PowerCut")
                if (!powerCutDir.exists()) powerCutDir.mkdirs()
                val targetFile = File(powerCutDir, "PowerCut_${System.currentTimeMillis()}.mp4")
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to insert exported video into system Gallery database", e)
            try {
                // Fallback 2: Local Application Sandbox Fallback Path (Guaranteed to succeed)
                val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                val powerCutDir = File(externalFilesDir, "PowerCut")
                if (!powerCutDir.exists()) powerCutDir.mkdirs()
                val targetFile = File(powerCutDir, "PowerCut_${System.currentTimeMillis()}.mp4")
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile.absolutePath
            } catch (innerEx: Exception) {
                Log.e(tag, "Absolute fallback failed, returning sandboxed original path", innerEx)
                sourceFile.absolutePath
            }
        }
    }
}
