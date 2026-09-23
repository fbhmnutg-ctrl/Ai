package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SettingsManager
import com.example.data.local.entity.LocalModelEntity
import com.example.engine.DeviceHardwareManager
import com.example.engine.NativeLlamaBridge
import com.example.ui.components.GgufTag
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDim
import com.example.ui.theme.NeonCyanSubtle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeural
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    chatViewModel: ChatViewModel,
    onNavigateToBenchmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }

    val cpuThreads by settingsManager.cpuThreads.collectAsStateWithLifecycle()
    val contextLength by settingsManager.contextLength.collectAsStateWithLifecycle()
    val isStreamingEnabled by settingsManager.isStreamingEnabled.collectAsStateWithLifecycle()
    val currentAppTheme by settingsManager.appTheme.collectAsStateWithLifecycle()
    val activeModel by chatViewModel.activeModel.collectAsStateWithLifecycle()

    val isGpuOffloadEnabled by settingsManager.isGpuOffloadEnabled.collectAsStateWithLifecycle()
    val gpuOffloadLayers by settingsManager.gpuOffloadLayers.collectAsStateWithLifecycle()
    val isOomGuardEnabled by settingsManager.isOomGuardEnabled.collectAsStateWithLifecycle()

    var unloadMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Settings & Engine",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Memory management, GPU offloading & native runtime",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Section 1: Active Model & Memory Unload (User Request: "adding an unload option to cancel the model")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                .testTag("memory_unload_section"),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (activeModel != null) NeonCyanSubtle else ObsidianSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (activeModel != null) NeonCyan else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Active Model in RAM",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeModel != null) "Allocated in memory" else "No model currently loaded",
                                color = if (activeModel != null) EmeraldGlow else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (activeModel != null) {
                        GgufTag(text = activeModel?.quantization ?: "GGUF", color = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeModel != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurface)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activeModel?.name ?: "",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Est. RAM: ~${activeModel?.requiredRamMb} MB • Context: ${activeModel?.contextLength} tok",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent Unload Option
                    Button(
                        onClick = {
                            chatViewModel.unloadModel()
                            unloadMessage = "Model unloaded and RAM freed successfully (0 MB allocated)."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("unload_active_model_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unload Model & Cancel Handle",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Memory is clean. Zero RAM allocated for neural weights. Select a template from the GGUF Hub or Uploaded Templates page to load.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                unloadMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        color = EmeraldGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Section 3: Text Display Streaming Preference (User Request)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                .testTag("text_streaming_setting_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Text Display Streaming",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isStreamingEnabled)
                                "Real-time streaming enabled (tokens display continuously as they generate)"
                            else
                                "Non-streaming mode (generates full response before displaying message)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Switch(
                        checked = isStreamingEnabled,
                        onCheckedChange = { settingsManager.setStreamingEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyanDim,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ObsidianSurface
                        ),
                        modifier = Modifier.testTag("streaming_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurface)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isStreamingEnabled)
                            "⚡ Real-time Token Streaming: Shows thought trajectories and answers incrementally, ideal for rapid conversational feedback."
                        else
                            "📦 Instant Complete Response: Processes the entire prompt in the background and renders the complete answer at once without mid-sentence UI updates.",
                        color = if (isStreamingEnabled) NeonCyan else VioletNeural,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Section 3B: Developer IDE Theme & Aesthetic
        val devTheme = com.example.ui.theme.LocalDevTheme.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, devTheme.border, RoundedCornerShape(12.dp))
                .testTag("developer_theme_setting_card"),
            colors = CardDefaults.cardColors(containerColor = devTheme.card),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = devTheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Developer IDE Theme",
                            color = devTheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = com.example.ui.theme.getDevThemeById(currentAppTheme).name,
                        color = devTheme.primary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Engineered dark palettes with IDE syntax highlighting & terminal aesthetics",
                    color = devTheme.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Presets Chips Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.ui.theme.AllDevThemes.forEach { themePreset ->
                        val isSelected = themePreset.id.equals(currentAppTheme, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) themePreset.surface else devTheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) themePreset.primary else devTheme.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsManager.setAppTheme(themePreset.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = themePreset.icon, fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = themePreset.name,
                                            color = if (isSelected) themePreset.primary else devTheme.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        // Color Swatches
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(themePreset.primary))
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(themePreset.secondary))
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(themePreset.tertiary))
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(themePreset.codeBg))
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active Theme",
                                        tint = themePreset.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3C: Hybrid GPU + CPU Offload & Low-Memory OOM Guard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NeonCyanDim, RoundedCornerShape(12.dp))
                .testTag("gpu_cpu_hybrid_offload_card"),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonCyanSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Hybrid GPU + CPU Offload",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                GgufTag(
                                    text = if (isGpuOffloadEnabled) "GPU / CPU HYBRID" else "PURE CPU",
                                    color = if (isGpuOffloadEnabled) EmeraldGlow else TextMuted
                                )
                            }
                            Text(
                                text = "Dynamic layer offloading for large models (3B, 5B, 7B, 8B)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isGpuOffloadEnabled,
                        onCheckedChange = { settingsManager.setGpuOffloadEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ObsidianSurface
                        ),
                        modifier = Modifier.testTag("gpu_offload_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "To accommodate large neural weights without crashing or model errancy, transformer layers are offloaded to Vulkan/OpenCL GPU compute while remaining layers execute on SIMD CPU vector units.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                if (isGpuOffloadEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GPU Offloaded Layers:",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (gpuOffloadLayers < 0) "AUTO-BALANCED" else "$gpuOffloadLayers Layers",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-1 to "AUTO", 16 to "16L (50%)", 28 to "28L (85%)", 32 to "FULL").forEach { (valLayers, label) ->
                            val isSelected = gpuOffloadLayers == valLayers
                            Button(
                                onClick = { settingsManager.setGpuOffloadLayers(valLayers) },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) NeonCyan else ObsidianSurface,
                                    contentColor = if (isSelected) ObsidianBg else TextSecondary
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Low-Memory OOM Guard Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Low-Memory Watchdog & OOM Guard",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Prevents process crashes by auto-scaling KV-cache context windows when system RAM is constrained",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }

                    Switch(
                        checked = isOomGuardEnabled,
                        onCheckedChange = { settingsManager.setOomGuardEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = EmeraldGlow,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = ObsidianSurface
                        ),
                        modifier = Modifier.testTag("oom_guard_switch")
                    )
                }
            }
        }

        // Section 4: CPU & Context Engine Preferences
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Inference Engine Defaults",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "llama.cpp native execution parameters",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CPU Threads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CPU Compute Threads:", color = TextPrimary, fontSize = 12.sp)
                    Text("$cpuThreads Threads", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = cpuThreads.toFloat(),
                    onValueChange = { settingsManager.setCpuThreads(it.toInt()) },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                // Context Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Default Context Window:", color = TextPrimary, fontSize = 12.sp)
                    Text("$contextLength Tokens", color = VioletNeural, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2048, 4096, 8192).forEach { size ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (contextLength == size) NeonCyanSubtle else ObsidianSurface)
                                .border(1.dp, if (contextLength == size) NeonCyan else ObsidianBorder, RoundedCornerShape(8.dp))
                                .clickable { settingsManager.setContextLength(size) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$size",
                                color = if (contextLength == size) NeonCyan else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (contextLength == size) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Device Architecture Diagnostics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Hardware & Runtime Telemetry",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                TelemetryRow(label = "Native Engine", value = "llama.cpp b9878 (libllama.so)")
                TelemetryRow(label = "CPU Architecture", value = NativeLlamaBridge.getDeviceArchitectureSummary())
                TelemetryRow(label = "ARM NEON SIMD", value = if (NativeLlamaBridge.isNativeAbiSupported()) "Active (Hardware Vectorization)" else "Emulated")
                TelemetryRow(label = "Model Container", value = "GGUF v3 Single-File Format")

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onNavigateToBenchmark,
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ObsidianBorder)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch Speed & Latency Benchmark", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}


