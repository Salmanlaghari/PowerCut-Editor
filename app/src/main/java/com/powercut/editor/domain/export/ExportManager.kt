package com.powercut.editor.domain.export

import android.content.Context
import android.os.Environment
import android.util.Log
import com.powercut.editor.core.base.Resource
import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.processing.VideoProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoProcessor: VideoProcessor
) {
    private val tag = "ExportManager"

    private val _exportState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val exportState: StateFlow<Resource<String>> = _exportState.asStateFlow()

    fun resetState() {
        _exportState.value = Resource.Idle
    }

    /**
     * Executes the video export according to the current project configuration.
     * Uses ultra fast "Instant Trim" if no scaling, speed change, transitions, audio, or filters are requested.
     * Otherwise, performs fully accelerated transcoding with requested options (resolution, filters).
     */
    suspend fun exportProject(project: VideoProject) {
        _exportState.value = Resource.Loading
        try {
            val outputDirectory = getAppOutputDir()
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }

            val outputFileName = "PowerCut_${System.currentTimeMillis()}.mp4"
            val outputFile = File(outputDirectory, outputFileName)
            val outputPath = outputFile.absolutePath

            val isInstantTrimPossible = !project.isMuted &&
                    project.selectedFilter == "none" &&
                    project.targetResolution == "1080p" &&
                    project.speedFactor == 1.0f &&
                    project.aspectPreset == "16:9" &&
                    project.transitionType == "none" &&
                    !project.hasBackgroundMusic &&
                    project.autoCaptionsLanguage == "off" &&
                    !project.isSilenceRemoverEnabled

            val success = if (isInstantTrimPossible) {
                Log.d(tag, "Using ultra-fast Instant Trim (Sab se Tez)")
                videoProcessor.instantTrim(
                    inputPath = project.videoPath,
                    outputPath = outputPath,
                    startMs = project.trimStartMs,
                    endMs = project.trimEndMs
                )
            } else {
                Log.d(tag, "Using transcode pipeline for upscale/filters/speed/audio")
                videoProcessor.processAndExport(
                    inputPath = project.videoPath,
                    outputPath = outputPath,
                    startMs = project.trimStartMs,
                    endMs = project.trimEndMs,
                    resolution = project.targetResolution,
                    filter = project.selectedFilter,
                    isMuted = project.isMuted,
                    speedFactor = project.speedFactor,
                    aspectPreset = project.aspectPreset,
                    transitionType = project.transitionType,
                    backgroundMusicPath = project.backgroundMusicPath,
                    backgroundMusicVolume = project.backgroundMusicVolume,
                    videoVolume = project.videoVolume,
                    autoCaptionsLanguage = project.autoCaptionsLanguage,
                    isSilenceRemoverEnabled = project.isSilenceRemoverEnabled
                )
            }

            if (success) {
                Log.d(tag, "Successfully exported video to: $outputPath")
                _exportState.value = Resource.Success(outputPath)
            } else {
                Log.e(tag, "Export failed during video processing")
                _exportState.value = Resource.Error("Video processing failed. Check logs for details.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Export failed with exception", e)
            _exportState.value = Resource.Error(e.message ?: "An unknown error occurred during export", e)
        }
    }

    private fun getAppOutputDir(): File {
        val publicMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        return if (publicMovies != null) {
            File(publicMovies, "PowerCut")
        } else {
            File(context.filesDir, "PowerCut")
        }
    }
}
