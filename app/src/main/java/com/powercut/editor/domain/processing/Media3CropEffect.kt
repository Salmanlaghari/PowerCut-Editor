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
}
