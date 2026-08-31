package com.powercut.editor.domain.template

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

/**
 * AI Template Engine — JSON-based template parser that auto-applies video cuts,
 * speed ramps, keyframe transitions, and text animations based on audio beat markers.
 */
class TemplateEngine {

    /**
     * Parse a template JSON string into a TemplateDefinition.
     */
    fun parseTemplate(jsonString: String): TemplateDefinition {
        val json = JSONObject(jsonString)
        
        return TemplateDefinition(
            name = json.optString("name", ""),
            description = json.optString("description", ""),
            beatCuts = json.optJSONObject("beatCuts")?.let { bc ->
                BeatCutConfig(
                    numBeats = bc.optInt("numBeats", 8),
                    transitionType = bc.optString("transitionType", "CROSSFADE")
                )
            },
            speedRamps = json.optJSONArray("speedRamps")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    SpeedRampConfig(
                        startTimeMs = obj.optLong("startTimeMs", 0),
                        endTimeMs = obj.optLong("endTimeMs", 1000),
                        curve = obj.optString("curve", "CONSTANT")
                    )
                }
            },
            keyframeAnimations = json.optJSONArray("keyframeAnimations")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    KeyframeAnimConfig(
                        property = obj.optString("property", "SCALE"),
                        startTimeMs = obj.optLong("startTimeMs", 0),
                        endTimeMs = obj.optLong("endTimeMs", 1000),
                        startValue = obj.optDouble("startValue", 1.0).toFloat(),
                        endValue = obj.optDouble("endValue", 1.0).toFloat(),
                        easing = obj.optString("easing", "EASE_OUT")
                    )
                }
            },
            textOverlays = json.optJSONArray("textOverlays")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    TextOverlayConfig(
                        content = obj.optString("content", ""),
                        startTimeMs = obj.optLong("startTimeMs", 0),
                        endTimeMs = obj.optLong("endTimeMs", 2000),
                        style = obj.optString("style", "BOLD"),
                        animation = obj.optString("animation", "FADE_IN")
                    )
                }
            },
            filterSequence = json.optJSONArray("filterSequence")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    FilterChangeConfig(
                        timeMs = obj.optLong("timeMs", 0),
                        name = obj.optString("name", ""),
                        intensity = obj.optDouble("intensity", 0.75).toFloat()
                    )
                }
            },
            transitions = json.optJSONArray("transitions")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    TransitionConfig(
                        startTimeMs = obj.optLong("startTimeMs", 0),
                        durationMs = obj.optLong("durationMs", 500),
                        type = obj.optString("type", "CROSSFADE")
                    )
                }
            }
        )
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
     */
    suspend fun applyTemplate(
        template: TemplateDefinition,
        audioDurationsMs: List<Long>,
        totalDurationMs: Long
    ): TemplateApplicationResult = withContext(Dispatchers.Default) {
        val edits = mutableListOf<TimelineEdit>()

        template.beatCuts?.let { beatCuts ->
            val beatInterval = totalDurationMs / beatCuts.numBeats
            for (i in 0 until beatCuts.numBeats) {
                val cutTime = i * beatInterval.toLong()
                edits.add(TimelineEdit.Cut(cutTime, beatCuts.transitionType))
            }
        }

        template.speedRamps?.forEach { ramp ->
            edits.add(TimelineEdit.SpeedRamp(ramp.startTimeMs, ramp.endTimeMs, ramp.curve))
        }

        template.keyframeAnimations?.forEach { anim ->
            edits.add(TimelineEdit.KeyframeAnimation(anim.property, anim.startTimeMs, anim.endTimeMs, anim.startValue, anim.endValue, anim.easing))
        }

        template.textOverlays?.forEach { text ->
            edits.add(TimelineEdit.TextOverlay(text.content, text.startTimeMs, text.endTimeMs, text.style, text.animation))
        }

        template.filterSequence?.forEach { filter ->
            edits.add(TimelineEdit.FilterChange(filter.timeMs, filter.name, filter.intensity))
        }

        template.transitions?.forEach { trans ->
            edits.add(TimelineEdit.Transition(trans.startTimeMs, trans.durationMs, trans.type))
        }

        TemplateApplicationResult(edits = edits, totalEdits = edits.size, estimatedRenderTimeMs = edits.size * 100L)
    }

    /**
     * Auto-detect beats from audio and generate a beat-synced template.
     */
    suspend fun autoGenerateFromBeats(
        beatTimestampsMs: List<Long>,
        totalDurationMs: Long,
        style: TemplateStyle = TemplateStyle.CINEMATIC
    ): TemplateDefinition = withContext(Dispatchers.Default) {
        TemplateDefinition(
            name = "Auto-Generated ${style.displayName}",
            description = "Auto-generated from ${beatTimestampsMs.size} detected beats",
            beatCuts = BeatCutConfig(numBeats = beatTimestampsMs.size, transitionType = style.defaultTransition),
            keyframeAnimations = beatTimestampsMs.mapIndexed { index, timeMs ->
                val nextTime = beatTimestampsMs.getOrNull(index + 1) ?: (timeMs + 500)
                KeyframeAnimConfig(
                    property = if (index % 2 == 0) "SCALE" else "ROTATION",
                    startTimeMs = timeMs, endTimeMs = nextTime,
                    startValue = if (index % 2 == 0) 1f else 0f,
                    endValue = if (index % 2 == 0) 1.1f else if (style == TemplateStyle.ENERGETIC) 5f else 2f,
                    easing = style.defaultEasing
                )
            },
            filterSequence = beatTimestampsMs.mapIndexed { index, timeMs ->
                FilterChangeConfig(timeMs = timeMs, name = style.defaultFilters[index % style.defaultFilters.size], intensity = 0.8f)
            }
        )
    }

    companion object {
        private const val PRESET_TIKTOK_VIRAL = """{"name":"TikTok Viral","description":"Fast cuts, zoom punches, neon flash effects","beatCuts":{"numBeats":16,"transitionType":"GLITCH"},"speedRamps":[{"startTimeMs":0,"endTimeMs":1000,"curve":"HERO"}],"keyframeAnimations":[{"property":"SCALE","startTimeMs":0,"endTimeMs":500,"startValue":1.0,"endValue":1.3,"easing":"BEZIER_CUBIC"}],"textOverlays":[{"content":"POWERCUT","startTimeMs":0,"endTimeMs":2000,"style":"NEON","animation":"BOUNCE"}],"filterSequence":[{"timeMs":0,"name":"Cyberpunk","intensity":0.9}]}"""
        private const val PRESET_REELS_CINEMATIC = """{"name":"Reels Cinematic","description":"Smooth cinematic grade with crossfade transitions","beatCuts":{"numBeats":8,"transitionType":"CROSSFADE"},"keyframeAnimations":[{"property":"OPACITY","startTimeMs":0,"endTimeMs":1000,"startValue":0.0,"endValue":1.0,"easing":"EASE_OUT"}],"filterSequence":[{"timeMs":0,"name":"Teal & Orange","intensity":0.85}],"transitions":[{"startTimeMs":0,"durationMs":500,"type":"CROSSFADE"}]}"""
        private const val PRESET_VLOG_TRAVEL = """{"name":"Vlog Travel","description":"Warm golden hour tones with smooth transitions","beatCuts":{"numBeats":12,"transitionType":"SMOOTH_CUT"},"keyframeAnimations":[{"property":"SCALE","startTimeMs":0,"endTimeMs":800,"startValue":1.05,"endValue":1.0,"easing":"EASE_IN_OUT"}],"filterSequence":[{"timeMs":0,"name":"Golden Hour","intensity":0.75}]}"""
        private const val PRESET_MUSIC_VIDEO = """{"name":"Music Video","description":"Beat-synced zoom punches with strobe effects","beatCuts":{"numBeats":32,"transitionType":"FLASH"},"speedRamps":[{"startTimeMs":0,"endTimeMs":500,"curve":"BULLET_TIME"}],"keyframeAnimations":[{"property":"SCALE","startTimeMs":0,"endTimeMs":200,"startValue":1.0,"endValue":1.5,"easing":"BOUNCE"}],"filterSequence":[{"timeMs":0,"name":"Cyberpunk Neon","intensity":1.0}]}"""
        private const val PRESET_BOOTSTRAP_AD = """{"name":"Bootstrap Ad","description":"Quick product showcase with text reveals","beatCuts":{"numBeats":6,"transitionType":"WIPE"},"keyframeAnimations":[{"property":"POSITION_X","startTimeMs":0,"endTimeMs":500,"startValue":100,"endValue":0,"easing":"BEZIER_CUBIC"}],"textOverlays":[{"content":"YOUR PRODUCT","startTimeMs":200,"endTimeMs":1500,"style":"BOLD","animation":"SLIDE_LEFT"}],"filterSequence":[{"timeMs":0,"name":"Bright Pop","intensity":0.7}]}"""
        private const val PRESET_ANIME_STYLE = """{"name":"Anime Style","description":"High-energy anime-inspired cuts with dramatic zooms","beatCuts":{"numBeats":20,"transitionType":"ZOOM_IN"},"keyframeAnimations":[{"property":"SCALE","startTimeMs":0,"endTimeMs":150,"startValue":2.0,"endValue":1.0,"easing":"BEZIER_QUINTIC"}],"filterSequence":[{"timeMs":0,"name":"Cyberpunk","intensity":1.0}]}"""
        private const val PRESET_RETRO_80S = """{"name":"Retro 80s","description":"Synthwave aesthetics with VHS effects","beatCuts":{"numBeats":12,"transitionType":"GLITCH"},"speedRamps":[{"startTimeMs":0,"endTimeMs":1000,"curve":"MONTAGE"}],"filterSequence":[{"timeMs":0,"name":"Vintage Film","intensity":0.9},{"timeMs":1000,"name":"Cyberpunk Neon","intensity":0.7}]}"""
        private const val PRESET_MINIMAL = """{"name":"Minimal","description":"Clean cuts with subtle fade transitions","beatCuts":{"numBeats":4,"transitionType":"FADE"},"keyframeAnimations":[{"property":"OPACITY","startTimeMs":0,"endTimeMs":500,"startValue":0.0,"endValue":1.0,"easing":"EASE_OUT"}],"filterSequence":[{"timeMs":0,"name":"Cool Tone","intensity":0.5}]}"""
    }
}

// ── Data Models ──

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

data class BeatCutConfig(val numBeats: Int = 8, val transitionType: String = "CROSSFADE")
data class SpeedRampConfig(val startTimeMs: Long = 0, val endTimeMs: Long = 1000, val curve: String = "CONSTANT")
data class KeyframeAnimConfig(val property: String = "SCALE", val startTimeMs: Long = 0, val endTimeMs: Long = 1000, val startValue: Float = 1f, val endValue: Float = 1f, val easing: String = "EASE_OUT")
data class TextOverlayConfig(val content: String = "", val startTimeMs: Long = 0, val endTimeMs: Long = 2000, val style: String = "BOLD", val animation: String = "FADE_IN")
data class FilterChangeConfig(val timeMs: Long = 0, val name: String = "", val intensity: Float = 0.75f)
data class TransitionConfig(val startTimeMs: Long = 0, val durationMs: Long = 500, val type: String = "CROSSFADE")

enum class TemplatePreset(val displayName: String) {
    TIKTOK_VIRAL("TikTok Viral"), REELS_CINEMATIC("Reels Cinematic"),
    VLOG_TRAVEL("Vlog Travel"), MUSIC_VIDEO("Music Video"),
    BOOTSTRAP_AD("Bootstrap Ad"), ANIME_STYLE("Anime Style"),
    RETRO_80S("Retro 80s"), MINIMAL("Minimal")
}

enum class TemplateStyle(val displayName: String, val defaultTransition: String, val defaultEasing: String, val defaultFilters: List<String>) {
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

data class TemplateApplicationResult(val edits: List<TimelineEdit>, val totalEdits: Int, val estimatedRenderTimeMs: Long)
