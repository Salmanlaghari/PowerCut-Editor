package com.powercut.editor.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val persistence: ProjectPersistence.Repository
) {
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

    suspend fun saveProject(file: java.io.File): Result<Unit> {
        val project = _currentProject.value ?: return Result.failure(
            IllegalStateException("No active project to save")
        )
        return persistence.saveProject(project, file)
    }

    suspend fun saveProject(): Result<String> {
        val project = _currentProject.value ?: return Result.failure(
            IllegalStateException("No active project to save")
        )
        return persistence.saveProject(project)
    }

    suspend fun loadProject(file: java.io.File): Result<VideoProject> {
        return persistence.loadProject(file).onSuccess { project ->
            _currentProject.value = project
        }
    }

    suspend fun loadProject(projectId: String): Result<VideoProject> {
        return persistence.loadProject(projectId).onSuccess { project ->
            _currentProject.value = project
        }
    }

    suspend fun deleteProject(file: java.io.File): Result<Unit> {
        return persistence.deleteProject(file)
    }
}
