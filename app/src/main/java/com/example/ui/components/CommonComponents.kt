package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DevThemeColors
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.LocalDevTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeural
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GgufTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            ),
            color = color
        )
    }
}

@Composable
fun EngineBadge(
    engineType: String,
    modifier: Modifier = Modifier
) {
    val isLocal = engineType == "LOCAL_GGUF"
    val accentColor = if (isLocal) NeonCyan else VioletNeural
    val label = if (isLocal) "libllama.so" else "ollama:11434"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(0.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = label,
            color = accentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun HuggingFaceBadge(
    modifier: Modifier = Modifier,
    text: String = "Hugging Face"
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFF9D00).copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF9D00).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "🤗",
                fontSize = 10.sp
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = Color(0xFFFFB347)
            )
        }
    }
}

@Composable
fun RamIndicatorMeter(
    requiredMb: Int,
    availableMb: Int,
    totalMb: Int,
    modifier: Modifier = Modifier
) {
    val isSafe = requiredMb <= (availableMb * 0.85f)
    val meterColor = if (isSafe) EmeraldGlow else Color(0xFFF59E0B)
    val ratio = (requiredMb.toFloat() / totalMb.toFloat()).coerceIn(0.05f, 1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = meterColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "RAM Required: ~${requiredMb} MB",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = if (isSafe) "Optimal" else "High Memory",
                color = meterColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = meterColor,
            trackColor = ObsidianBorder
        )
    }
}

/**
 * Developer IDE Code Block with Syntax Highlighting and macOS/Linux Terminal styling.
 */
@Composable
fun CodeBlockView(
    code: String,
    language: String = "code",
    modifier: Modifier = Modifier
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val devTheme = LocalDevTheme.current

    val highlightedCode = remember(code, devTheme) {
        highlightSyntax(code, language, devTheme)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = devTheme.codeBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border)
    ) {
        Column {
            // IDE Window Header (Traffic lights + Language + Copy)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(devTheme.codeHeaderBg)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Window Traffic Lights
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF27C93F)))

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = language.ifEmpty { "code" }.lowercase(),
                        color = devTheme.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        isCopied = true
                        scope.launch {
                            delay(1500)
                            isCopied = false
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCopied) devTheme.secondary else devTheme.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Code Content with Horizontal Scroll and Highlighting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = highlightedCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

/**
 * Developer Syntax Highlighter supporting Python, Kotlin, Rust, JS/TS, C++, Shell, SQL, JSON
 */
private fun highlightSyntax(code: String, lang: String, theme: DevThemeColors): AnnotatedString {
    return buildAnnotatedString {
        append(code)

        val keywords = listOf(
            "fun", "val", "var", "class", "interface", "object", "package", "import", "return",
            "if", "else", "when", "for", "while", "do", "try", "catch", "finally", "throw",
            "def", "async", "await", "from", "as", "with", "yield", "lambda", "self", "None", "True", "False",
            "let", "mut", "fn", "struct", "enum", "impl", "trait", "pub", "use", "mod", "match",
            "function", "const", "export", "default", "type", "null", "undefined", "new",
            "select", "from", "where", "insert", "into", "update", "delete", "create", "table"
        )

        // Highlight Strings
        val stringRegex = Regex("""(".*?"|'.*?')""")
        stringRegex.findAll(code).forEach { match ->
            addStyle(
                SpanStyle(color = theme.codeString),
                match.range.first,
                match.range.last + 1
            )
        }

        // Highlight Comments
        val commentRegex = Regex("""(//.*|#.*)""")
        commentRegex.findAll(code).forEach { match ->
            addStyle(
                SpanStyle(color = theme.codeComment),
                match.range.first,
                match.range.last + 1
            )
        }

        // Highlight Numbers
        val numberRegex = Regex("""\b\d+(\.\d+)?\b""")
        numberRegex.findAll(code).forEach { match ->
            addStyle(
                SpanStyle(color = theme.codeNumber),
                match.range.first,
                match.range.last + 1
            )
        }

        // Highlight Keywords
        keywords.forEach { kw ->
            val kwRegex = Regex("""\b$kw\b""")
            kwRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = theme.codeKeyword, fontWeight = FontWeight.Bold),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        // Highlight Functions (e.g. `foo(`)
        val funcRegex = Regex("""\b([a-zA-Z_]\w*)\s*(?=\()""")
        funcRegex.findAll(code).forEach { match ->
            addStyle(
                SpanStyle(color = theme.codeFunction),
                match.range.first,
                match.range.last + 1
            )
        }
    }
}

@Composable
fun MetricChip(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ObsidianSurface)
            .border(0.5.dp, ObsidianBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon()
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
