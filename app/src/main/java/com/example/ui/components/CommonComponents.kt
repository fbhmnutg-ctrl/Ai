package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanSubtle
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
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
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
    val label = if (isLocal) "Local GGUF" else "Ollama Server"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = label,
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
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
        color = Color(0xFFFF9D00).copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9D00).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
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
                    fontSize = 11.sp
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
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = if (isSafe) "Optimal Fit" else "High RAM load",
                color = meterColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = meterColor,
            trackColor = ObsidianBorder
        )
    }
}

@Composable
fun CodeBlockView(
    code: String,
    language: String = "code",
    modifier: Modifier = Modifier
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianCard)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifEmpty { "code" },
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
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
                        tint = if (isCopied) EmeraldGlow else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = code,
                modifier = Modifier.padding(12.dp),
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
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
            .border(1.dp, ObsidianBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon()
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
