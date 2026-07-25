package com.powercut.editor.core.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

object LanguageHelper {
    /**
     * Resolves a string resource dynamically based on the custom language selection ("en" or "ur").
     */
    @Composable
    fun getString(resId: Int, language: String): String {
        val context = LocalContext.current
        return try {
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(Locale(language))
            val localizedContext = context.createConfigurationContext(config)
            localizedContext.resources.getString(resId)
        } catch (e: Exception) {
            context.resources.getString(resId)
        }
    }

    /**
     * Resolves layout direction based on custom language selection.
     */
    fun getLayoutDirection(language: String): LayoutDirection {
        return if (language == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr
    }
}
