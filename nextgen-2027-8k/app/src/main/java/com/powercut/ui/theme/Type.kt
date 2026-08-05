package com.powercut.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = TextStyle(
    color = TextPrimary,
    fontWeight = FontWeight.Normal
)

val PowerCutTypography = Typography(
    headlineLarge  = base.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = base.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge     = base.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = base.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge      = base.copy(fontSize = 15.sp),
    bodyMedium     = base.copy(fontSize = 14.sp),
    bodySmall      = base.copy(fontSize = 12.sp, color = TextSecondary),
    labelLarge     = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelSmall     = base.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
)
