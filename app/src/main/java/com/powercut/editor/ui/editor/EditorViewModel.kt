package com.powercut.editor.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powercut.editor.core.base.Resource
import com.powercut.editor.core.utils.UriHelper
import com.powercut.editor.data.ProjectRepository
import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.export.ExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _currentScreen = MutableStateFlow("home") // "home", "editor", "export"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en") // "en", "ur"
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    val currentProject: StateFlow<VideoProject?> = projectRepository.currentProject
    val exportState: StateFlow<Resource<String>> = exportManager.exportState

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == "en") "ur" else "en"
    }

    fun selectVideo(context: Context, uri: Uri) {
        viewModelScope.launch {
            val path = UriHelper.getPathFromUri(context, uri)
            if (path != null) {
                // Determine video duration (mocking or getting via metadata retriever)
                val file = File(path)
                val durationMs = if (file.exists()) {
                    // Quick default or estimation. In production we'd retrieve using MediaMetadataRetriever,
                    // but since a user can select different files we will set default 10000ms and allow setting it on load.
                    15000L
                } else {
                    10000L
                }

                val project = VideoProject(
                    videoPath = path,
                    durationMs = durationMs,
                    trimStartMs = 0L,
                    trimEndMs = durationMs
                )
                projectRepository.setProject(project)
                _currentScreen.value = "editor"
            }
        }
    }

    fun setVideoDuration(durationMs: Long) {
        projectRepository.updateProject { project ->
            project.copy(
                durationMs = durationMs,
                trimEndMs = if (project.trimEndMs == 15000L || project.trimEndMs == 0L) durationMs else project.trimEndMs
            )
        }
    }

    fun updateTrim(startMs: Long, endMs: Long) {
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
        projectRepository.updateProject { project ->
            project.copy(selectedFilter = filterId)
        }
    }

    fun toggleMute() {
        projectRepository.updateProject { project ->
            project.copy(isMuted = !project.isMuted)
        }
    }

    fun updateSpeed(speedFactor: Float) {
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
            project.copy(backgroundMusicPath = path)
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

    fun startExport() {
        val project = currentProject.value ?: return
        _currentScreen.value = "export"
        viewModelScope.launch {
            exportManager.exportProject(project)
        }
    }

    fun resetToHome() {
        projectRepository.clear()
        exportManager.resetState()
        _currentScreen.value = "home"
    }

    fun navigateToEditor() {
        _currentScreen.value = "editor"
    }
}
