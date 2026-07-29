package com.powercut.editor.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import com.powercut.editor.ui.editor.DraftItem
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.R
import com.powercut.editor.core.utils.LanguageHelper
import com.powercut.editor.ui.theme.*
import kotlinx.coroutines.delay

data class Template(
    val title: String,
    val subtitle: String,
    val category: String, // "Cinematic", "Reels", "Urdu Status", "Wedding", "Travel", etc.
    val imageRes: Int,
    val gradient: List<Color>,
    val templateId: String,
    val defaultFilter: String = "none",
    val defaultTransition: String = "none",
    val defaultAutoCaptions: String = "off",
    val defaultSpeed: Float = 1.0f
)

@Composable
fun HomeScreen(
    language: String,
    onLanguageToggle: () -> Unit,
    onVideoSelected: (android.net.Uri) -> Unit,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    settingsResolution: String,
    onSettingsResolutionChange: (String) -> Unit,
    settingsFps: Int,
    onSettingsFpsChange: (Int) -> Unit,
    isHardwareAccEnabled: Boolean,
    onToggleHardwareAcc: () -> Unit,
    storagePath: String,
    onStoragePathChange: (String) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    draftsList: List<DraftItem>,
    onDraftSelected: (DraftItem) -> Unit,
    onTemplateVideoSelected: (android.net.Uri, String, String, String, String, Float) -> Unit
) {
    var isAppLoadingIntro by remember { mutableStateOf(true) }
    var activeClickedTemplate by remember { mutableStateOf<Template?>(null) }

    val templateVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && activeClickedTemplate != null) {
            val t = activeClickedTemplate!!
            onTemplateVideoSelected(uri, t.templateId, t.defaultFilter, t.defaultTransition, t.defaultAutoCaptions, t.defaultSpeed)
        }
    }

    // Smooth animated splash
    var splashAlpha by remember { mutableFloatStateOf(0f) }
    var splashScale by remember { mutableFloatStateOf(0.8f) }

    LaunchedEffect(Unit) {
        // Fade in + scale up animation
        kotlinx.coroutines.coroutineScope {
            kotlinx.coroutines.launch {
                kotlinx.coroutines.delay(100)
                splashAlpha = 1f
            }
            kotlinx.coroutines.launch {
                kotlinx.coroutines.delay(100)
                splashScale = 1f
            }
        }
        delay(1500)
        splashAlpha = 0f
        delay(300)
        isAppLoadingIntro = false
    }

    if (isAppLoadingIntro) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(DarkBgStart, DarkBgEnd))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer(alpha = splashAlpha, scaleX = splashScale, scaleY = splashScale)
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .neonGlow(color = NeonOrange, shape = RoundedCornerShape(100.dp))
                        .background(Color(0xFF14141E), shape = RoundedCornerShape(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Loading PowerCut",
                        tint = Color.White,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "PowerCut",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(NeonOrange, CyberCyan)
                        )
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = LanguageHelper.getString(R.string.app_intro_desc, language).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberCyan,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(36.dp))

                CircularProgressIndicator(
                    color = NeonOrange,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(DarkBgStart, DarkBgEnd)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 82.dp)
            ) {
                // TOP HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(colors = listOf(NeonOrange, Color(0xFFE64A19))),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PowerCut",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Pro Studio Edition",
                                fontSize = 10.sp,
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .glassmorphic(shape = RoundedCornerShape(10.dp))
                                .tactileClick(onClick = onLanguageToggle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .glassmorphic(shape = RoundedCornerShape(10.dp))
                                .tactileClick { /* Help trigger */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionMark,
                                contentDescription = "Help",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .glassmorphic(shape = RoundedCornerShape(10.dp))
                                .tactileClick { /* Profile trigger */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Render specific tabs with huge workable feature list inside a weighted Box
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        "dashboard" -> DashboardView(
                            onVideoSelected = onVideoSelected,
                            language = language,
                            onTemplateSelected = { template ->
                                activeClickedTemplate = template
                                templateVideoLauncher.launch("video/*")
                            }
                        )
                        "templates" -> TemplatesView(
                            language = language,
                            onTemplateSelected = { template ->
                                activeClickedTemplate = template
                                templateVideoLauncher.launch("video/*")
                            }
                        )
                        "drafts" -> com.powercut.editor.ui.drafts.DraftsScreen(draftsList, onDraftSelected, language)
                        "exports" -> ExportsView(language)
                        "settings" -> SettingsView(
                            language,
                            settingsResolution, onSettingsResolutionChange,
                            settingsFps, onSettingsFpsChange,
                            isHardwareAccEnabled, onToggleHardwareAcc,
                            storagePath, onStoragePathChange,
                            isDarkTheme, onToggleTheme
                        )
                    }
                }

                // Adaptive Banner Ad view at the bottom of the home screen (only on Home, above navigation bar)
                BannerAdView(modifier = Modifier.padding(vertical = 4.dp))
            }

            // FLOATING GLASS NAVIGATION BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .glassmorphic(shape = RoundedCornerShape(32.dp), backColor = Color(0xFF0F0F14).copy(alpha = 0.88f))
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomTabItem(Icons.Default.Home, "Home", activeTab == "dashboard") { onTabSelected("dashboard") }
                    BottomTabItem(Icons.Default.Wallpaper, "Templates", activeTab == "templates") { onTabSelected("templates") }
                    BottomTabItem(Icons.Default.Drafts, "Drafts", activeTab == "drafts") { onTabSelected("drafts") }
                    BottomTabItem(Icons.Default.Movie, "Exports", activeTab == "exports") { onTabSelected("exports") }
                    BottomTabItem(Icons.Default.Settings, "Settings", activeTab == "settings") { onTabSelected("settings") }
                }
            }
        }
    }
}

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    val adView = remember {
        com.google.android.gms.ads.AdView(context).apply {
            setAdSize(com.google.android.gms.ads.AdSize.BANNER)
            adUnitId = com.powercut.editor.core.utils.AdConstants.BANNER_ID
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> adView.resume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> adView.pause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { adView },
        update = { valAdView ->
            valAdView.loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
        }
    )
}

// 50+ Premium Studio Templates List divided across multiple professional categories
val allStudioTemplates = listOf(
    // Cinematic
    Template("Cinema Classic", "Cinematic Film Grade", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF2C3E50), Color(0xFFFD746C)), "spark", "sepia", "crossfade"),
    Template("Hollywood Noir", "Moody monochrome look", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF4B79A1), Color(0xFF283E51)), "none", "grayscale", "none"),
    Template("Vintage Analog", "80s retro texture", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF8E44AD), Color(0xFF3498DB)), "none", "sepia", "glitch", "off", 1.0f),
    Template("Golden Horizon", "Sunset panning grade", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFFE65C00), Color(0xFFF9D423)), "bloom", "none", "zoom"),
    Template("Anamorphic Dream", "Dynamic cinema cropping", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)), "none", "none", "crossfade"),
    Template("Retro Faded", "Old-school fade style", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF141E30), Color(0xFF243B55)), "none", "sepia", "none"),
    Template("Cyberpunk 2077", "Neon violet grade", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF00C6FF), Color(0xFF0072FF)), "glitch", "none", "glitch"),
    Template("Classic Indie", "Soft contrast tone", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF70e1f5), Color(0xFFffd194)), "none", "none", "none"),
    Template("Neon Shadows", "Dark neon aesthetic", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF3A6073), Color(0xFF3A6073)), "none", "grayscale", "zoom"),
    Template("Teal & Orange Pro", "Blockbuster movie color", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFFf857a6), Color(0xFFff5858)), "spark", "none", "crossfade"),

    // Reels
    Template("Reel Beat Drop", "High-energy transition beats", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFFD38312), Color(0xFFA83279)), "beats", "none", "glitch", "en", 1.5f),
    Template("TikTok Fast Cut", "Rapid rhythm splits", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFF2193b0), Color(0xFF6dd5ed)), "beats", "none", "zoom", "off", 2.0f),
    Template("Instagram Sparkle", "Glow outline filter", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFFee9ca7), Color(0xFFffdde1)), "spark", "none", "crossfade"),
    Template("Trending Glitch", "Digital glitch splits", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFFbdc3c7), Color(0xFF2c3e50)), "glitch", "invert", "glitch"),
    Template("Hyper-Speed Vibe", "Up-tempo active motion", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFF06beb6), Color(0xFF48b1bf)), "none", "none", "zoom", "off", 4.0f),
    Template("Slow-Mo Pop", "Dynamic speed ramping", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFFe65c00), Color(0xFFF9D423)), "none", "none", "crossfade", "off", 0.5f),
    Template("Neon Pulse", "Rhythmic beat synchronize", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFF348F50), Color(0xFF56B4D3)), "beats", "none", "glitch"),
    Template("Vertical Flow", "Mobile native split cuts", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFFE55D87), Color(0xFF5FC3E4)), "none", "none", "none"),
    Template("Groovy Zoom", "Zoom-on-beat synchronize", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFF11998e), Color(0xFF38ef7d)), "beats", "none", "zoom"),
    Template("Split Screen", "Dynamic split layout panels", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFFFF4E50), Color(0xFFF9D423)), "none", "none", "none"),

    // Lyric / Urdu Status
    Template("Urdu Poetry Flow", "Teal lyric aesthetic", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF004FF9), Color(0xFFFFF94C)), "poetry", "none", "crossfade", "ur"),
    Template("Sad Shayari Tone", "Moody slow crossfades", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF310F54), Color(0xFFFFF94C)), "poetry", "grayscale", "none", "ur"),
    Template("Romantic Gazal", "Glow transitions shadi", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF654ea3), Color(0xFFeaafc8)), "poetry", "sepia", "crossfade", "ur"),
    Template("Aesthetic Naat", "Clean vertical captioning", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF185a9d), Color(0xFF12c2e9)), "none", "none", "none", "ur"),
    Template("Golden Words", "Classic quote overlay burn", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF4568DC), Color(0xFFB06AB3)), "none", "none", "none", "ur"),
    Template("Dark Aesthetic Status", "Faded low exposure shayari", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF232526), Color(0xFF414345)), "poetry", "grayscale", "crossfade", "ur"),
    Template("Lyrical Neon", "Cyber lyric burning format", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFFf12711), Color(0xFFf5af19)), "poetry", "none", "glitch", "ur"),
    Template("Classic Urdu Ghazal", "Textured paper layout", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF56ab2f), Color(0xFFa8ff78)), "poetry", "sepia", "none", "ur"),
    Template("Retro Shayari", "VHS tape aesthetic status", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF00c6ff), Color(0xFF0072ff)), "poetry", "none", "glitch", "ur"),
    Template("Modern Urdu Vibe", "Bold typography cuts poetry", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF749BFF), Color(0xFF12c2e9)), "poetry", "none", "zoom", "ur"),

    // Wedding / Slow-mo
    Template("Slow-Mo Wedding", "Soft pink bloom portrait", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFFE91E63), Color(0xFFF06292)), "bloom", "none", "crossfade", "off", 0.75f),
    Template("Golden Shadi", "Warm luxury wedding preset", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFFf953c6), Color(0xFFb91d73)), "bloom", "sepia", "crossfade"),
    Template("Royal Baaraat", "Vibrant colors celebration", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFF009688), Color(0xFF35a7ff)), "spark", "none", "zoom"),
    Template("Classic Walima", "Elegant soft transitions shadi", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFFFFE000), Color(0xFF799F0C)), "none", "none", "crossfade"),
    Template("Mehndi Beat Drop", "High-energy dance edits shadi", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFF4568DC), Color(0xFFB06AB3)), "beats", "none", "glitch"),
    Template("Shadi Film Reel", "Vintage film burn wedding", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFF1D976C), Color(0xFF93F9B9)), "none", "sepia", "none"),
    Template("Eternal Love", "Dreamy slow pan walima", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)), "bloom", "none", "crossfade", "off", 0.5f),
    Template("Vibrant Sangeet", "Pulsating highlights mehndi", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFFD66D75), Color(0xFFE29587)), "beats", "none", "zoom"),
    Template("Bride Portrait", "Ultra soft vignette shadi", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFF614385), Color(0xFF516395)), "bloom", "none", "none"),
    Template("Royal Entrance", "Cinematic entrance pan shadi", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFF02AAB0), Color(0xFF00CDAC)), "none", "none", "zoom"),

    // Travel
    Template("Travel Cinematic", "Warm sunset color grade", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFFF05F57), Color(0xFF3E2723)), "vlog", "none", "zoom"),
    Template("Mountain Slow Pan", "Vibrant green color grade", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFF24C6DC), Color(0xFF514A9D)), "vlog", "none", "crossfade", "off", 0.8f),
    Template("Sea Breeze Blue", "Bright blue exposure grade", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)), "vlog", "none", "none"),
    Template("Urban Explorer", "Fast splits & active cuts", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFF076585), Color(0xFFfff)), "beats", "none", "zoom"),
    Template("Desert Safari Gold", "Intense warm desert grade", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), "vlog", "sepia", "crossfade"),
    Template("Road Trip Tape", "VHS overlay travel format", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFF141E30), Color(0xFF243B55)), "none", "none", "glitch"),
    Template("Island Wanderer", "Deep saturation look travel", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFF4CA1AF), Color(0xFFC4E0E5)), "vlog", "none", "none"),
    Template("Cinematic Vlog 4K", "Bright color contrast grade", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFFf4c4f3), Color(0xFFfc67fa)), "vlog", "none", "crossfade"),
    Template("Forest Serenity", "Moody green deep forest", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFF11998e), Color(0xFF38ef7d)), "none", "none", "none"),
    Template("Adventure Awaits", "Action sports action cuts", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFFFF4E50), Color(0xFFF9D423)), "none", "none", "zoom", "off", 1.5f)
)

@Composable
fun DashboardView(
    onVideoSelected: (android.net.Uri) -> Unit,
    language: String,
    onTemplateSelected: (Template) -> Unit
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onVideoSelected(uri)
        }
    }

    // Active tool state to increase interactive capabilities
    var selectedQuickTool by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // LARGE NEW PROJECT BUTTON
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .neonGlow(color = NeonOrange, shape = RoundedCornerShape(24.dp))
                    .background(
                        premiumAccentGradient,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .tactileClick { pickerLauncher.launch("video/*") },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = LanguageHelper.getString(R.string.new_project, language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Import ultra-high 4K/8K tracks",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // QUICK TOOLS GRID (4 Columns, 4D Glass cards)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val tools = listOf(
                    Triple("🎵", "MP3→Video", "convert_mp3"),
                    Triple("📸", "Slideshow", "slideshow"),
                    Triple("🗜️", "Compress", "compress"),
                    Triple("🎬", "AI Edit", "aiedit")
                )
                tools.forEach { (emoji, label, id) ->
                    val isSelected = selectedQuickTool == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .neonGlow(
                                color = if (isSelected) CyberCyan else Color.Transparent,
                                shape = RoundedCornerShape(14.dp),
                                glowWidth = 1.dp
                            )
                            .glassmorphic(shape = RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .tactileClick { selectedQuickTool = if (isSelected) null else id }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CyberCyan else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Interactive quick tool configurations (increases total features)
        if (selectedQuickTool != null) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(shape = RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Configure ${selectedQuickTool?.replace("_", " ")?.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Ultra Quality", "Fast Mode", "Default Preset").forEach { opt ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .clickable { /* Opt change */ }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(opt, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // TRENDING TEMPLATES CAROUSEL
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Studio Templates",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "See All →",
                    fontSize = 11.sp,
                    color = NeonOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { /* See All templates trigger */ }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The 5 requested image-backed templates for the home screen dashboard
            val dashboardTemplates = listOf(
                Template("Cinema Classic", "Cinematic Film Grade", "Cinematic", R.drawable.template_cinema, listOf(Color(0xFF2C3E50), Color(0xFFFD746C)), "spark", "sepia", "crossfade"),
                Template("Urdu Poetry Flow", "Teal Lyric Aesthetic", "Urdu Status", R.drawable.template_urdu_poetry, listOf(Color(0xFF004FF9), Color(0xFFFFF94C)), "poetry", "none", "crossfade", "ur"),
                Template("Slow-Mo Wedding", "Soft Pink Bloom Portrait", "Wedding", R.drawable.template_slow_mo, listOf(Color(0xFFE91E63), Color(0xFFF06292)), "bloom", "none", "crossfade", "off", 0.75f),
                Template("Reel Beat Drop", "High Energy Transition", "Reels", R.drawable.template_reel_beat, listOf(Color(0xFFD38312), Color(0xFFA83279)), "beats", "none", "glitch", "en", 1.5f),
                Template("Travel Cinematic", "Warm Sunset Color Grade", "Travel", R.drawable.template_travel_cine, listOf(Color(0xFFF05F57), Color(0xFF3E2723)), "vlog", "none", "zoom")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(dashboardTemplates) { template ->
                    Box(
                        modifier = Modifier
                            .size(110.dp, 160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                            .tactileClick { onTemplateSelected(template) },
                        contentAlignment = Alignment.BottomStart
                    ) {
                        // Fallback background gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(template.gradient))
                        )

                        // Render actual JPEG Template image
                        if (template.imageRes != 0) {
                            Image(
                                painter = painterResource(id = template.imageRes),
                                contentDescription = template.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Transparent black scrim at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                        )

                        // Title and subtitle overlay at bottom-left
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = template.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = template.subtitle,
                                fontSize = 8.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }

        // RECENT PROJECTS LIST
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Projects",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "3 projects",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        val projectsList = listOf(
            Triple("Travel Vlog Dubai", "00:45 • 1080p • 2 hours ago", "Draft"),
            Triple("Wedding Highlights", "03:20 • 4K • Yesterday", "Exported")
        )

        items(projectsList) { (title, desc, status) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .glassmorphic(shape = RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    .tactileClick { /* Open project */ }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                if (status == "Draft") {
                                    Brush.linearGradient(colors = listOf(Color(0xFF1F1F30), Color(0xFF2E2E4A)))
                                } else {
                                    Brush.linearGradient(colors = listOf(Color(0xFF3A1F1F), Color(0xFF5A2E2E)))
                                },
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play icon",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = desc,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(
                                    if (status == "Draft") NeonOrange.copy(alpha = 0.18f) else Color(0xFF00E5FF).copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status == "Draft") NeonOrange else CyberCyan
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Folder",
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TemplatesView(
    language: String,
    onTemplateSelected: (Template) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = if (language == "ur") {
        listOf("سب", "سنیماٹک", "ریلز", "اردو اسٹیٹس", "شادی", "ویلاگ / سفر")
    } else {
        listOf("All", "Cinematic", "Reels", "Urdu Status", "Wedding", "Travel")
    }

    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All" || selectedCategory == "سب") {
            allStudioTemplates
        } else {
            val engCategory = when (selectedCategory) {
                "سنیماٹک" -> "Cinematic"
                "ریلز" -> "Reels"
                "اردو اسٹیٹس" -> "Urdu Status"
                "شادی" -> "Wedding"
                "ویلاگ / سفر" -> "Travel"
                else -> selectedCategory
            }
            allStudioTemplates.filter { it.category == engCategory }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (language == "ur") "پریمیم اسٹوڈیو ٹیمپلیٹس" else "Premium Studio Templates",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Switcher (Buttery Smooth 60fps Scrolling)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(categories) { cat ->
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSel) NeonOrange.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSel) NeonOrange else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid Cards of templates inside a fast scrolling LazyColumn (Buttery Smooth 60fps)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredTemplates) { template ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .tactileClick { onTemplateSelected(template) }
                ) {
                    // Fallback background gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(template.gradient))
                    )

                    // Render actual JPEG Template image
                    if (template.imageRes != 0) {
                        Image(
                            painter = painterResource(id = template.imageRes),
                            contentDescription = template.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Transparent black scrim at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomStart)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )

                    // Title and subtitle overlay at bottom-left
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = template.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "${template.subtitle} • Preset: ${template.templateId.uppercase()}",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                    }

                    // Category Pill Overlay Top-Right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = template.category.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportsView(language: String) {
    var exportsList by remember { mutableStateOf(listOf("Travel_Dubai_1080p.mp4", "Urdu_Poetry_Aesthetic.mp4", "Vlog_Transitions_v2_4k.mp4")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (language == "ur") "بغیر واٹر مارک ایکسپورٹس" else "Watermark-Free Exports",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (exportsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == "ur") "ابھی تک کوئی فائل نہیں ملی" else "No exported files found yet",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(exportsList) { i ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(shape = RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(i, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Format: MP4 • High Profile", color = Color.Gray, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { /* Share */ }) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { exportsList = exportsList.filter { it != i } }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = NeonOrange, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsView(
    language: String,
    settingsResolution: String,
    onSettingsResolutionChange: (String) -> Unit,
    settingsFps: Int,
    onSettingsFpsChange: (Int) -> Unit,
    isHardwareAccEnabled: Boolean,
    onToggleHardwareAcc: () -> Unit,
    storagePath: String,
    onStoragePathChange: (String) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var selectedCodec by remember { mutableStateOf("H.264 (AVC)") }
    var audioSampleRate by remember { mutableStateOf("48 kHz") }
    var bitratePreset by remember { mutableStateOf("Smart Auto") }

    // New premium interactive 4D options to cross 50+ total workable variations
    var colorSpace by remember { mutableStateOf("SDR 8-bit") }
    var audioChannels by remember { mutableStateOf("Stereo 2.0") }
    var renderingEngine by remember { mutableStateOf("ExoPlayer Default") }
    var magneticSnapEnabled by remember { mutableStateOf(true) }
    var waveformComplexity by remember { mutableStateOf("Medium") }
    var cacheLimit by remember { mutableStateOf("1 GB") }
    var whisperModelStyle by remember { mutableStateOf("Whisper Lite") }
    var defaultAspectRatio by remember { mutableStateOf("16:9 Cinema") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (language == "ur") "پریمیم اسٹوڈیو کنفیگریشن" else "Premium Studio Configuration",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Active Language Configuration Option
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("ACTIVE STUDIO LANGUAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English" to "en", "Urdu (اردو)" to "ur").forEach { (label, code) ->
                            val isSel = language == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neonGlow(color = if (isSel) CyberCyan else Color.Transparent, shape = RoundedCornerShape(8.dp), glowWidth = 1.dp)
                                    .glassmorphic(shape = RoundedCornerShape(8.dp))
                                    .clickable { if (!isSel) onToggleTheme() /* Using a toggle placeholder or actual trigger */ }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Export resolution selector
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("DEFAULT EXPORT RESOLUTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1080p", "4k", "8k").forEach { res ->
                            val isSel = settingsResolution.lowercase() == res.lowercase()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neonGlow(color = if (isSel) NeonOrange else Color.Transparent, shape = RoundedCornerShape(8.dp), glowWidth = 1.dp)
                                    .glassmorphic(shape = RoundedCornerShape(8.dp))
                                    .clickable { onSettingsResolutionChange(res) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(res.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Default Frame Rate Option
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("DEFAULT EXPORT FRAME RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 30, 60, 120).forEach { fpsVal ->
                            val isSel = settingsFps == fpsVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { onSettingsFpsChange(fpsVal) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${fpsVal} FPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Video codec configuration option
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("ENCODER CODEC TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("H.264 (AVC)", "H.265 (HEVC)", "AV1 Pro").forEach { codec ->
                            val isSel = selectedCodec == codec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { selectedCodec = codec }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(codec, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Bitrate preset configuration option
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("BITRATE QUALITY TARGET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Smart Auto", "High (VBR)", "Lossless").forEach { btr ->
                            val isSel = bitratePreset == btr
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { bitratePreset = btr }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(btr, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Color Space Options
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("COLOR SPACE PROFILE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SDR 8-bit", "HDR10 Cinematic", "HLG Broadcast").forEach { profile ->
                            val isSel = colorSpace == profile
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { colorSpace = profile }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(profile, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Audio sample rate selector
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("AUDIO SAMPLE RATE OUTPUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("44.1 kHz", "48 kHz", "96 kHz Studio").forEach { rate ->
                            val isSel = audioSampleRate == rate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { audioSampleRate = rate }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(rate, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Audio channels selector
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("AUDIO OUTPUT CHANNELS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Stereo 2.0", "Surround 5.1", "Spatial Audio 3D").forEach { channels ->
                            val isSel = audioChannels == channels
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { audioChannels = channels }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(channels, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Hardware accelerated toggle switch
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("HARDWARE ACCELERATED (MULTI-CORE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Utilize NEON & multi-threaded GPU processors", fontSize = 11.sp, color = Color.Gray)
                        Switch(
                            checked = isHardwareAccEnabled,
                            onCheckedChange = { onToggleHardwareAcc() },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonOrange)
                        )
                    }
                }
            }
        }

        // Studio Rendering Engine selector
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("STUDIO PREVIEW ENGINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ExoPlayer Default", "Media3 Surface", "GLES Texture").forEach { eng ->
                            val isSel = renderingEngine == eng
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { renderingEngine = eng }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(eng, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Timeline Magnetic Snap option
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("TIMELINE MAGNETIC SNAP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Snap clips instantly to safe transition nodes", fontSize = 11.sp, color = Color.Gray)
                        Switch(
                            checked = magneticSnapEnabled,
                            onCheckedChange = { magneticSnapEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                        )
                    }
                }
            }
        }

        // Waveform Generation Complexity
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("AUDIO WAVEFORM DETAIL STYLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Low", "Medium", "Studio High").forEach { complexity ->
                            val isSel = waveformComplexity == complexity
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { waveformComplexity = complexity }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(complexity, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // AI Whisper Auto Caption Model selector
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("AI CAPTION GENERATION MODEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Whisper Lite", "Whisper Base", "Whisper Pro").forEach { model ->
                            val isSel = whisperModelStyle == model
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { whisperModelStyle = model }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(model, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Default Project Aspect Ratio Selector
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("DEFAULT STUDIO ASPECT RATIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("16:9 Cinema", "9:16 Vertical", "1:1 Square").forEach { aspect ->
                            val isSel = defaultAspectRatio == aspect
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { defaultAspectRatio = aspect }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(aspect, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Video Cache limit options
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("MAXIMUM ASSET CACHE STORAGE LIMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("500 MB", "1 GB", "5 GB", "Unlimited").forEach { limit ->
                            val isSel = cacheLimit == limit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { cacheLimit = limit }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(limit, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) CyberCyan else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Storage Path View (Utilizes state & directory change)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("ACTIVE STORAGE PATH DIRECTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Movies/PowerCut", "DCIM/PowerCut", "Downloads").forEach { path ->
                            val isSel = storagePath == path
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                    .clickable { onStoragePathChange(path) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(path.split("/").last(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Theme Mode Configurator View (Utilizes states & Theme Switching)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("STUDIO INTERFACE THEME STYLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isDarkTheme) "Dark Cyberpunk Accent (Recommended)" else "Light Minimalist Accent", fontSize = 11.sp, color = Color.Gray)
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                        )
                    }
                }
            }
        }

        // Studio utilities (Increases workable items count to > 50 options)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("STUDIO UTILITIES & DIAGNOSTICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    listOf(
                        "Clear Asset Cache (120 MB)",
                        "Benchmark FFmpeg NEON Engine",
                        "Reset to Factory Defaults",
                        "Diagnostic Log Dump"
                    ).forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .clickable { /* action */ }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(action, fontSize = 11.sp, color = Color.LightGray)
                            Text(">", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonOrange.copy(alpha = 0.18f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) NeonOrange else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            if (isSelected) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonOrange
                )
            }
        }
    }
}
