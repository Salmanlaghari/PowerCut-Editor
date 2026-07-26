package com.powercut.editor.ui

import com.powercut.editor.data.ProjectRepository
import com.powercut.editor.data.VideoProject
import com.powercut.editor.domain.export.ExportManager
import com.powercut.editor.ui.editor.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var projectRepository: ProjectRepository
    private lateinit var exportManager: ExportManager
    private lateinit var viewModel: EditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        projectRepository = ProjectRepository()
        exportManager = mock(ExportManager::class.java)
        viewModel = EditorViewModel(projectRepository, exportManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialScreen_isHome() {
        assertEquals("home", viewModel.currentScreen.value)
    }

    @Test
    fun toggleLanguage_switchesEnAndUr() {
        assertEquals("en", viewModel.currentLanguage.value)
        viewModel.toggleLanguage()
        assertEquals("ur", viewModel.currentLanguage.value)
        viewModel.toggleLanguage()
        assertEquals("en", viewModel.currentLanguage.value)
    }

    @Test
    fun setProject_updatesActiveProject() {
        projectRepository.setProject(VideoProject(videoPath = "/test.mp4", durationMs = 1000L))
        assertNotNull(viewModel.currentProject.value)
        assertEquals("/test.mp4", viewModel.currentProject.value?.videoPath)
    }

    @Test
    fun updateResolution_updatesProjectTargetResolution() {
        projectRepository.setProject(VideoProject(videoPath = "/test.mp4", durationMs = 10000L))
        viewModel.updateResolution("4K")
        assertEquals("4K", viewModel.currentProject.value?.targetResolution)
    }

    @Test
    fun updateFilter_updatesProjectFilter() {
        projectRepository.setProject(VideoProject(videoPath = "/test.mp4", durationMs = 10000L))
        viewModel.updateFilter("sepia")
        assertEquals("sepia", viewModel.currentProject.value?.selectedFilter)
    }

    @Test
    fun toggleMute_togglesMutedState() {
        projectRepository.setProject(VideoProject(videoPath = "/test.mp4", durationMs = 10000L, isMuted = false))
        viewModel.toggleMute()
        assertEquals(true, viewModel.currentProject.value?.isMuted)
        viewModel.toggleMute()
        assertEquals(false, viewModel.currentProject.value?.isMuted)
    }

    @Test
    fun resetToHome_clearsProjectAndResetsScreen() {
        projectRepository.setProject(VideoProject(videoPath = "/test.mp4", durationMs = 10000L))
        viewModel.navigateToEditor()
        assertEquals("editor", viewModel.currentScreen.value)

        viewModel.resetToHome()
        assertEquals("home", viewModel.currentScreen.value)
        assertNull(viewModel.currentProject.value)
    }
}
