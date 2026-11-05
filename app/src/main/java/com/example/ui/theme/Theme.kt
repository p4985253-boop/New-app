package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SoundboxColorScheme = darkColorScheme(
    primary = SoundboxEmerald,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = SoundboxTeal,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = SoundboxAmber,
    onTertiary = Color.Black,
    background = SoundboxDarkBg,
    onBackground = SoundboxTextPrimary,
    surface = SoundboxSurface,
    onSurface = SoundboxTextPrimary,
    surfaceVariant = SoundboxCardBg,
    onSurfaceVariant = SoundboxTextSecondary,
    outline = SoundboxCardBorder
)

@Composable
fun SmartSoundBoxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoundboxColorScheme,
        typography = Typography,
        content = content
    )
}
