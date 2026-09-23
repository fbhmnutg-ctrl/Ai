package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.HuggingFaceModel
import com.example.data.remote.ModelArchitectureClass
import com.example.ui.theme.LocalDevTheme
import com.example.ui.viewmodel.HuggingFaceViewModel

@Composable
fun HuggingFaceScreen(
    viewModel: HuggingFaceViewModel,
    onNavigateToModelHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val devTheme = LocalDevTheme.current
    val context = LocalContext.current
    val models by viewModel.models.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedClass by viewModel.selectedClass.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val inspectingModel by viewModel.inspectingHfModel.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(devTheme.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = devTheme.surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(devTheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "HuggingFace",
                                    tint = devTheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Hugging Face Explorer",
                                    color = devTheme.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Text models under 4 Billion parameters (< 4B)",
                                    color = devTheme.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.fetchModels() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = devTheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = devTheme.textSecondary
                                )
                            }
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Search full name, author, or keyword (gemma, qwen, 1.5b)...",
                                color = devTheme.textMuted,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = devTheme.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = devTheme.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hf_search_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = devTheme.primary,
                            unfocusedBorderColor = devTheme.border,
                            focusedContainerColor = devTheme.bg,
                            unfocusedContainerColor = devTheme.bg,
                            focusedTextColor = devTheme.textPrimary,
                            unfocusedTextColor = devTheme.textPrimary
                        ),
                        singleLine = true
                    )

                    // Architecture Filter Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ModelArchitectureClass.entries.toTypedArray()) { archItem ->
                            val isSelected = selectedClass == archItem
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedClass(archItem) },
                                label = {
                                    Text(
                                        text = archItem.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = devTheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = devTheme.primary,
                                    labelColor = devTheme.textSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) devTheme.primary else devTheme.border
                                )
                            )
                        }
                    }
                }
            }

            // Model List
            if (models.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No models found matching your query",
                            color = devTheme.textSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Try adjusting your search or switching architecture class",
                            color = devTheme.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "SHOWING ${models.size} TEXT MODELS (< 4B PARAMETERS)",
                            color = devTheme.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    items(models, key = { it.id }) { hfModel ->
                        HuggingFaceModelCard(
                            model = hfModel,
                            onInspect = { viewModel.inspectModel(hfModel) },
                            onAddToHub = {
                                viewModel.addModelToLocalHub(hfModel)
                            },
                            onDirectDownload = {
                                viewModel.downloadDirectlyFromHf(hfModel)
                            },
                            onCopyId = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("HuggingFace Model ID", hfModel.id)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied: ${hfModel.id}", Toast.LENGTH_SHORT).show()
                            },
                            onOpenHf = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${hfModel.id}"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        // Inspection & Quant Selection Dialog
        inspectingModel?.let { model ->
            HuggingFaceInspectDialog(
                model = model,
                onDismiss = { viewModel.inspectModel(null) },
                onAddWithQuant = { quant ->
                    viewModel.addModelToLocalHub(model, quant)
                    viewModel.inspectModel(null)
                },
                onDirectDownloadWithQuant = { quant ->
                    viewModel.downloadDirectlyFromHf(model, quant)
                    viewModel.inspectModel(null)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun HuggingFaceModelCard(
    model: HuggingFaceModel,
    onInspect: () -> Unit,
    onAddToHub: () -> Unit,
    onDirectDownload: () -> Unit,
    onCopyId: () -> Unit,
    onOpenHf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val devTheme = LocalDevTheme.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, devTheme.border, RoundedCornerShape(12.dp))
            .testTag("hf_model_card_${model.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = devTheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Author badge + Parameter badge + GGUF badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = devTheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = model.parameterCount,
                            color = devTheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = devTheme.bg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border)
                    ) {
                        Text(
                            text = model.architectureClass.uppercase(),
                            color = devTheme.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (model.hasGguf) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "GGUF READY",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Stats: Downloads and Likes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Downloads",
                            tint = devTheme.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatCount(model.downloads),
                            color = devTheme.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Likes",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatCount(model.likes),
                            color = devTheme.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Full Model Name Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInspect() }
            ) {
                Text(
                    text = model.id, // Full model name: e.g. "google/gemma-2-2b-it"
                    color = devTheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (model.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = model.description,
                        color = devTheme.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopyId,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Model Name",
                            tint = devTheme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenHf,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = "Open Hugging Face",
                            tint = devTheme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onInspect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = devTheme.bg,
                            contentColor = devTheme.textPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Quants", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDirectDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("direct_download_btn_${model.id}")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Download GGUF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HuggingFaceInspectDialog(
    model: HuggingFaceModel,
    onDismiss: () -> Unit,
    onAddWithQuant: (String) -> Unit,
    onDirectDownloadWithQuant: (String) -> Unit = {}
) {
    val devTheme = LocalDevTheme.current
    var selectedQuant by remember { mutableStateOf("Q4_K_M") }
    val quants = listOf("Q4_K_M", "Q5_K_M", "Q6_K", "Q8_0", "IQ4_NL")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = devTheme.surface,
        title = {
            Column {
                Text(
                    text = "HuggingFace Model Details",
                    color = devTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = model.id,
                    color = devTheme.primary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = model.description,
                    color = devTheme.textSecondary,
                    fontSize = 12.sp
                )

                Surface(
                    color = devTheme.bg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HfDetailRow("Full Name", model.id)
                        HfDetailRow("Author / Org", model.author)
                        HfDetailRow("Parameter Count", "${model.parameterCount} (< 4B Verified)")
                        HfDetailRow("Architecture", model.architectureClass.uppercase())
                        HfDetailRow("Task Pipeline", model.pipelineTag)
                        HfDetailRow("Total Downloads", "${model.downloads}")
                        HfDetailRow("Likes", "${model.likes}")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select Quantization for Mobile Hub:",
                    color = devTheme.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quants) { quant ->
                        val isSelected = selectedQuant == quant
                        Surface(
                            modifier = Modifier.clickable { selectedQuant = quant },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) devTheme.primary.copy(alpha = 0.15f) else devTheme.bg,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.dp else 0.5.dp,
                                if (isSelected) devTheme.primary else devTheme.border
                            )
                        ) {
                            Text(
                                text = quant,
                                color = if (isSelected) devTheme.primary else devTheme.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onAddWithQuant(selectedQuant) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = devTheme.bg,
                        contentColor = devTheme.textPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Only", fontSize = 11.sp)
                }

                Button(
                    onClick = { onDirectDownloadWithQuant(selectedQuant) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Direct Download ($selectedQuant)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = devTheme.bg,
                    contentColor = devTheme.textSecondary
                )
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun HfDetailRow(label: String, value: String) {
    val devTheme = LocalDevTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = devTheme.textMuted, fontSize = 11.sp)
        Text(
            text = value,
            color = devTheme.textPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
