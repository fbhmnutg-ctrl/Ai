package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeId: String = "CATPPUCCIN",
    content: @Composable () -> Unit
) {
    val devTheme = getDevThemeById(themeId)

    val dynamicDevColorScheme = darkColorScheme(
        primary = devTheme.primary,
        onPrimary = devTheme.bg,
        primaryContainer = devTheme.surface,
        onPrimaryContainer = devTheme.textPrimary,

        secondary = devTheme.secondary,
        onSecondary = devTheme.bg,
        secondaryContainer = devTheme.card,
        onSecondaryContainer = devTheme.textPrimary,

        tertiary = devTheme.tertiary,
        onTertiary = devTheme.bg,

        background = devTheme.bg,
        onBackground = devTheme.textPrimary,

        surface = devTheme.surface,
        onSurface = devTheme.textPrimary,
        surfaceVariant = devTheme.card,
        onSurfaceVariant = devTheme.textSecondary,

        outline = devTheme.border,
        outlineVariant = devTheme.borderLight,

        error = CrimsonError,
        onError = Color(0xFF450A0A)
    )

    DeveloperThemeProvider(themeId = themeId) {
        MaterialTheme(
            colorScheme = dynamicDevColorScheme,
            typography = Typography,
            content = content
        )
    }
}
