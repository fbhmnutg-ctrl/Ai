package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.engine.QuantizationEngine
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.GgufTag
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDim
import com.example.ui.theme.NeonCyanSubtle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardHover
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeural
import com.example.ui.viewmodel.ModelHubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadedTemplatesScreen(
    viewModel: ModelHubViewModel,
    onModelSelectedForChat: (LocalModelEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val uploadedModels by viewModel.uploadedModels.collectAsStateWithLifecycle()
    val statusMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()
    val inspectingModel by viewModel.inspectingModel.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // System File Picker for importing GGUF files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importGgufFile(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Uploaded Templates",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "User-imported GGUF models & custom on-device weights",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Quick Unload Action
                IconButton(
                    onClick = { viewModel.unloadModel() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorder, CircleShape)
                        .testTag("unload_model_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Unload Model from RAM",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Import GGUF File
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("import_gguf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = ObsidianBg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import GGUF",
                        color = ObsidianBg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Add Custom Template
                OutlinedButton(
                    onClick = { showAddCustomDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("add_custom_template_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Template",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Model Count & Overview Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STORED TEMPLATES: ${uploadedModels.size}",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Ready for on-device inference",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // Content: Empty State vs Uploaded Templates List
            if (uploadedModels.isEmpty()) {
                UploadedTemplatesEmptyState(
                    onImportClick = { filePickerLauncher.launch("*/*") },
                    onAddTemplateClick = { showAddCustomDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uploadedModels, key = { it.id }) { model ->
                        UploadedTemplateCard(
                            model = model,
                            onChatClick = { onModelSelectedForChat(model) },
                            onInspectClick = { viewModel.inspectModel(model) },
                            onDeleteClick = { viewModel.deleteModel(model) },
                            onUnloadClick = { viewModel.unloadModel() }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }

        // SnackBar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Custom Template Dialog
        if (showAddCustomDialog) {
            AddCustomTemplateDialog(
                onDismiss = { showAddCustomDialog = false },
                onAdd = { name, arch, quant, paramCount, url ->
                    viewModel.addCustomTemplate(
                        name = name,
                        architecture = arch,
                        quantization = quant,
                        parameterCount = paramCount,
                        downloadUrl = url
                    )
                    showAddCustomDialog = false
                }
            )
        }

        // GGUF Inspector Dialog
        inspectingModel?.let { model ->
            UploadedModelInspectorDialog(
                model = model,
                onDismiss = { viewModel.inspectModel(null) },
                onChat = {
                    viewModel.inspectModel(null)
                    onModelSelectedForChat(model)
                },
                onUpdateArchitecture = { newArch ->
                    viewModel.updateModelArchitecture(model, newArch)
                }
            )
        }
    }
}

@Composable
fun UploadedTemplatesEmptyState(
    onImportClick: () -> Unit,
    onAddTemplateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(NeonCyanSubtle)
                .border(1.dp, NeonCyanDim, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Uploaded Templates Yet",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "This page stores models and templates imported from your phone or external links. Import a GGUF file or add a model link to load on-device models.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onImportClick,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("empty_state_import_button")
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import GGUF File From Phone", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onAddTemplateClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("empty_state_add_template_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Custom Template", color = TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun UploadedTemplateCard(
    model: LocalModelEntity,
    onChatClick: () -> Unit,
    onInspectClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUnloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .testTag("uploaded_model_card_${model.id}"),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: Name & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = model.filename,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GgufTag(text = model.architecture.uppercase(), color = VioletNeural)
                    GgufTag(text = model.quantization, color = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spec row: Params, RAM, Size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PARAMS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(model.parameterCount, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("EST. RAM", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("~${model.requiredRamMb} MB", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("STORAGE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    val sizeMb = (model.sizeBytes / (1024 * 1024)).toInt()
                    val sizeStr = if (sizeMb > 1024) String.format("%.2f GB", sizeMb / 1024f) else "$sizeMb MB"
                    Text(sizeStr, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("STATUS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("On-Device", color = EmeraldGlow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary: Chat with Template
                Button(
                    onClick = onChatClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("chat_with_template_${model.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ObsidianBg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Run in Chat",
                        color = ObsidianBg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Unload from RAM
                OutlinedButton(
                    onClick = onUnloadClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("unload_template_${model.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Unload",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Unload",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Inspect GGUF Details
                IconButton(
                    onClick = onInspectClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Inspect GGUF",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Delete Template
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Template",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCustomTemplateDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, architecture: String, quantization: String, parameterCount: String, url: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var architecture by remember { mutableStateOf("llama") }
    var quantization by remember { mutableStateOf("Q6_K_P") }
    var parameterCount by remember { mutableStateOf("1.5B") }
    var downloadUrl by remember { mutableStateOf("") }

    val commonArchitectures = listOf("gemma", "llama", "qwen2", "mistral", "deepseek", "phi3")
    val commonQuants = listOf("Q6_K_P", "Q6_K", "Q8_0", "Q5_K_M", "Q4_K_M", "IQ4_NL", "IQ3_XXS", "Q3_K_M", "FP16")
    val quantInfo = QuantizationEngine.find(quantization)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Text("Add Custom Template", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure a custom GGUF template profile to load on-device with exact llama.cpp precision parameters.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        val lower = it.lowercase()
                        if ("gemma" in lower || "gamma" in lower || "codegemma" in lower || "paligemma" in lower) {
                            architecture = "gemma"
                        } else if ("qwen" in lower) {
                            architecture = "qwen2"
                        } else if ("deepseek" in lower) {
                            architecture = "deepseek"
                        } else if ("mistral" in lower || "mixtral" in lower) {
                            architecture = "mistral"
                        } else if ("phi" in lower) {
                            architecture = "phi3"
                        }
                        val extracted = QuantizationEngine.extractFromFilename(it)
                        if (extracted != "Q4_K_M") {
                            quantization = extracted
                        }
                    },
                    label = { Text("Template Name", color = TextSecondary, fontSize = 11.sp) },
                    placeholder = { Text("e.g., Gemma 2 2B Q6_K_P Custom", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Architecture Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Model Architecture:", color = TextMuted, fontSize = 10.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(commonArchitectures) { archItem ->
                            val isSelected = architecture.equals(archItem, ignoreCase = true)
                            Surface(
                                modifier = Modifier.clickable { architecture = archItem },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) NeonCyanSubtle else ObsidianSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) NeonCyan else ObsidianBorder
                                )
                            ) {
                                Text(
                                    text = archItem.uppercase(),
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = architecture,
                        onValueChange = { architecture = it },
                        label = { Text("Architecture", color = TextSecondary, fontSize = 10.sp) },
                        placeholder = { Text("gemma, llama, qwen2, deepseek", color = TextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = quantization,
                        onValueChange = { quantization = it },
                        label = { Text("Quantization", color = TextSecondary, fontSize = 10.sp) },
                        placeholder = { Text("Q6_K_P, Q8_0, IQ4_NL", color = TextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Quantization Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Quantization Level:", color = TextMuted, fontSize = 10.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(commonQuants) { q ->
                            val isSelected = quantization.equals(q, ignoreCase = true)
                            val qMeta = QuantizationEngine.find(q)
                            val badgeColor = Color(qMeta.category.badgeColorHex)
                            Surface(
                                modifier = Modifier.clickable { quantization = q },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) badgeColor.copy(alpha = 0.25f) else ObsidianSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) badgeColor else ObsidianBorder
                                )
                            ) {
                                Text(
                                    text = q,
                                    color = if (isSelected) badgeColor else TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Quantization Real-Time Detail Card
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(quantInfo.category.badgeColorHex).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${quantInfo.displayName} (${quantInfo.bitsPerWeight} bpw)",
                                color = Color(quantInfo.category.badgeColorHex),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = quantInfo.qualityTier,
                                color = TextPrimary,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = quantInfo.description,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = parameterCount,
                    onValueChange = { parameterCount = it },
                    label = { Text("Parameter Count", color = TextSecondary, fontSize = 11.sp) },
                    placeholder = { Text("e.g. 135M, 1B, 1.5B, 3B, 7B", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { 
                        downloadUrl = it
                        val extracted = QuantizationEngine.extractFromFilename(it)
                        if (extracted != "Q4_K_M") {
                            quantization = extracted
                        }
                    },
                    label = { Text("Hugging Face / GGUF URL (Optional)", color = TextSecondary, fontSize = 11.sp) },
                    placeholder = { Text("https://huggingface.co/...", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            name.trim(),
                            architecture.trim().ifEmpty { "llama" },
                            quantization.trim().ifEmpty { "Q6_K_P" },
                            parameterCount.trim().ifEmpty { "1.5B" },
                            downloadUrl.trim().ifEmpty { null }
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Add Template", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun UploadedModelInspectorDialog(
    model: LocalModelEntity,
    onDismiss: () -> Unit,
    onChat: () -> Unit,
    onUpdateArchitecture: (String) -> Unit
) {
    val commonArchitectures = listOf("gemma", "llama", "qwen2", "mistral", "deepseek", "phi3")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Column {
                Text(
                    text = "GGUF Architecture Inspector",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = model.name,
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InspectorRow(label = "Filename", value = model.filename)
                InspectorRow(label = "Active Architecture", value = model.architecture.uppercase())
                InspectorRow(label = "Quantization", value = model.quantization)
                InspectorRow(label = "Context Length", value = "${model.contextLength} tokens")
                InspectorRow(label = "Parameters", value = model.parameterCount)
                InspectorRow(label = "Est. RAM", value = "${model.requiredRamMb} MB")
                InspectorRow(label = "Storage Size", value = String.format("%.1f MB", model.sizeBytes / (1024.0 * 1024.0)))
                InspectorRow(label = "Local Path", value = model.filePath ?: "In-App Memory Map")
                InspectorRow(label = "Engine Pipeline", value = "llama.cpp ARM64 NEON")

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Reassign Template Architecture:", color = TextMuted, fontSize = 10.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(commonArchitectures) { archItem ->
                        val isCurrent = model.architecture.equals(archItem, ignoreCase = true)
                        Surface(
                            modifier = Modifier.clickable {
                                if (!isCurrent) onUpdateArchitecture(archItem)
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
                onClick = onChat,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Launch Chat", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun InspectorRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
