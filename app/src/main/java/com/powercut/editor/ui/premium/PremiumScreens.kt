package com.powercut.editor.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powercut.editor.ui.theme.AccentPrimary
import com.powercut.editor.ui.theme.AccentSecondary
import com.powercut.editor.ui.theme.CyberCyan
import com.powercut.editor.ui.theme.DarkBgStart
import com.powercut.editor.ui.theme.GlassBackground
import com.powercut.editor.ui.theme.NeonOrange
import com.powercut.editor.ui.theme.OnPrimary
import com.powercut.editor.ui.theme.OnSurfaceSecondary
import com.powercut.editor.ui.theme.Surface
import com.powercut.editor.ui.theme.SurfaceVariant
import com.powercut.editor.ui.theme.glassmorphic
import com.powercut.editor.ui.theme.neonGlow
import com.powercut.editor.ui.theme.premiumAccentGradient
import com.powercut.editor.ui.theme.tactileClick

// ═══════════════════════════════════════════════════════════════
//  PREMIUM UI SCREENS — Dark Glassmorphic Theme
// ═══════════════════════════════════════════════════════════════

// ─── Category Chip (for horizontal scroll bar) ─────────────────
@Composable
fun CategoryChip(
    emoji: String,
    name: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgBrush = if (isSelected) premiumAccentGradient else Brush.linearGradient(
        listOf(Color.Transparent, Color.Transparent)
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(brush = bgBrush)
            .background(
                color = if (!isSelected) SurfaceVariant.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 16.sp)
            Text(
                name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else OnSurfaceSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$count",
                fontSize = 8.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.7f) else OnSurfaceSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Option Toggle Card ────────────────────────────────────────
@Composable
fun OptionToggleCard(
    option: PremiumOption,
    onToggle: () -> Unit
) {
    val isEnabled by option.isEnabled
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isEnabled) AccentSecondary.copy(alpha = 0.12f) else SurfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isEnabled) AccentSecondary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.04f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(option.emoji, fontSize = 18.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        option.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) OnPrimary else OnPrimary.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        option.description,
                        fontSize = 9.sp,
                        color = OnSurfaceSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (isEnabled) AccentSecondary else Color.White.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isEnabled) AccentSecondary else Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isEnabled) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Enabled",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Slider Control Row ────────────────────────────────────────
@Composable
fun SliderControlRow(slider: SliderControl) {
    val value by slider.currentValue
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(slider.emoji, fontSize = 14.sp)
                Text(slider.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
            }
            Text(
                if (slider.stepSize >= 1f) "${value.toInt()}" else String.format("%.2f", value),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
        }
        Slider(
            value = value,
            onValueChange = { slider.currentValue.value = it },
            valueRange = slider.minValue..slider.maxValue,
            colors = SliderDefaults.colors(
                activeTrackColor = CyberCyan,
                thumbColor = CyberCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.08f)
            ),
            modifier = Modifier.height(24.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${slider.minValue.toInt()}", fontSize = 8.sp, color = OnSurfaceSecondary.copy(alpha = 0.5f))
            Text("${slider.maxValue.toInt()}", fontSize = 8.sp, color = OnSurfaceSecondary.copy(alpha = 0.5f))
        }
    }
}

// ─── Collapsible Section ───────────────────────────────────────
@Composable
fun CollapsibleSection(
    title: String,
    emoji: String,
    itemCount: Int,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(200), label = "expand")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Surface.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emoji, fontSize = 16.sp)
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                Box(
                    modifier = Modifier
                        .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("$itemCount", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = OnSurfaceSecondary,
                modifier = Modifier.rotate(rotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  MAIN PREMIUM HUB SCREEN
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumHubScreen(
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(DarkBgStart, DarkBgStart))
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .glassmorphic(shape = RoundedCornerShape(10.dp))
                    .tactileClick(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnPrimary, modifier = Modifier.size(18.dp))
            }
            Column {
                Text("Premium Studio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                Text(
                    "${PremiumRegistry.totalOptions}+ professional tools",
                    fontSize = 11.sp,
                    color = OnSurfaceSecondary
                )
            }
        }

        // Total count banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .neonGlow(color = AccentSecondary, shape = RoundedCornerShape(14.dp), glowWidth = 1.dp)
                .background(premiumAccentGradient, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Studio Pro Features", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Professional video editing suite", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${PremiumRegistry.totalOptions}+",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Grid via LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PremiumRegistry.categories) { cat ->
                CategoryCard(cat) { onCategoryClick(cat.id) }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CategoryCard(
    cat: PremiumRegistry.Category,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphic(shape = RoundedCornerShape(16.dp))
            .tactileClick(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentSecondary.copy(alpha = 0.2f), AccentPrimary.copy(alpha = 0.2f))),
                        RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(cat.emoji, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(cat.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                Text(cat.description, fontSize = 10.sp, color = OnSurfaceSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .background(CyberCyan.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${cat.optionCount}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = CyberCyan)
                }
                Text("options", fontSize = 8.sp, color = OnSurfaceSecondary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  CATEGORY DETAIL SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDetailScreen(
    categoryId: String,
    onBack: () -> Unit
) {
    val cat = PremiumRegistry.categories.find { it.id == categoryId } ?: return
    val options = PremiumRegistry.getOptionsForCategory(categoryId)
    val sliders = PremiumRegistry.getSliderControlsForCategory(categoryId)

    // Group options by their sub-category field
    val grouped = options.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBgStart, DarkBgStart)))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .glassmorphic(shape = RoundedCornerShape(10.dp))
                    .tactileClick(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OnPrimary, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("${cat.emoji} ${cat.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                Text("${options.size} options available", fontSize = 10.sp, color = OnSurfaceSecondary)
            }
            // Enabled counter
            val enabledCount = options.count { it.isEnabled.value }
            Box(
                modifier = Modifier
                    .background(
                        if (enabledCount > 0) AccentSecondary.copy(alpha = 0.15f) else SurfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(1.dp, if (enabledCount > 0) AccentSecondary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "$enabledCount active",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabledCount > 0) AccentSecondary else OnSurfaceSecondary
                )
            }
        }

        // Quick filter row
        var showOnlyEnabled by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipPill("All", !showOnlyEnabled) { showOnlyEnabled = false }
            FilterChipPill("Active Only", showOnlyEnabled) { showOnlyEnabled = true }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main content — LazyColumn with collapsible sections
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Slider controls section
            if (sliders.isNotEmpty()) {
                item {
                    CollapsibleSection("Adjustments", "🎛️", sliders.size, initiallyExpanded = true) {
                        sliders.forEach { slider ->
                            SliderControlRow(slider)
                        }
                    }
                }
            }

            // Grouped option sections
            grouped.forEach { (subCategory, subOptions) ->
                val filtered = if (showOnlyEnabled) subOptions.filter { it.isEnabled.value } else subOptions
                if (filtered.isNotEmpty()) {
                    item {
                        key(subCategory) {
                            CollapsibleSection(
                                title = subCategory,
                                emoji = filtered.first().emoji,
                                itemCount = filtered.size,
                                initiallyExpanded = subCategory == grouped.keys.first()
                            ) {
                                // Use FlowRow for compact grid layout
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    maxItemsInEachRow = 2
                                ) {
                                    filtered.forEach { option ->
                                        Box(modifier = Modifier.weight(1f, fill = true).fillMaxWidth()) {
                                            OptionToggleCard(option) {
                                                option.isEnabled.value = !option.isEnabled.value
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selector options (for project settings etc.)
            val selectorOptions = when (categoryId) {
                "project_settings" -> getProjectSelectorOptions()
                "export_settings" -> getExportSelectorOptions()
                else -> emptyList()
            }
            if (selectorOptions.isNotEmpty()) {
                item {
                    CollapsibleSection("Preferences", "⚙️", selectorOptions.size, initiallyExpanded = true) {
                        selectorOptions.forEach { sel ->
                            SelectorOptionRow(sel)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ─── Filter Chip Pill ──────────────────────────────────────────
@Composable
private fun FilterChipPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) AccentSecondary.copy(alpha = 0.2f) else SurfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                if (isSelected) AccentSecondary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) AccentSecondary else OnSurfaceSecondary
        )
    }
}

// ─── Selector Option Row ───────────────────────────────────────
@Composable
fun SelectorOptionRow(selector: SelectorOption) {
    val selectedIdx by selector.selectedIndex
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(selector.emoji, fontSize = 14.sp)
            Text(selector.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(selector.options.size) { idx ->
                val isSel = idx == selectedIdx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSel) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSel) CyberCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selector.selectedIndex.value = idx }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        selector.options[idx],
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) CyberCyan else OnSurfaceSecondary
                    )
                }
            }
        }
    }
}

// ─── Helper: Project Settings Selectors ────────────────────────
private fun getExportSelectorOptions(): List<SelectorOption> = listOf(
    SelectorOption("ex_audio_channels", "Audio Channels", "🔈", listOf("Mono", "Stereo", "5.1 Surround"))
)

private fun getProjectSelectorOptions(): List<SelectorOption> = listOf(
    SelectorOption("ps_autosave_interval", "Auto-Save Interval", "⏱️", listOf("30s", "1min", "2min", "5min", "10min")),
    SelectorOption("ps_undo_count", "Undo Count", "↩️", listOf("10", "25", "50", "100", "Unlimited")),
    SelectorOption("pt_default_duration", "Default Clip Duration", "⏱️", listOf("3s", "5s", "10s", "15s", "30s")),
    SelectorOption("pt_track_height", "Track Height", "📏", listOf("Compact", "Normal", "Tall", "Extra Tall")),
    SelectorOption("pv_preview_quality", "Preview Quality", "🖥️", listOf("Quarter", "Half", "Full", "Auto")),
    SelectorOption("ps_cache_size", "Cache Size", "💾", listOf("1 GB", "2 GB", "5 GB", "10 GB", "Unlimited")),
    SelectorOption("ex_audio_channels", "Audio Channels", "🔈", listOf("Mono", "Stereo", "5.1 Surround"))
)

// ═══════════════════════════════════════════════════════════════
//  QUICK PREMIUM TOOLBAR (Embeddable in EditorScreen)
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumQuickBar(
    onOpenFull: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PremiumRegistry.categories.forEach { cat ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenFull)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cat.emoji, fontSize = 18.sp)
                    Text(cat.name, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = OnPrimary, maxLines = 1)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  PREMIUM OPTION DETAIL SHEET (Full-screen info for one option)
// ═══════════════════════════════════════════════════════════════

@Composable
fun PremiumOptionDetailSheet(
    option: PremiumOption,
    onDismiss: () -> Unit,
    onToggle: () -> Unit
) {
    val isEnabled by option.isEnabled
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBgStart, DarkBgStart)))
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Option Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurfaceSecondary)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Close", fontSize = 11.sp, color = OnSurfaceSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Big emoji
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.linearGradient(listOf(AccentSecondary.copy(alpha = 0.15f), AccentPrimary.copy(alpha = 0.15f))),
                    RoundedCornerShape(24.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(option.emoji, fontSize = 40.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(option.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(option.description, fontSize = 13.sp, color = OnSurfaceSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Category: ${option.category}", fontSize = 11.sp, color = CyberCyan)

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neonGlow(
                    color = if (isEnabled) AccentSecondary else Color.Gray,
                    shape = RoundedCornerShape(14.dp),
                    glowWidth = 1.dp
                )
                .background(
                    brush = if (isEnabled) premiumAccentGradient else Brush.linearGradient(
                        listOf(SurfaceVariant, SurfaceVariant)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onToggle)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isEnabled) "✓ ENABLED" else "ENABLE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) Color.White else OnSurfaceSecondary,
                letterSpacing = 1.sp
            )
        }
    }
}
