package com.powercut.editor.domain.ai

import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter

sealed class AIFilter(
    val id: String,
    val nameResId: Int,
    val description: String
) {
    object None : AIFilter("none", com.powercut.editor.R.string.gpu_filter_none, "Original video colors")
    object Sepia : AIFilter("sepia", com.powercut.editor.R.string.gpu_filter_sepia, "Warm AI Sepia look with golden hues")
    object Grayscale : AIFilter("grayscale", com.powercut.editor.R.string.gpu_filter_grayscale, "Vintage cinematic black and white filter")
    object Invert : AIFilter("invert", com.powercut.editor.R.string.gpu_filter_invert, "Retro future inverted aesthetic neon style")

    fun getGpuImageFilter(): GPUImageFilter {
        return when (this) {
            is None -> GPUImageFilter()
            is Sepia -> GPUImageSepiaToneFilter()
            is Grayscale -> GPUImageGrayscaleFilter()
            is Invert -> GPUImageColorInvertFilter()
        }
    }

    companion object {
        fun fromId(id: String): AIFilter {
            return when (id.lowercase()) {
                "sepia" -> Sepia
                "grayscale" -> Grayscale
                "invert" -> Invert
                else -> None
            }
        }

        val all = listOf(None, Sepia, Grayscale, Invert)
    }
}
