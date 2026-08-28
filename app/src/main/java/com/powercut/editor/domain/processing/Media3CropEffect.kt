package com.powercut.editor.domain.processing

import androidx.media3.effect.Crop
import com.powercut.editor.data.VideoProject

/** Builds the centered aspect-ratio crop used by both preview and Transformer export. */
object Media3CropEffect {
    fun forProject(project: VideoProject): Crop? {
        val targetAspect = when (project.cropPreset.trim().lowercase()) {
            "1:1", "square" -> 1f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            "4:5" -> 4f / 5f
            "21:9" -> 21f / 9f
            "3:4" -> 3f / 4f
            "2:3" -> 2f / 3f
            else -> return null
        }
        val sourceWidth = project.resolutionWidth
        val sourceHeight = project.resolutionHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return null
        val sourceAspect = sourceWidth.toFloat() / sourceHeight.toFloat()
        return if (sourceAspect > targetAspect) {
            val halfWidth = (targetAspect / sourceAspect).coerceIn(0.01f, 1f)
            Crop(-halfWidth, halfWidth, -1f, 1f)
        } else if (sourceAspect < targetAspect) {
            val halfHeight = (sourceAspect / targetAspect).coerceIn(0.01f, 1f)
            Crop(-1f, 1f, -halfHeight, halfHeight)
        } else null
    }

    /**
     * Builds a manual crop from the user's freeform slider values.
     * cropLeftF / cropTopF are normalized 0..1 distances from the left/top
     * edge to the crop's left/top edge; cropRightF / cropBottomF are 0..1
     * distances to the crop's right/bottom edge. Media3's Crop takes
     * (left, right, top, bottom) in normalized -1..1 coords, so we map
     * the slider 0..1 box into that space (left = 2*L - 1, right = 2*R - 1).
     * A no-op crop (0, 0, 1, 1) maps to the full frame.
     */
    fun forManualCrop(project: VideoProject): Crop? {
        val l = project.cropLeftF.coerceIn(0f, 1f)
        val t = project.cropTopF.coerceIn(0f, 1f)
        val r = project.cropRightF.coerceIn(0f, 1f)
        val b = project.cropBottomF.coerceIn(0f, 1f)
        if (l == 0f && t == 0f && r == 1f && b == 1f) return null
        if (r <= l || b <= t) return null
        val left = 2f * l - 1f
        val right = 2f * r - 1f
        val top = 2f * t - 1f
        val bottom = 2f * b - 1f
        return Crop(left, right, top, bottom)
    }
}
