package com.powercut.editor.data

import org.json.JSONArray
import org.json.JSONObject

data class AppliedEffect(
    val featureId: String,
    val category: String,
    val videoChain: String = "",
    val audioChain: String = "",
    val parameters: Map<String, Any> = emptyMap(),
    val isPro: Boolean = false
)

data class TextOverlay(
    val id: String = "",
    val text: String = "",
    val positionX: Float = 0.5f,
    val positionY: Float = 0.85f,
    val scale: Float = 1.0f,
    val colorHex: String = "#FFFFFF",
    val fontSize: Float = 24f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val hasShadow: Boolean = false,
    val hasOutline: Boolean = false,
    val hasGlow: Boolean = false,
    val hasNeon: Boolean = false,
    val backgroundColorHex: String = "#00000000",
    val backgroundOpacity: Float = 0.5f,
    val animationType: String = "fade",
    val animationDurationMs: Int = 500,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
)

data class AudioTrack(
    val id: String = "",
    val name: String = "",
    val sourceUri: String = "",
    val sourcePath: String = "",
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isLooping: Boolean = false,
    val isDuckingEnabled: Boolean = false,
    val duckThreshold: Float = 0.05f,
    val duckRatio: Float = 8.0f,
    val fadeInMs: Int = 0,
    val fadeOutMs: Int = 0,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
)

data class VideoProject(
    val name: String = "Untitled Project",
    val videoPath: String = "",
    val resolutionWidth: Int = 1920,
    val resolutionHeight: Int = 1080,
    val fps: Int = 30,
    val durationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val targetResolution: String = "1080p",
    val selectedFilter: String = "none",
    val isMuted: Boolean = false,

    // High-priority features
    val speedFactor: Float = 1.0f,
    val aspectPreset: String = "16:9",
    val transitionType: String = "none",
    /**
     * Duration, in seconds, of each inter-clip transition.
     *
     * This is the amount of footage the transition OVERLAPS at every cut point,
     * so the exported timeline is sum(clips) - (transitions * this value).
     * Clamped per cut point at export time so it can never exceed the clips it
     * joins (see TransitionCatalog.clampDuration).
     */
    val transitionDurationSec: Float = 0.7f,
    val backgroundMusicPath: String? = null,
    val backgroundMusicVolume: Float = 0.5f,
    val videoVolume: Float = 1.0f,
    val autoCaptionsLanguage: String = "off",
    val isSilenceRemoverEnabled: Boolean = false,

    // Professional Editing Features
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val cropPreset: String = "free",
    val speedCurve: String = "constant",
    val activeTextOverlay: String? = null,
    val textAnimationType: String = "fade",
    val textStyleId: String = "classic",
    val textPositionX: Float = 0.5f,
    val textPositionY: Float = 0.85f,
    val textColorHex: String = "#FFFFFF",
    val textFontSize: Float = 24f,
    // Extended text styling
    val textBold: Boolean = false,
    val textItalic: Boolean = false,
    val textShadow: Boolean = false,
    val textOutline: Boolean = false,
    val textGlow: Boolean = false,
    val textNeon: Boolean = false,
    val textBgColor: String = "#00000000",   // ARGB hex, transparent default
    val textBgOpacity: Float = 0.5f,
    val stickerType: String = "none",

    // Advanced Exclusive features
    val activeTemplateId: String = "none",
    val visualizerStyle: String = "none",
    val isBeatSyncEnabled: Boolean = false,
    val active3DShapeMask: String = "none",

    // NextGen Pro features
    val imageOverlayPath: String? = null,
    val imageOverlayOpacity: Float = 1.0f,
    val imageOverlayScale: Float = 1.0f,
    val imageOverlayX: Float = 0.5f,
    val imageOverlayY: Float = 0.5f,
    val selectedEffect: String = "none",
    val activeLayers: List<String> = emptyList(),

    // Green Screen / Chroma Key
    val greenScreenEnabled: Boolean = false,
    val greenScreenColor: String = "green",
    val greenScreenThreshold: Float = 0.4f,
    val greenScreenBackgroundPath: String? = null,
    val greenScreenAutoBgIndex: Int = -1,

    // Eraser Tools
    val eraserMode: String = "none",
    val eraserBrushSize: Float = 30f,
    val eraserTolerance: Float = 0.5f,
    val eraserSoftEdge: Boolean = true,

    // Image Editor
    val imageEditorBrightness: Float = 0f,
    val imageEditorContrast: Float = 1f,
    val imageEditorSaturation: Float = 1f,
    val imageEditorBlur: Float = 0f,
    val imageEditorSharpen: Float = 0f,
    val imageEditorTemperature: Float = 0f,
    val imageEditorVignette: Float = 0f,
    val imageEditorGrain: Float = 0f,
    val imageEditorFade: Float = 0f,
    val imageEditorHighlights: Float = 0f,
    val imageEditorShadows: Float = 0f,
    val imageEditorExposure: Float = 0f,

    // Video Orientation Tools
    val orientationMode: String = "free",
    val verticalSafeZone: Boolean = false,
    val horizontalLetterbox: Boolean = false,
    val autoReframeEnabled: Boolean = false,

    // ── v4.4.0 Premium Looks (50+ Brightness / HDR / iPhone grades) ──
    val activePremiumLook: String = "none",

    // ── NEW v4.0 CapCut-sync Pro features ──
    val blendMode: String = "none",
    val isReverseEnabled: Boolean = false,
    val freezeFrameMs: Long = 0L,
    val colorLift: Float = 0f,
    val colorGamma: Float = 0f,
    val colorGain: Float = 0f,
    val audioEffect: String = "none",
    val voiceChangerPitch: Float = 0f,
    val isAudioDuckingEnabled: Boolean = false,
    val borderStyle: String = "none",
    val watermarkPath: String? = null,
    val vignetteStyle: String = "none",

    // ── v6.0.0 PREMIUM EXPORT features ──
    /** Target frame rate for export (24/30/60/120). Replaces the old hardcoded fps=30. */
    val targetFps: Int = 30,
    /** 10-bit HDR (BT.2020 PQ) export pipeline. */
    val isHdrEnabled: Boolean = false,
    /** High-bitrate visually-lossless export (lower CRF + higher maxrate). */
    val isHighBitrateEnabled: Boolean = false,
    /** Batch export queue membership (Pro). */
    val isBatchExport: Boolean = false,

    // ── v6.0.0 AI feature pipeline ──
    /** Active AI feature id from PremiumFeatureCatalog (e.g. "ai_frame_interp"). "none" = disabled. */
    val activeAiFeature: String = "none",

    // ── v6.0.0 Social media export preset ──
    /** Social preset id from PremiumFeatureCatalog (e.g. "sm_tiktok"). "none" = use aspectPreset. */
    val socialPreset: String = "none",

    // ── v6.0.0 Pro tier ──
    /** Whether the user has unlocked Pro tier (disables watermark, unlocks Pro assets). */
    val isProTier: Boolean = false,

    // ── Phase 1: Professional Timeline ──
    val timeline: VideoTimeline = VideoTimeline(),
    // ── v6.1.0 Keyframe Animation ──
    val keyframeTracks: List<KeyframeTrack> = emptyList(),
    val activeKeyframePreset: String = "none",
    // ── JSON persistence: active effects/filters ──
    val activeEffects: List<AppliedEffect> = emptyList(),
    // ── JSON persistence: text overlays ──
    val textOverlays: List<TextOverlay> = emptyList(),
    // ── JSON persistence: audio tracks ──
    val audioTracks: List<AudioTrack> = emptyList()
) {
    val isTrimmed: Boolean
        get() = trimStartMs > 0L || trimEndMs < durationMs && trimEndMs > 0L

    val isSpeedChanged: Boolean
        get() = speedFactor != 1.0f

    val hasBackgroundMusic: Boolean
        get() = !backgroundMusicPath.isNullOrBlank()

    val isGreenScreenActive: Boolean
        get() = greenScreenEnabled

    val isEraserActive: Boolean
        get() = eraserMode != "none"

    val isImageEditorActive: Boolean
        get() = imageEditorBrightness != 0f || imageEditorContrast != 1f || imageEditorSaturation != 1f ||
                imageEditorBlur != 0f || imageEditorSharpen != 0f || imageEditorTemperature != 0f ||
                imageEditorVignette != 0f || imageEditorGrain != 0f || imageEditorFade != 0f ||
                imageEditorExposure != 0f || imageEditorHighlights != 0f || imageEditorShadows != 0f

    val isVerticalMode: Boolean
        get() = orientationMode == "vertical" || aspectPreset == "9:16"

    val isHorizontalMode: Boolean
        get() = orientationMode == "horizontal" || aspectPreset == "16:9"

    val isBlendModeActive: Boolean
        get() = blendMode != "none"

    val isReversed: Boolean
        get() = isReverseEnabled

    val hasFreezeFrame: Boolean
        get() = freezeFrameMs > 0L

    val isColorCurvesActive: Boolean
        get() = colorLift != 0f || colorGamma != 0f || colorGain != 0f

    val isAudioEffectActive: Boolean
        get() = audioEffect != "none"

    val isVoiceChanged: Boolean
        get() = voiceChangerPitch != 0f

    // ── v6.0.0 computed properties ──
    val isAiFeatureActive: Boolean
        get() = activeAiFeature != "none"

    val isHdrExport: Boolean
        get() = isHdrEnabled

    val isHighBitrate: Boolean
        get() = isHighBitrateEnabled

    val hasSocialPreset: Boolean
        get() = socialPreset != "none"

    val isAudioDuckingActive: Boolean
        get() = isAudioDuckingEnabled

    val isBorderStyleActive: Boolean
        get() = borderStyle != "none"

    val hasWatermark: Boolean
        get() = !watermarkPath.isNullOrBlank()

    val isVignetteStyleActive: Boolean
        get() = vignetteStyle != "none"

    /** v4.4.0: true when a premium Brightness/HDR/iPhone look is active. */
    val isPremiumLookActive: Boolean
        get() = activePremiumLook != "none" && activePremiumLook.isNotBlank()

    companion object {

        private const val KEY_FORMAT_VERSION = "format_version"
        private const val KEY_FORMAT_VERSION_VALUE = "1.0"
        private const val KEY_PROJECT = "project"
        private const val KEY_TIMELINE = "timeline"
        private const val KEY_EFFECTS = "effects"
        private const val KEY_TEXT_OVERLAYS = "text_overlays"
        private const val KEY_AUDIO_TRACKS = "audio_tracks"
        private const val KEY_KEYFRAME_TRACKS = "keyframe_tracks"

        fun toJson(project: VideoProject): JSONObject {
            val root = JSONObject()
            root.put(KEY_FORMAT_VERSION, KEY_FORMAT_VERSION_VALUE)

            val projectJson = JSONObject().apply {
                put("name", project.name)
                put("resolution_width", project.resolutionWidth)
                put("resolution_height", project.resolutionHeight)
                put("fps", project.fps)
                put("duration_ms", project.durationMs)
                put("trim_start_ms", project.trimStartMs)
                put("trim_end_ms", project.trimEndMs)
                put("aspect_preset", project.aspectPreset)
                put("speed_factor", project.speedFactor)
                put("transition_type", project.transitionType)
                put("transition_duration_sec", project.transitionDurationSec)
                put("rotation_degrees", project.rotationDegrees)
                put("is_flipped_horizontal", project.isFlippedHorizontal)
                put("is_flipped_vertical", project.isFlippedVertical)
                put("crop_preset", project.cropPreset)
                put("speed_curve", project.speedCurve)
                put("sticker_type", project.stickerType)
                put("active_template_id", project.activeTemplateId)
                put("visualizer_style", project.visualizerStyle)
                put("is_beat_sync_enabled", project.isBeatSyncEnabled)
                put("active_3d_shape_mask", project.active3DShapeMask)
                put("green_screen_enabled", project.greenScreenEnabled)
                put("green_screen_color", project.greenScreenColor)
                put("green_screen_threshold", project.greenScreenThreshold)
                put("green_screen_background_path", project.greenScreenBackgroundPath)
                put("green_screen_auto_bg_index", project.greenScreenAutoBgIndex)
                put("eraser_mode", project.eraserMode)
                put("eraser_brush_size", project.eraserBrushSize)
                put("eraser_tolerance", project.eraserTolerance)
                put("eraser_soft_edge", project.eraserSoftEdge)
                put("orientation_mode", project.orientationMode)
                put("vertical_safe_zone", project.verticalSafeZone)
                put("horizontal_letterbox", project.horizontalLetterbox)
                put("auto_reframe_enabled", project.autoReframeEnabled)
                put("active_premium_look", project.activePremiumLook)
                put("blend_mode", project.blendMode)
                put("is_reverse_enabled", project.isReverseEnabled)
                put("freeze_frame_ms", project.freezeFrameMs)
                put("color_lift", project.colorLift)
                put("color_gamma", project.colorGamma)
                put("color_gain", project.colorGain)
                put("audio_effect", project.audioEffect)
                put("voice_changer_pitch", project.voiceChangerPitch)
                put("is_audio_ducking_enabled", project.isAudioDuckingEnabled)
                put("border_style", project.borderStyle)
                put("vignette_style", project.vignetteStyle)
                put("target_fps", project.targetFps)
                put("is_hdr_enabled", project.isHdrEnabled)
                put("is_high_bitrate_enabled", project.isHighBitrateEnabled)
                put("is_batch_export", project.isBatchExport)
                put("active_ai_feature", project.activeAiFeature)
                put("social_preset", project.socialPreset)
                put("is_pro_tier", project.isProTier)
                put("active_keyframe_preset", project.activeKeyframePreset)
            }
            root.put(KEY_PROJECT, projectJson)

            val timelineJson = JSONObject().apply {
                put("zoom_level", project.timeline.zoomLevel)
                put("playhead_pos_ms", project.timeline.playheadPosMs)
                val tracksArray = JSONArray()
                for (track in project.timeline.tracks) {
                    val trackJson = JSONObject().apply {
                        put("id", track.id)
                        put("type", track.type.name)
                        put("label", track.label)
                        put("is_locked", track.isLocked)
                        put("is_visible", track.isVisible)
                        val clipsArray = JSONArray()
                        for (clip in track.clips) {
                            clipsArray.put(clipToJson(clip))
                        }
                        put("clips", clipsArray)
                    }
                    tracksArray.put(trackJson)
                }
                put("tracks", tracksArray)
            }
            root.put(KEY_TIMELINE, timelineJson)

            val effectsArray = JSONArray()
            for (effect in project.activeEffects) {
                effectsArray.put(effectToJson(effect))
            }
            root.put(KEY_EFFECTS, effectsArray)

            val overlaysArray = JSONArray()
            for (overlay in project.textOverlays) {
                overlaysArray.put(textOverlayToJson(overlay))
            }
            root.put(KEY_TEXT_OVERLAYS, overlaysArray)

            val audioTracksArray = JSONArray()
            for (audioTrack in project.audioTracks) {
                audioTracksArray.put(audioTrackToJson(audioTrack))
            }
            root.put(KEY_AUDIO_TRACKS, audioTracksArray)

            val keyframeTracksArray = JSONArray()
            for (kfTrack in project.keyframeTracks) {
                keyframeTracksArray.put(keyframeTrackToJson(kfTrack))
            }
            root.put(KEY_KEYFRAME_TRACKS, keyframeTracksArray)

            return root
        }

        fun fromJson(json: JSONObject): VideoProject {
            val projectJson = json.getJSONObject(KEY_PROJECT)

            val timelineJson = json.getJSONObject(KEY_TIMELINE)
            val tracksList = mutableListOf<TimelineTrack>()
            val tracksArray = timelineJson.getJSONArray("tracks")
            for (i in 0 until tracksArray.length()) {
                val trackJson = tracksArray.getJSONObject(i)
                val clipsList = mutableListOf<TimelineClip>()
                val clipsArray = trackJson.getJSONArray("clips")
                for (j in 0 until clipsArray.length()) {
                    clipsList.add(clipFromJson(clipsArray.getJSONObject(j)))
                }
                tracksList.add(
                    TimelineTrack(
                        id = trackJson.getString("id"),
                        type = TrackType.valueOf(trackJson.getString("type")),
                        clips = clipsList,
                        isLocked = trackJson.optBoolean("is_locked", false),
                        isVisible = trackJson.optBoolean("is_visible", true),
                        label = trackJson.optString("label", trackJson.getString("type"))
                    )
                )
            }
            val timeline = VideoTimeline(
                tracks = tracksList,
                zoomLevel = timelineJson.optDouble("zoom_level", 1.0).toFloat(),
                playheadPosMs = timelineJson.optLong("playhead_pos_ms", 0L)
            )

            val effectsList = mutableListOf<AppliedEffect>()
            val effectsArray = json.getJSONArray(KEY_EFFECTS)
            for (i in 0 until effectsArray.length()) {
                effectsList.add(effectFromJson(effectsArray.getJSONObject(i)))
            }

            val overlaysList = mutableListOf<TextOverlay>()
            val overlaysArray = json.getJSONArray(KEY_TEXT_OVERLAYS)
            for (i in 0 until overlaysArray.length()) {
                overlaysList.add(textOverlayFromJson(overlaysArray.getJSONObject(i)))
            }

            val audioTracksList = mutableListOf<AudioTrack>()
            val audioTracksArray = json.getJSONArray(KEY_AUDIO_TRACKS)
            for (i in 0 until audioTracksArray.length()) {
                audioTracksList.add(audioTrackFromJson(audioTracksArray.getJSONObject(i)))
            }

            val keyframeTracksList = mutableListOf<KeyframeTrack>()
            val keyframeTracksArray = json.optJSONArray(KEY_KEYFRAME_TRACKS)
            if (keyframeTracksArray != null) {
                for (i in 0 until keyframeTracksArray.length()) {
                    keyframeTracksList.add(keyframeTrackFromJson(keyframeTracksArray.getJSONObject(i)))
                }
            }

            return VideoProject(
                name = projectJson.optString("name", "Untitled Project"),
                resolutionWidth = projectJson.optInt("resolution_width", 1920),
                resolutionHeight = projectJson.optInt("resolution_height", 1080),
                fps = projectJson.optInt("fps", 30),
                durationMs = projectJson.optLong("duration_ms", 0L),
                trimStartMs = projectJson.optLong("trim_start_ms", 0L),
                trimEndMs = projectJson.optLong("trim_end_ms", 0L),
                aspectPreset = projectJson.optString("aspect_preset", "16:9"),
                speedFactor = projectJson.optDouble("speed_factor", 1.0).toFloat(),
                transitionType = projectJson.optString("transition_type", "none"),
                transitionDurationSec = projectJson.optDouble("transition_duration_sec", 0.7).toFloat(),
                rotationDegrees = projectJson.optDouble("rotation_degrees", 0.0).toFloat(),
                isFlippedHorizontal = projectJson.optBoolean("is_flipped_horizontal", false),
                isFlippedVertical = projectJson.optBoolean("is_flipped_vertical", false),
                cropPreset = projectJson.optString("crop_preset", "free"),
                speedCurve = projectJson.optString("speed_curve", "constant"),
                stickerType = projectJson.optString("sticker_type", "none"),
                activeTemplateId = projectJson.optString("active_template_id", "none"),
                visualizerStyle = projectJson.optString("visualizer_style", "none"),
                isBeatSyncEnabled = projectJson.optBoolean("is_beat_sync_enabled", false),
                active3DShapeMask = projectJson.optString("active_3d_shape_mask", "none"),
                greenScreenEnabled = projectJson.optBoolean("green_screen_enabled", false),
                greenScreenColor = projectJson.optString("green_screen_color", "green"),
                greenScreenThreshold = projectJson.optDouble("green_screen_threshold", 0.4).toFloat(),
                greenScreenBackgroundPath = projectJson.optString("green_screen_background_path", null),
                greenScreenAutoBgIndex = projectJson.optInt("green_screen_auto_bg_index", -1),
                eraserMode = projectJson.optString("eraser_mode", "none"),
                eraserBrushSize = projectJson.optDouble("eraser_brush_size", 30.0).toFloat(),
                eraserTolerance = projectJson.optDouble("eraser_tolerance", 0.5).toFloat(),
                eraserSoftEdge = projectJson.optBoolean("eraser_soft_edge", true),
                orientationMode = projectJson.optString("orientation_mode", "free"),
                verticalSafeZone = projectJson.optBoolean("vertical_safe_zone", false),
                horizontalLetterbox = projectJson.optBoolean("horizontal_letterbox", false),
                autoReframeEnabled = projectJson.optBoolean("auto_reframe_enabled", false),
                activePremiumLook = projectJson.optString("active_premium_look", "none"),
                blendMode = projectJson.optString("blend_mode", "none"),
                isReverseEnabled = projectJson.optBoolean("is_reverse_enabled", false),
                freezeFrameMs = projectJson.optLong("freeze_frame_ms", 0L),
                colorLift = projectJson.optDouble("color_lift", 0.0).toFloat(),
                colorGamma = projectJson.optDouble("color_gamma", 0.0).toFloat(),
                colorGain = projectJson.optDouble("color_gain", 0.0).toFloat(),
                audioEffect = projectJson.optString("audio_effect", "none"),
                voiceChangerPitch = projectJson.optDouble("voice_changer_pitch", 0.0).toFloat(),
                isAudioDuckingEnabled = projectJson.optBoolean("is_audio_ducking_enabled", false),
                borderStyle = projectJson.optString("border_style", "none"),
                vignetteStyle = projectJson.optString("vignette_style", "none"),
                targetFps = projectJson.optInt("target_fps", 30),
                isHdrEnabled = projectJson.optBoolean("is_hdr_enabled", false),
                isHighBitrateEnabled = projectJson.optBoolean("is_high_bitrate_enabled", false),
                isBatchExport = projectJson.optBoolean("is_batch_export", false),
                activeAiFeature = projectJson.optString("active_ai_feature", "none"),
                socialPreset = projectJson.optString("social_preset", "none"),
                isProTier = projectJson.optBoolean("is_pro_tier", false),
                timeline = timeline,
                keyframeTracks = keyframeTracksList,
                activeKeyframePreset = projectJson.optString("active_keyframe_preset", "none"),
                activeEffects = effectsList,
                textOverlays = overlaysList,
                audioTracks = audioTracksList
            )
        }

        private fun clipToJson(clip: TimelineClip): JSONObject {
            val parameters = JSONObject().apply {
                put("volume", clip.volume)
                put("opacity", clip.opacity)
                put("rotation", clip.rotation)
                put("scale", clip.scale)
                put("pos_x", clip.posX)
                put("pos_y", clip.posY)
            }
            return JSONObject().apply {
                put("id", clip.id)
                put("name", clip.name)
                put("source_uri", clip.path)
                put("source_path", clip.path)
                put("type", clip.type.name)
                put("start_time_ms", clip.startTimeMs)
                put("duration_ms", clip.durationMs)
                put("media_duration_ms", clip.mediaDurationMs)
                put("trim_start_ms", clip.trimStartMs)
                put("trim_end_ms", clip.trimEndMs)
                put("speed_factor", clip.speedFactor)
                put("layer_index", clip.layerIndex)
                put("is_locked", clip.isLocked)
                put("is_visible", clip.isVisible)
                put("parameters", parameters)
            }
        }

        private fun clipFromJson(json: JSONObject): TimelineClip {
            val parameters = json.optJSONObject("parameters") ?: JSONObject()
            return TimelineClip(
                id = json.optString("id"),
                name = json.optString("name", ""),
                path = json.optString("source_path", json.optString("source_uri", "")),
                type = TrackType.valueOf(json.optString("type", "VIDEO")),
                startTimeMs = json.optLong("start_time_ms", 0L),
                durationMs = json.optLong("duration_ms", 0L),
                mediaDurationMs = json.optLong("media_duration_ms", 0L),
                trimStartMs = json.optLong("trim_start_ms", 0L),
                trimEndMs = json.optLong("trim_end_ms", 0L),
                speedFactor = json.optDouble("speed_factor", 1.0).toFloat(),
                layerIndex = json.optInt("layer_index", 0),
                isLocked = json.optBoolean("is_locked", false),
                isVisible = json.optBoolean("is_visible", true),
                volume = parameters.optDouble("volume", 1.0).toFloat(),
                opacity = parameters.optDouble("opacity", 1.0).toFloat(),
                rotation = parameters.optDouble("rotation", 0.0).toFloat(),
                scale = parameters.optDouble("scale", 1.0).toFloat(),
                posX = parameters.optDouble("pos_x", 0.5).toFloat(),
                posY = parameters.optDouble("pos_y", 0.5).toFloat()
            )
        }

        private fun effectToJson(effect: AppliedEffect): JSONObject {
            val parametersJson = JSONObject()
            for ((key, value) in effect.parameters) {
                parametersJson.put(key, value)
            }
            return JSONObject().apply {
                put("feature_id", effect.featureId)
                put("category", effect.category)
                put("video_chain", effect.videoChain)
                put("audio_chain", effect.audioChain)
                put("parameters", parametersJson)
                put("is_pro", effect.isPro)
            }
        }

        private fun effectFromJson(json: JSONObject): AppliedEffect {
            val parametersMap = mutableMapOf<String, Any>()
            val parametersJson = json.optJSONObject("parameters")
            if (parametersJson != null) {
                val keysIterator = parametersJson.keys()
                while (keysIterator.hasNext()) {
                    val key = keysIterator.next()
                    parametersMap[key] = parametersJson.get(key)
                }
            }
            return AppliedEffect(
                featureId = json.optString("feature_id", ""),
                category = json.optString("category", ""),
                videoChain = json.optString("video_chain", ""),
                audioChain = json.optString("audio_chain", ""),
                parameters = parametersMap,
                isPro = json.optBoolean("is_pro", false)
            )
        }

        private fun textOverlayToJson(overlay: TextOverlay): JSONObject {
            return JSONObject().apply {
                put("id", overlay.id)
                put("text", overlay.text)
                put("position_x", overlay.positionX)
                put("position_y", overlay.positionY)
                put("scale", overlay.scale)
                put("color_hex", overlay.colorHex)
                put("font_size", overlay.fontSize)
                put("bold", overlay.isBold)
                put("italic", overlay.isItalic)
                put("shadow", overlay.hasShadow)
                put("outline", overlay.hasOutline)
                put("glow", overlay.hasGlow)
                put("neon", overlay.hasNeon)
                put("background_color", overlay.backgroundColorHex)
                put("background_opacity", overlay.backgroundOpacity)
                put("animation_type", overlay.animationType)
                put("animation_duration_ms", overlay.animationDurationMs)
                put("start_time_ms", overlay.startTimeMs)
                put("end_time_ms", overlay.endTimeMs)
            }
        }

        private fun textOverlayFromJson(json: JSONObject): TextOverlay {
            return TextOverlay(
                id = json.optString("id"),
                text = json.optString("text", ""),
                positionX = json.optDouble("position_x", 0.5).toFloat(),
                positionY = json.optDouble("position_y", 0.85).toFloat(),
                scale = json.optDouble("scale", 1.0).toFloat(),
                colorHex = json.optString("color_hex", "#FFFFFF"),
                fontSize = json.optDouble("font_size", 24.0).toFloat(),
                isBold = json.optBoolean("bold", false),
                isItalic = json.optBoolean("italic", false),
                hasShadow = json.optBoolean("shadow", false),
                hasOutline = json.optBoolean("outline", false),
                hasGlow = json.optBoolean("glow", false),
                hasNeon = json.optBoolean("neon", false),
                backgroundColorHex = json.optString("background_color", "#00000000"),
                backgroundOpacity = json.optDouble("background_opacity", 0.5).toFloat(),
                animationType = json.optString("animation_type", "fade"),
                animationDurationMs = json.optInt("animation_duration_ms", 500),
                startTimeMs = json.optLong("start_time_ms", 0L),
                endTimeMs = json.optLong("end_time_ms", 0L)
            )
        }

        private fun audioTrackToJson(track: AudioTrack): JSONObject {
            return JSONObject().apply {
                put("id", track.id)
                put("name", track.name)
                put("source_uri", track.sourceUri)
                put("source_path", track.sourcePath)
                put("volume", track.volume)
                put("is_muted", track.isMuted)
                put("is_looping", track.isLooping)
                put("is_ducking_enabled", track.isDuckingEnabled)
                put("duck_threshold", track.duckThreshold)
                put("duck_ratio", track.duckRatio)
                put("fade_in_ms", track.fadeInMs)
                put("fade_out_ms", track.fadeOutMs)
                put("start_time_ms", track.startTimeMs)
                put("end_time_ms", track.endTimeMs)
            }
        }

        private fun audioTrackFromJson(json: JSONObject): AudioTrack {
            return AudioTrack(
                id = json.optString("id"),
                name = json.optString("name", ""),
                sourceUri = json.optString("source_uri", ""),
                sourcePath = json.optString("source_path", ""),
                volume = json.optDouble("volume", 1.0).toFloat(),
                isMuted = json.optBoolean("is_muted", false),
                isLooping = json.optBoolean("is_looping", false),
                isDuckingEnabled = json.optBoolean("is_ducking_enabled", false),
                duckThreshold = json.optDouble("duck_threshold", 0.05).toFloat(),
                duckRatio = json.optDouble("duck_ratio", 8.0).toFloat(),
                fadeInMs = json.optInt("fade_in_ms", 0),
                fadeOutMs = json.optInt("fade_out_ms", 0),
                startTimeMs = json.optLong("start_time_ms", 0L),
                endTimeMs = json.optLong("end_time_ms", 0L)
            )
        }

        private fun keyframeTrackToJson(track: KeyframeTrack): JSONObject {
            val keyframesArray = JSONArray()
            for (kf in track.keyframes) {
                keyframesArray.put(keyframeToJson(kf))
            }
            return JSONObject().apply {
                put("clip_id", track.clipId)
                put("keyframes", keyframesArray)
            }
        }

        private fun keyframeTrackFromJson(json: JSONObject): KeyframeTrack {
            val keyframesList = mutableListOf<Keyframe>()
            val keyframesArray = json.optJSONArray("keyframes")
            if (keyframesArray != null) {
                for (i in 0 until keyframesArray.length()) {
                    keyframesList.add(keyframeFromJson(keyframesArray.getJSONObject(i)))
                }
            }
            return KeyframeTrack(
                clipId = json.optString("clip_id"),
                keyframes = keyframesList
            )
        }

        private fun keyframeToJson(keyframe: Keyframe): JSONObject {
            return JSONObject().apply {
                put("time_ms", keyframe.timeMs)
                put("property", keyframe.property)
                put("value", keyframe.value)
                put("easing", keyframe.easing.name)
            }
        }

        private fun keyframeFromJson(json: JSONObject): Keyframe {
            return Keyframe(
                timeMs = json.optLong("time_ms", 0L),
                property = json.optString("property", ""),
                    value = json.optDouble("value", 0.0).toFloat(),
                easing = runCatching { KeyframeEasing.valueOf(json.optString("easing", "LINEAR")) }.getOrDefault(KeyframeEasing.LINEAR)
            )
        }
    }
}
