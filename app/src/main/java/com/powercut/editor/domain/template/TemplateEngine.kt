package com.powercut.editor.domain.template

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AI Template Engine — JSON-based template parser that auto-applies video cuts,
 * speed ramps, keyframe transitions, and text animations based on audio beat markers.
 */
class TemplateEngine {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Parse a template JSON string into a TemplateDefinition.
     */
    fun parseTemplate(jsonString: String): TemplateDefinition {
        return json.decodeFromString(jsonString)
    }

    /**
     * Parse a built-in preset template.
     */
    fun getPresetTemplate(preset: TemplatePreset): TemplateDefinition {
        return when (preset) {
            TemplatePreset.TIKTOK_VIRAL -> parseTemplate(PRESET_TIKTOK_VIRAL)
            TemplatePreset.REELS_CINEMATIC -> parseTemplate(PRESET_REELS_CINEMATIC)
            TemplatePreset.VLOG_TRAVEL -> parseTemplate(PRESET_VLOG_TRAVEL)
            TemplatePreset.MUSIC_VIDEO -> parseTemplate(PRESET_MUSIC_VIDEO)
            TemplatePreset.BOOTSTRAP_AD -> parseTemplate(PRESET_BOOTSTRAP_AD)
            TemplatePreset.ANIME_STYLE -> parseTemplate(PRESET_ANIME_STYLE)
            TemplatePreset.RETRO_80S -> parseTemplate(PRESET_RETRO_80S)
            TemplatePreset.MINIMAL -> parseTemplate(PRESET_MINIMAL)
        }
    }

    /**
     * Apply a template to the current project timeline.
     * Returns a list of timeline edits to apply.
     */
    suspend fun applyTemplate(
        template: TemplateDefinition,
        audioDurationsMs: List<Long>,
        totalDurationMs: Long
    ): TemplateApplicationResult = withContext(Dispatchers.Default) {
        val edits = mutableListOf<TimelineEdit>()

        // 1. Apply beat-synced cuts
        template.beatCuts?.let { beatCuts ->
            val beatInterval = totalDurationMs / beatCuts.numBeats
            for (i in 0 until beatCuts.numBeats) {
                val cutTime = i * beatInterval.toLong()
                edits.add(
                    TimelineEdit.Cut(
                        timeMs = cutTime,
                        transitionType = beatCuts.transitionType
                    )
                )
            }
        }

        // 2. Apply speed ramps
        template.speedRamps?.forEach { ramp ->
            edits.add(
                TimelineEdit.SpeedRamp(
                    startTimeMs = ramp.startTimeMs,
                    endTimeMs = ramp.endTimeMs,
                    speedCurve = ramp.curve
                )
            )
        }

        // 3. Apply keyframe animations
        template.keyframeAnimations?.forEach { anim ->
            edits.add(
                TimelineEdit.KeyframeAnimation(
                    property = anim.property,
                    startTimeMs = anim.startTimeMs,
                    endTimeMs = anim.endTimeMs,
                    startValue = anim.startValue,
                    endValue = anim.endValue,
                    easing = anim.easing
                )
            )
        }

        // 4. Apply text overlays
        template.textOverlays?.forEach { text ->
            edits.add(
                TimelineEdit.TextOverlay(
                    text = text.content,
                    startTimeMs = text.startTimeMs,
                    endTimeMs = text.endTimeMs,
                    style = text.style,
                    animation = text.animation
                )
            )
        }

        // 5. Apply filter changes
        template.filterSequence?.forEach { filter ->
            edits.add(
                TimelineEdit.FilterChange(
                    timeMs = filter.timeMs,
                    filterName = filter.name,
                    intensity = filter.intensity
                )
            )
        }

        // 6. Apply transitions between cuts
        template.transitions?.forEach { trans ->
            edits.add(
                TimelineEdit.Transition(
                    startTimeMs = trans.startTimeMs,
                    durationMs = trans.durationMs,
                    type = trans.type
                )
            )
        }

        TemplateApplicationResult(
            edits = edits,
            totalEdits = edits.size,
            estimatedRenderTimeMs = edits.size * 100L
        )
    }

    /**
     * Auto-detect beats from audio and generate a beat-synced template.
     */
    suspend fun autoGenerateFromBeats(
        beatTimestampsMs: List<Long>,
        totalDurationMs: Long,
        style: TemplateStyle = TemplateStyle.CINEMATIC
    ): TemplateDefinition = withContext(Dispatchers.Default) {
        val numBeats = beatTimestampsMs.size

        TemplateDefinition(
            name = "Auto-Generated ${style.displayName}",
            description = "Auto-generated from $numBeats detected beats",
            beatCuts = BeatCutConfig(
                numBeats = numBeats,
                transitionType = style.defaultTransition
            ),
            keyframeAnimations = generateBeatSyncedAnimations(beatTimestampsMs, style),
            filterSequence = generateBeatSyncedFilters(beatTimestampsMs, style),
            transitions = generateTransitions(beatTimestampsMs, style)
        )
    }

    private fun generateBeatSyncedAnimations(
        beats: List<Long>,
        style: TemplateStyle
    ): List<KeyframeAnimConfig> {
        return beats.mapIndexed { index, timeMs ->
            val nextTime = beats.getOrNull(index + 1) ?: (timeMs + 500)
            KeyframeAnimConfig(
                property = if (index % 2 == 0) "SCALE" else "ROTATION",
                startTimeMs = timeMs,
                endTimeMs = nextTime,
                startValue = if (index % 2 == 0) 1f else 0f,
                endValue = if (index % 2 == 0) 1.1f else if (style == TemplateStyle.ENERGETIC) 5f else 2f,
                easing = style.defaultEasing
            )
        }
    }

    private fun generateBeatSyncedFilters(
        beats: List<Long>,
        style: TemplateStyle
    ): List<FilterChangeConfig> {
        val filters = style.defaultFilters
        return beats.mapIndexed { index, timeMs ->
            FilterChangeConfig(
                timeMs = timeMs,
                name = filters[index % filters.size],
                intensity = 0.8f
            )
        }
    }

    private fun generateTransitions(
        beats: List<Long>,
        style: TemplateStyle
    ): List<TransitionConfig> {
        return beats.mapIndexed { index, timeMs ->
            if (index % 3 == 0) {
                TransitionConfig(
                    startTimeMs = timeMs - 100,
                    durationMs = 200,
                    type = style.defaultTransition
                )
            } else null
        }.filterNotNull()
    }

    companion object {
        // ── Built-in preset templates ──

        private const val PRESET_TIKTOK_VIRAL = """
        {
            "name": "TikTok Viral",
            "description": "Fast cuts, zoom punches, neon flash effects",
            "beatCuts": { "numBeats": 16, "transitionType": "GLITCH" },
            "speedRamps": [
                { "startTimeMs": 0, "endTimeMs": 1000, "curve": "HERO" }
            ],
            "keyframeAnimations": [
                { "property": "SCALE", "startTimeMs": 0, "endTimeMs": 500, "startValue": 1.0, "endValue": 1.3, "easing": "BEZIER_CUBIC" }
            ],
            "textOverlays": [
                { "content": "POWERCUT", "startTimeMs": 0, "endTimeMs": 2000, "style": "NEON", "animation": "BOUNCE" }
            ],
            "filterSequence": [
                { "timeMs": 0, "name": "Cyberpunk", "intensity": 0.9 }
            ]
        }
        """

        private const val PRESET_REELS_CINEMATIC = """
        {
            "name": "Reels Cinematic",
            "description": "Smooth cinematic grade with crossfade transitions",
            "beatCuts": { "numBeats": 8, "transitionType": "CROSSFADE" },
            "keyframeAnimations": [
                { "property": "OPACITY", "startTimeMs": 0, "endTimeMs": 1000, "startValue": 0.0, "endValue": 1.0, "easing": "EASE_OUT" }
            ],
            "filterSequence": [
                { "timeMs": 0, "name": "Teal & Orange", "intensity": 0.85 }
            ],
            "transitions": [
                { "startTimeMs": 0, "durationMs": 500, "type": "CROSSFADE" }
            ]
        }
        """

        private const val PRESET_VLOG_TRAVEL = """
        {
            "name": "Vlog Travel",
            "description": "Warm golden hour tones with smooth transitions",
            "beatCuts": { "numBeats": 12, "transitionType": "SMOOTH_CUT" },
            "keyframeAnimations": [
                { "property": "SCALE", "startTimeMs": 0, "endTimeMs": 800, "startValue": 1.05, "endValue": 1.0, "easing": "EASE_IN_OUT" }
            ],
            "filterSequence": [
                { "timeMs": 0, "name": "Golden Hour", "intensity": 0.75 }
            ]
        }
        """

        private const val PRESET_MUSIC_VIDEO = """
        {
            "name": "Music Video",
            "description": "Beat-synced zoom punches with strobe effects",
            "beatCuts": { "numBeats": 32, "transitionType": "FLASH" },
            "speedRamps": [
                { "startTimeMs": 0, "endTimeMs": 500, "curve": "BULLET_TIME" }
            ],
            "keyframeAnimations": [
                { "property": "SCALE", "startTimeMs": 0, "endTimeMs": 200, "startValue": 1.0, "endValue": 1.5, "easing": "BOUNCE" },
                { "property": "ROTATION", "startTimeMs": 0, "endTimeMs": 300, "startValue": 0, "endValue": 5, "easing": "ELASTIC" }
            ],
            "filterSequence": [
                { "timeMs": 0, "name": "Cyberpunk Neon", "intensity": 1.0 }
            ]
        }
        """

        private const val PRESET_BOOTSTRAP_AD = """
        {
            "name": "Bootstrap Ad",
            "description": "Quick product showcase with text reveals",
            "beatCuts": { "numBeats": 6, "transitionType": "WIPE" },
            "keyframeAnimations": [
                { "property": "POSITION_X", "startTimeMs": 0, "endTimeMs": 500, "startValue": 100, "endValue": 0, "easing": "BEZIER_CUBIC" }
            ],
            "textOverlays": [
                { "content": "YOUR PRODUCT", "startTimeMs": 200, "endTimeMs": 1500, "style": "BOLD", "animation": "SLIDE_LEFT" }
            ],
            "filterSequence": [
                { "timeMs": 0, "name": "Bright Pop", "intensity": 0.7 }
            ]
        }
        """

        private const val PRESET_ANIME_STYLE = """
        {
            "name": "Anime Style",
            "description": "High-energy anime-inspired cuts with dramatic zooms",
            "beatCuts": { "numBeats": 20, "transitionType": "ZOOM_IN" },
            "keyframeAnimations": [
                { "property": "SCALE", "startTimeMs": 0, "endTimeMs": 150, "startValue": 2.0, "endValue": 1.0, "easing": "BEZIER_QUINTIC" },
                { "property": "ROTATION", "startTimeMs": 0, "endTimeMs": 200, "startValue": -10, "endValue": 0, "easing": "ELASTIC" }
            ],
            "filterSequence": [
                { "timeMs": 0, "name": "Cyberpunk", "intensity": 1.0 }
            ]
        }
        """

        private const val PRESET_RETRO_80S = """
        {
            "name": "Retro 80s",
            "description": "Synthwave aesthetics with VHS effects",
            "beatCuts": { "numBeats": 12, "transitionType": "GLITCH" },
            "speedRamps": [
                { "startTimeMs": 0, "endTimeMs": 1000, "curve": "MONTAGE" }
            ],
            "keyframeAnimations": [
                { "property": "FILTER_INTENSITY", "startTimeMs": 0, "endTimeMs": 2000, "startValue": 0.5, "endValue": 1.0, "easing": "EASE_IN_OUT" }
            ],
            "filterSequence": [
                { "timeMs": 0, "name": "Vintage Film", "intensity": 0.9 },
                { "timeMs": 1000, "name": "Cyberpunk Neon", "intensity": 0.7 }
            ]
        }
        """

        private const val PRESET_MINIMAL = """
        {
            "name": "Minimal",
            "description": "Clean cuts with subtle fade transitions",
            "beatCuts": { "numBeats": 4, "transitionType": "FADE" },
            "keyframeAnimations": [
                { "property": "OPACITY", "startTimeMs": 0, "endTimeMs": 500, "startValue": 0.0, "endValue": 1.0, "easing": "EASE_OUT" }
            ],
            "filterSequence": [
                { "timeMs": 0, "name": "Cool Tone", "intensity": 0.5 }
            ]
        }
        """
    }
}

// ── Data Models ──

@Serializable
data class TemplateDefinition(
    val name: String = "",
    val description: String = "",
    val beatCuts: BeatCutConfig? = null,
    val speedRamps: List<SpeedRampConfig>? = null,
    val keyframeAnimations: List<KeyframeAnimConfig>? = null,
    val textOverlays: List<TextOverlayConfig>? = null,
    val filterSequence: List<FilterChangeConfig>? = null,
    val transitions: List<TransitionConfig>? = null
)

@Serializable
data class BeatCutConfig(
    val numBeats: Int = 8,
    val transitionType: String = "CROSSFADE"
)

@Serializable
data class SpeedRampConfig(
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 1000,
    val curve: String = "CONSTANT"
)

@Serializable
data class KeyframeAnimConfig(
    val property: String = "SCALE",
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 1000,
    val startValue: Float = 1f,
    val endValue: Float = 1f,
    val easing: String = "EASE_OUT"
)

@Serializable
data class TextOverlayConfig(
    val content: String = "",
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 2000,
    val style: String = "BOLD",
    val animation: String = "FADE_IN"
)

@Serializable
data class FilterChangeConfig(
    val timeMs: Long = 0,
    val name: String = "",
    val intensity: Float = 0.75f
)

@Serializable
data class TransitionConfig(
    val startTimeMs: Long = 0,
    val durationMs: Long = 500,
    val type: String = "CROSSFADE"
)

enum class TemplatePreset(val displayName: String) {
    TIKTOK_VIRAL("TikTok Viral"),
    REELS_CINEMATIC("Reels Cinematic"),
    VLOG_TRAVEL("Vlog Travel"),
    MUSIC_VIDEO("Music Video"),
    BOOTSTRAP_AD("Bootstrap Ad"),
    ANIME_STYLE("Anime Style"),
    RETRO_80S("Retro 80s"),
    MINIMAL("Minimal")
}

enum class TemplateStyle(
    val displayName: String,
    val defaultTransition: String,
    val defaultEasing: String,
    val defaultFilters: List<String>
) {
    CINEMATIC("Cinematic", "CROSSFADE", "EASE_IN_OUT", listOf("Teal & Orange", "Film Noir", "Golden Hour")),
    ENERGETIC("Energetic", "GLITCH", "BEZIER_CUBIC", listOf("Cyberpunk", "Neon Glow", "Flash")),
    DRAMATIC("Dramatic", "ZOOM_IN", "BEZIER_QUINTIC", listOf("Film Noir", "B&W Drama", "Dramatic")),
    PLAYFUL("Playful", "WIPE", "BOUNCE", listOf("Vintage Film", "Cool Tone", "Bright Pop")),
    SMOOTH("Smooth", "FADE", "EASE_OUT", listOf("Cool Tone", "Soft Glow", "Muted"))
}

sealed class TimelineEdit {
    data class Cut(val timeMs: Long, val transitionType: String) : TimelineEdit()
    data class SpeedRamp(val startTimeMs: Long, val endTimeMs: Long, val speedCurve: String) : TimelineEdit()
    data class KeyframeAnimation(val property: String, val startTimeMs: Long, val endTimeMs: Long, val startValue: Float, val endValue: Float, val easing: String) : TimelineEdit()
    data class TextOverlay(val text: String, val startTimeMs: Long, val endTimeMs: Long, val style: String, val animation: String) : TimelineEdit()
    data class FilterChange(val timeMs: Long, val filterName: String, val intensity: Float) : TimelineEdit()
    data class Transition(val startTimeMs: Long, val durationMs: Long, val type: String) : TimelineEdit()
}

data class TemplateApplicationResult(
    val edits: List<TimelineEdit>,
    val totalEdits: Int,
    val estimatedRenderTimeMs: Long
)
