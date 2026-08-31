package com.powercut.editor.domain.keyframe

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Precision keyframe engine supporting linear, bezier, and custom easing functions.
 * Manages keyframes for all transform properties (Position, Scale, Rotation, Opacity, etc.)
 */
class KeyframeEngine {
    private val keyframeTracks = mutableMapOf<KeyframeProperty, MutableList<Keyframe>>()

    /**
     * Add a keyframe for a specific property at a given time.
     */
    fun addKeyframe(
        property: KeyframeProperty,
        timeMs: Long,
        value: Float,
        easing: EasingFunction = EasingFunction.LINEAR
    ) {
        val track = keyframeTracks.getOrPut(property) { mutableListOf() }
        
        // Remove existing keyframe at this time
        track.removeAll { abs(it.timeMs - timeMs) < 10 }
        
        // Add new keyframe
        track.add(Keyframe(timeMs = timeMs, value = value, easing = easing))
        
        // Sort by time
        track.sortBy { it.timeMs }
    }

    /**
     * Remove a keyframe at a specific time for a property.
     */
    fun removeKeyframe(property: KeyframeProperty, timeMs: Long) {
        keyframeTracks[property]?.removeAll { abs(it.timeMs - timeMs) < 10 }
    }

    /**
     * Evaluate the interpolated value at a given time.
     */
    fun evaluate(property: KeyframeProperty, timeMs: Long): Float {
        val track = keyframeTracks[property] ?: return property.defaultValue
        
        if (track.isEmpty()) return property.defaultValue
        if (track.size == 1) return track.first().value
        
        // Find surrounding keyframes
        val before = track.lastOrNull { it.timeMs <= timeMs } ?: track.first()
        val after = track.firstOrNull { it.timeMs > timeMs } ?: track.last()
        
        if (before.timeMs == after.timeMs) return before.value
        
        // Calculate interpolation progress
        val progress = ((timeMs - before.timeMs).toFloat() / (after.timeMs - before.timeMs)).coerceIn(0f, 1f)
        
        // Apply easing function
        val easedProgress = before.easing.evaluate(progress)
        
        // Interpolate value
        return before.value + (after.value - before.value) * easedProgress
    }

    /**
     * Get all keyframes for a property.
     */
    fun getKeyframes(property: KeyframeProperty): List<Keyframe> {
        return keyframeTracks[property]?.toList() ?: emptyList()
    }

    /**
     * Get all properties that have keyframes.
     */
    fun getActiveProperties(): Set<KeyframeProperty> {
        return keyframeTracks.keys.toSet()
    }

    /**
     * Clear all keyframes for a property.
     */
    fun clearProperty(property: KeyframeProperty) {
        keyframeTracks.remove(property)
    }

    /**
     * Clear all keyframes.
     */
    fun clearAll() {
        keyframeTracks.clear()
    }

    /**
     * Copy keyframes from one property to another.
     */
    fun copyKeyframes(from: KeyframeProperty, to: KeyframeProperty) {
        keyframeTracks[from]?.let { keyframes ->
            keyframeTracks[to] = keyframes.map { it.copy() }.toMutableList()
        }
    }

    /**
     * Reverse keyframes for a property.
     */
    fun reverseKeyframes(property: KeyframeProperty) {
        keyframeTracks[property]?.let { track ->
            if (track.size < 2) return
            
            val minTime = track.first().timeMs
            val maxTime = track.last().timeMs
            
            val reversed = track.map { keyframe ->
                keyframe.copy(timeMs = maxTime - (keyframe.timeMs - minTime))
            }.reversed()
            
            keyframeTracks[property] = reversed.toMutableList()
        }
    }

    /**
     * Apply easing to all keyframes in a property.
     */
    fun applyEasing(property: KeyframeProperty, easing: EasingFunction) {
        keyframeTracks[property]?.forEach { keyframe ->
            keyframe.easing = easing
        }
    }

    /**
     * Generate preset keyframes for common animations.
     */
    fun applyPreset(property: KeyframeProperty, preset: KeyframePreset, durationMs: Long) {
        clearProperty(property)
        
        when (preset) {
            KeyframePreset.FADE_IN -> {
                addKeyframe(property, 0L, 0f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 4, 1f, EasingFunction.EASE_OUT)
            }
            KeyframePreset.FADE_OUT -> {
                addKeyframe(property, durationMs * 3 / 4, 1f, EasingFunction.EASE_IN)
                addKeyframe(property, durationMs, 0f, EasingFunction.EASE_IN)
            }
            KeyframePreset.SLIDE_LEFT -> {
                addKeyframe(property, 0L, 100f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 3, 0f, EasingFunction.EASE_OUT)
            }
            KeyframePreset.SLIDE_RIGHT -> {
                addKeyframe(property, 0L, -100f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 3, 0f, EasingFunction.EASE_OUT)
            }
            KeyframePreset.ZOOM_IN -> {
                addKeyframe(property, 0L, 0.5f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 3, 1f, EasingFunction.EASE_OUT)
            }
            KeyframePreset.ZOOM_OUT -> {
                addKeyframe(property, 0L, 1.5f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 3, 1f, EasingFunction.EASE_OUT)
            }
            KeyframePreset.ROTATE_IN -> {
                addKeyframe(property, 0L, -180f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 3, 0f, EasingFunction.EASE_OUT)
            }
            KeyframePreset.BOUNCE -> {
                addKeyframe(property, 0L, 0f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 4, 1.2f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 2, 0.9f, EasingFunction.EASE_IN_OUT)
                addKeyframe(property, durationMs * 3 / 4, 1.05f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs, 1f, EasingFunction.EASE_IN)
            }
            KeyframePreset.ELASTIC -> {
                addKeyframe(property, 0L, 0f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 3, 1.3f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs / 2, 0.85f, EasingFunction.EASE_IN_OUT)
                addKeyframe(property, durationMs * 2 / 3, 1.1f, EasingFunction.EASE_OUT)
                addKeyframe(property, durationMs, 1f, EasingFunction.EASE_IN)
            }
        }
    }
}

/**
 * Keyframe property types.
 */
enum class KeyframeProperty(
    val displayName: String,
    val defaultValue: Float,
    val color: Color
) {
    POSITION_X("Position X", 0.5f, Color(0xFFFF9500)),  // Orange
    POSITION_Y("Position Y", 0.5f, Color(0xFFFF9500)),
    SCALE("Scale", 1f, Color(0xFF00D4FF)),              // Cyan
    ROTATION("Rotation", 0f, Color(0xFFFFD700)),        // Gold
    OPACITY("Opacity", 1f, Color(0xFFFF2D55)),           // Pink
    FILTER_INTENSITY("Filter Intensity", 0.75f, Color(0xFF8B5CF6)), // Purple
    MASK_FEATHER("Mask Feather", 0f, Color(0xFF34C759)), // Green
    BLUR("Blur", 0f, Color(0xFF5856D6))                 // Indigo
}

/**
 * Keyframe data class.
 */
data class Keyframe(
    val timeMs: Long,
    val value: Float,
    var easing: EasingFunction = EasingFunction.LINEAR,
    val id: String = java.util.UUID.randomUUID().toString()
)

/**
 * Easing functions for keyframe interpolation.
 */
enum class EasingFunction(val displayName: String) {
    LINEAR("Linear") {
        override fun evaluate(t: Float): Float = t
    },
    EASE_IN("Ease In") {
        override fun evaluate(t: Float): Float = t * t
    },
    EASE_OUT("Ease Out") {
        override fun evaluate(t: Float): Float = t * (2 - t)
    },
    EASE_IN_OUT("Ease In-Out") {
        override fun evaluate(t: Float): Float = if (t < 0.5f) 2 * t * t else -1 + (4 - 2 * t) * t
    },
    BEZIER_CUBIC("Bezier Cubic") {
        override fun evaluate(t: Float): Float = t * t * t
    },
    BEZIER_QUARTIC("Bezier Quartic") {
        override fun evaluate(t: Float): Float = t * t * t * t
    },
    BEZIER_QUINTIC("Bezier Quintic") {
        override fun evaluate(t: Float): Float = t * t * t * t * t
    },
    BOUNCE("Bounce") {
        override fun evaluate(t: Float): Float {
            if (t < 1 / 2.75f) return 7.5625f * t * t
            else if (t < 2 / 2.75f) return 7.5625f * (t - 1.5f / 2.75f) * (t - 1.5f / 2.75f) + 0.75f
            else if (t < 2.5 / 2.75f) return 7.5625f * (t - 2.25f / 2.75f) * (t - 2.25f / 2.75f) + 0.9375f
            else return 7.5625f * (t - 2.625f / 2.75f) * (t - 2.625f / 2.75f) + 0.984375f
        }
    },
    ELASTIC("Elastic") {
        override fun evaluate(t: Float): Float {
            if (t == 0f || t == 1f) return t
            return -0.5f * kotlin.math.pow(2f, 10f * (t - 1)) * kotlin.math.sin((t - 1.1f) * 5 * Math.PI.toFloat())
        }
    }

    abstract fun evaluate(t: Float): Float
}

/**
 * Keyframe presets for common animations.
 */
enum class KeyframePreset(val displayName: String) {
    FADE_IN("Fade In"),
    FADE_OUT("Fade Out"),
    SLIDE_LEFT("Slide Left"),
    SLIDE_RIGHT("Slide Right"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    ROTATE_IN("Rotate In"),
    BOUNCE("Bounce"),
    ELASTIC("Elastic")
}
