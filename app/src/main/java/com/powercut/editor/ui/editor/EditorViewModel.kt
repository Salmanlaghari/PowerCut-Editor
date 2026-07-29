package com.powercut.editor.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powercut.editor.core.base.Resource
import com.powercut.editor.core.utils.UriHelper
import com.powercut.editor.data.ProjectRepository
import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.export.ExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val projectRepository: ProjectRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _currentScreen = MutableStateFlow("home") // "home" (dashboard), "editor", "export"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _currentDashboardTab = MutableStateFlow("dashboard") // "dashboard", "templates", "exports", "settings"
    val currentDashboardTab: StateFlow<String> = _currentDashboardTab.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en") // "en", "ur"
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    val currentProject: StateFlow<VideoProject?> = projectRepository.currentProject
    val exportState: StateFlow<Resource<String>> = exportManager.exportState

    // Premium Settings state
    private val _selectedResolution = MutableStateFlow("1080p")
    val selectedResolution: StateFlow<String> = _selectedResolution.asStateFlow()

    private val _selectedFps = MutableStateFlow(30)
    val selectedFps: StateFlow<Int> = _selectedFps.asStateFlow()

    private val _isHardwareAccEnabled = MutableStateFlow(true)
    val isHardwareAccEnabled: StateFlow<Boolean> = _isHardwareAccEnabled.asStateFlow()

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
        json.put("imageOverlayPath", project.imageOverlayPath ?: "")
        json.put("imageOverlayOpacity", project.imageOverlayOpacity.toDouble())
        json.put("imageOverlayScale", project.imageOverlayScale.toDouble())
        json.put("selectedEffect", project.selectedEffect)

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
            imageOverlayPath = json.optString("imageOverlayPath", "").let { if (it.isEmpty()) null else it },
            imageOverlayOpacity = json.optDouble("imageOverlayOpacity", 1.0).toFloat(),
            imageOverlayScale = json.optDouble("imageOverlayScale", 1.0).toFloat(),
            selectedEffect = json.optString("selectedEffect", "none")
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

    fun updateStoragePath(path: String) {
        _selectedStoragePath.value = path
    }

    fun toggleTheme() {
        _isDarkThemeEnabled.value = !_isDarkThemeEnabled.value
    }

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
            val path = UriHelper.getPathFromUri(context, uri)
            if (path != null) {
                val file = File(path)
                val durationMs = if (file.exists()) {
                    15000L
                } else {
                    10000L
                }

                val project = VideoProject(
                    videoPath = path,
                    durationMs = durationMs,
                    trimStartMs = 0L,
                    trimEndMs = durationMs,
                    targetResolution = _selectedResolution.value,
                    activeTemplateId = templateId,
                    selectedFilter = filter,
                    transitionType = transition,
                    autoCaptionsLanguage = captions,
                    speedFactor = speed
                )
                projectRepository.setProject(project)
                _currentScreen.value = "editor"
            }
        }
    }

    fun setVideoDuration(durationMs: Long) {
        projectRepository.updateProject { project ->
            project.copy(
                durationMs = durationMs,
                trimEndMs = if (project.trimEndMs == 15000L || project.trimEndMs == 0L) durationMs else project.trimEndMs
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

    fun navigateToExport() {
        _currentScreen.value = "export"
    }

    fun startExportWithSettings(resolution: String, fps: Int, isNoWatermark: Boolean, isHardwareAcc: Boolean) {
        // Update project configuration before start
        projectRepository.updateProject { project ->
            project.copy(
                targetResolution = resolution
            )
        }
        val project = currentProject.value ?: return
        viewModelScope.launch {
            exportManager.exportProject(project)
        }
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
