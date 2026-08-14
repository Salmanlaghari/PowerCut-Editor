package com.powercut.editor.domain.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.powercut.editor.core.base.Resource
import com.powercut.editor.data.VideoProject
import com.powercut.editor.data.TrackType
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

    /**
     * Export progress 0-100, published during the encode so the foreground
     * service can update its notification and the UI can show a live bar.
     * 0 = preparing / unknown, -1 = idle.
     */
    private val _progress = MutableStateFlow(-1)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    fun resetState() {
        _exportState.value = Resource.Idle
        _progress.value = -1
    }

    /** Called by the foreground service when something blows up outside exportProject. */
    fun publishError(message: String) {
        _exportState.value = Resource.Error(message, Exception(message))
    }

    /** Update progress (clamped 0-100). Called by VideoProcessor's stats callback. */
    fun updateProgress(pct: Int) {
        _progress.value = pct.coerceIn(0, 100)
    }

    // ──────────────────────────────────────────────────────────────────────
    //  v4.4.0 PREMIUM FFmpeg MEDIA CONVERTER — MP3 → MP4 (workable, not fake)
    //  Streams the picked audio (content:// URI) to a temp file, then runs the
    //  real FFmpeg audioToVideo pipeline (color source + drawtext + libx264 +
    //  AAC) and saves the result into the public Movies/PowerCut gallery.
    // ──────────────────────────────────────────────────────────────────────
    suspend fun convertMp3ToMp4(audioUri: android.net.Uri): String? {
        _exportState.value = Resource.Loading
        _progress.value = 5
        var tempAudio: File? = null
        var tempOutput: File? = null
        var gallerySaved = false
        try {
            val secureDir = context.externalCacheDir?.let { ext ->
                val dir = File(ext, "PowerCutExports")
                if (!dir.exists()) dir.mkdirs()
                dir
            } ?: File(context.cacheDir, "PowerCutExports").let { dir ->
                if (!dir.exists()) dir.mkdirs()
                dir
            }

            // 1) Stream the audio content:// URI to a real temp file FFmpeg can read.
            _progress.value = 15
            tempAudio = File(secureDir, "audio_in_${System.currentTimeMillis()}.mp3")
            val copiedOk = context.contentResolver.openInputStream(audioUri)?.use { input ->
                java.io.FileOutputStream(tempAudio).use { output ->
                    val buffer = ByteArray(2 * 1024 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                    output.fd.sync()
                    true
                }
            } ?: false

            if (!copiedOk || !tempAudio.exists() || tempAudio.length() == 0L) {
                _exportState.value = Resource.Error("Could not read the selected audio file.", Exception("audio copy failed"))
                _progress.value = 0
                return null
            }
            _progress.value = 30

            // 2) Run the real FFmpeg audioToVideo conversion.
            tempOutput = File(secureDir, "mp3_to_mp4_${System.currentTimeMillis()}.mp4")
            val ok = videoProcessor.audioToVideo(
                inputPath = tempAudio.absolutePath,
                outputPath = tempOutput.absolutePath
            )
            _progress.value = 85

            if (!ok || !tempOutput.exists() || tempOutput.length() == 0L) {
                _exportState.value = Resource.Error("FFmpeg conversion failed. The audio may be unsupported.", Exception("audioToVideo returned false"))
                _progress.value = 0
                return null
            }

            // 3) Save the MP4 into the public Movies/PowerCut gallery.
            val galleryPath = saveToPublicGallery(context, tempOutput)
            _progress.value = 100
            if (galleryPath != null) {
                gallerySaved = true
                _exportState.value = Resource.Success(galleryPath)
                return galleryPath
            } else {
                // Gallery save failed — keep the temp file as the result.
                gallerySaved = false
                _exportState.value = Resource.Success(tempOutput.absolutePath)
                return tempOutput.absolutePath
            }
        } catch (e: Exception) {
            Log.e(tag, "convertMp3ToMp4 exception", e)
            _exportState.value = Resource.Error("Conversion failed: ${e.message}", e)
            _progress.value = 0
            return null
        } finally {
            tempAudio?.delete()
            // v5.0.0 FIX: Only delete temp output if the gallery save succeeded.
            if (gallerySaved) tempOutput?.delete()
            kotlinx.coroutines.delay(600)
            _progress.value = -1
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  v4.5.0 PREMIUM QUICK TOOLS (Compress / Slideshow / AI Edit)
    //  All real FFmpeg pipelines, workable — not fake Toasts.
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * v4.5.0 — Compress a picked video (content:// URI) to a smaller MP4.
     * Streams the URI to temp, runs [VideoProcessor.compressVideo], saves to
     * the public Movies/PowerCut gallery. Returns the gallery path or null.
     */
    suspend fun compressVideo(videoUri: android.net.Uri, qualityPreset: String = "balanced"): String? {
        _exportState.value = Resource.Loading
        _progress.value = 5
        var tempInput: File? = null
        var tempOutput: File? = null
        var gallerySaved = false
        try {
            val secureDir = context.externalCacheDir?.let { ext ->
                val dir = File(ext, "PowerCutExports"); if (!dir.exists()) dir.mkdirs(); dir
            } ?: File(context.cacheDir, "PowerCutExports").let { dir ->
                if (!dir.exists()) dir.mkdirs(); dir
            }
            _progress.value = 15
            tempInput = File(secureDir, "compress_in_${System.currentTimeMillis()}.mp4")
            val copiedOk = streamUriToTemp(videoUri, tempInput)
            if (!copiedOk) {
                _exportState.value = Resource.Error("Could not read the selected video.", Exception("video copy failed"))
                _progress.value = 0; return null
            }
            _progress.value = 35
            tempOutput = File(secureDir, "compressed_${System.currentTimeMillis()}.mp4")
            val ok = videoProcessor.compressVideo(tempInput.absolutePath, tempOutput.absolutePath, qualityPreset)
            _progress.value = 85
            if (!ok || !tempOutput.exists() || tempOutput.length() == 0L) {
                _exportState.value = Resource.Error("Compression failed. The video may be unsupported.", Exception("compressVideo returned false"))
                _progress.value = 0; return null
            }
            val galleryPath = saveToPublicGallery(context, tempOutput)
            _progress.value = 100
            if (galleryPath != null) {
                gallerySaved = true
                _exportState.value = Resource.Success(galleryPath)
                return galleryPath
            } else {
                gallerySaved = false
                _exportState.value = Resource.Success(tempOutput.absolutePath)
                return tempOutput.absolutePath
            }
        } catch (e: Exception) {
            Log.e(tag, "compressVideo exception", e)
            _exportState.value = Resource.Error("Compression failed: ${e.message}", e)
            _progress.value = 0; return null
        } finally {
            tempInput?.delete()
            if (gallerySaved) tempOutput?.delete()
            kotlinx.coroutines.delay(600); _progress.value = -1
        }
    }

    /**
     * v4.5.0 — Build a video slideshow from picked images (content:// URIs).
     * Streams each image to temp, runs [VideoProcessor.imagesToSlideshow],
     * saves the MP4 to the public Movies/PowerCut gallery.
     */
    suspend fun createSlideshow(imageUris: List<android.net.Uri>, perImageSec: Double = 2.5): String? {
        if (imageUris.isEmpty()) {
            _exportState.value = Resource.Error("No images selected.", Exception("empty list"))
            return null
        }
        _exportState.value = Resource.Loading
        _progress.value = 5
        val tempImages = mutableListOf<File>()
        var tempOutput: File? = null
        var gallerySaved = false
        try {
            val secureDir = context.externalCacheDir?.let { ext ->
                val dir = File(ext, "PowerCutExports"); if (!dir.exists()) dir.mkdirs(); dir
            } ?: File(context.cacheDir, "PowerCutExports").let { dir ->
                if (!dir.exists()) dir.mkdirs(); dir
            }
            // Stream each image URI to a temp file.
            _progress.value = 15
            imageUris.forEachIndexed { idx, uri ->
                val ext = guessImageExt(uri)
                val tmp = File(secureDir, "slide_${System.currentTimeMillis()}_${idx}.$ext")
                if (!streamUriToTemp(uri, tmp)) {
                    _exportState.value = Resource.Error("Could not read image #${idx + 1}.", Exception("image copy failed"))
                    _progress.value = 0; return null
                }
                tempImages.add(tmp)
                _progress.value = 15 + (idx + 1) * 40 / imageUris.size
            }
            _progress.value = 60
            tempOutput = File(secureDir, "slideshow_${System.currentTimeMillis()}.mp4")
            val ok = videoProcessor.imagesToSlideshow(
                imagePaths = tempImages.map { it.absolutePath },
                outputPath = tempOutput.absolutePath,
                perImageSec = perImageSec
            )
            _progress.value = 90
            if (!ok || !tempOutput.exists() || tempOutput.length() == 0L) {
                _exportState.value = Resource.Error("Slideshow creation failed.", Exception("imagesToSlideshow returned false"))
                _progress.value = 0; return null
            }
            val galleryPath = saveToPublicGallery(context, tempOutput)
            _progress.value = 100
            if (galleryPath != null) {
                gallerySaved = true
                _exportState.value = Resource.Success(galleryPath)
                return galleryPath
            } else {
                gallerySaved = false
                _exportState.value = Resource.Success(tempOutput.absolutePath)
                return tempOutput.absolutePath
            }
        } catch (e: Exception) {
            Log.e(tag, "createSlideshow exception", e)
            _exportState.value = Resource.Error("Slideshow failed: ${e.message}", e)
            _progress.value = 0; return null
        } finally {
            tempImages.forEach { it.delete() }
            if (gallerySaved) tempOutput?.delete()
            kotlinx.coroutines.delay(600); _progress.value = -1
        }
    }

    /**
     * v4.5.0 — AI Edit quick tool: apply an AI auto-enhance grade to a picked
     * video (content:// URI). Streams to temp, runs [VideoProcessor.applyAiEdit],
     * saves the enhanced MP4 to the public Movies/PowerCut gallery.
     */
    suspend fun applyAiEdit(videoUri: android.net.Uri): String? {
        _exportState.value = Resource.Loading
        _progress.value = 5
        var tempInput: File? = null
        var tempOutput: File? = null
        var gallerySaved = false
        try {
            val secureDir = context.externalCacheDir?.let { ext ->
                val dir = File(ext, "PowerCutExports"); if (!dir.exists()) dir.mkdirs(); dir
            } ?: File(context.cacheDir, "PowerCutExports").let { dir ->
                if (!dir.exists()) dir.mkdirs(); dir
            }
            _progress.value = 15
            tempInput = File(secureDir, "aiedit_in_${System.currentTimeMillis()}.mp4")
            if (!streamUriToTemp(videoUri, tempInput)) {
                _exportState.value = Resource.Error("Could not read the selected video.", Exception("video copy failed"))
                _progress.value = 0; return null
            }
            _progress.value = 35
            tempOutput = File(secureDir, "ai_edited_${System.currentTimeMillis()}.mp4")
            val ok = videoProcessor.applyAiEdit(tempInput.absolutePath, tempOutput.absolutePath)
            _progress.value = 85
            if (!ok || !tempOutput.exists() || tempOutput.length() == 0L) {
                _exportState.value = Resource.Error("AI Edit failed. The video may be unsupported.", Exception("applyAiEdit returned false"))
                _progress.value = 0; return null
            }
            val galleryPath = saveToPublicGallery(context, tempOutput)
            _progress.value = 100
            if (galleryPath != null) {
                gallerySaved = true
                _exportState.value = Resource.Success(galleryPath)
                return galleryPath
            } else {
                gallerySaved = false
                _exportState.value = Resource.Success(tempOutput.absolutePath)
                return tempOutput.absolutePath
            }
        } catch (e: Exception) {
            Log.e(tag, "applyAiEdit exception", e)
            _exportState.value = Resource.Error("AI Edit failed: ${e.message}", e)
            _progress.value = 0; return null
        } finally {
            tempInput?.delete()
            if (gallerySaved) tempOutput?.delete()
            kotlinx.coroutines.delay(600); _progress.value = -1
        }
    }

    // ── Helpers shared by the v4.5.0 quick tools ──────────────────────────

    /** Streams a content:// URI to a temp file (2 MB buffer). */
    private fun streamUriToTemp(uri: android.net.Uri, dest: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(2 * 1024 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush(); output.fd.sync(); true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(tag, "streamUriToTemp failed for $uri", e); false
        }
    }

    /** Guesses an image extension from the URI's MIME type. */
    private fun guessImageExt(uri: android.net.Uri): String {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("bmp") -> "bmp"
            mime.contains("gif") -> "gif"
            else -> "jpg"
        }
    }

    /**
     * Executes the video export according to the current project configuration.
     * Uses ultra fast "Instant Trim" if no scaling, speed change, transitions, audio, or filters are requested.
     * Otherwise, performs fully accelerated transcoding with requested options (resolution, filters).
     */
    suspend fun exportProject(project: VideoProject) {
        _exportState.value = Resource.Loading
        _progress.value = 0
        var tempInputFile: File? = null
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
            // v4.1: We copy the input content:// URI to a real temp file before
            // running FFmpeg (the SAF protocol was unreliable and caused export
            // failures). So we need space for BOTH the temp input copy (~input
            // size) AND the output (~input size / 2 after compression). The temp
            // input file is deleted right after FFmpeg finishes (see finally{}),
            // so peak usage is input + output. We add a 150 MB safety floor.
            //
            // v4.2 LONG-VIDEO: For 60-minute 1080p videos the input can be 4-8 GB
            // and the output 2-4 GB, so peak usage can hit ~12 GB + temp copy.
            // We bump the safety floor to 500 MB for long videos and surface a
            // clear, actionable error (with the exact GB numbers) so the user
            // knows exactly how much to free.
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
            // For content:// URIs we need input copy + output. For local files
            // the input is already on disk, so only output space is needed.
            val needsTempCopy = project.videoPath.startsWith("content://")
            val estimatedOutputSize = if (inputSize > 0) inputSize / 2 else 250L * 1024 * 1024
            // Detect long videos (≥ 15 min ≈ >1.5 GB 1080p) and use a bigger safety floor.
            val isLongVideo = inputSize > 1_500L * 1024 * 1024
            val safetyFloor = if (isLongVideo) 500L * 1024 * 1024 else 150L * 1024 * 1024
            val minRequiredSpace = if (needsTempCopy && inputSize > 0) {
                // temp input copy + output + safety floor
                inputSize + estimatedOutputSize + safetyFloor
            } else {
                maxOf(safetyFloor, estimatedOutputSize)
            }
            // v4.3: Hard 15 GB floor for very long (≥30 min) videos to prevent
            // mid-export disk-full crashes. 60-min 1080p can need ~12 GB peak.
            val hardFloor15GB = 15L * 1024 * 1024 * 1024
            val effectiveMinRequired = if (isLongVideo && inputSize > 3L * 1024 * 1024 * 1024) {
                maxOf(minRequiredSpace, hardFloor15GB)
            } else {
                minRequiredSpace
            }
            if (availableSpace < effectiveMinRequired) {
                val availableGB = availableSpace / (1024.0 * 1024 * 1024)
                val requiredGB = effectiveMinRequired / (1024.0 * 1024 * 1024)
                val msg = if (isLongVideo) {
                    "Not enough storage for this long video. Free ${String.format("%.1f", requiredGB - availableGB)} GB " +
                    "(you have ${String.format("%.1f", availableGB)} GB, need ${String.format("%.1f", requiredGB)} GB). " +
                    "Tip: export at 720p, or delete large files, then retry."
                } else {
                    "Storage full! Only ${availableSpace / (1024 * 1024)}MB available, " +
                    "need ~${minRequiredSpace / (1024 * 1024)}MB. Free up storage and try again."
                }
                _exportState.value = Resource.Error(
                    msg,
                    Exception("Insufficient storage: $availableGB GB available, need $requiredGB GB")
                )
                return
            }
            _progress.value = 2 // space OK

            val tempFileName = "powercut_process_${System.currentTimeMillis()}.mp4"
            val tempOutputFile = File(secureDir, tempFileName)
            val tempOutputPath = tempOutputFile.absolutePath

            // Resolve video path: if it's a content:// URI, copy to temp file for FFmpeg
            // (run on IO — the copy of long/multi-GB videos must never block the caller)
            val videoPath = withContext(kotlinx.coroutines.Dispatchers.IO) {
                val resolved = resolveVideoPath(context, project.videoPath, secureDir)
                // If we created a temp copy, track it for cleanup after export
                if (resolved != null && project.videoPath.startsWith("content://")) {
                    val f = File(resolved)
                    if (f.exists() && f.name.startsWith("input_")) {
                        tempInputFile = f
                    }
                }
                resolved
            }
            if (videoPath == null) {
                _exportState.value = Resource.Error(
                    "Could not access video file. Please re-import the video.",
                    Exception("Failed to resolve video path")
                )
                return
            }
            _progress.value = 5 // input resolved

            // ═══════════════════════════════════════════════════════════
            // MULTI-CLIP TIMELINE EXPORT
            // If the timeline has multiple VIDEO clips, use the multi-clip
            // pipeline (FFmpeg concat + xfade transitions). Otherwise,
            // fall through to the single-clip pipeline below.
            // ═══════════════════════════════════════════════════════════
            val videoClips = project.timeline.tracks
                .filter { it.type == com.powercut.editor.data.TrackType.VIDEO }
                .flatMap { it.clips }
                .sortedBy { it.startTimeMs }
            if (videoClips.size >= 2) {
                Log.d(tag, "Multi-clip timeline detected: ${videoClips.size} clips — using concat pipeline")
                val multiOk = videoProcessor.processMultiClipTimeline(
                    clips = videoClips,
                    outputPath = tempOutputPath,
                    resolution = project.targetResolution,
                    project = project,
                    onProgress = { pct -> updateProgress(10 + pct * 80 / 100) }
                )
                if (multiOk && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                    _progress.value = 90
                    val galleryPath = saveToPublicGallery(context, tempOutputFile)
                    if (galleryPath != null) {
                        _progress.value = 100
                        _exportState.value = Resource.Success(galleryPath)
                    } else {
                        _progress.value = 100
                        _exportState.value = Resource.Success(tempOutputPath)
                    }
                    return
                } else {
                    Log.w(tag, "Multi-clip export failed, falling back to single-clip")
                }
            }

            // ═══════════════════════════════════════════════════════════
            // FFmpeg VideoProcessor pipeline (handles all edits)
            // ═══════════════════════════════════════════════════════════

            // THERMAL PRE-CHECK (v4.2): warn (but don't block) if the device is
            // already hot before starting a long encode. The export will still
            // proceed — the foreground service + wake lock keep it alive — but
            // a hot phone is far more likely to throttle and fail.
            if (videoProcessor.isDeviceTooHotForLongExport()) {
                val temp = videoProcessor.getBatteryTemperatureCelsius()
                Log.w(tag, "Device is hot ($temp°C) before export — high risk of thermal failure")
                // We continue anyway; the user has already committed to the export.
            }

            // Check if input is audio file
            val isAudioInput = videoProcessor.isAudioFile(videoPath)

            // v6.4.0 FIX: Added ALL missing edit checks so that ANY user edit
            // forces the full transcode pipeline (processAndExport) instead of
            // falling through to instantTrim (stream copy). Previously, many
            // edits were NOT checked here — watermark, image editor adjustments,
            // blend mode, reverse, freeze frame, color curves, audio effects,
            // voice changer, audio ducking, border style, vignette style,
            // premium looks, HDR, high bitrate, AI features, social presets,
            // target FPS != 30 — so the export silently used stream copy and
            // produced an output identical to the input with no edits applied.
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
                    project.orientationMode == "free" &&
                    // ── v6.4.0 NEW CHECKS (previously missing) ──
                    !project.hasWatermark &&
                    !project.isBlendModeActive &&
                    !project.isReversed &&
                    !project.hasFreezeFrame &&
                    !project.isColorCurvesActive &&
                    !project.isAudioEffectActive &&
                    !project.isVoiceChanged &&
                    !project.isAudioDuckingActive &&
                    !project.isBorderStyleActive &&
                    !project.isVignetteStyleActive &&
                    !project.isPremiumLookActive &&
                    project.targetFps == 30 &&
                    !project.isHdrExport &&
                    !project.isHighBitrate &&
                    !project.isAiFeatureActive &&
                    !project.hasSocialPreset

            val success = if (isInstantTrimPossible) {
                Log.d(tag, "Using ultra-fast Instant Trim (Sab se Tez)")
                _progress.value = 10
                videoProcessor.instantTrim(
                    inputPath = videoPath,
                    outputPath = tempOutputPath,
                    startMs = project.trimStartMs,
                    endMs = project.trimEndMs
                )
            } else {
                Log.d(tag, "Using transcode pipeline for upscale/filters/speed/audio")
                _progress.value = 10
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
                    textPositionX = project.textPositionX,
                    textPositionY = project.textPositionY,
                    textColorHex = project.textColorHex,
                    textFontSize = project.textFontSize,
                    textStyleId = project.textStyleId,
                    textBold = project.textBold,
                    textItalic = project.textItalic,
                    textShadow = project.textShadow,
                    textOutline = project.textOutline,
                    textGlow = project.textGlow,
                    textNeon = project.textNeon,
                    textBgColor = project.textBgColor,
                    textBgOpacity = project.textBgOpacity,
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
                    watermarkPath = project.watermarkPath ?: videoProcessor.getWatermarkFile(),
                    vignetteStyle = project.vignetteStyle,
                    premiumLookId = project.activePremiumLook,
                    // v6.0.0 Premium export + AI + social
                    targetFps = project.targetFps,
                    isHdrEnabled = project.isHdrEnabled,
                    isHighBitrateEnabled = project.isHighBitrateEnabled,
                    activeAiFeature = project.activeAiFeature,
                    socialPreset = project.socialPreset,
                    // v7.1 Keyframe animation
                    keyframeTracks = project.keyframeTracks,
                    keyframeClipId = "",
                    onProgress = { pct -> updateProgress(pct) }
                )
            }

            if (success && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                Log.d(tag, "Successfully processed video inside app sandbox: $tempOutputPath")
                _progress.value = 95 // encoding done, saving to gallery

                val galleryPath = saveToPublicGallery(context, tempOutputFile)

                if (galleryPath != null) {
                    Log.d(tag, "Successfully registered output in system gallery: $galleryPath")
                    _progress.value = 100
                    _exportState.value = Resource.Success(galleryPath)
                } else {
                    Log.w(tag, "Could not insert in MediaStore, falling back to secure sandbox path")
                    _progress.value = 100
                    _exportState.value = Resource.Success(tempOutputPath)
                }
            } else {
                Log.e(tag, "Export failed during video processing")
                // Try auto-recovery: retry with 1080p resolution (if not already)
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
                        textPositionX = project.textPositionX,
                        textPositionY = project.textPositionY,
                        textColorHex = project.textColorHex,
                        textFontSize = project.textFontSize,
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
                        watermarkPath = project.watermarkPath ?: videoProcessor.getWatermarkFile(),
                        vignetteStyle = project.vignetteStyle,
                        premiumLookId = project.activePremiumLook,
                        textStyleId = project.textStyleId,
                        textBold = project.textBold,
                        textItalic = project.textItalic,
                        textShadow = project.textShadow,
                        textOutline = project.textOutline,
                        textGlow = project.textGlow,
                        textNeon = project.textNeon,
                        textBgColor = project.textBgColor,
                        textBgOpacity = project.textBgOpacity,
                        targetFps = project.targetFps,
                        isHdrEnabled = project.isHdrEnabled,
                        isHighBitrateEnabled = project.isHighBitrateEnabled,
                        activeAiFeature = project.activeAiFeature,
                        socialPreset = project.socialPreset,
                        // v7.1 Keyframe animation
                        keyframeTracks = project.keyframeTracks,
                        keyframeClipId = "",
                        onProgress = { pct -> updateProgress(pct) }
                    )
                    if (retrySuccess && tempOutputFile.exists() && tempOutputFile.length() > 0) {
                        val galleryPath = saveToPublicGallery(context, tempOutputFile)
                        _progress.value = 100
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
        } finally {
            // v4.3: Clean up any temp overlay files created during export
            // (content:// URIs that were stream-copied to cache for FFmpeg)
            try {
                videoProcessor.cleanupOverlayTempFiles()
            } catch (e: Exception) {
                Log.w(tag, "Could not clean up overlay temp files: ${e.message}")
            }

            // Always clean up the temp input copy to reclaim space (v4.1)
            tempInputFile?.let { f ->
                try {
                    if (f.exists()) {
                        val sizeMB = f.length() / (1024 * 1024)
                        val deleted = f.delete()
                        Log.d(tag, "Cleaned up temp input file: ${f.absolutePath} (${sizeMB}MB, deleted=$deleted)")
                    }
                    Unit
                } catch (cleanupEx: Exception) {
                    Log.w(tag, "Could not delete temp input file: ${cleanupEx.message}")
                }
            }
        }
    }

    /**
     * Resolve video path for FFmpeg processing.
     *
     * IMPORTANT (v4.1 fix): We ALWAYS stream-copy content:// URIs to a real temp
     * file and return the local file path. The previous implementation tried the
     * FFmpeg-Kit SAF protocol (`getSafParameterForRead`) first, but the resulting
     * `saf:N` pseudo-paths are unreliable for transcoding operations — FFmpeg
     * frequently fails to seek/decode through them, producing "Export failed"
     * even for short clips. Copying to a real file is the most robust approach
     * and works for every FFmpeg operation (seek, decode, re-encode, filters).
     *
     * The stream copy uses an 8 MB buffer so it never loads the whole file into
     * memory, and we delete the temp input file after the export completes (see
     * the finally{} block in exportProject) so the space is reclaimed.
     */
    private fun resolveVideoPath(context: Context, videoPath: String, tempDir: File): String? {
        // Regular file path — return directly
        if (!videoPath.startsWith("content://")) {
            return if (File(videoPath).exists()) videoPath else null
        }

        // Content URI — ALWAYS copy to a real temp file (SAF protocol is unreliable)
        return try {
            val uri = android.net.Uri.parse(videoPath)
            Log.d(tag, "Copying content URI to temp file for reliable FFmpeg processing: $videoPath")
            streamCopyToTemp(context, uri, tempDir)
        } catch (e: Exception) {
            Log.e(tag, "Failed to copy content URI to temp file: $videoPath — ${e.message}")
            null
        }
    }

    /**
     * Stream-copy a content URI to a temp file using a large buffer.
     * Never loads the whole file into memory. This is now the PRIMARY path for
     * content:// URIs (v4.1) because the SAF protocol was unreliable for FFmpeg
     * transcoding. The temp file is deleted after export completes.
     */
    private fun streamCopyToTemp(context: Context, uri: android.net.Uri, tempDir: File): String? {
        var tempFile: File? = null
        return try {
            tempFile = File(tempDir, "input_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024 * 1024) // 8 MB buffer
                    var read: Int
                    var totalBytes = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalBytes += read
                    }
                    output.flush()
                    // Force the data to physical storage so FFmpeg can read it
                    output.fd.sync()
                    Log.d(tag, "Streamed $totalBytes bytes (${totalBytes / (1024 * 1024)} MB) to temp file")
                }
            } ?: run {
                Log.e(tag, "openInputStream returned null for URI: $uri")
                return null
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                Log.d(tag, "Temp input file ready: ${tempFile.absolutePath} (${tempFile.length() / (1024 * 1024)} MB)")
                tempFile.absolutePath
            } else {
                Log.e(tag, "Temp file is empty or missing after copy")
                tempFile?.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "streamCopyToTemp failed: ${e.message}")
            // Clean up partial copy
            try { tempFile?.delete() } catch (_: Exception) {}
            null
        }
    }

    private fun saveToPublicGallery(context: Context, sourceFile: File): String? {
        val resolver = context.contentResolver
        // v5.0.0 FIX: Capture the exact filename so the returned path matches the real file.
        val fileName = "PowerCut_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
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
                // v5.0.0 FIX: Verify the output stream actually opened and bytes were written.
                var bytesWritten = 0L
                val streamOk = resolver.openOutputStream(uri)?.use { outStream ->
                    sourceFile.inputStream().use { inStream ->
                        val buffer = ByteArray(1024 * 1024)
                        var read: Int
                        while (inStream.read(buffer).also { read = it } != -1) {
                            outStream.write(buffer, 0, read)
                            bytesWritten += read
                        }
                        outStream.flush()
                    }
                    true
                } ?: false

                if (!streamOk || bytesWritten == 0L) {
                    Log.e(tag, "saveToPublicGallery: output stream failed or wrote 0 bytes")
                    try { resolver.delete(uri, null, null) } catch (_: Exception) {}
                    return null
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                // v5.0.0 FIX: Return the path with the SAME filename we used for DISPLAY_NAME.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    uri.toString()
                } else {
                    uri.path ?: "Movies/PowerCut/$fileName"
                }
            } else {
                // Fallback 1: Direct File Copy to Public Movies Directory
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val powerCutDir = File(publicDir, "PowerCut")
                if (!powerCutDir.exists()) powerCutDir.mkdirs()
                val targetFile = File(powerCutDir, fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to insert exported video into system Gallery database", e)
            try {
                // Fallback 2: Local Application Sandbox Fallback Path
                val externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                val powerCutDir = File(externalFilesDir, "PowerCut")
                if (!powerCutDir.exists()) powerCutDir.mkdirs()
                val targetFile = File(powerCutDir, fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile.absolutePath
            } catch (innerEx: Exception) {
                Log.e(tag, "Absolute fallback failed, returning null", innerEx)
                null
            }
        }
    }
}
