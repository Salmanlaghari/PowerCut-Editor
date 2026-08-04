package com.powercut.editor

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
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
        window.setBackgroundDrawableResource(android.R.color.black)

        // Load AdMob ads
        loadAppOpenAd()
        loadInterstitialAd()
        loadRewardedAd()

        // Request permissions AFTER content is set (non-blocking)
        setContent {
            val language by viewModel.currentLanguage.collectAsState()
            val layoutDirection = LanguageHelper.getLayoutDirection(language)
            val isDarkTheme by viewModel.isDarkThemeEnabled.collectAsState()

            // Request permissions after first composition
            LaunchedEffect(Unit) {
                requestStoragePermissions()
            }

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
                        val exportProgress by viewModel.exportProgress.collectAsState()

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

                        // Import loading state
                        val isImporting by viewModel.isImporting.collectAsState()
                        val importProgress by viewModel.importProgress.collectAsState()
                        val importError by viewModel.importError.collectAsState()

                        // Show premium loading dialog during video import
                        if (isImporting) {
                            androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF0B0F1A), Color(0xFF161B26))
                                            ),
                                            RoundedCornerShape(24.dp)
                                        )
                                        .border(
                                            1.dp,
                                            Brush.horizontalGradient(
                                                listOf(
                                                    com.powercut.editor.ui.theme.NeonOrange.copy(0.5f),
                                                    com.powercut.editor.ui.theme.CyberCyan.copy(0.5f)
                                                )
                                            ),
                                            RoundedCornerShape(24.dp)
                                        )
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { importProgress / 100f },
                                            color = com.powercut.editor.ui.theme.NeonOrange,
                                            strokeWidth = 4.dp,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = if (language == "ur") "ویڈیو لوڈ ہو رہی ہے..." else "Importing video…",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (language == "ur") "براہ کرم انتظار کریں — بڑی ویڈیوز پر تھوڑا وقت لگتا ہے"
                                            else "Please wait — large videos take a moment",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "$importProgress%",
                                            color = com.powercut.editor.ui.theme.CyberCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Show error toast if import failed
                        LaunchedEffect(importError) {
                            importError?.let { error ->
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
                                viewModel.clearImportError()
                            }
                        }

                        when (currentScreen) {
                            "home" -> {
                                HomeScreen(
                                    language = language,
                                    onLanguageToggle = { viewModel.toggleLanguage() },
                                    onVideoSelected = { uri ->
                                        importVideoWithAd(uri)
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
                                        importVideoWithAd(uri, tId, filt, trans, caps, speed)
                                    },
                                    // v4.4.0 Premium FFmpeg Media Converter: MP3 -> MP4
                                    onConvertMp3ToMp4 = { audioUri ->
                                        viewModel.convertMp3ToMp4(audioUri)
                                    },
                                    // v4.5.0 Premium Quick Tools (all workable)
                                    onCompressVideo = { videoUri ->
                                        viewModel.compressVideo(videoUri)
                                    },
                                    onCreateSlideshow = { imageUris ->
                                        viewModel.createSlideshow(imageUris)
                                    },
                                    onApplyAiEdit = { videoUri ->
                                        viewModel.applyAiEdit(videoUri)
                                    },
                                    // v4.6.0: pass quick-tool export feedback to HomeScreen so the user
                                    // actually sees progress / success / error when MP3->MP4, Slideshow,
                                    // Compress or AI Edit runs.
                                    quickToolState = exportState,
                                    quickToolProgress = exportProgress
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
                                        },
                                        onUpdateImageOverlay = { path ->
                                            viewModel.updateImageOverlay(path)
                                        },
                                        onUpdateImageOverlayOpacity = { opacity ->
                                            viewModel.updateImageOverlayOpacity(opacity)
                                        },
                                        onUpdateImageOverlayScale = { scale ->
                                            viewModel.updateImageOverlayScale(scale)
                                        },
                                        onUpdateSelectedEffect = { effect ->
                                            viewModel.updateSelectedEffect(effect)
                                        },
                                        onAddLayer = { layerId ->
                                            viewModel.addLayer(layerId)
                                        },
                                        onRemoveLayer = { layerId ->
                                            viewModel.removeLayer(layerId)
                                        },
                                        // Green Screen
                                        onToggleGreenScreen = { viewModel.toggleGreenScreen() },
                                        onUpdateGreenScreenColor = { viewModel.updateGreenScreenColor(it) },
                                        onUpdateGreenScreenThreshold = { viewModel.updateGreenScreenThreshold(it) },
                                        onSelectAutoBackground = { viewModel.selectAutoBackground(it) },
                                        onPickCustomBackground = { /* picker handled in screen */ },
                                        // Eraser
                                        onUpdateEraserMode = { viewModel.updateEraserMode(it) },
                                        onUpdateEraserBrushSize = { viewModel.updateEraserBrushSize(it) },
                                        onUpdateEraserTolerance = { viewModel.updateEraserTolerance(it) },
                                        onToggleEraserSoftEdge = { viewModel.toggleEraserSoftEdge() },
                                        onUndoEraser = { /* undo handled in screen */ },
                                        onResetEraser = { viewModel.resetEraser() },
                                        // Image Editor
                                        onUpdateImageEditorBrightness = { viewModel.updateImageEditorBrightness(it) },
                                        onUpdateImageEditorContrast = { viewModel.updateImageEditorContrast(it) },
                                        onUpdateImageEditorSaturation = { viewModel.updateImageEditorSaturation(it) },
                                        onUpdateImageEditorBlur = { viewModel.updateImageEditorBlur(it) },
                                        onUpdateImageEditorSharpen = { viewModel.updateImageEditorSharpen(it) },
                                        onUpdateImageEditorTemperature = { viewModel.updateImageEditorTemperature(it) },
                                        onUpdateImageEditorVignette = { viewModel.updateImageEditorVignette(it) },
                                        onUpdateImageEditorGrain = { viewModel.updateImageEditorGrain(it) },
                                        onUpdateImageEditorFade = { viewModel.updateImageEditorFade(it) },
                                        onUpdateImageEditorHighlights = { viewModel.updateImageEditorHighlights(it) },
                                        onUpdateImageEditorShadows = { viewModel.updateImageEditorShadows(it) },
                                        onUpdateImageEditorExposure = { viewModel.updateImageEditorExposure(it) },
                                        onResetImageEditor = { viewModel.resetImageEditor() },
                                        // Orientation
                                        onUpdateOrientationMode = { viewModel.updateOrientationMode(it) },
                                        onToggleVerticalSafeZone = { viewModel.toggleVerticalSafeZone() },
                                        onToggleHorizontalLetterbox = { viewModel.toggleHorizontalLetterbox() },
                                        onToggleAutoReframe = { viewModel.toggleAutoReframe() },
                                        // NEW v4.0 CapCut-sync Pro callbacks
                                        onUpdateBlendMode = { viewModel.updateBlendMode(it) },
                                        onToggleReverse = { viewModel.toggleReverse() },
                                        onUpdateFreezeFrame = { viewModel.updateFreezeFrame(it) },
                                        onUpdateColorLift = { viewModel.updateColorLift(it) },
                                        onUpdateColorGamma = { viewModel.updateColorGamma(it) },
                                        onUpdateColorGain = { viewModel.updateColorGain(it) },
                                        onUpdateAudioEffect = { viewModel.updateAudioEffect(it) },
                                        onUpdateVoiceChangerPitch = { viewModel.updateVoiceChangerPitch(it) },
                                        onToggleAudioDucking = { viewModel.toggleAudioDucking() },
                                        onUpdateBorderStyle = { viewModel.updateBorderStyle(it) },
                                        onUpdateVignetteStyle = { viewModel.updateVignetteStyle(it) },
                                        onUpdatePremiumLook = { viewModel.updatePremiumLook(it) }
                                    )
                                } ?: viewModel.resetToHome()
                            }
                            "export" -> {
                                ExportScreen(
                                    exportState = exportState,
                                    language = language,
                                    onDone = {
                                        isWatermarkRemoved = false
                                        viewModel.resetToHome()
                                    },
                                    onBackToEditor = { viewModel.navigateToEditor() },
                                    onImportNewVideo = { uri ->
                                        importVideoWithAd(uri)
                                    },
                                    isWatermarkRemoved = isWatermarkRemoved,
                                    onRemoveWatermarkRequested = {
                                        showRewardedAd(onEarnedReward = {
                                            isWatermarkRemoved = true
                                            Toast.makeText(this@MainActivity, "Watermark successfully removed!", Toast.LENGTH_SHORT).show()
                                        })
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

    private fun requestStoragePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Request granular media permissions + POST_NOTIFICATIONS
            // (POST_NOTIFICATIONS is required for the export foreground-service
            //  notification that keeps long exports alive in the background.)
            val permissions = mutableListOf<String>()
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            if (permissions.isNotEmpty()) {
                requestPermissions(permissions.toTypedArray(), 1001)
            }
        } else {
            // Android 12 and below: Request legacy storage permissions
            val permissions = mutableListOf<String>()
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            if (permissions.isNotEmpty()) {
                requestPermissions(permissions.toTypedArray(), 1002)
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

    // ─────────────────────────────────────────────────────────────────────
    //  v5.0.0 AD-BASED WATERMARK AT IMPORT TIME
    //  User request: "import ke time agar user ads per click kare to remove
    //  watermark video import ho, agar ads per click na kare to watermark ke
    //  sath video import aaye."
    //
    //  Flow: When the user picks a video to import, we show a rewarded ad
    //  FIRST. If they watch it (or the ad fails to load — graceful fallback),
    //  isWatermarkRemoved = true → the export will have NO watermark. The ad
    //  is always offered; the export pipeline reads isWatermarkRemoved when
    //  the user eventually taps EXPORT.
    // ─────────────────────────────────────────────────────────────────────
    private fun importVideoWithAd(
        uri: android.net.Uri,
        templateId: String = "none",
        filter: String = "none",
        transition: String = "none",
        captions: String = "off",
        speed: Float = 1.0f
    ) {
        // v5.0.0 AD-BASED WATERMARK AT IMPORT TIME.
        // Show a rewarded ad when the user picks a video to import.
        // - If watched to completion (reward earned): mark watermark removed so
        //   the eventual export has NO watermark, then import.
        // - If skipped/closed early (no reward): watermark stays, then import.
        // - If ad fails to load (fallback): grant reward (no watermark), import.
        // The import ALWAYS proceeds; only the watermark decision changes.
        showRewardedAd(
            onEarnedReward = {
                // User watched the ad (or fallback) — remove watermark.
                isWatermarkRemoved = true
                Toast.makeText(
                    this,
                    "Ad rewarded! Watermark removed for this video.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onAdDismissed = {
                // Ad closed — import the video regardless of reward.
                // isWatermarkRemoved was set to true above only if reward was earned.
                viewModel.selectVideo(this, uri, templateId, filter, transition, captions, speed)
            }
        )
    }

    private fun showRewardedAd(
        onEarnedReward: () -> Unit,
        onAdDismissed: (() -> Unit)? = null
    ) {
        rewardedAd?.let { ad ->
            // v5.0.0: Track whether the user actually earned the reward so the
            // dismiss callback can branch on it. OnUserEarnedRewardListener fires
            // only when the ad is watched to completion; FullScreenContentCallback
            // fires on ad dismissed/failed regardless.
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Ad closed (watched or skipped). OnUserEarnedRewardListener
                    // already called onEarnedReward if the user watched to completion.
                    when {
                        // Reward earned AND a dismiss callback exists → call dismiss
                        // (e.g. importVideoWithAd: watermark set above, now import).
                        rewardEarned && onAdDismissed != null -> onAdDismissed!!.invoke()
                        // Reward earned, no dismiss callback → onEarnedReward already
                        // fired, nothing more to do (export watermark removal case).
                        rewardEarned -> { /* already handled */ }
                        // No reward (user skipped) AND dismiss callback exists →
                        // proceed WITHOUT reward (import with watermark).
                        onAdDismissed != null -> onAdDismissed!!.invoke()
                        // No reward, no dismiss callback → fall back to onEarnedReward
                        // so the flow still completes (backward compatible).
                        else -> onEarnedReward()
                    }
                    rewardedAd = null
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Ad failed to show — treat as fallback (grant reward so flow
                    // continues), then fire dismiss so callers still proceed.
                    onEarnedReward()
                    onAdDismissed?.invoke()
                    rewardedAd = null
                    loadRewardedAd()
                }
            }
            ad.show(this) {
                // OnUserEarnedRewardListener — user watched the ad to completion.
                rewardEarned = true
                onEarnedReward()
            }
        } ?: run {
            // Fallback: ad not loaded. Grant reward so user flows remain smooth,
            // then fire the dismiss callback so callers like importVideoWithAd
            // still proceed with the import.
            Toast.makeText(this, "Ad loading... Watermark unlocked instantly!", Toast.LENGTH_SHORT).show()
            onEarnedReward()
            onAdDismissed?.invoke()
            loadRewardedAd()
        }
    }
}
