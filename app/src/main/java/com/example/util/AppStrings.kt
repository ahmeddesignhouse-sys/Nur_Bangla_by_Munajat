package com.example.util

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Current app language code ("bn" = Bangla default, "en" = English).
 * Updating this CompositionLocal recomposes the entire Compose tree immediately with zero restart.
 */
val LocalAppLanguage = staticCompositionLocalOf { "bn" }

fun isEnglishLang(code: String): Boolean = code == "en"

/**
 * Concise bilingual picker for UI strings across all screens.
 */
fun tr(isEnglish: Boolean, bn: String, en: String): String = if (isEnglish) en else bn
