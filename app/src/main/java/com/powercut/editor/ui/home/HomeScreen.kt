package com.powercut.editor.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.R
import com.powercut.editor.core.utils.LanguageHelper
import com.powercut.editor.ui.theme.DarkBgEnd
import com.powercut.editor.ui.theme.DarkBgStart
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.tactileClick
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    language: String,
    onLanguageToggle: () -> Unit,
    onVideoSelected: (android.net.Uri) -> Unit,

    // Bottom navigation states & settings callbacks
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
        // High-end intro splash loading animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBgStart, DarkBgEnd)
                    )
                ),
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
        // MAIN WORKSPACE LAYOUT WITH FLOAT GLASS NAVIGATION BAR
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBgStart, DarkBgEnd)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 82.dp) // Spacing for floating navigation bar
            ) {
                // 1. TOP HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo + App Name + Pro Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(NeonOrange, Color(0xFFE64A19))
                                    ),
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
                                text = "Pro Video Editor",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Help + Profile Buttons
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

                // TAB RENDERING SWITCHER
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

            // 6. PREMIUM BOTTOM NAVIGATION BAR (GLASSMORPHISM WITH CYBER GLOW)
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
                        .glassmorphic(shape = RoundedCornerShape(32.dp), backColor = Color(0xFF14141A).copy(alpha = 0.85f))
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

// -------------------------------------------------------------
// DASHBOARD TAB COMPOSABLE VIEW
// -------------------------------------------------------------
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // 2. LARGE NEW PROJECT GRADIENT ORANGE ACTION BUTTON
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .neonGlow(color = NeonOrange, shape = RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NeonOrange, Color(0xFFE64A19))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
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
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
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
                            text = "Import video & start editing",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. QUICK TOOLS GRID (4 Columns, Glassmorphic buttons)
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
                tools.forEach { (emoji, label, _) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .glassmorphic(shape = RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                            .tactileClick { /* Quick tool trigger */ }
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
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4. TRENDING TEMPLATES CAROUSEL
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Templates",
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
                Pair("🔥 Reels Beat", Brush.horizontalGradient(colors = listOf(NeonOrange, Color(0xFFFF9800)))),
                Pair("✨ Cinematic", Brush.horizontalGradient(colors = listOf(Color(0xFF7C4DFF), Color(0xFF536DFE)))),
                Pair("🎵 Lyric Video", Brush.horizontalGradient(colors = listOf(CyberCyan, Color(0xFF009688)))),
                Pair("💖 Birthday", Brush.horizontalGradient(colors = listOf(Color(0xFFE91E63), Color(0xFFF06292))))
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templatesList) { (title, brush) ->
                    Box(
                        modifier = Modifier
                            .size(110.dp, 160.dp)
                            .background(brush, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .tactileClick { /* Select Template */ },
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
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

        // 5. RECENT PROJECTS LIST
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
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Gradient video thumbnail
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                if (status == "Draft") {
                                    Brush.linearGradient(colors = listOf(Color(0xFF1A237E), Color(0xFF283593)))
                                } else {
                                    Brush.linearGradient(colors = listOf(Color(0xFFBF360C), Color(0xFFE65100)))
                                },
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play icon",
                            tint = Color.White.copy(alpha = 0.6f),
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
                        // Status badge (Draft vs Exported)
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(
                                    if (status == "Draft") NeonOrange.copy(alpha = 0.18f) else Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status == "Draft") NeonOrange else Color(0xFF4CAF50)
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

// Extra templates view
@Composable
fun TemplatesView(language: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Trending Pro Templates", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
        }
        val items = listOf("Vlog Cinematic Spark", "Wedding Golden Glow", "Cyberpunk Beat Drop", "Urdu Status Aesthetic")
        items(items) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(i, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// Extra exports view
@Composable
fun ExportsView(language: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Watermark-Free Exports", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
        }
        val items = listOf("Travel_Dubai_1080p.mp4", "Lyric_Video_Urdu_Poetry.mp4")
        items(items) { i ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(i, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// Extra settings view
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
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Premium Studio Configuration", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("DEFAULT EXPORT RESOLUTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1080p", "4k", "8k").forEach { res ->
                            val isSel = settingsResolution.lowercase() == res.lowercase()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neonGlow(color = if (isSel) NeonOrange else Color.Transparent, shape = RoundedCornerShape(8.dp))
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

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glassmorphic(shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("HARDWARE ACCELERATED (MULTI-CORE)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Utilize NEON & GPU processors", fontSize = 11.sp, color = Color.Gray)
                        Switch(
                            checked = isHardwareAccEnabled,
                            onCheckedChange = { onToggleHardwareAcc() },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonOrange)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// REUSABLE NAVIGATION & BOTTOM BAR ITEM
// -------------------------------------------------------------
@Composable
fun BottomTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
