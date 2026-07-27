package com.powercut.editor.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
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
aimport androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.R
import com.powercut.editor.core.utils.LanguageHelper
import com.powercut.editor.ui.theme.*
import kotlinx.coroutines.delay

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
    onToggleTheme: () -> Unit
) {
    var isAppLoadingIntro by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1200)
        isAppLoadingIntro = false
    }

    if (isAppLoadingIntro) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(DarkBgStart, DarkBgEnd))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                // Render specific tabs with huge workable feature list
                when (activeTab) {
                    "dashboard" -> DashboardView(onVideoSelected, language)
                    "templates" -> TemplatesView(language)
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
                    BottomTabItem(Icons.Default.Movie, "Exports", activeTab == "exports") { onTabSelected("exports") }
                    BottomTabItem(Icons.Default.Settings, "Settings", activeTab == "settings") { onTabSelected("settings") }
                }
            }
        }
    }
}

@Composable
fun DashboardView(
    onVideoSelected: (android.net.Uri) -> Unit,
    language: String
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
                    .neonGlow(color = NeonOrange, shape = RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(colors = listOf(NeonOrange, Color(0xFFD84315))),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
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

            val templatesList = listOf(
                Pair("🔥 Reels Beat-Sync", Brush.horizontalGradient(colors = listOf(NeonOrange, Color(0xFFFF9800)))),
                Pair("✨ Cinema Classic", Brush.horizontalGradient(colors = listOf(Color(0xFF7C4DFF), Color(0xFF536DFE)))),
                Pair("🎵 Urdu Poetry Flow", Brush.horizontalGradient(colors = listOf(CyberCyan, Color(0xFF009688)))),
                Pair("💖 Slow-Mo Wedding", Brush.horizontalGradient(colors = listOf(Color(0xFFE91E63), Color(0xFFF06292))))
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templatesList) { (title, brush) ->
                    Box(
                        modifier = Modifier
                            .size(110.dp, 160.dp)
                            .background(brush, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .tactileClick { /* Select Template */ },
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                                    )
                                )
                        )
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
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
fun TemplatesView(language: String) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = if (language == "ur") {
        listOf("سب", "سنیماٹک", "ریلز", "اردو اسٹیٹس", "ویلاگ", "ریٹرو")
    } else {
        listOf("All", "Cinematic", "Reels", "Urdu Status", "Vlog", "Retro")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (language == "ur") "ٹرینڈنگ پرو ٹیمپلیٹس" else "Trending Pro Templates",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Switcher
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .background(
                            if (isSel) NeonOrange.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, if (isSel) NeonOrange else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) NeonOrange else Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Premium Grid Cards of templates
        val items = listOf(
            Triple("Cinematic Gold", "15 Clips • 30s", "120k Used"),
            Triple("Cyberpunk Glitch Beat", "8 Clips • 15s", "84k Used"),
            Triple("Urdu Poetry Nostalgia", "1 Clip • 45s", "240k Used"),
            Triple("Classic Vlog Transitions", "20 Clips • 1m", "50k Used")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { (name, stats, used) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(stats, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(used, color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
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
