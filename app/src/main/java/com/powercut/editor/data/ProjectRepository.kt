package com.powercut.editor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor() {
    private val _currentProject = MutableStateFlow<VideoProject?>(null)
    val currentProject: StateFlow<VideoProject?> = _currentProject.asStateFlow()

    fun setProject(project: VideoProject?) {
        _currentProject.value = project
    }

    fun updateProject(update: (VideoProject) -> VideoProject) {
        _currentProject.value?.let {
            _currentProject.value = update(it)
        }
    }

    fun clear() {
        _currentProject.value = null
    }
}
