package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF8CEEFF),

    secondary = EmeraldGlow,
    onSecondary = Color(0xFF003825),
    secondaryContainer = Color(0xFF005237),
    onSecondaryContainer = Color(0xFF8DF6C0),

    tertiary = VioletNeural,
    onTertiary = Color(0xFF2E1568),
    tertiaryContainer = Color(0xFF452B81),
    onTertiaryContainer = Color(0xFFEADBFF),

    background = ObsidianBg,
    onBackground = TextPrimary,

    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,

    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderLight,

    error = CrimsonError,
    onError = Color(0xFF450A0A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark theme as requested
    dynamicColor: Boolean = false, // Keep bespoke crafted aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
