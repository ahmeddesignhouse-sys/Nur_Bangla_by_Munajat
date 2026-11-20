package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentTheme
import com.example.data.model.ThemeMode

val NoorShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun NoorBanglaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentTheme: AccentTheme = AccentTheme.ISLAMIC_GREEN,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = getColorScheme(accentTheme = accentTheme, darkTheme = isDark),
        typography = NoorTypography,
        shapes = NoorShapes,
        content = content
    )
}
