package com.powercut.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.powercut.editor.core.utils.LanguageHelper
import com.powercut.editor.ui.editor.EditorScreen
import com.powercut.editor.ui.editor.EditorViewModel
import com.powercut.editor.ui.export.ExportScreen
import com.powercut.editor.ui.home.HomeScreen
import com.powercut.editor.ui.theme.PowerCutTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val language by viewModel.currentLanguage.collectAsState()
            val layoutDirection = LanguageHelper.getLayoutDirection(language)
            val isDarkTheme by viewModel.isDarkThemeEnabled.collectAsState()

            // Enforce correct RTL/LTR direction dynamically for multi-lingual support
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                PowerCutTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isDarkTheme) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                    ) {
                        val currentScreen by viewModel.currentScreen.collectAsState()
                        val currentDashboardTab by viewModel.currentDashboardTab.collectAsState()
                        val project by viewModel.currentProject.collectAsState()
                        val exportState by viewModel.exportState.collectAsState()

                        val settingsRes by viewModel.selectedResolution.collectAsState()
                        val settingsFps by viewModel.selectedFps.collectAsState()
                        val isHardwareAccEnabled by viewModel.isHardwareAccEnabled.collectAsState()
                        val storagePath by viewModel.selectedStoragePath.collectAsState()

                        when (currentScreen) {
                            "home" -> {
                                HomeScreen(
                                    language = language,
                                    onLanguageToggle = { viewModel.toggleLanguage() },
                                    onVideoSelected = { uri ->
                                        viewModel.selectVideo(this@MainActivity, uri)
                                    },
                                    activeTab = currentDashboardTab,
                                    onTabSelected = { tab ->
                                        viewModel.updateDashboardTab(tab)
                                    },
                                    settingsResolution = settingsRes,
                                    onSettingsResolutionChange = { res ->
                                        viewModel.updateSettingsResolution(res)
                                    },
                                    settingsFps = settingsFps,
                                    onSettingsFpsChange = { fps ->
                                        viewModel.updateSettingsFps(fps)
                                    },
                                    isHardwareAccEnabled = isHardwareAccEnabled,
                                    onToggleHardwareAcc = {
                                        viewModel.toggleHardwareAcc()
                                    },
                                    storagePath = storagePath,
                                    onStoragePathChange = { path ->
                                        viewModel.updateStoragePath(path)
                                    },
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = {
                                        viewModel.toggleTheme()
                                    }
                                )
                            }
                            "editor" -> {
                                project?.let { activeProject ->
                                    EditorScreen(
                                        project = activeProject,
                                        language = language,
                                        onBack = { viewModel.resetToHome() },
                                        onUpdateTrim = { start, end ->
                                            viewModel.updateTrim(start, end)
                                        },
                                        onUpdateResolution = { res ->
                                            viewModel.updateResolution(res)
                                        },
                                        onUpdateFilter = { filterId ->
                                            viewModel.updateFilter(filterId)
                                        },
                                        onToggleMute = { viewModel.toggleMute() },
                                        onExport = { viewModel.navigateToExport() },
                                        onDurationRetrieved = { duration ->
                                            viewModel.setVideoDuration(duration)
                                        },
                                        onUpdateSpeed = { speed ->
                                            viewModel.updateSpeed(speed)
                                        },
                                        onUpdateAspectPreset = { aspect ->
                                            viewModel.updateAspectPreset(aspect)
                                        },
                                        onUpdateTransition = { trans ->
                                            viewModel.updateTransition(trans)
                                        },
                                        onUpdateBackgroundMusic = { path ->
                                            viewModel.updateBackgroundMusic(path)
                                        },
                                        onUpdateMusicVolume = { vol ->
                                            viewModel.updateMusicVolume(vol)
                                        },
                                        onUpdateVideoVolume = { vol ->
                                            viewModel.updateVideoVolume(vol)
                                        },
                                        onUpdateAutoCaptions = { lang ->
                                            viewModel.updateAutoCaptions(lang)
                                        },
                                        onToggleSilenceRemover = {
                                            viewModel.toggleSilenceRemover()
                                        },
                                        onUpdateRotation = {
                                            viewModel.updateRotation()
                                        },
                                        onToggleFlipHorizontal = {
                                            viewModel.toggleFlipHorizontal()
                                        },
                                        onToggleFlipVertical = {
                                            viewModel.toggleFlipVertical()
                                        },
                                        onUpdateCropPreset = { crop ->
                                            viewModel.updateCropPreset(crop)
                                        },
                                        onUpdateSpeedCurve = { curve ->
                                            viewModel.updateSpeedCurve(curve)
                                        },
                                        onUpdateTextOverlay = { text ->
                                            viewModel.updateTextOverlay(text)
                                        },
                                        onUpdateTextAnimation = { anim ->
                                            viewModel.updateTextAnimation(anim)
                                        },
                                        onUpdateStickerType = { sticker ->
                                            viewModel.updateStickerType(sticker)
                                        },
                                        onUpdateTemplate = { tempId ->
                                            viewModel.updateTemplate(tempId)
                                        },
                                        onUpdateVisualizerStyle = { style ->
                                            viewModel.updateVisualizerStyle(style)
                                        },
                                        onToggleBeatSync = {
                                            viewModel.toggleBeatSync()
                                        },
                                        onUpdate3DShapeMask = { mask ->
                                            viewModel.update3DShapeMask(mask)
                                        }
                                    )
                                } ?: viewModel.resetToHome()
                            }
                            "export" -> {
                                ExportScreen(
                                    exportState = exportState,
                                    language = language,
                                    onDone = { viewModel.resetToHome() },
                                    onBackToEditor = { viewModel.navigateToEditor() },
                                    onStartExport = { res, fps, wm, hw ->
                                        viewModel.startExportWithSettings(res, fps, wm, hw)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
