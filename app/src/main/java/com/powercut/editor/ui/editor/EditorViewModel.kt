package com.powercut.editor.ui.editor

import android.content.Context
import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powercut.editor.core.base.Resource
import com.powercut.editor.core.utils.UriHelper
import com.powercut.editor.data.*
import java.util.UUID
import com.powercut.editor.domain.export.ExportForegroundService
import com.powercut.editor.domain.export.ExportManager
import com.powercut.editor.domain.processing.VideoProcessor
import com.powercut.editor.domain.processing.RoyaltyFreeMusicGenerator
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
    private val videoProcessor: VideoProcessor,
    private val royaltyFreeMusicGenerator: RoyaltyFreeMusicGenerator
) : ViewModel() {

    private val _currentScreen = MutableStateFlow("home") // "home" (dashboard), "editor", "export"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _currentDashboardTab = MutableStateFlow("dashboard") // "dashboard", "templates", "exports", "settings"
    val currentDashboardTab: StateFlow<String> = _currentDashboardTab.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en") // "en", "ur"
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    val currentProject: StateFlow<VideoProject?> = projectRepository.currentProject
    val exportState: StateFlow<Resource<String>> = exportManager.exportState

    /** Royalty-free music generation status (separate from export). */
    private val _musicState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val musicState: StateFlow<Resource<String>> = _musicState.asStateFlow()

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

    // ═══════════════════════════════════════════════════════════════════════
    // UNDO / REDO SYSTEM — snapshot-based (max 30 states)
    // ═══════════════════════════════════════════════════════════════════════
    private val _undoStack = java.util.ArrayDeque<VideoProject>(30)
    private val _redoStack = java.util.ArrayDeque<VideoProject>(30)
    private val _undoCount = MutableStateFlow(0)
    val undoCount: StateFlow<Int> = _undoCount.asStateFlow()
    private val _redoCount = MutableStateFlow(0)
    val redoCount: StateFlow<Int> = _redoCount.asStateFlow()

    /** Call before any project mutation to push the current state onto the undo stack. */
    fun pushUndoState() {
        val current = projectRepository.currentProject.value ?: return
        if (_undoStack.size >= 30) _undoStack.pollLast()
        _undoStack.push(current)
        _redoStack.clear() // new change invalidates redo
        _undoCount.value = _undoStack.size
        _redoCount.value = 0
    }

    /** Undo: restore the previous project state. */
    fun undo() {
        val prev = _undoStack.pollFirst() ?: return
        val current = projectRepository.currentProject.value ?: return
        if (_redoStack.size >= 30) _redoStack.pollLast()
        _redoStack.push(current)
        projectRepository.setProject(prev)
        _undoCount.value = _undoStack.size
        _redoCount.value = _redoStack.size
    }

    /** Redo: restore the next project state. */
    fun redo() {
        val next = _redoStack.pollFirst() ?: return
        val current = projectRepository.currentProject.value ?: return
        if (_undoStack.size >= 30) _undoStack.pollLast()
        _undoStack.push(current)
        projectRepository.setProject(next)
        _undoCount.value = _undoStack.size
        _redoCount.value = _redoStack.size
    }

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

    fun deleteDraft(context: Context, draft: DraftItem) {
        viewModelScope.launch {
            try {
                val draftsDir = File(context.filesDir, "drafts")
                val draftFile = File(draftsDir, "${draft.id}.json")
                if (draftFile.exists()) {
                    draftFile.delete()
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        json.put("transitionDurationSec", project.transitionDurationSec.toDouble())
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
        json.put("textStyleId", project.textStyleId)
        json.put("textPositionX", project.textPositionX)
        json.put("textPositionY", project.textPositionY)
        json.put("textColorHex", project.textColorHex)
        json.put("textFontSize", project.textFontSize)
        json.put("textBold", project.textBold)
        json.put("textItalic", project.textItalic)
        json.put("textShadow", project.textShadow)
        json.put("textOutline", project.textOutline)
        json.put("textGlow", project.textGlow)
        json.put("textNeon", project.textNeon)
        json.put("textBgColor", project.textBgColor)
        json.put("textBgOpacity", project.textBgOpacity.toDouble())
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
        json.put("imageOverlayEffect", project.imageOverlayEffect)
        json.put("imageOverlayAnim", project.imageOverlayAnim)
        json.put("upscaleFactor", project.upscaleFactor.toDouble())
        json.put("drawingJson", project.drawingJson)

        // Professional Timeline Serialization
        val timelineJson = JSONObject()
        timelineJson.put("zoomLevel", project.timeline.zoomLevel.toDouble())
        timelineJson.put("playheadPosMs", project.timeline.playheadPosMs)

        val tracksArray = JSONArray()
        for (track in project.timeline.tracks) {
            val trackJson = JSONObject()
            trackJson.put("id", track.id)
            trackJson.put("type", track.type.name)
            trackJson.put("isLocked", track.isLocked)
            trackJson.put("isVisible", track.isVisible)
            trackJson.put("label", track.label)

            val trackClipsArray = JSONArray()
            for (clip in track.clips) {
                val clipJson = JSONObject()
                clipJson.put("id", clip.id)
                clipJson.put("name", clip.name)
                clipJson.put("path", clip.path)
                clipJson.put("type", clip.type.name)
                clipJson.put("startTimeMs", clip.startTimeMs)
                clipJson.put("durationMs", clip.durationMs)
                clipJson.put("mediaDurationMs", clip.mediaDurationMs)
                clipJson.put("trimStartMs", clip.trimStartMs)
                clipJson.put("trimEndMs", clip.trimEndMs)
                clipJson.put("speedFactor", clip.speedFactor.toDouble())
                clipJson.put("layerIndex", clip.layerIndex)
                clipJson.put("isLocked", clip.isLocked)
                clipJson.put("isVisible", clip.isVisible)
                clipJson.put("isSelected", clip.isSelected)
                clipJson.put("volume", clip.volume.toDouble())
                clipJson.put("opacity", clip.opacity.toDouble())
                clipJson.put("rotation", clip.rotation.toDouble())
                clipJson.put("scale", clip.scale.toDouble())
                clipJson.put("posX", clip.posX.toDouble())
                clipJson.put("posY", clip.posY.toDouble())
                trackClipsArray.put(clipJson)
            }
            trackJson.put("clips", trackClipsArray)
            tracksArray.put(trackJson)
        }
        timelineJson.put("tracks", tracksArray)
        json.put("timeline", timelineJson)
        // v6.1.0 Keyframe Animation serialization
        val kfTracksArray = JSONArray()
        for (kt in project.keyframeTracks) {
            val ktJson = JSONObject()
            ktJson.put("clipId", kt.clipId)
            val kfArray = JSONArray()
            for (kf in kt.keyframes) {
                val kfJson = JSONObject()
                kfJson.put("timeMs", kf.timeMs)
                kfJson.put("property", kf.property)
                kfJson.put("value", kf.value.toDouble())
                kfJson.put("easing", kf.easing.name)
                kfArray.put(kfJson)
            }
            ktJson.put("keyframes", kfArray)
            kfTracksArray.put(ktJson)
        }
        json.put("keyframeTracks", kfTracksArray)
        json.put("activeKeyframePreset", project.activeKeyframePreset)

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
            transitionDurationSec = json.optDouble("transitionDurationSec", 0.7).toFloat(),
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
            textStyleId = json.optString("textStyleId", "classic"),
            textPositionX = json.optDouble("textPositionX", 0.5).toFloat(),
            textPositionY = json.optDouble("textPositionY", 0.85).toFloat(),
            textColorHex = json.optString("textColorHex", "#FFFFFF"),
            textFontSize = json.optDouble("textFontSize", 24.0).toFloat(),
            textBold = json.optBoolean("textBold", false),
            textItalic = json.optBoolean("textItalic", false),
            textShadow = json.optBoolean("textShadow", false),
            textOutline = json.optBoolean("textOutline", false),
            textGlow = json.optBoolean("textGlow", false),
            textNeon = json.optBoolean("textNeon", false),
            textBgColor = json.optString("textBgColor", "#00000000"),
            textBgOpacity = json.optDouble("textBgOpacity", 0.5).toFloat(),
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
            isProTier = json.optBoolean("isProTier", false),
            imageOverlayEffect = json.optString("imageOverlayEffect", "none"),
            imageOverlayAnim = json.optString("imageOverlayAnim", "none"),
            upscaleFactor = json.optDouble("upscaleFactor", 1.0).toFloat(),
            drawingJson = json.optString("drawingJson", ""),
            timeline = json.optJSONObject("timeline")?.let { tJson ->
                VideoTimeline(
                    zoomLevel = tJson.optDouble("zoomLevel", 1.0).toFloat(),
                    playheadPosMs = tJson.optLong("playheadPosMs", 0L),
                    tracks = tJson.optJSONArray("tracks")?.let { trArr ->
                        (0 until trArr.length()).map { i ->
                            val trJson = trArr.getJSONObject(i)
                            TimelineTrack(
                                id = trJson.optString("id", UUID.randomUUID().toString()),
                                type = TrackType.valueOf(trJson.optString("type", TrackType.VIDEO.name)),
                                isLocked = trJson.optBoolean("isLocked", false),
                                isVisible = trJson.optBoolean("isVisible", true),
                                label = trJson.optString("label", ""),
                                clips = trJson.optJSONArray("clips")?.let { clArr ->
                                    (0 until clArr.length()).map { j ->
                                        val clJson = clArr.getJSONObject(j)
                                        TimelineClip(
                                            id = clJson.optString("id", UUID.randomUUID().toString()),
                                            name = clJson.optString("name", ""),
                                            path = clJson.optString("path", ""),
                                            type = TrackType.valueOf(clJson.optString("type", TrackType.VIDEO.name)),
                                            startTimeMs = clJson.optLong("startTimeMs", 0L),
                                            durationMs = clJson.optLong("durationMs", 0L),
                                            mediaDurationMs = clJson.optLong("mediaDurationMs", 0L),
                                            trimStartMs = clJson.optLong("trimStartMs", 0L),
                                            trimEndMs = clJson.optLong("trimEndMs", 0L),
                                            speedFactor = clJson.optDouble("speedFactor", 1.0).toFloat(),
                                            layerIndex = clJson.optInt("layerIndex", 0),
                                            isLocked = clJson.optBoolean("isLocked", false),
                                            isVisible = clJson.optBoolean("isVisible", true),
                                            isSelected = clJson.optBoolean("isSelected", false),
                                            volume = clJson.optDouble("volume", 1.0).toFloat(),
                                            opacity = clJson.optDouble("opacity", 1.0).toFloat(),
                                            rotation = clJson.optDouble("rotation", 0.0).toFloat(),
                                            scale = clJson.optDouble("scale", 1.0).toFloat(),
                                            posX = clJson.optDouble("posX", 0.5).toFloat(),
                                            posY = clJson.optDouble("posY", 0.5).toFloat()
                                        )
                                    }
                                } ?: emptyList()
                            )
                        }
                    } ?: emptyList()
                )
            } ?: VideoTimeline(),
            // v6.1.0 Keyframe Animation
            keyframeTracks = json.optJSONArray("keyframeTracks")?.let { kfArr ->
                (0 until kfArr.length()).map { i ->
                    val kfJson = kfArr.getJSONObject(i)
                    KeyframeTrack(
                        clipId = kfJson.optString("clipId", ""),
                        keyframes = kfJson.optJSONArray("keyframes")?.let { kArr ->
                            (0 until kArr.length()).map { j ->
                                val k = kArr.getJSONObject(j)
                                Keyframe(
                                    timeMs = k.optLong("timeMs", 0L),
                                    property = k.optString("property", "position"),
                                    value = k.optDouble("value", 0.0).toFloat(),
                                    easing = KeyframeEasing.valueOf(k.optString("easing", KeyframeEasing.LINEAR.name))
                                )
                            }
                        } ?: emptyList()
                    )
                }
            } ?: emptyList(),
            activeKeyframePreset = json.optString("activeKeyframePreset", "none")
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

                    val initialClip = com.powercut.editor.data.TimelineClip(
                        name = path.substringAfterLast("/"),
                        path = path,
                        type = com.powercut.editor.data.TrackType.VIDEO,
                        startTimeMs = 0L,
                        durationMs = realDurationMs,
                        mediaDurationMs = realDurationMs
                    )

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
                        speedFactor = speed,
                        timeline = com.powercut.editor.data.VideoTimeline(
                            tracks = listOf(
                                com.powercut.editor.data.TimelineTrack(
                                    type = com.powercut.editor.data.TrackType.VIDEO,
                                    label = "Main Video",
                                    clips = listOf(initialClip)
                                ),
                                com.powercut.editor.data.TimelineTrack(type = com.powercut.editor.data.TrackType.AUDIO, label = "Background Music"),
                                com.powercut.editor.data.TimelineTrack(type = com.powercut.editor.data.TrackType.TEXT, label = "Text & Titles"),
                                com.powercut.editor.data.TimelineTrack(type = com.powercut.editor.data.TrackType.STICKER, label = "Stickers"),
                                com.powercut.editor.data.TimelineTrack(type = com.powercut.editor.data.TrackType.OVERLAY, label = "Overlays")
                            )
                        )
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
        pushUndoState()
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
        pushUndoState()
        projectRepository.updateProject { project ->
            project.copy(selectedFilter = filterId)
        }
    }

    fun toggleMute() {
        pushUndoState()
        projectRepository.updateProject { project ->
            project.copy(isMuted = !project.isMuted)
        }
    }

    fun updateSpeed(speedFactor: Float) {
        pushUndoState()
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
            // Also reflect the music on the timeline as an AUDIO clip so the
            // user sees a layer on the Background Music track. If a music clip
            // already exists, replace it; otherwise add a new one.
            val updatedTracks = if (path.isNullOrBlank()) {
                // Music removed — clear the AUDIO track clips
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.AUDIO) track.copy(clips = emptyList()) else track
                }
            } else {
                val musicClip = TimelineClip(
                    name = "Background Music",
                    path = path,
                    type = TrackType.AUDIO,
                    startTimeMs = 0L,
                    durationMs = project.durationMs,
                    mediaDurationMs = project.durationMs,
                    volume = project.backgroundMusicVolume,
                    layerIndex = 0
                )
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.AUDIO) {
                        // Replace any existing clip (single BGM at a time)
                        track.copy(clips = listOf(musicClip))
                    } else {
                        track
                    }
                }
            }
            project.copy(
                backgroundMusicPath = path,
                timeline = project.timeline.copy(tracks = updatedTracks)
            )
        }
    }

    /**
     * Generate a REAL royalty-free music track using FFmpeg audio synthesis
     * and set it as the background music. This makes the "Royalty Free Music"
     * feature fully functional — tapping a track creates an actual audio file
     * that gets mixed into the exported video.
     */
    fun generateAndSetRoyaltyFreeMusic(trackId: String) {
        viewModelScope.launch {
            _musicState.value = Resource.Loading
            try {
                val path = royaltyFreeMusicGenerator.generateTrack(trackId, 30)
                if (path != null) {
                    // Use updateBackgroundMusic so the timeline AUDIO track
                    // also gets a clip (not just the flat field).
                    updateBackgroundMusic(path)
                    _musicState.value = Resource.Success(path)
                    Log.d("EditorViewModel", "Royalty-free music set: $trackId -> $path")
                } else {
                    _musicState.value = Resource.Error("Failed to generate music track")
                }
            } catch (e: Exception) {
                _musicState.value = Resource.Error("Music generation error: ${e.message}")
            }
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

    /**
     * Sets how long each inter-clip transition lasts, in seconds.
     *
     * Clamped to a sane authoring range here; the export pipeline clamps again
     * per cut point so a long transition can never exceed the clips it joins.
     */
    fun updateTransitionDuration(durationSec: Float) {
        projectRepository.updateProject { project ->
            project.copy(transitionDurationSec = durationSec.coerceIn(0.1f, 5.0f))
        }
    }

    fun updateRotation() {
        pushUndoState()
        projectRepository.updateProject { project ->
            val nextDegrees = (project.rotationDegrees + 90f) % 360f
            project.copy(rotationDegrees = nextDegrees)
        }
    }

    fun toggleFlipHorizontal() {
        pushUndoState()
        projectRepository.updateProject { project ->
            project.copy(isFlippedHorizontal = !project.isFlippedHorizontal)
        }
    }

    fun toggleFlipVertical() {
        pushUndoState()
        projectRepository.updateProject { project ->
            project.copy(isFlippedVertical = !project.isFlippedVertical)
        }
    }

    fun updateCropPreset(crop: String) {
        pushUndoState()
        projectRepository.updateProject { project ->
            project.copy(cropPreset = crop)
        }
    }

    // ── v7.2 Image overlay studio + upscale + canvas drawing ──

    /** v7.2 — Real export upscale factor (1x / 2x / 4x). */
    fun updateUpscale(factor: Float) {
        pushUndoState()
        projectRepository.updateProject { project ->
            project.copy(upscaleFactor = factor.coerceIn(1f, 4f))
        }
    }

    /** v7.2 — Per-overlay image effect (grayscale/sepia/vignette/…), applied to the overlay at export. */
    fun updateImageOverlayEffect(effect: String) {
        pushUndoState()
        projectRepository.updateProject { project ->
            project.copy(imageOverlayEffect = effect)
        }
    }

    /** v7.2 — Overlay entrance animation (fade/slide/zoom/…), rendered with real time expressions at export. */
    fun updateImageOverlayAnim(anim: String) {
        pushUndoState()
        projectRepository.updateProject { project ->
            project.copy(imageOverlayAnim = anim)
        }
    }

    /** v7.2 — Persist canvas drawing strokes (normalized JSON) so they render at export. */
    fun updateDrawing(json: String) {
        projectRepository.updateProject { project ->
            project.copy(drawingJson = json)
        }
    }

    /** v7.2 — Deletes the currently selected timeline clip (Delete Section). */
    fun deleteSelectedClip() {
        pushUndoState()
        projectRepository.updateProject { project ->
            val updatedTracks = project.timeline.tracks.map { track ->
                val selected = track.clips.filter { it.isSelected }
                if (selected.isEmpty()) track else track.copy(clips = track.clips - selected)
            }
            project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
        }
    }

    /** v7.2 — Applies a text background/stroke preset using the REAL text-style flags the export pipeline renders. */
    fun applyTextBgStyle(style: String) {
        pushUndoState()
        projectRepository.updateProject { project ->
            when (style.lowercase()) {
                "none" -> project.copy(
                    textShadow = false, textOutline = false, textGlow = false, textNeon = false,
                    textBgColor = "#00000000", textBgOpacity = 0.5f
                )
                "solid_bg" -> project.copy(textBgColor = "#000000", textBgOpacity = 0.6f)
                "outline" -> project.copy(textOutline = true, textShadow = false, textGlow = false, textNeon = false)
                "shadow" -> project.copy(textShadow = true, textOutline = false, textGlow = false, textNeon = false)
                "glow" -> project.copy(textGlow = true, textShadow = false, textOutline = false, textNeon = false)
                "neon" -> project.copy(textNeon = true, textShadow = false, textOutline = false, textGlow = false)
                "3d_shadow" -> project.copy(textShadow = true, textBgColor = "#000000", textBgOpacity = 0.3f)
                "double_outline" -> project.copy(textOutline = true, textShadow = true, textGlow = false, textNeon = false)
                "gradient_bg" -> project.copy(textBgColor = "#1a1a2e", textBgOpacity = 0.5f)
                "blur_bg" -> project.copy(textBgColor = "#000000", textBgOpacity = 0.35f)
                "box_bg" -> project.copy(textBgColor = "#000000", textBgOpacity = 0.7f)
                "strip_bg" -> project.copy(textBgColor = "#111111", textBgOpacity = 0.5f)
                else -> project.copy(
                    textShadow = false, textOutline = false, textGlow = false, textNeon = false,
                    textBgColor = "#00000000", textBgOpacity = 0.5f
                )
            }
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
            // Reflect the text overlay on the timeline as a TEXT clip so the
            // user sees a layer on the Text & Titles track.
            val updatedTracks = if (text.isNullOrBlank()) {
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.TEXT) track.copy(clips = emptyList()) else track
                }
            } else {
                val textClip = TimelineClip(
                    name = text.take(20),
                    path = "",
                    type = TrackType.TEXT,
                    startTimeMs = 0L,
                    durationMs = project.durationMs,
                    mediaDurationMs = project.durationMs,
                    layerIndex = 0
                )
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.TEXT) {
                        track.copy(clips = listOf(textClip))
                    } else {
                        track
                    }
                }
            }
            project.copy(
                activeTextOverlay = text,
                timeline = project.timeline.copy(tracks = updatedTracks)
            )
        }
    }

    fun updateTextAnimation(anim: String) {
        projectRepository.updateProject { project ->
            project.copy(textAnimationType = anim)
        }
    }

    fun updateTextStyle(styleId: String) {
        projectRepository.updateProject { project ->
            project.copy(textStyleId = styleId)
        }
    }

    fun updateTextPositionX(x: Float) {
        projectRepository.updateProject { project ->
            project.copy(textPositionX = x.coerceIn(0f, 1f))
        }
    }

    fun updateTextPositionY(y: Float) {
        projectRepository.updateProject { project ->
            project.copy(textPositionY = y.coerceIn(0f, 1f))
        }
    }

    fun updateTextColor(hex: String) {
        projectRepository.updateProject { project ->
            project.copy(textColorHex = hex)
        }
    }

    fun updateTextFontSize(size: Float) {
        projectRepository.updateProject { project ->
            project.copy(textFontSize = size.coerceIn(8f, 120f))
        }
    }

    fun toggleTextBold() {
        projectRepository.updateProject { it.copy(textBold = !it.textBold) }
    }

    fun toggleTextItalic() {
        projectRepository.updateProject { it.copy(textItalic = !it.textItalic) }
    }

    fun toggleTextShadow() {
        projectRepository.updateProject { it.copy(textShadow = !it.textShadow) }
    }

    fun toggleTextOutline() {
        projectRepository.updateProject { it.copy(textOutline = !it.textOutline) }
    }

    fun toggleTextGlow() {
        projectRepository.updateProject { it.copy(textGlow = !it.textGlow) }
    }

    fun toggleTextNeon() {
        projectRepository.updateProject { it.copy(textNeon = !it.textNeon) }
    }

    fun updateTextBgColor(hex: String) {
        projectRepository.updateProject { it.copy(textBgColor = hex) }
    }

    fun updateTextBgOpacity(opacity: Float) {
        projectRepository.updateProject { it.copy(textBgOpacity = opacity.coerceIn(0f, 1f)) }
    }

    fun updateStickerType(sticker: String) {
        projectRepository.updateProject { project ->
            // Reflect the sticker on the timeline as a STICKER clip so the
            // user sees a layer on the Stickers track.
            val updatedTracks = if (sticker == "none" || sticker.isBlank()) {
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.STICKER) track.copy(clips = emptyList()) else track
                }
            } else {
                val stickerClip = TimelineClip(
                    name = sticker,
                    path = "",
                    type = TrackType.STICKER,
                    startTimeMs = 0L,
                    durationMs = project.durationMs,
                    mediaDurationMs = project.durationMs,
                    layerIndex = 0
                )
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.STICKER) {
                        track.copy(clips = listOf(stickerClip))
                    } else {
                        track
                    }
                }
            }
            project.copy(
                stickerType = sticker,
                timeline = project.timeline.copy(tracks = updatedTracks)
            )
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
            // Reflect the image overlay on the timeline as an OVERLAY clip so
            // the user sees a layer on the Overlays track.
            val updatedTracks = if (path.isNullOrBlank()) {
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.OVERLAY) track.copy(clips = emptyList()) else track
                }
            } else {
                val overlayClip = TimelineClip(
                    name = "Image Overlay",
                    path = path,
                    type = TrackType.OVERLAY,
                    startTimeMs = 0L,
                    durationMs = project.durationMs,
                    mediaDurationMs = project.durationMs,
                    opacity = project.imageOverlayOpacity,
                    scale = project.imageOverlayScale,
                    posX = project.imageOverlayX,
                    posY = project.imageOverlayY,
                    layerIndex = 0
                )
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.OVERLAY) {
                        track.copy(clips = listOf(overlayClip))
                    } else {
                        track
                    }
                }
            }
            project.copy(
                imageOverlayPath = path,
                timeline = project.timeline.copy(tracks = updatedTracks)
            )
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

    fun updateImageOverlayX(x: Float) {
        projectRepository.updateProject { project ->
            project.copy(imageOverlayX = x.coerceIn(0f, 1f))
        }
    }

    fun updateImageOverlayY(y: Float) {
        projectRepository.updateProject { project ->
            project.copy(imageOverlayY = y.coerceIn(0f, 1f))
        }
    }

    fun updateImageOverlayCrop(crop: String) {
        projectRepository.updateProject { project ->
            project.copy(cropPreset = crop)
        }
    }

    fun updateSelectedEffect(effect: String) {
        pushUndoState()
        projectRepository.updateProject { project ->
            // Reflect the effect on the timeline as an EFFECT clip so the user
            // sees a layer indicating an active effect.
            val updatedTracks = if (effect == "none" || effect.isBlank()) {
                project.timeline.tracks.map { track ->
                    if (track.type == TrackType.EFFECT) track.copy(clips = emptyList()) else track
                }
            } else {
                val effectClip = TimelineClip(
                    name = effect,
                    path = "",
                    type = TrackType.EFFECT,
                    startTimeMs = 0L,
                    durationMs = project.durationMs,
                    mediaDurationMs = project.durationMs,
                    layerIndex = 0
                )
                // The default timeline has 5 tracks (VIDEO, AUDIO, TEXT,
                // STICKER, OVERLAY) but no EFFECT track. If one doesn't exist
                // yet, append it so the effect shows on the timeline.
                val hasEffectTrack = project.timeline.tracks.any { it.type == TrackType.EFFECT }
                if (hasEffectTrack) {
                    project.timeline.tracks.map { track ->
                        if (track.type == TrackType.EFFECT) {
                            track.copy(clips = listOf(effectClip))
                        } else {
                            track
                        }
                    }
                } else {
                    project.timeline.tracks + TimelineTrack(
                        type = TrackType.EFFECT,
                        label = "Effects",
                        clips = listOf(effectClip)
                    )
                }
            }
            project.copy(
                selectedEffect = effect,
                timeline = project.timeline.copy(tracks = updatedTracks)
            )
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
        pushUndoState()
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

    fun updatePlayhead(posMs: Long) {
        projectRepository.updateProject { project ->
            project.copy(timeline = project.timeline.copy(playheadPosMs = posMs))
        }
    }

    fun updateZoom(zoom: Float) {
        projectRepository.updateProject { project ->
            project.copy(timeline = project.timeline.copy(zoomLevel = zoom))
        }
    }

    // ── Keyframe animation support ──
    fun handleKeyframeAnimUpdate(command: String) {
        pushUndoState()
        projectRepository.updateProject { project ->
            val selectedClip = project.timeline.tracks.flatMap { it.clips }.find { it.isSelected }
                ?: project.timeline.tracks.flatMap { it.clips }.firstOrNull()
            val clipId = selectedClip?.id ?: "main_video"
            val clipDuration = selectedClip?.durationMs ?: project.durationMs
            val clipStart = selectedClip?.startTimeMs ?: 0L
            val playhead = project.timeline.playheadPosMs

            fun easingFromString(s: String): KeyframeEasing = when (s.lowercase()) {
                "linear" -> KeyframeEasing.LINEAR
                "easein" -> KeyframeEasing.EASE_IN
                "easeout" -> KeyframeEasing.EASE_OUT
                "easeinout" -> KeyframeEasing.EASE_IN_OUT
                "bounce" -> KeyframeEasing.BOUNCE
                "elastic" -> KeyframeEasing.ELASTIC
                "back" -> KeyframeEasing.EASE_IN_OUT
                "spring" -> KeyframeEasing.EASE_OUT
                else -> KeyframeEasing.LINEAR
            }

            fun buildPresetKeyframes(preset: String): List<KeyframeTrack> = when (preset.lowercase()) {
                "zoomin", "zoomin" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "scale", value = 1.0f, easing = KeyframeEasing.EASE_IN_OUT),
                        Keyframe(timeMs = clipDuration / 2, property = "scale", value = 1.25f, easing = KeyframeEasing.EASE_IN_OUT),
                        Keyframe(timeMs = clipDuration, property = "scale", value = 1.5f, easing = KeyframeEasing.EASE_IN_OUT)
                    ))
                )
                "zoomout", "zoomout" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "scale", value = 1.5f, easing = KeyframeEasing.EASE_IN_OUT),
                        Keyframe(timeMs = clipDuration / 2, property = "scale", value = 1.25f, easing = KeyframeEasing.EASE_IN_OUT),
                        Keyframe(timeMs = clipDuration, property = "scale", value = 1.0f, easing = KeyframeEasing.EASE_IN_OUT)
                    ))
                )
                "panlr", "panlr" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "position_x", value = 0.0f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration, property = "position_x", value = 1.0f, easing = KeyframeEasing.LINEAR)
                    ))
                )
                "panrl", "panrl" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "position_x", value = 1.0f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration, property = "position_x", value = 0.0f, easing = KeyframeEasing.LINEAR)
                    ))
                )
                "spin360", "spin360" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "rotation", value = 0.0f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration, property = "rotation", value = 360.0f, easing = KeyframeEasing.LINEAR)
                    ))
                )
                "fadeio", "fadeio" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "opacity", value = 0.0f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = (clipDuration * 0.2f).toLong(), property = "opacity", value = 1.0f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = (clipDuration * 0.8f).toLong(), property = "opacity", value = 1.0f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration, property = "opacity", value = 0.0f, easing = KeyframeEasing.LINEAR)
                    ))
                )
                "pulse", "pulse" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "scale", value = 1.0f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration / 4, property = "scale", value = 1.1f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration / 2, property = "scale", value = 1.0f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration * 3 / 4, property = "scale", value = 1.1f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration, property = "scale", value = 1.0f, easing = KeyframeEasing.LINEAR)
                    ))
                )
                "wobble", "wobble" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "position_x", value = 0.5f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration / 4, property = "position_x", value = 0.6f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration / 2, property = "position_x", value = 0.5f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration * 3 / 4, property = "position_x", value = 0.4f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration, property = "position_x", value = 0.5f, easing = KeyframeEasing.LINEAR)
                    ))
                )
                "slideup", "slideup" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "position_y", value = 1.0f, easing = KeyframeEasing.EASE_IN_OUT),
                        Keyframe(timeMs = clipDuration, property = "position_y", value = 0.0f, easing = KeyframeEasing.EASE_IN_OUT)
                    ))
                )
                "slidedown", "slidedown" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "position_y", value = 0.0f, easing = KeyframeEasing.EASE_IN_OUT),
                        Keyframe(timeMs = clipDuration, property = "position_y", value = 1.0f, easing = KeyframeEasing.EASE_IN_OUT)
                    ))
                )
                "bouncein", "bouncein" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "scale", value = 0.0f, easing = KeyframeEasing.EASE_OUT),
                        Keyframe(timeMs = (clipDuration * 0.6f).toLong(), property = "scale", value = 1.05f, easing = KeyframeEasing.EASE_OUT),
                        Keyframe(timeMs = (clipDuration * 0.8f).toLong(), property = "scale", value = 0.95f, easing = KeyframeEasing.EASE_OUT),
                        Keyframe(timeMs = clipDuration, property = "scale", value = 1.0f, easing = KeyframeEasing.EASE_OUT)
                    ))
                )
                "shake", "shake" -> listOf(
                    KeyframeTrack(clipId, listOf(
                        Keyframe(timeMs = 0, property = "position_x", value = 0.5f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration / 6, property = "position_x", value = 0.55f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration / 3, property = "position_x", value = 0.45f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration / 2, property = "position_x", value = 0.5f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration * 2 / 3, property = "position_x", value = 0.55f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration * 5 / 6, property = "position_x", value = 0.45f, easing = KeyframeEasing.LINEAR),
                        Keyframe(timeMs = clipDuration, property = "position_x", value = 0.5f, easing = KeyframeEasing.LINEAR)
                    ))
                )
                else -> emptyList()
            }

            when {
                command.startsWith("preset:", ignoreCase = true) -> {
                    val presetId = command.substringAfter("preset:", "").lowercase()
                    val newTracks = buildPresetKeyframes(presetId)
                    val otherTracks = project.keyframeTracks.filter { it.clipId != clipId }
                    project.copy(
                        activeKeyframePreset = presetId,
                        keyframeTracks = otherTracks + newTracks
                    )
                }
                command.startsWith("add:", ignoreCase = true) -> {
                    val property = command.substringAfter("add:", "").lowercase()
                    val currentKfs = project.keyframeTracks.filter { it.clipId == clipId }.flatMap { it.keyframes }
                    val existingForProp = currentKfs.filter { it.property == property }
                    val value = when (property) {
                        "scale" -> selectedClip?.scale ?: 1.0f
                        "position_x" -> selectedClip?.posX ?: 0.5f
                        "position_y" -> selectedClip?.posY ?: 0.5f
                        "rotation" -> selectedClip?.rotation ?: 0f
                        "opacity" -> selectedClip?.opacity ?: 1.0f
                        else -> 1.0f
                    }
                    val newKf = Keyframe(timeMs = playhead.coerceIn(0, clipDuration), property = property, value = value, easing = KeyframeEasing.LINEAR)
                    val updatedTracks = project.keyframeTracks.map { track ->
                        if (track.clipId == clipId && track.keyframes.any { it.property == property }) {
                            track.copy(keyframes = track.keyframes + newKf)
                        } else if (track.clipId == clipId) {
                            track.copy(keyframes = track.keyframes + newKf)
                        } else {
                            track
                        }
                    }
                    val hasTrackForClip = updatedTracks.any { it.clipId == clipId && it.keyframes.any { kf -> kf.property == property } }
                    val finalTracks = if (!hasTrackForClip) {
                        updatedTracks + KeyframeTrack(clipId, listOf(newKf))
                    } else {
                        updatedTracks
                    }
                    project.copy(keyframeTracks = finalTracks)
                }
                command.startsWith("clear", ignoreCase = true) -> {
                    project.copy(
                        keyframeTracks = project.keyframeTracks.filter { it.clipId != clipId },
                        activeKeyframePreset = "none"
                    )
                }
                command.startsWith("copy:", ignoreCase = true) -> {
                    val property = command.substringAfter("copy:", "").lowercase()
                    val sourceKfs = project.keyframeTracks.filter { it.clipId == clipId }.flatMap { it.keyframes }.filter { it.property == property }
                    if (sourceKfs.isNotEmpty()) {
                        val allProps = listOf("position_x", "position_y", "scale", "rotation", "opacity").filter { it != property }
                        val newTracks = allProps.map { prop ->
                            KeyframeTrack(clipId, sourceKfs.map { kf -> kf.copy(property = prop) })
                        }
                        val otherTracks = project.keyframeTracks.filter { it.clipId != clipId }
                        project.copy(keyframeTracks = otherTracks + newTracks)
                    } else {
                        project.copy(keyframeTracks = project.keyframeTracks.filter { it.clipId != clipId })
                    }
                }
                command.startsWith("reverse", ignoreCase = true) -> {
                    val currentKfs = project.keyframeTracks.filter { it.clipId == clipId }.flatMap { it.keyframes }
                    val reversed = currentKfs.map { it.copy(timeMs = (clipDuration - it.timeMs).coerceAtLeast(0)) }.sortedBy { it.timeMs }
                    val updated = reversed.groupBy { it.property }.map { (prop, kfs) ->
                        KeyframeTrack(clipId, kfs)
                    }
                    val otherTracks = project.keyframeTracks.filter { it.clipId != clipId }
                    project.copy(keyframeTracks = otherTracks + updated)
                }
                command.contains(":") -> {
                    val parts = command.split(":")
                    if (parts.size >= 2) {
                        val property = parts[0].lowercase()
                        val easingStr = parts[1]
                        val easing = easingFromString(easingStr)
                        val updated = project.keyframeTracks.map { track ->
                            if (track.clipId == clipId) {
                                track.copy(keyframes = track.keyframes.map { kf ->
                                    if (kf.property == property) kf.copy(easing = easing) else kf
                                })
                            } else track
                        }
                        project.copy(keyframeTracks = updated)
                    } else {
                        project.copy(activeKeyframePreset = command)
                    }
                }
                else -> {
                    project.copy(activeKeyframePreset = command)
                }
            }
        }
    }

    fun selectTimelineClip(clipId: String) {
        projectRepository.updateProject { project ->
            val updatedTracks = project.timeline.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    clip.copy(isSelected = clip.id == clipId)
                })
            }
            project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
        }
    }

    fun moveTimelineClip(clipId: String, newStartMs: Long) {
        projectRepository.updateProject { project ->
            val updatedTracks = project.timeline.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) clip.copy(startTimeMs = newStartMs) else clip
                })
            }
            project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
        }
    }

    fun trimTimelineClip(clipId: String, newTrimStartMs: Long, newTrimEndMs: Long) {
        projectRepository.updateProject { project ->
            val updatedTracks = project.timeline.tracks.map { track ->
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        val newDurationMs = ((newTrimEndMs - newTrimStartMs) / clip.speedFactor).toLong()
                        clip.copy(
                            trimStartMs = newTrimStartMs,
                            trimEndMs = newTrimEndMs,
                            durationMs = newDurationMs
                        )
                    } else clip
                })
            }
            project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
        }
    }

    fun addTimelineClip(clip: TimelineClip, trackType: TrackType) {
        projectRepository.updateProject { project ->
            val updatedTracks = project.timeline.tracks.map { track ->
                if (track.type == trackType) {
                    track.copy(clips = track.clips + clip)
                } else {
                    track
                }
            }
            project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
        }
    }

    fun removeTimelineClip(clipId: String) {
        projectRepository.updateProject { project ->
            val updatedTracks = project.timeline.tracks.map { track ->
                track.copy(clips = track.clips.filterNot { it.id == clipId })
            }
            project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
        }
    }

    fun updateTimelineClip(updatedClip: TimelineClip) {
        projectRepository.updateProject { project ->
            val updatedTracks = project.timeline.tracks.map { track ->
                track.copy(clips = track.clips.map { if (it.id == updatedClip.id) updatedClip else it })
            }
            project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
        }
    }

    fun splitTimelineClip(clipId: String, splitTimeMs: Long) {
        projectRepository.updateProject { project ->
            val updatedTracks = project.timeline.tracks.map { track ->
                val clipToSplit = track.clips.find { it.id == clipId }
                if (clipToSplit != null && splitTimeMs > clipToSplit.startTimeMs && splitTimeMs < clipToSplit.startTimeMs + clipToSplit.durationMs) {
                    val localSplitTimeMs = splitTimeMs - clipToSplit.startTimeMs
                    val mediaSplitOffset = (localSplitTimeMs * clipToSplit.speedFactor).toLong()

                    val firstHalf = clipToSplit.copy(
                        durationMs = localSplitTimeMs,
                        trimEndMs = clipToSplit.trimStartMs + mediaSplitOffset
                    )

                    val secondHalf = clipToSplit.copy(
                        id = UUID.randomUUID().toString(),
                        startTimeMs = splitTimeMs,
                        durationMs = clipToSplit.durationMs - localSplitTimeMs,
                        trimStartMs = clipToSplit.trimStartMs + mediaSplitOffset
                    )
                    
                    val newClips = track.clips.flatMap { 
                        if (it.id == clipId) listOf(firstHalf, secondHalf) else listOf(it)
                    }
                    track.copy(clips = newClips)
                } else {
                    track
                }
            }
            project.copy(timeline = project.timeline.copy(tracks = updatedTracks))
        }
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

    /** Update the target frame rate for export. */
    fun updateTargetFps(fps: Int) {
        projectRepository.updateProject { project ->
            project.copy(targetFps = fps)
        }
    }
}
