package com.powercut.editor.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.R
import com.powercut.editor.core.utils.LanguageHelper
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.tactileClick
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    language: String,
    onLanguageToggle: () -> Unit,
    onVideoSelected: (android.net.Uri) -> Unit,

    // Bottom tab Navigation and settings states
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

    // App Intro timer
    LaunchedEffect(Unit) {
        delay(1500)
        isAppLoadingIntro = false
    }

    if (isAppLoadingIntro) {
        // App Intro / Loading screen with beautiful neon animations and branding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070B)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .neonGlow(color = Color(0xFFFF0055), shape = RoundedCornerShape(100.dp))
                        .background(Color(0xFF14141E), shape = RoundedCornerShape(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Loading PowerCut",
                        tint = Color(0xFFFF0055),
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PowerCut",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF0055), Color(0xFF00E5FF))
                        )
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = LanguageHelper.getString(R.string.app_intro_desc, language).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                CircularProgressIndicator(
                    color = Color(0xFFFF0055),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    } else {
        // MAIN WORKSPACE INTERFACE WITH BOTTOM BAR NAVIGATION
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF0A0A0F) else Color(0xFFF5F5FA))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp) // Spacing for bottom floating navigation bar
            ) {
                // TOP LOGO BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PowerCut",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )

                    // Compact dynamic language toggler
                    Box(
                        modifier = Modifier
                            .glassmorphic(shape = RoundedCornerShape(24.dp), borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f), backColor = if (isDarkTheme) Color(0xFF1E1E1E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f))
                            .tactileClick(onClick = onLanguageToggle)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = LanguageHelper.getString(R.string.language_toggle, language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF008B8B)
                        )
                    }
                }

                // NAVIGATION TAB DISPATCHER
                when (activeTab) {
                    "dashboard" -> DashboardView(onVideoSelected, isDarkTheme, language)
                    "templates" -> TemplatesView(isDarkTheme, language)
                    "exports" -> ExportsView(isDarkTheme, language)
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

            // FLOATING GLASSMORPHIC BOTTOM NAVIGATION BAR
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
                        .glassmorphic(shape = RoundedCornerShape(32.dp), backColor = if (isDarkTheme) Color(0xFF12121A).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(Icons.Default.Home, "DASHBOARD", activeTab == "dashboard", isDarkTheme) { onTabSelected("dashboard") }
                    BottomNavItem(Icons.Default.Wallpaper, "TEMPLATES", activeTab == "templates", isDarkTheme) { onTabSelected("templates") }
                    BottomNavItem(Icons.Default.Movie, "EXPORTS", activeTab == "exports", isDarkTheme) { onTabSelected("exports") }
                    BottomNavItem(Icons.Default.Settings, "SETTINGS", activeTab == "settings", isDarkTheme) { onTabSelected("settings") }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REUSABLE DASHBOARD COMPOSABLES
// -------------------------------------------------------------

@Composable
fun DashboardView(
    onVideoSelected: (android.net.Uri) -> Unit,
    isDarkTheme: Boolean,
    language: String
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onVideoSelected(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Pulse 3D New Project trigger card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .neonGlow(color = Color(0xFFFF0055), shape = RoundedCornerShape(20.dp))
                    .background(Color(0xFF151522), shape = RoundedCornerShape(20.dp))
                    .tactileClick { pickerLauncher.launch("video/*") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFF0055), RoundedCornerShape(50.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = LanguageHelper.getString(R.string.new_project, language).uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Horizontal Templates Carousel
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = LanguageHelper.getString(R.string.templates, language).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF008B8B),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val samples = listOf("TikTok Spark", "Poetry Status", "Cinema Epic", "Vlog beats", "Wedding Glow")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(samples) { s ->
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .glassmorphic(shape = RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Icon(imageVector = Icons.Default.Wallpaper, contentDescription = s, tint = Color(0xFFFF0055), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(s, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.White else Color.Black)
                        }
                    }
                }
            }
        }

        // Recent Projects list
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = LanguageHelper.getString(R.string.recent_projects, language).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF008B8B),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        val projects = listOf("PowerCut_Project_01.mp4", "Urdu_Poetry_Status.mp4", "Vlog_Teaser_HDR.mp4")
        items(projects) { p ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFFF0055).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.OndemandVideo, contentDescription = "Video", tint = Color(0xFFFF0055))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkTheme) Color.White else Color.Black)
                        Text("Duration: 15s • Dec 2026", fontSize = 11.sp, color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f))
                    }
                    Icon(imageVector = Icons.Default.Folder, contentDescription = "Folder", tint = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun TemplatesView(isDarkTheme: Boolean, language: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("50+ READY-TO-USE TRENDING TEMPLATES", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (isDarkTheme) Color.White else Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
        }

        val designTemplates = listOf(
            "TikTok Spark Glow" to "Trending social edits, transition presets",
            "Urdu Poetry Status" to "Sad and romantic poetical status layouts",
            "Vlog Cinematic Beats" to "Travel stories with dynamic beat syncing",
            "Wedding Lights" to "Luxury golden slow motion frames",
            "Glitch Cyberpunk" to "Neon hacking futuristic design",
            "Instagram Reels Bloom" to "Aesthetic floral and soft filters",
            "Retro VHS Tape" to "Vintage nostalgic analog effect"
        )

        items(designTemplates) { (name, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFF00E5FF).copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Wallpaper, contentDescription = "Temp", tint = Color(0xFF00E5FF))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (isDarkTheme) Color.White else Color.Black)
                        Text(desc, fontSize = 11.sp, color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun ExportsView(isDarkTheme: Boolean, language: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("MY RECENT HIGH QUALITY EXPORTS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (isDarkTheme) Color.White else Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
        }

        val exports = listOf("PowerCut_Export_1080p_30fps.mp4", "Urdu_Poetry_Status_4k_60fps.mp4")
        items(exports) { exp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).background(Color(0xFFFF0055).copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Movie, contentDescription = "Exp", tint = Color(0xFFFF0055))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(exp, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isDarkTheme) Color.White else Color.Black)
                        Text("MIME: video/mp4 • Watermark Free", fontSize = 11.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold)
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(LanguageHelper.getString(R.string.settings, language).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (isDarkTheme) Color.White else Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Resolution settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LanguageHelper.getString(R.string.resolution, language).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.White else Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1080p", "4k", "8k").forEach { res ->
                            val isSel = settingsResolution.lowercase() == res.lowercase()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neonGlow(color = if (isSel) Color(0xFFFF0055) else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                    .glassmorphic(shape = RoundedCornerShape(8.dp))
                                    .tactileClick { onSettingsResolutionChange(res) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(res.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isSel) Color(0xFFFF0055) else if (isDarkTheme) Color.White else Color.Black)
                            }
                        }
                    }
                }
            }
        }

        // FPS settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LanguageHelper.getString(R.string.fps_settings, language).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.White else Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(24, 30, 60).forEach { fps ->
                            val isSel = settingsFps == fps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neonGlow(color = if (isSel) Color(0xFF00E5FF) else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                    .glassmorphic(shape = RoundedCornerShape(8.dp))
                                    .tactileClick { onSettingsFpsChange(fps) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${fps} FPS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isSel) Color(0xFF00E5FF) else if (isDarkTheme) Color.White else Color.Black)
                            }
                        }
                    }
                }
            }
        }

        // Hardware Acceleration Toggle
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Computer, contentDescription = "Hardware", tint = Color(0xFFFF0055))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(LanguageHelper.getString(R.string.hardware_acceleration, language), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.White else Color.Black)
                    }
                    Switch(
                        checked = isHardwareAccEnabled,
                        onCheckedChange = { onToggleHardwareAcc() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Storage Path selection
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(LanguageHelper.getString(R.string.storage_path, language).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.White else Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Movies/PowerCut", "Cache/PowerCut").forEach { path ->
                            val isSel = storagePath == path
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neonGlow(color = if (isSel) Color(0xFFFF0055) else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                    .glassmorphic(shape = RoundedCornerShape(8.dp))
                                    .tactileClick { onStoragePathChange(path) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(path, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isSel) Color(0xFFFF0055) else if (isDarkTheme) Color.White else Color.Black)
                            }
                        }
                    }
                }
            }
        }

        // Theme Toggle Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = "Theme", tint = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(LanguageHelper.getString(R.string.select_theme, language), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.White else Color.Black)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF0055),
                            checkedTrackColor = Color(0xFFFF0055).copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .tactileClick(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) {
                Color(0xFFFF0055)
            } else if (isDarkTheme) {
                Color.White.copy(alpha = 0.4f)
            } else {
                Color.Black.copy(alpha = 0.4f)
            },
            modifier = Modifier.size(24.dp)
        )
    }
}
