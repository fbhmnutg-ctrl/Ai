package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Developer Theme definitions for true software engineers.
 */
data class DevThemeColors(
    val id: String,
    val name: String,
    val icon: String,
    val bg: Color,
    val surface: Color,
    val card: Color,
    val cardHover: Color,
    val border: Color,
    val borderLight: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val userBubbleBg: Color,
    val userBubbleBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val codeBg: Color,
    val codeHeaderBg: Color,
    val codeKeyword: Color,
    val codeString: Color,
    val codeFunction: Color,
    val codeComment: Color,
    val codeNumber: Color
)

val CatppuccinMochaTheme = DevThemeColors(
    id = "CATPPUCCIN",
    name = "Catppuccin Mocha",
    icon = "🐱",
    bg = Color(0xFF181825),
    surface = Color(0xFF1E1E2E),
    card = Color(0xFF26283B),
    cardHover = Color(0xFF313244),
    border = Color(0xFF3B3E52),
    borderLight = Color(0xFF45475A),
    primary = Color(0xFFCBA6F7),       // Mauve
    secondary = Color(0xFFA6E3A1),     // Green
    tertiary = Color(0xFF89B4FA),      // Sapphire
    accent = Color(0xFF89DCEB),        // Sky
    userBubbleBg = Color(0xFF313244),
    userBubbleBorder = Color(0xFF45475A),
    textPrimary = Color(0xFFCDD6F4),
    textSecondary = Color(0xFFA6ADC8),
    textMuted = Color(0xFF6C7086),
    codeBg = Color(0xFF11111B),
    codeHeaderBg = Color(0xFF181825),
    codeKeyword = Color(0xFFCBA6F7),   // Mauve
    codeString = Color(0xFFA6E3A1),    // Green
    codeFunction = Color(0xFF89B4FA),  // Blue
    codeComment = Color(0xFF6C7086),   // Overlay
    codeNumber = Color(0xFFFAB387)     // Peach
)

val TokyoNightTheme = DevThemeColors(
    id = "TOKYO_NIGHT",
    name = "Tokyo Night",
    icon = "🗼",
    bg = Color(0xFF1A1B26),
    surface = Color(0xFF1F2335),
    card = Color(0xFF24283B),
    cardHover = Color(0xFF292E42),
    border = Color(0xFF3B4261),
    borderLight = Color(0xFF414868),
    primary = Color(0xFF7DCFFF),       // Cyan
    secondary = Color(0xFF9ECE6A),     // Green
    tertiary = Color(0xFFBB9AF7),      // Magenta
    accent = Color(0xFF7AA2F7),        // Blue
    userBubbleBg = Color(0xFF292E42),
    userBubbleBorder = Color(0xFF3B4261),
    textPrimary = Color(0xFFC0CAF5),
    textSecondary = Color(0xFF9AA5CE),
    textMuted = Color(0xFF565F89),
    codeBg = Color(0xFF16161E),
    codeHeaderBg = Color(0xFF1F2335),
    codeKeyword = Color(0xFFBB9AF7),   // Magenta
    codeString = Color(0xFF9ECE6A),    // Green
    codeFunction = Color(0xFF7AA2F7),  // Blue
    codeComment = Color(0xFF565F89),   // Comment Grey
    codeNumber = Color(0xFFFF9E64)     // Orange
)

val GitHubDarkTheme = DevThemeColors(
    id = "GITHUB_DARK",
    name = "GitHub Dark",
    icon = "🐙",
    bg = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    card = Color(0xFF21262D),
    cardHover = Color(0xFF30363D),
    border = Color(0xFF30363D),
    borderLight = Color(0xFF484F58),
    primary = Color(0xFF58A6FF),       // GitHub Blue
    secondary = Color(0xFF3FB950),     // GitHub Green
    tertiary = Color(0xFFD2A8FF),      // GitHub Purple
    accent = Color(0xFF79C0FF),
    userBubbleBg = Color(0xFF21262D),
    userBubbleBorder = Color(0xFF30363D),
    textPrimary = Color(0xFFE6EDF3),
    textSecondary = Color(0xFF8B949E),
    textMuted = Color(0xFF6E7681),
    codeBg = Color(0xFF010409),
    codeHeaderBg = Color(0xFF161B22),
    codeKeyword = Color(0xFFFF7B72),   // Coral Red
    codeString = Color(0xFFA5D6FF),    // Light Blue
    codeFunction = Color(0xFFD2A8FF),  // Purple
    codeComment = Color(0xFF8B949E),   // Grey
    codeNumber = Color(0xFF79C0FF)     // Cyan
)

val MonokaiProTheme = DevThemeColors(
    id = "MONOKAI",
    name = "Monokai Pro",
    icon = "⚡",
    bg = Color(0xFF121314),
    surface = Color(0xFF1C1E20),
    card = Color(0xFF25282C),
    cardHover = Color(0xFF2D3136),
    border = Color(0xFF373C43),
    borderLight = Color(0xFF49515A),
    primary = Color(0xFFFFD866),       // Yellow
    secondary = Color(0xFFA9DC76),     // Mint Green
    tertiary = Color(0xFFFF6188),      // Pink
    accent = Color(0xFF78DCE8),        // Cyan
    userBubbleBg = Color(0xFF25282C),
    userBubbleBorder = Color(0xFF373C43),
    textPrimary = Color(0xFFFCFCFA),
    textSecondary = Color(0xFFC1C0C0),
    textMuted = Color(0xFF727072),
    codeBg = Color(0xFF0D0E0E),
    codeHeaderBg = Color(0xFF1C1E20),
    codeKeyword = Color(0xFFFF6188),   // Pink
    codeString = Color(0xFFFFD866),    // Yellow
    codeFunction = Color(0xFFA9DC76),  // Mint Green
    codeComment = Color(0xFF727072),   // Grey
    codeNumber = Color(0xFFAB9DF2)     // Violet
)

val VSCodeDarkTheme = DevThemeColors(
    id = "VS_CODE",
    name = "VS Code Dark Modern",
    icon = "💻",
    bg = Color(0xFF181818),
    surface = Color(0xFF1F1F1F),
    card = Color(0xFF282828),
    cardHover = Color(0xFF323232),
    border = Color(0xFF3C3C3C),
    borderLight = Color(0xFF4A4A4A),
    primary = Color(0xFF0078D4),       // VS Azure
    secondary = Color(0xFF4EC9B0),     // Cyan Type
    tertiary = Color(0xFFC586C0),      // Purple
    accent = Color(0xFF9CDCFE),        // Variable Blue
    userBubbleBg = Color(0xFF282828),
    userBubbleBorder = Color(0xFF3C3C3C),
    textPrimary = Color(0xFFCCCCCC),
    textSecondary = Color(0xFF9D9D9D),
    textMuted = Color(0xFF6E6E6E),
    codeBg = Color(0xFF141414),
    codeHeaderBg = Color(0xFF1F1F1F),
    codeKeyword = Color(0xFFC586C0),   // Purple
    codeString = Color(0xFFCE9178),    // Orange/Brown
    codeFunction = Color(0xFFDCDCAA),  // Yellow
    codeComment = Color(0xFF6A9955),   // Green
    codeNumber = Color(0xFFB5CEA8)     // Mint
)

val AllDevThemes = listOf(
    CatppuccinMochaTheme,
    TokyoNightTheme,
    GitHubDarkTheme,
    MonokaiProTheme,
    VSCodeDarkTheme
)

fun getDevThemeById(id: String): DevThemeColors {
    return AllDevThemes.find { it.id.equals(id, ignoreCase = true) } ?: CatppuccinMochaTheme
}

val LocalDevTheme = staticCompositionLocalOf { CatppuccinMochaTheme }

@Composable
fun DeveloperThemeProvider(
    themeId: String,
    content: @Composable () -> Unit
) {
    val theme = getDevThemeById(themeId)
    CompositionLocalProvider(LocalDevTheme provides theme) {
        content()
    }
}
