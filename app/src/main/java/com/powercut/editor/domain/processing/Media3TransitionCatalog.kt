package com.powercut.editor.domain.processing

import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Effect
import androidx.media3.transformer.Effects
import com.powercut.editor.data.KeyframeTrack

@UnstableApi
object Media3TransitionCatalog {
    const val NONE = "none"
    const val DEFAULT_DURATION_MS = 500L
    const val MIN_DURATION_MS = 100L

    data class TransitionSpec(
        val id: String,
        val displayName: String,
        val description: String
    )

    val TRANSITIONS = listOf(
        TransitionSpec("cross_dissolve", "Cross Dissolve", "Smooth fade from Clip A to Clip B"),
        TransitionSpec("wipe_left", "Wipe Left", "Clip B slides in from left to right"),
        TransitionSpec("wipe_right", "Wipe Right", "Clip B slides in from right to left"),
        TransitionSpec("wipe_up", "Wipe Up", "Clip B slides in from bottom to top"),
        TransitionSpec("wipe_down", "Wipe Down", "Clip B slides in from top to bottom"),
        TransitionSpec("slide_left", "Slide Left", "Clip A exits left, Clip B enters right"),
        TransitionSpec("slide_right", "Slide Right", "Clip A exits right, Clip B enters left"),
        TransitionSpec("zoom_transition", "Zoom Transition", "Clip A zooms out, Clip B zooms in"),
        TransitionSpec("rotate_transition", "Rotate Transition", "180° rotation with zoom"),
        TransitionSpec("heart_mask", "Heart Reveal", "Heart shape grows to reveal Clip B"),
        TransitionSpec("circle_reveal", "Circle Reveal", "Circle expands from center"),
        TransitionSpec("diamond_reveal", "Diamond Reveal", "Diamond shape expands from center")
    )

    fun buildTransitionEffects(
        transitionType: String,
        durationMs: Long,
        keyframeTracks: List<KeyframeTrack> = emptyList()
    ): Effects {
        if (transitionType == NONE || durationMs < MIN_DURATION_MS) {
            return Effects(emptyList(), emptyList())
        }

        val effects = mutableListOf<Effect>()

        when (transitionType) {
            "cross_dissolve" -> {
                // Dissolve is handled natively by Media3 xfade
            }
            "wipe_left", "wipe_right", "wipe_up", "wipe_down", "slide_left", "slide_right" -> {
                // Slide transitions use position animation via keyframes
                // Keyframe data drives the movement
            }
            "zoom_transition" -> {
                // Zoom transitions use scale animation via keyframes
            }
            "rotate_transition" -> {
                // Rotation transitions use rotation animation via keyframes
            }
            "heart_mask", "circle_reveal", "diamond_reveal" -> {
                // Mask reveals use crop/wipe with animation
            }
        }

        return Effects(emptyList(), effects)
    }
}