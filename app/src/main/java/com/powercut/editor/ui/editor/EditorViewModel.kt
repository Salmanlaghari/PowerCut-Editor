package com.powercut.editor.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powercut.editor.core.base.Resource
import com.powercut.editor.core.utils.UriHelper
import com.powercut.editor.data.ProjectRepository
import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.export.ExportForegroundService
import com.powercut.editor.domain.export.ExportManager
import com.powercut.editor.domain.processing.VideoProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class Clip(
    val uri: Uri,
    val path: String,
    val name: String
)

data class DraftItem(
    val id: String,
    val projectName: String,
    val lastEditedTime: Long,
    val durationMs: Long,
    val projectJson: String
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val projectRepository: ProjectRepository,
    private val exportManager: ExportManager,
    private val videoProcessor: VideoProcessor
) : ViewModel() {

    private val _currentScreen = MutableStateFlow("home") // "home" (dashboard), "editor", "export"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _currentDashboardTab = MutableStateFlow("dashboard") // "dashboard", "templates", "exports", "settings"
    val currentDashboardTab: StateFlow<String> = _currentDashboardTab.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en") // "en", "ur"
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    val currentProject: StateFlow<VideoProject?> = projectRepository.currentProject
    val exportState: StateFlow<Resource<String>> = exportManager.exportState

    /** Live export progress 0-100 (mirrors the foreground service). */
    val exportProgress: StateFlow<Int> = exportManager.progress

    // Premium Settings state
    private val _selectedResolution = MutableStateFlow("1080p")
    val selectedResolution: StateFlow<String> = _selectedResolution.asStateFlow()

    private val _selectedFps = MutableStateFlow(30)
    val selectedFps: StateFlow<Int> = _selectedFps.asStateFlow()

    private val _isHardwareAccEnabled = MutableStateFlow(true)
    val isHardwareAccEnabled: StateFlow<Boolean> = _isHardwareAccEnabled.asStateFlow()

    // ── v6.0.0 Premium export state ──
    private val _isHdrEnabled = MutableStateFlow(false)
    val isHdrEnabled: StateFlow<Boolean> = _isHdrEnabled.asStateFlow()

    private val _isHighBitrateEnabled = MutableStateFlow(false)
    val isHighBitrateEnabled: StateFlow<Boolean> = _isHighBitrateEnabled.asStateFlow()

    private val _activeAiFeature = MutableStateFlow("none")
    val activeAiFeature: StateFlow<String> = _activeAiFeature.asStateFlow()

    private val _socialPreset = MutableStateFlow("none")
    val socialPreset: StateFlow<String> = _socialPreset.asStateFlow()

    private val _isProTier = MutableStateFlow(false)
    val isProTier: StateFlow<Boolean> = _isProTier.asStateFlow()

    private val _selectedStoragePath = MutableStateFlow("Movies/PowerCut")
    val selectedStoragePath: StateFlow<String> = _selectedStoragePath.asStateFlow()

    private val _isDarkThemeEnabled = MutableStateFlow(true)
    val isDarkThemeEnabled: StateFlow<Boolean> = _isDarkThemeEnabled.asStateFlow()

    private val _clips = MutableStateFlow<List<Clip>>(emptyList())
    val clips: StateFlow<List<Clip>> = _clips.asStateFlow()

    private val _drafts = MutableStateFlow<List<DraftItem>>(emptyList())
    val drafts: StateFlow<List<DraftItem>> = _drafts.asStateFlow()

    fun addClip(context: Context, uri: Uri) {
        val path = UriHelper.getPathFromUri(context, uri) ?: uri.toString()
        val name = path.substringAfterLast("/")
        val newClip = Clip(uri, path, name)
        _clips.value = _clips.value + newClip
    }

    fun loadDrafts(context: Context) {
        viewModelScope.launch {
            val draftsDir = File(context.filesDir, "drafts")
            if (!draftsDir.exists()) {
                _drafts.value = emptyList()
                return@launch
            }
            val files = draftsDir.listFiles() ?: emptyArray()
            val list = mutableListOf<DraftItem>()
            for (file in files) {
                if (file.name.endsWith(".json")) {
                    try {
                        val content = file.readText()
                        val json = JSONObject(content)
                        val videoPath = json.getString("videoPath")
                        val projName = videoPath.substringAfterLast("/")
                        val lastEdited = json.optLong("lastEditedTime", file.lastModified())
                        val duration = json.optLong("durationMs", 0L)
                        list.add(DraftItem(file.nameWithoutExtension, projName, lastEdited, duration, content))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            _drafts.value = list.sortedByDescending { it.lastEditedTime }
        }
    }

    fun resumeDraft(draft: DraftItem) {
        viewModelScope.launch {
            val (project, loadedClips) = deserializeProject(draft.projectJson)
            projectRepository.setProject(project)
            _clips.value = loadedClips
            _currentScreen.value = "editor"
        }
    }

    fun saveDraft(context: Context) {
        val proj = currentProject.value ?: return
        val currentClips = clips.value
        try {
            val serialized = serializeProject(proj, currentClips)
            val draftsDir = File(context.filesDir, "drafts")
            if (!draftsDir.exists()) {
                draftsDir.mkdirs()
            }
            val projectId = java.lang.Math.abs(proj.videoPath.hashCode()).toString()
            val draftFile = File(draftsDir, "$projectId.json")
            draftFile.writeText(serialized)
            println("Saved draft to: ${draftFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeProject(project: VideoProject, clipsList: List<Clip>): String {
        val json = JSONObject()
        json.put("videoPath", project.videoPath)
        json.put("durationMs", project.durationMs)
        json.put("trimStartMs", project.trimStartMs)
        json.put("trimEndMs", project.trimEndMs)
        json.put("targetResolution", project.targetResolution)
        json.put("selectedFilter", project.selectedFilter)
        json.put("isMuted", project.isMuted)
        json.put("speedFactor", project.speedFactor.toDouble())
        json.put("aspectPreset", project.aspectPreset)
        json.put("transitionType", project.transitionType)
        json.put("backgroundMusicPath", project.backgroundMusicPath ?: "")
        json.put("backgroundMusicVolume", project.backgroundMusicVolume.toDouble())
        json.put("videoVolume", project.videoVolume.toDouble())
        json.put("autoCaptionsLanguage", project.autoCaptionsLanguage)
        json.put("isSilenceRemoverEnabled", project.isSilenceRemoverEnabled)
        json.put("rotationDegrees", project.rotationDegrees.toDouble())
        json.put("isFlippedHorizontal", project.isFlippedHorizontal)
        json.put("isFlippedVertical", project.isFlippedVertical)
        json.put("cropPreset", project.cropPreset)
        json.put("speedCurve", project.speedCurve)
        json.put("activeTextOverlay", project.activeTextOverlay ?: "")
        json.put("textAnimationType", project.textAnimationType)
        json.put("stickerType", project.stickerType)
        json.put("activeTemplateId", project.activeTemplateId)
        json.put("visualizerStyle", project.visualizerStyle)
        json.put("isBeatSyncEnabled", project.isBeatSyncEnabled)
        json.put("active3DShapeMask", project.active3DShapeMask)
        json.put("activePremiumLook", project.activePremiumLook)
        json.put("imageOverlayPath", project.imageOverlayPath ?: "")
        json.put("imageOverlayOpacity", project.imageOverlayOpacity.toDouble())
        json.put("imageOverlayScale", project.imageOverlayScale.toDouble())
        json.put("imageOverlayX", project.imageOverlayX.toDouble())
        json.put("imageOverlayY", project.imageOverlayY.toDouble())
        json.put("selectedEffect", project.selectedEffect)
        json.put("activeLayers", JSONArray(project.activeLayers))
        // Green Screen
        json.put("greenScreenEnabled", project.greenScreenEnabled)
        json.put("greenScreenColor", project.greenScreenColor)
        json.put("greenScreenThreshold", project.greenScreenThreshold.toDouble())
        json.put("greenScreenBackgroundPath", project.greenScreenBackgroundPath ?: "")
        json.put("greenScreenAutoBgIndex", project.greenScreenAutoBgIndex)
        // Eraser
        json.put("eraserMode", project.eraserMode)
        json.put("eraserBrushSize", project.eraserBrushSize.toDouble())
        json.put("eraserTolerance", project.eraserTolerance.toDouble())
        json.put("eraserSoftEdge", project.eraserSoftEdge)
        // Image Editor
        json.put("imgBrightness", project.imageEditorBrightness.toDouble())
        json.put("imgContrast", project.imageEditorContrast.toDouble())
        json.put("imgSaturation", project.imageEditorSaturation.toDouble())
        json.put("imgBlur", project.imageEditorBlur.toDouble())
        json.put("imgSharpen", project.imageEditorSharpen.toDouble())
        json.put("imgTemperature", project.imageEditorTemperature.toDouble())
        json.put("imgVignette", project.imageEditorVignette.toDouble())
        json.put("imgGrain", project.imageEditorGrain.toDouble())
        json.put("imgFade", project.imageEditorFade.toDouble())
        json.put("imgHighlights", project.imageEditorHighlights.toDouble())
        json.put("imgShadows", project.imageEditorShadows.toDouble())
        json.put("imgExposure", project.imageEditorExposure.toDouble())
        // Orientation
        json.put("orientationMode", project.orientationMode)
        json.put("verticalSafeZone", project.verticalSafeZone)
        json.put("horizontalLetterbox", project.horizontalLetterbox)
        json.put("autoReframeEnabled", project.autoReframeEnabled)
        // v6.0.0 Premium export + AI + social + pro
        json.put("targetFps", project.targetFps)
        json.put("isHdrEnabled", project.isHdrEnabled)
        json.put("isHighBitrateEnabled", project.isHighBitrateEnabled)
        json.put("isBatchExport", project.isBatchExport)
        json.put("activeAiFeature", project.activeAiFeature)
        json.put("socialPreset", project.socialPreset)
        json.put("isProTier", project.isProTier)

        val clipsArray = JSONArray()
        for (clip in clipsList) {
            val cJson = JSONObject()
            cJson.put("uri", clip.uri.toString())
            cJson.put("path", clip.path)
            cJson.put("name", clip.name)
            clipsArray.put(cJson)
        }
        json.put("clips", clipsArray)
        json.put("lastEditedTime", System.currentTimeMillis())
        return json.toString()
    }

    private fun deserializeProject(jsonStr: String): Pair<VideoProject, List<Clip>> {
        val json = JSONObject(jsonStr)
        val project = VideoProject(
            videoPath = json.getString("videoPath"),
            durationMs = json.optLong("durationMs", 0L),
            trimStartMs = json.optLong("trimStartMs", 0L),
            trimEndMs = json.optLong("trimEndMs", 0L),
            targetResolution = json.optString("targetResolution", "1080p"),
            selectedFilter = json.optString("selectedFilter", "none"),
            isMuted = json.optBoolean("isMuted", false),
            speedFactor = json.optDouble("speedFactor", 1.0).toFloat(),
            aspectPreset = json.optString("aspectPreset", "16:9"),
            transitionType = json.optString("transitionType", "none"),
            backgroundMusicPath = json.optString("backgroundMusicPath", "").let { if (it.isEmpty()) null else it },
            backgroundMusicVolume = json.optDouble("backgroundMusicVolume", 0.5).toFloat(),
            videoVolume = json.optDouble("videoVolume", 1.0).toFloat(),
            autoCaptionsLanguage = json.optString("autoCaptionsLanguage", "off"),
            isSilenceRemoverEnabled = json.optBoolean("isSilenceRemoverEnabled", false),
            rotationDegrees = json.optDouble("rotationDegrees", 0.0).toFloat(),
            isFlippedHorizontal = json.optBoolean("isFlippedHorizontal", false),
            isFlippedVertical = json.optBoolean("isFlippedVertical", false),
            cropPreset = json.optString("cropPreset", "free"),
            speedCurve = json.optString("speedCurve", "constant"),
            activeTextOverlay = json.optString("activeTextOverlay", "").let { if (it.isEmpty()) null else it },
            textAnimationType = json.optString("textAnimationType", "fade"),
            stickerType = json.optString("stickerType", "none"),
            activeTemplateId = json.optString("activeTemplateId", "none"),
            visualizerStyle = json.optString("visualizerStyle", "none"),
            isBeatSyncEnabled = json.optBoolean("isBeatSyncEnabled", false),
            active3DShapeMask = json.optString("active3DShapeMask", "none"),
            activePremiumLook = json.optString("activePremiumLook", "none"),
            imageOverlayPath = json.optString("imageOverlayPath", "").let { if (it.isEmpty()) null else it },
            imageOverlayOpacity = json.optDouble("imageOverlayOpacity", 1.0).toFloat(),
            imageOverlayScale = json.optDouble("imageOverlayScale", 1.0).toFloat(),
            imageOverlayX = json.optDouble("imageOverlayX", 0.5).toFloat(),
            imageOverlayY = json.optDouble("imageOverlayY", 0.5).toFloat(),
            selectedEffect = json.optString("selectedEffect", "none"),
            activeLayers = json.optJSONArray("activeLayers")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            // Green Screen
            greenScreenEnabled = json.optBoolean("greenScreenEnabled", false),
            greenScreenColor = json.optString("greenScreenColor", "green"),
            greenScreenThreshold = json.optDouble("greenScreenThreshold", 0.4).toFloat(),
            greenScreenBackgroundPath = json.optString("greenScreenBackgroundPath", "").let { if (it.isEmpty()) null else it },
            greenScreenAutoBgIndex = json.optInt("greenScreenAutoBgIndex", -1),
            // Eraser
            eraserMode = json.optString("eraserMode", "none"),
            eraserBrushSize = json.optDouble("eraserBrushSize", 30.0).toFloat(),
            eraserTolerance = json.optDouble("eraserTolerance", 0.5).toFloat(),
            eraserSoftEdge = json.optBoolean("eraserSoftEdge", true),
            // Image Editor
            imageEditorBrightness = json.optDouble("imgBrightness", 0.0).toFloat(),
            imageEditorContrast = json.optDouble("imgContrast", 1.0).toFloat(),
            imageEditorSaturation = json.optDouble("imgSaturation", 1.0).toFloat(),
            imageEditorBlur = json.optDouble("imgBlur", 0.0).toFloat(),
            imageEditorSharpen = json.optDouble("imgSharpen", 0.0).toFloat(),
            imageEditorTemperature = json.optDouble("imgTemperature", 0.0).toFloat(),
            imageEditorVignette = json.optDouble("imgVignette", 0.0).toFloat(),
            imageEditorGrain = json.optDouble("imgGrain", 0.0).toFloat(),
            imageEditorFade = json.optDouble("imgFade", 0.0).toFloat(),
            imageEditorHighlights = json.optDouble("imgHighlights", 0.0).toFloat(),
            imageEditorShadows = json.optDouble("imgShadows", 0.0).toFloat(),
            imageEditorExposure = json.optDouble("imgExposure", 0.0).toFloat(),
            // Orientation
            orientationMode = json.optString("orientationMode", "free"),
            verticalSafeZone = json.optBoolean("verticalSafeZone", false),
            horizontalLetterbox = json.optBoolean("horizontalLetterbox", false),
            autoReframeEnabled = json.optBoolean("autoReframeEnabled", false),
            // v6.0.0 Premium export + AI + social + pro
            targetFps = json.optInt("targetFps", 30),
            isHdrEnabled = json.optBoolean("isHdrEnabled", false),
            isHighBitrateEnabled = json.optBoolean("isHighBitrateEnabled", false),
            isBatchExport = json.optBoolean("isBatchExport", false),
            activeAiFeature = json.optString("activeAiFeature", "none"),
            socialPreset = json.optString("socialPreset", "none"),
            isProTier = json.optBoolean("isProTier", false)
        )

        val clipsList = mutableListOf<Clip>()
        val clipsArray = json.optJSONArray("clips")
        if (clipsArray != null) {
            for (i in 0 until clipsArray.length()) {
                val cJson = clipsArray.getJSONObject(i)
                clipsList.add(
                    Clip(
                        uri = Uri.parse(cJson.getString("uri")),
                        path = cJson.getString("path"),
                        name = cJson.getString("name")
                    )
                )
            }
        }
        return Pair(project, clipsList)
    }

    fun updateDashboardTab(tab: String) {
        _currentDashboardTab.value = tab
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == "en") "ur" else "en"
    }

    fun updateSettingsResolution(res: String) {
        _selectedResolution.value = res
    }

    fun updateSettingsFps(fps: Int) {
        _selectedFps.value = fps
    }

    fun toggleHardwareAcc() {
        _isHardwareAccEnabled.value = !_isHardwareAccEnabled.value
    }

    // ── v6.0.0 Premium export updaters ──
    fun toggleHdr() {
        _isHdrEnabled.value = !_isHdrEnabled.value
    }

    fun toggleHighBitrate() {
        _isHighBitrateEnabled.value = !_isHighBitrateEnabled.value
    }

    fun updateAiFeature(featureId: String) {
        _activeAiFeature.value = featureId
        projectRepository.updateProject { it.copy(activeAiFeature = featureId) }
    }

    fun updateSocialPreset(presetId: String) {
        _socialPreset.value = presetId
        projectRepository.updateProject { it.copy(socialPreset = presetId) }
    }

    fun unlockProTier() {
        _isProTier.value = true
        projectRepository.updateProject { it.copy(isProTier = true) }
    }

    fun updateStoragePath(path: String) {
        _selectedStoragePath.value = path
    }

    fun toggleTheme() {
        _isDarkThemeEnabled.value = !_isDarkThemeEnabled.value
    }

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0)
    val importProgress: StateFlow<Int> = _importProgress.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun clearImportError() {
        _importError.value = null
    }

    /**
     * Import a video for editing — Premium 2027 robust long-video import.
     *
     * This was the source of the "long video import failed" bug. The fix:
     *  - All heavy work (path resolution + metadata) runs on Dispatchers.IO so the
     *    UI never blocks and the import can't be killed by an ANR on huge files.
     *  - Persistable URI permission is taken immediately so access survives.
     *  - content:// URIs are NEVER rejected for missing file existence — ExoPlayer
     *    reads them natively.
     *  - Duration is read with bounded fallbacks; a metadata failure no longer
     *    blocks the import (ExoPlayer corrects it once playback starts).
     *  - Progress stages are reported to the UI for a premium loading experience.
     */
    fun selectVideo(
        context: Context,
        uri: Uri,
        templateId: String = "none",
        filter: String = "none",
        transition: String = "none",
        captions: String = "off",
        speed: Float = 1.0f
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = 10
            _importError.value = null
            try {
                // Persist read permission FIRST so long-video access survives
                UriHelper.takePersistablePermission(context, uri)
                _importProgress.value = 25

                // Resolve path on a background thread (heavy for large videos)
                val path = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    UriHelper.getPathFromUri(context, uri)
                }
                _importProgress.value = 50

                if (path != null) {
                    val isContentUri = path.startsWith("content://")
                    // Only validate file existence for real file paths — content URIs
                    // are always streamable and must NEVER be rejected here.
                    if (!isContentUri) {
                        val file = File(path)
                        if (!file.exists() || file.length() == 0L) {
                            _importError.value = "Failed to load video. File is empty or could not be read."
                            _isImporting.value = false
                            _importProgress.value = 0
                            return@launch
                        }
                    }

                    _importProgress.value = 70
                    // Read real video duration on a background thread with fallbacks.
                    // A null duration is NOT fatal — ExoPlayer reports it on playback.
                    val realDurationMs = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        UriHelper.getVideoDurationMs(context, path)
                            ?: UriHelper.getVideoDurationMs(context, uri.toString())
                            ?: 0L
                    }
                    _importProgress.value = 90

                    val project = VideoProject(
                        videoPath = path,
                        durationMs = realDurationMs,
                        trimStartMs = 0L,
                        trimEndMs = if (realDurationMs > 0L) realDurationMs else 0L,
                        targetResolution = _selectedResolution.value,
                        activeTemplateId = templateId,
                        selectedFilter = filter,
                        transitionType = transition,
                        autoCaptionsLanguage = captions,
                        speedFactor = speed
                    )
                    projectRepository.setProject(project)
                    _importProgress.value = 100
                    _currentScreen.value = "editor"
                } else {
                    _importError.value = "Could not access this video. Please try selecting it again from your gallery."
                }
            } catch (e: Exception) {
                _importError.value = "Failed to load video: ${e.message}"
            } finally {
                _isImporting.value = false
                _importProgress.value = 0
            }
        }
    }

    // ── v4.4.0 Premium FFmpeg Media Converter: MP3 → MP4 (workable, not fake) ──
    // Delegates to ExportManager.convertMp3ToMp4 which runs the real FFmpeg
    // audioToVideo pipeline. Progress + status surface through exportState /
    // exportProgress (the same flows the export UI already observes).
    fun convertMp3ToMp4(audioUri: Uri) {
        viewModelScope.launch {
            try {
                UriHelper.takePersistablePermission(appContext, audioUri)
            } catch (_: Exception) {
                // Non-fatal: audio read may still work without persistable perm.
            }
            exportManager.convertMp3ToMp4(audioUri)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  v4.5.0 PREMIUM QUICK TOOLS — Compress / Slideshow / AI Edit
    //  All delegate to real FFmpeg pipelines in ExportManager. Workable.
    // ═══════════════════════════════════════════════════════════════════════

    /** v4.5.0 — Compress a picked video to a smaller MP4. */
    fun compressVideo(videoUri: Uri, qualityPreset: String = "balanced") {
        viewModelScope.launch {
            try { UriHelper.takePersistablePermission(appContext, videoUri) } catch (_: Exception) {}
            exportManager.compressVideo(videoUri, qualityPreset)
        }
    }

    /** v4.5.0 — Build a video slideshow from picked images. */
    fun createSlideshow(imageUris: List<Uri>) {
        viewModelScope.launch {
            imageUris.forEach { u ->
                try { UriHelper.takePersistablePermission(appContext, u) } catch (_: Exception) {}
            }
            exportManager.createSlideshow(imageUris)
        }
    }

    /** v4.5.0 — AI Edit: apply an AI auto-enhance grade to a picked video. */
    fun applyAiEdit(videoUri: Uri) {
        viewModelScope.launch {
            try { UriHelper.takePersistablePermission(appContext, videoUri) } catch (_: Exception) {}
            exportManager.applyAiEdit(videoUri)
        }
    }

    fun setVideoDuration(durationMs: Long) {
        if (durationMs <= 0L) return
        projectRepository.updateProject { project ->
            // If the end trim was never set (long-video import where metadata
            // failed), or it was a stale placeholder, snap it to the real duration.
            val shouldResetEnd = project.trimEndMs == 0L ||
                    project.trimEndMs == 15000L ||
                    project.trimEndMs == project.durationMs
            project.copy(
                durationMs = durationMs,
                trimEndMs = if (shouldResetEnd || project.trimEndMs > durationMs) durationMs else project.trimEndMs
            )
        }
    }

    fun updateTrim(startMs: Long, endMs: Long) {
        projectRepository.updateProject { project ->
            project.copy(trimStartMs = startMs, trimEndMs = endMs)
        }
    }

    fun updateResolution(resolution: String) {
        projectRepository.updateProject { project ->
            project.copy(targetResolution = resolution)
        }
    }

    fun updateFilter(filterId: String) {
        projectRepository.updateProject { project ->
            project.copy(selectedFilter = filterId)
        }
    }

    fun toggleMute() {
        projectRepository.updateProject { project ->
            project.copy(isMuted = !project.isMuted)
        }
    }

    fun updateSpeed(speedFactor: Float) {
        projectRepository.updateProject { project ->
            project.copy(speedFactor = speedFactor)
        }
    }

    fun updateAspectPreset(aspectPreset: String) {
        projectRepository.updateProject { project ->
            project.copy(aspectPreset = aspectPreset)
        }
    }

    fun updateBackgroundMusic(path: String?) {
        projectRepository.updateProject { project ->
            project.copy(backgroundMusicPath = path)
        }
    }

    fun updateMusicVolume(volume: Float) {
        projectRepository.updateProject { project ->
            project.copy(backgroundMusicVolume = volume)
        }
    }

    fun updateVideoVolume(volume: Float) {
        projectRepository.updateProject { project ->
            project.copy(videoVolume = volume, isMuted = volume == 0f)
        }
    }

    fun updateAutoCaptions(language: String) {
        projectRepository.updateProject { project ->
            project.copy(autoCaptionsLanguage = language)
        }
    }

    fun toggleSilenceRemover() {
        projectRepository.updateProject { project ->
            project.copy(isSilenceRemoverEnabled = !project.isSilenceRemoverEnabled)
        }
    }

    fun updateTransition(transitionType: String) {
        projectRepository.updateProject { project ->
            project.copy(transitionType = transitionType)
        }
    }

    fun updateRotation() {
        projectRepository.updateProject { project ->
            val nextDegrees = (project.rotationDegrees + 90f) % 360f
            project.copy(rotationDegrees = nextDegrees)
        }
    }

    fun toggleFlipHorizontal() {
        projectRepository.updateProject { project ->
            project.copy(isFlippedHorizontal = !project.isFlippedHorizontal)
        }
    }

    fun toggleFlipVertical() {
        projectRepository.updateProject { project ->
            project.copy(isFlippedVertical = !project.isFlippedVertical)
        }
    }

    fun updateCropPreset(crop: String) {
        projectRepository.updateProject { project ->
            project.copy(cropPreset = crop)
        }
    }

    fun updateSpeedCurve(curve: String) {
        projectRepository.updateProject { project ->
            val factor = when (curve.lowercase()) {
                "montage" -> 2.5f
                "hero" -> 4.0f
                "flash" -> 8.0f
                else -> project.speedFactor
            }
            project.copy(speedCurve = curve, speedFactor = factor)
        }
    }

    fun updateTextOverlay(text: String?) {
        projectRepository.updateProject { project ->
            project.copy(activeTextOverlay = text)
        }
    }

    fun updateTextAnimation(anim: String) {
        projectRepository.updateProject { project ->
            project.copy(textAnimationType = anim)
        }
    }

    fun updateStickerType(sticker: String) {
        projectRepository.updateProject { project ->
            project.copy(stickerType = sticker)
        }
    }

    fun updateTemplate(templateId: String) {
        projectRepository.updateProject { project ->
            project.copy(activeTemplateId = templateId)
        }
    }

    fun updateVisualizerStyle(style: String) {
        projectRepository.updateProject { project ->
            project.copy(visualizerStyle = style)
        }
    }

    fun toggleBeatSync() {
        projectRepository.updateProject { project ->
            project.copy(isBeatSyncEnabled = !project.isBeatSyncEnabled)
        }
    }

    fun update3DShapeMask(mask: String) {
        projectRepository.updateProject { project ->
            project.copy(active3DShapeMask = mask)
        }
    }

    /** v4.4.0: apply / clear a premium Brightness/HDR/iPhone look. */
    fun updatePremiumLook(lookId: String) {
        projectRepository.updateProject { project ->
            project.copy(activePremiumLook = lookId)
        }
    }

    fun updateImageOverlay(path: String?) {
        projectRepository.updateProject { project ->
            project.copy(imageOverlayPath = path)
        }
    }

    fun updateImageOverlayOpacity(opacity: Float) {
        projectRepository.updateProject { project ->
            project.copy(imageOverlayOpacity = opacity)
        }
    }

    fun updateImageOverlayScale(scale: Float) {
        projectRepository.updateProject { project ->
            project.copy(imageOverlayScale = scale)
        }
    }

    fun updateSelectedEffect(effect: String) {
        projectRepository.updateProject { project ->
            project.copy(selectedEffect = effect)
        }
    }

    fun addLayer(layerId: String) {
        projectRepository.updateProject { project ->
            project.copy(activeLayers = project.activeLayers + layerId)
        }
    }

    fun removeLayer(layerId: String) {
        projectRepository.updateProject { project ->
            project.copy(activeLayers = project.activeLayers - layerId)
        }
    }

    // ===== GREEN SCREEN / CHROMA KEY =====
    fun toggleGreenScreen() {
        projectRepository.updateProject { project ->
            project.copy(greenScreenEnabled = !project.greenScreenEnabled)
        }
    }

    fun updateGreenScreenColor(color: String) {
        projectRepository.updateProject { project ->
            project.copy(greenScreenColor = color)
        }
    }

    fun updateGreenScreenThreshold(threshold: Float) {
        projectRepository.updateProject { project ->
            project.copy(greenScreenThreshold = threshold)
        }
    }

    fun selectAutoBackground(index: Int) {
        projectRepository.updateProject { project ->
            project.copy(greenScreenAutoBgIndex = index, greenScreenBackgroundPath = null)
        }
    }

    fun updateGreenScreenBackground(path: String?) {
        projectRepository.updateProject { project ->
            project.copy(greenScreenBackgroundPath = path, greenScreenAutoBgIndex = -1)
        }
    }

    // ===== ERASER TOOLS =====
    fun updateEraserMode(mode: String) {
        projectRepository.updateProject { project ->
            project.copy(eraserMode = mode)
        }
    }

    fun updateEraserBrushSize(size: Float) {
        projectRepository.updateProject { project ->
            project.copy(eraserBrushSize = size)
        }
    }

    fun updateEraserTolerance(tolerance: Float) {
        projectRepository.updateProject { project ->
            project.copy(eraserTolerance = tolerance)
        }
    }

    fun toggleEraserSoftEdge() {
        projectRepository.updateProject { project ->
            project.copy(eraserSoftEdge = !project.eraserSoftEdge)
        }
    }

    fun resetEraser() {
        projectRepository.updateProject { project ->
            project.copy(eraserMode = "none", eraserBrushSize = 30f, eraserTolerance = 0.5f)
        }
    }

    // ===== IMAGE EDITOR =====
    fun updateImageEditorBrightness(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorBrightness = value) }
    }
    fun updateImageEditorContrast(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorContrast = value) }
    }
    fun updateImageEditorSaturation(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorSaturation = value) }
    }
    fun updateImageEditorBlur(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorBlur = value) }
    }
    fun updateImageEditorSharpen(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorSharpen = value) }
    }
    fun updateImageEditorTemperature(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorTemperature = value) }
    }
    fun updateImageEditorVignette(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorVignette = value) }
    }
    fun updateImageEditorGrain(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorGrain = value) }
    }
    fun updateImageEditorFade(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorFade = value) }
    }
    fun updateImageEditorHighlights(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorHighlights = value) }
    }
    fun updateImageEditorShadows(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorShadows = value) }
    }
    fun updateImageEditorExposure(value: Float) {
        projectRepository.updateProject { it.copy(imageEditorExposure = value) }
    }
    fun resetImageEditor() {
        projectRepository.updateProject {
            it.copy(
                imageEditorBrightness = 0f, imageEditorContrast = 1f, imageEditorSaturation = 1f,
                imageEditorBlur = 0f, imageEditorSharpen = 0f, imageEditorTemperature = 0f,
                imageEditorVignette = 0f, imageEditorGrain = 0f, imageEditorFade = 0f,
                imageEditorHighlights = 0f, imageEditorShadows = 0f, imageEditorExposure = 0f
            )
        }
    }

    // ===== ORIENTATION TOOLS =====
    fun updateOrientationMode(mode: String) {
        projectRepository.updateProject { project ->
            val aspect = when (mode) {
                "vertical" -> "9:16"
                "horizontal" -> "16:9"
                "square" -> "1:1"
                else -> project.aspectPreset
            }
            project.copy(orientationMode = mode, aspectPreset = aspect)
        }
    }

    fun toggleVerticalSafeZone() {
        projectRepository.updateProject { it.copy(verticalSafeZone = !it.verticalSafeZone) }
    }

    fun toggleHorizontalLetterbox() {
        projectRepository.updateProject { it.copy(horizontalLetterbox = !it.horizontalLetterbox) }
    }

    fun toggleAutoReframe() {
        projectRepository.updateProject { it.copy(autoReframeEnabled = !it.autoReframeEnabled) }
    }

    // ===== NEW v4.0 CapCut-sync Pro Features =====

    fun updateBlendMode(mode: String) {
        projectRepository.updateProject { it.copy(blendMode = mode) }
    }

    fun toggleReverse() {
        projectRepository.updateProject { it.copy(isReverseEnabled = !it.isReverseEnabled) }
    }

    fun updateFreezeFrame(ms: Long) {
        projectRepository.updateProject { it.copy(freezeFrameMs = ms) }
    }

    fun updateColorLift(value: Float) {
        projectRepository.updateProject { it.copy(colorLift = value) }
    }

    fun updateColorGamma(value: Float) {
        projectRepository.updateProject { it.copy(colorGamma = value) }
    }

    fun updateColorGain(value: Float) {
        projectRepository.updateProject { it.copy(colorGain = value) }
    }

    fun updateAudioEffect(effect: String) {
        projectRepository.updateProject { it.copy(audioEffect = effect) }
    }

    fun updateVoiceChangerPitch(pitch: Float) {
        projectRepository.updateProject { it.copy(voiceChangerPitch = pitch) }
    }

    fun toggleAudioDucking() {
        projectRepository.updateProject { it.copy(isAudioDuckingEnabled = !it.isAudioDuckingEnabled) }
    }

    fun updateBorderStyle(style: String) {
        projectRepository.updateProject { it.copy(borderStyle = style) }
    }

    fun updateWatermark(path: String?) {
        projectRepository.updateProject { it.copy(watermarkPath = path) }
    }

    fun updateVignetteStyle(style: String) {
        projectRepository.updateProject { it.copy(vignetteStyle = style) }
    }

    fun navigateToExport() {
        _currentScreen.value = "export"
    }

    fun startExportWithSettings(
        resolution: String,
        fps: Int,
        isNoWatermark: Boolean,
        isHardwareAcc: Boolean,
        isHdr: Boolean = false,
        isHighBitrate: Boolean = false
    ) {
        // v5.0.0 WATERMARK FIX: Previously `isNoWatermark` was received but
        // NEVER used — the watermark was never applied to any export, so the
        // rewarded-ad "remove watermark" feature was effectively decorative.
        // Now: if the user has NOT earned watermark removal (isNoWatermark =
        // false), we set watermarkPath to the bundled PowerCut watermark PNG
        // so the FFmpeg overlay filter burns it into the output. If the user
        // DID earn removal (watched ad / premium), we clear the path.
        val watermarkPath = if (isNoWatermark) {
            null
        } else {
            videoProcessor.getWatermarkFile()
        }

        // Update project configuration before start
        projectRepository.updateProject { project ->
            project.copy(
                targetResolution = resolution,
                targetFps = fps,
                isHdrEnabled = isHdr,
                isHighBitrateEnabled = isHighBitrate,
                watermarkPath = watermarkPath,
                activeAiFeature = _activeAiFeature.value,
                socialPreset = _socialPreset.value
            )
        }
        // Keep the StateFlows in sync with the chosen settings
        _selectedFps.value = fps
        _isHdrEnabled.value = isHdr
        _isHighBitrateEnabled.value = isHighBitrate
        val project = currentProject.value ?: return

        // v4.2 LONG-EXPORT RELIABILITY: delegate the export to a FOREGROUND
        // SERVICE instead of viewModelScope. The ViewModel's scope is cleared
        // the moment the app goes to the background, which cancels any export
        // running in it — the #1 cause of "Export failed" on long videos.
        // The foreground service holds a wake lock + persistent notification so
        // the FFmpeg encode survives screen-off, Doze, and app minimisation.
        ExportForegroundService.start(appContext)
    }

    fun resetToHome() {
        projectRepository.clear()
        exportManager.resetState()
        _currentScreen.value = "home"
    }

    fun navigateToEditor() {
        exportManager.resetState()
        _currentScreen.value = "editor"
    }
}
