package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.engine.QuantizationEngine
import com.example.engine.QuantCategory
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
import com.example.engine.DeviceHardwareInfo
import com.example.engine.GgufMetadata
import com.example.ui.components.EngineBadge
import com.example.ui.components.GgufTag
import com.example.ui.components.HuggingFaceBadge
import com.example.ui.components.RamIndicatorMeter
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
import com.example.ui.viewmodel.ModelFilter
import com.example.ui.viewmodel.ModelHubViewModel

@Composable
fun ModelHubScreen(
    viewModel: ModelHubViewModel,
    onModelSelectedForChat: (LocalModelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allModels by viewModel.allModels.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val hardwareInfo by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    val importedMetadata by viewModel.importedGgufMetadata.collectAsStateWithLifecycle()
    val inspectingModel by viewModel.inspectingModel.collectAsStateWithLifecycle()
    var showQuantGuideDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importGgufFile(uri)
        }
    }

    val filteredModels = allModels.filter { model ->
        val matchesFilter = when (selectedFilter) {
            ModelFilter.ALL -> true
            ModelFilter.INSTALLED -> model.isDownloaded
            ModelFilter.CATALOG -> !model.isDownloaded
        }
        val matchesSearch = model.name.contains(searchQuery, ignoreCase = true) ||
                model.architecture.contains(searchQuery, ignoreCase = true) ||
                model.quantization.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Hardware Status Dashboard Card
        HardwareStatusHeader(
            hardwareInfo = hardwareInfo,
            onImportGgufClick = { filePickerLauncher.launch("*/*") },
            onQuantGuideClick = { showQuantGuideDialog = true }
        )

        // Filters and Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search models, arch, or quant (Q6_K_P, Q8_0, IQ4_NL)...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("model_search_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == ModelFilter.ALL,
                    onClick = { viewModel.setFilter(ModelFilter.ALL) },
                    label = { Text("All Models (${allModels.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyanSubtle,
                        selectedLabelColor = NeonCyan,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilter == ModelFilter.INSTALLED,
                    onClick = { viewModel.setFilter(ModelFilter.INSTALLED) },
                    label = { Text("Installed (${allModels.count { it.isDownloaded }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyanSubtle,
                        selectedLabelColor = NeonCyan,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = selectedFilter == ModelFilter.CATALOG,
                    onClick = { viewModel.setFilter(ModelFilter.CATALOG) },
                    label = { Text("Downloadable") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyanSubtle,
                        selectedLabelColor = NeonCyan,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Models List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredModels, key = { it.id }) { model ->
                ModelCard(
                    model = model,
                    hardwareInfo = hardwareInfo,
                    onSelect = { onModelSelectedForChat(model) },
                    onDownload = { viewModel.startModelDownload(model) },
                    onCancelDownload = { viewModel.cancelDownload(model.id) },
                    onDelete = { viewModel.deleteModel(model) },
                    onInspect = { viewModel.inspectModel(model) }
                )
            }
        }
    }

    // Quantization Guide Dialog
    if (showQuantGuideDialog) {
        QuantizationMatrixDialog(
            onDismiss = { showQuantGuideDialog = false }
        )
    }

    // GGUF Import Confirmation Dialog
    if (importedMetadata != null) {
        val meta = importedMetadata!!
        val quantInfo = QuantizationEngine.find(meta.quantization)
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportDialog() },
            containerColor = ObsidianCard,
            title = {
                Text(
                    text = if (meta.isValid) "GGUF Model Detected" else "Import Failed",
                    color = if (meta.isValid) NeonCyan else Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (meta.isValid) {
                        Text(
                            text = "Successfully verified GGUF container format from device storage.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        GgufDetailRow("Model Name", meta.modelName)
                        GgufDetailRow("Architecture", meta.architecture)
                        GgufDetailRow("Quantization", "${meta.quantization} (${quantInfo.bitsPerWeight} bpw)")
                        GgufDetailRow("Quality Tier", quantInfo.qualityTier)
                        GgufDetailRow("File Size", meta.fileSizeFormatted)
                        GgufDetailRow("Tensors Count", "${meta.tensorCount}")
                        GgufDetailRow("Layers", "${meta.layerCount}")
                        GgufDetailRow("Context Window", "${meta.contextLength} tokens")
                        GgufDetailRow("Est. RAM Required", "~${meta.estimatedRamRequiredMb} MB")
                    } else {
                        Text(
                            text = meta.parseErrorMessage ?: "Selected file is not a valid GGUF container.",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissImportDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("OK", color = Color(0xFF00363D))
                }
            }
        )
    }

    // Model Inspect Dialog
    if (inspectingModel != null) {
        val m = inspectingModel!!
        val quantInfo = QuantizationEngine.find(m.quantization)
        AlertDialog(
            onDismissRequest = { viewModel.inspectModel(null) },
            containerColor = ObsidianCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = m.name, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                    GgufTag(
                        text = m.quantization,
                        color = Color(quantInfo.category.badgeColorHex)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = m.description, color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Quantization Deep-Dive Box
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(quantInfo.category.badgeColorHex), modifier = Modifier.size(16.dp))
                                Text(text = quantInfo.displayName, color = Color(quantInfo.category.badgeColorHex), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "• Fidelity: ${quantInfo.qualityTier} (~${quantInfo.bitsPerWeight} bits/weight)", color = TextPrimary, fontSize = 11.sp)
                            Text(text = "• Math & Logic: ${quantInfo.description}", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "• Ideal For: ${quantInfo.recommendedUse}", color = TextMuted, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    GgufDetailRow("Architecture", m.architecture.uppercase())
                    GgufDetailRow("Quantization", m.quantization)
                    GgufDetailRow("Parameters", m.parameterCount)
                    GgufDetailRow("Size on Disk", String.format("%.2f GB", m.sizeBytes / (1024.0 * 1024.0 * 1024.0)))
                    GgufDetailRow("RAM Footprint", "~${m.requiredRamMb} MB")
                    GgufDetailRow("Context Length", "${m.contextLength} tokens")
                    GgufDetailRow("Source", m.source)
                    if (m.filePath != null) {
                        GgufDetailRow("Path", m.filePath)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Reassign Architecture Template:", color = TextMuted, fontSize = 10.sp)
                    val commonArchitectures = listOf("gemma", "llama", "qwen2", "mistral", "deepseek", "phi3")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(commonArchitectures) { archItem ->
                            val isCurrent = m.architecture.equals(archItem, ignoreCase = true)
                            Surface(
                                modifier = Modifier.clickable {
                                    if (!isCurrent) viewModel.updateModelArchitecture(m, archItem)
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCurrent) NeonCyanSubtle else ObsidianSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isCurrent) 1.dp else 0.5.dp,
                                    if (isCurrent) NeonCyan else ObsidianBorder
                                )
                            ) {
                                Text(
                                    text = archItem.uppercase(),
                                    color = if (isCurrent) NeonCyan else TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.inspectModel(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Close", color = Color(0xFF00363D))
                }
            }
        )
    }
}

@Composable
fun QuantizationMatrixDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Text("GGUF Quantization Matrix", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Supported quantization levels in llama.cpp engine. Quantization maps high-precision float weights into discrete bit representations to fit larger models into device RAM without erratic behavior.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                QuantizationEngine.ALL_QUANTS.forEach { q ->
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(q.category.badgeColorHex).copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = q.displayName,
                                    color = Color(q.category.badgeColorHex),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = Color(q.category.badgeColorHex).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${q.bitsPerWeight} bpw",
                                        color = Color(q.category.badgeColorHex),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Quality: ${q.qualityTier}",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = q.description,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "Recommended: ${q.recommendedUse}",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Close", color = Color(0xFF00363D))
            }
        }
    )
}

@Composable
fun HardwareStatusHeader(
    hardwareInfo: DeviceHardwareInfo,
    onImportGgufClick: () -> Unit,
    onQuantGuideClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Memory & Compute",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${hardwareInfo.deviceName} • ${hardwareInfo.cpuCores} CPU Cores (${hardwareInfo.supportedAbis})",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Quants Guide Button
                    Button(
                        onClick = onQuantGuideClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletNeural.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = VioletNeural, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quants", color = VioletNeural, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Import GGUF button
                    Button(
                        onClick = onImportGgufClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyanSubtle),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("import_gguf_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import .GGUF", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // RAM Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RAM Available: ${hardwareInfo.availableRamMb} MB / ${hardwareInfo.totalRamMb} MB",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${hardwareInfo.usedRamPercent}% in use",
                    color = if (hardwareInfo.usedRamPercent > 80) Color(0xFFF59E0B) else EmeraldGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { hardwareInfo.usedRamPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (hardwareInfo.usedRamPercent > 80) Color(0xFFF59E0B) else EmeraldGlow,
                trackColor = ObsidianBorder
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Optimal fit: ${hardwareInfo.recommendedMaxModelParam}",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ModelCard(
    model: LocalModelEntity,
    hardwareInfo: DeviceHardwareInfo,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHf = model.source == "HUGGING_FACE"
    val quantInfo = QuantizationEngine.find(model.quantization)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isHf) Color(0xFFFF9D00).copy(alpha = 0.4f) else ObsidianBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = model.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    GgufTag(
                        text = model.quantization,
                        color = if (isHf) Color(0xFFFFB347) else Color(quantInfo.category.badgeColorHex)
                    )
                    if (isHf) {
                        HuggingFaceBadge(text = "Hugging Face")
                    }
                }

                IconButton(
                    onClick = onInspect,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Inspect model",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = model.description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Specs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Arch: ${model.architecture}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Params: ${model.parameterCount}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (model.sizeBytes < 1024L * 1024L * 1024L) {
                        String.format("%.0f MB", model.sizeBytes / (1024.0 * 1024.0))
                    } else {
                        String.format("%.2f GB", model.sizeBytes / (1024.0 * 1024.0 * 1024.0))
                    },
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RAM requirement meter
            RamIndicatorMeter(
                requiredMb = model.requiredRamMb,
                availableMb = hardwareInfo.availableRamMb,
                totalMb = hardwareInfo.totalRamMb
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row
            if (model.downloadProgress > 0f && model.downloadProgress < 1.0f) {
                // Downloading state: A loading bar appears!
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHf) {
                                "Downloading from huggingface.co... ${(model.downloadProgress * 100).toInt()}%"
                            } else {
                                "Downloading quantized weights... ${(model.downloadProgress * 100).toInt()}%"
                            },
                            color = if (isHf) Color(0xFFFFB347) else NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        TextButton(
                            onClick = onCancelDownload,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Cancel", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { model.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (isHf) Color(0xFFFF9D00) else NeonCyan,
                        trackColor = ObsidianBorder
                    )
                }
            } else if (model.isDownloaded) {
                // Ready / Installed: A "Start to try" button appears!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(16.dp))
                        Text("Installed locally", color = EmeraldGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (model.id.startsWith("imported-")) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                        Button(
                            onClick = onSelect,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag(if (isHf) "start_to_try_button" else "select_model_${model.id}")
                        ) {
                            if (isHf) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isHf) "Start to try" else "Load & Chat",
                                color = Color(0xFF00363D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Not downloaded yet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHf) Color(0xFFFF9D00).copy(alpha = 0.2f) else NeonCyanSubtle
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHf) Color(0xFFFF9D00) else NeonCyan.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag(if (isHf) "download_hf_template_button" else "download_model_${model.id}")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = if (isHf) Color(0xFFFFB347) else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHf) "Download Test Template" else "Download Model",
                            color = if (isHf) Color(0xFFFFB347) else NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GgufDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
