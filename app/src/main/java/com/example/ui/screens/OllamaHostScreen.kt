package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.data.local.entity.LocalModelEntity
import com.example.data.ollama.OllamaModelTag
import com.example.ui.components.GgufTag
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanSubtle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeural
import com.example.ui.viewmodel.OllamaConnectionState
import com.example.ui.viewmodel.OllamaHostViewModel

@Composable
fun OllamaHostScreen(
    viewModel: OllamaHostViewModel,
    onSelectModelForChat: (LocalModelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val hostUrl by viewModel.hostUrl.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val remoteModels by viewModel.remoteModels.collectAsStateWithLifecycle()
    val pullProgress by viewModel.pullProgress.collectAsStateWithLifecycle()

    var pullModelInput by remember { mutableStateOf("") }
    var hostUrlInput by remember { mutableStateOf(hostUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Setup Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lan, contentDescription = null, tint = VioletNeural, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Ollama Host Connection",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Connection Badge
                    when (val state = connectionState) {
                        is OllamaConnectionState.Connected -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(EmeraldGlow.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldGlow))
                                Text("${state.latencyMs}ms • v${state.version}", color = EmeraldGlow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        is OllamaConnectionState.Connecting -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                        }
                        is OllamaConnectionState.Error -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                Text("Offline", color = Color(0xFFEF4444), fontSize = 11.sp)
                            }
                        }
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Connect to Ollama running on localhost (Termux), your PC on Wi-Fi, or remote server:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hostUrlInput,
                        onValueChange = {
                            hostUrlInput = it
                            viewModel.setHostUrl(it)
                        },
                        placeholder = { Text("http://192.168.1.x:11434", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ollama_host_url_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioletNeural,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = { viewModel.testConnection() },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletNeural),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("ollama_test_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Preset quick buttons
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OllamaUrlQuickPill(label = "Localhost (Termux)") {
                        hostUrlInput = "http://127.0.0.1:11434"
                        viewModel.setHostUrl(hostUrlInput)
                        viewModel.testConnection()
                    }
                    OllamaUrlQuickPill(label = "Emulator Host (10.0.2.2)") {
                        hostUrlInput = "http://10.0.2.2:11434"
                        viewModel.setHostUrl(hostUrlInput)
                        viewModel.testConnection()
                    }
                }
            }
        }

        // Pull Model Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pull Model from Registry",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Instruct Ollama server to pull models (e.g., llama3.2:1b, deepseek-r1:1.5b):",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pullModelInput,
                        onValueChange = { pullModelInput = it },
                        placeholder = { Text("llama3.2:1b", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ollama_pull_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (pullModelInput.isNotBlank()) {
                                viewModel.pullModel(pullModelInput.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyanSubtle),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("ollama_pull_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pull", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Pull Progress Indicator
                if (pullProgress != null && pullProgress!!.isPulling) {
                    val p = pullProgress!!
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${p.modelName}: ${p.status}",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            TextButton(
                                onClick = { viewModel.cancelPull() },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Cancel", color = Color(0xFFEF4444), fontSize = 11.sp)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { p.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NeonCyan,
                            trackColor = ObsidianBorder
                        )
                    }
                }
            }
        }

        // Remote Models List
        Text(
            text = "MODELS AVAILABLE ON OLLAMA SERVER (${remoteModels.size})",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        if (remoteModels.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No remote models detected yet", color = TextSecondary, fontSize = 13.sp)
                    Text("Connect to your Ollama server or pull a model above", color = TextMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(remoteModels, key = { it.name }) { tag ->
                    RemoteModelCard(
                        tag = tag,
                        onSelectForChat = {
                            val entity = LocalModelEntity(
                                id = tag.name,
                                name = tag.name,
                                filename = tag.name,
                                architecture = tag.details?.family ?: "llama",
                                quantization = tag.details?.quantizationLevel ?: "Q4_K_M",
                                parameterCount = tag.details?.parameterSize ?: "3B",
                                sizeBytes = tag.size,
                                requiredRamMb = 2000,
                                contextLength = 4096,
                                isDownloaded = true,
                                downloadProgress = 1.0f,
                                source = "OLLAMA",
                                description = "Served via Ollama REST API"
                            )
                            onSelectModelForChat(entity)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RemoteModelCard(
    tag: OllamaModelTag,
    onSelectForChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = tag.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    GgufTag(text = tag.details?.quantizationLevel ?: "GGUF", color = VioletNeural)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Family: ${tag.details?.family ?: "llama"} • Size: ${String.format("%.2f GB", tag.size / (1024.0 * 1024.0 * 1024.0))}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = onSelectForChat,
                colors = ButtonDefaults.buttonColors(containerColor = VioletNeural),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Chat", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OllamaUrlQuickPill(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = ObsidianCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
