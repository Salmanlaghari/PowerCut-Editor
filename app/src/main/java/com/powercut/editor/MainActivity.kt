package com.powercut.editor

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.powercut.editor.core.utils.LanguageHelper
import com.powercut.editor.core.utils.AdConstants
import com.powercut.editor.ui.editor.NextGenEditorScreen
import com.powercut.editor.ui.editor.EditorViewModel
import com.powercut.editor.ui.export.ExportScreen
import com.powercut.editor.ui.home.HomeScreen
import com.powercut.editor.ui.theme.PowerCutTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()
    private var appOpenAd: AppOpenAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastShownAt: Long = 0L

    private var isWatermarkRemoved by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)

        // Load AdMob ads
        loadAppOpenAd()
        loadInterstitialAd()
        loadRewardedAd()

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
                        val draftsList by viewModel.drafts.collectAsState()

                        LaunchedEffect(currentDashboardTab) {
                            if (currentDashboardTab == "drafts") {
                                viewModel.loadDrafts(this@MainActivity)
                            }
                        }

                        LaunchedEffect(exportState) {
                            if (exportState is com.powercut.editor.core.base.Resource.Success) {
                                showInterstitialAd()
                            }
                        }

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
                                    },
                                    draftsList = draftsList,
                                    onDraftSelected = { draft ->
                                        viewModel.resumeDraft(draft)
                                    },
                                    onTemplateVideoSelected = { uri, tId, filt, trans, caps, speed ->
                                        viewModel.selectVideo(this@MainActivity, uri, tId, filt, trans, caps, speed)
                                    }
                                )
                            }
                            "editor" -> {
                                project?.let { activeProject ->
                                    NextGenEditorScreen(
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
                                        },
                                        onAddClip = { uri ->
                                            viewModel.addClip(this@MainActivity, uri)
                                        },
                                        onSaveDraft = {
                                            viewModel.saveDraft(this@MainActivity)
                                        }
                                    )
                                } ?: viewModel.resetToHome()
                            }
                            "export" -> {
                                ExportScreen(
                                    exportState = exportState,
                                    language = language,
                                    onDone = {
                                        isWatermarkRemoved = false // Reset watermark state
                                        viewModel.resetToHome()
                                    },
                                    onBackToEditor = { viewModel.navigateToEditor() },
                                    isWatermarkRemoved = isWatermarkRemoved,
                                    onRemoveWatermarkRequested = {
                                        showRewardedAd {
                                            isWatermarkRemoved = true
                                            Toast.makeText(this@MainActivity, "Watermark successfully removed!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onStartExport = { res, fps, wm, hw ->
                                        viewModel.startExportWithSettings(res, fps, isWatermarkRemoved || wm, hw)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadAppOpenAd() {
        AppOpenAd.load(
            this,
            AdConstants.APP_OPEN_ID,
            AdRequest.Builder().build(),
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    showAppOpenAd()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                }
            }
        )
    }

    private fun showAppOpenAd() {
        appOpenAd?.show(this)
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            AdConstants.INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialAd() {
        val now = System.currentTimeMillis()
        if (now - lastShownAt >= 30000L) { // 30 seconds minimum gap for premium spacing
            interstitialAd?.let { ad ->
                ad.show(this)
                lastShownAt = now
                interstitialAd = null
                loadInterstitialAd() // pre-load next one
            }
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this,
            AdConstants.REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    private fun showRewardedAd(onEarnedReward: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.show(this) {
                onEarnedReward()
            }
            rewardedAd = null
            loadRewardedAd() // reload the ad unit
        } ?: run {
            // Fallback reward if ad failed to load so user flows remain smooth
            Toast.makeText(this, "Ad loading... Watermark unlocked instantly!", Toast.LENGTH_SHORT).show()
            onEarnedReward()
            loadRewardedAd()
        }
    }
}
