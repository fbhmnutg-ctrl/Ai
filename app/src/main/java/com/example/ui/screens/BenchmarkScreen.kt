package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.viewmodel.BenchmarkResult
import com.example.ui.viewmodel.BenchmarkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    viewModel: BenchmarkViewModel,
    modifier: Modifier = Modifier
) {
    val downloadedModels by viewModel.downloadedModels.collectAsStateWithLifecycle()
    val isBenchmarking by viewModel.isBenchmarking.collectAsStateWithLifecycle()
    val benchmarkProgress by viewModel.benchmarkProgress.collectAsStateWithLifecycle()
    val currentPhase by viewModel.currentPhase.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val hardwareInfo by viewModel.hardwareInfo.collectAsStateWithLifecycle()

    var selectedModel by remember(downloadedModels) {
        mutableStateOf(downloadedModels.firstOrNull())
    }
    var threadCount by remember { mutableStateOf(4) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Benchmark Config Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hardware Inference Benchmark",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Test token throughput and prompt evaluation speed on ${hardwareInfo.cpuCores}-core CPU",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Model Selector
                Text("Select Model to Benchmark", color = TextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedModel?.name ?: "No models installed",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(ObsidianCard)
                    ) {
                        downloadedModels.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${model.name} (${model.quantization})",
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    selectedModel = model
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Threads Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Compute Threads", color = TextSecondary, fontSize = 13.sp)
                    Text("$threadCount Cores", color = EmeraldGlow, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = threadCount.toFloat(),
                    onValueChange = { threadCount = it.toInt() },
                    valueRange = 1f..hardwareInfo.cpuCores.toFloat().coerceAtLeast(4f),
                    steps = (hardwareInfo.cpuCores - 2).coerceAtLeast(0),
                    enabled = !isBenchmarking,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldGlow,
                        activeTrackColor = EmeraldGlow,
                        inactiveTrackColor = ObsidianBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Run Benchmark Button
                Button(
                    onClick = {
                        selectedModel?.let { viewModel.runBenchmark(it, threadCount) }
                    },
                    enabled = !isBenchmarking && selectedModel != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("run_benchmark_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    if (isBenchmarking) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF00363D), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Benchmarking...", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00363D))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Benchmark CUJ", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                    }
                }

                // Benchmark Progress
                if (isBenchmarking) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = currentPhase,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        LinearProgressIndicator(
                            progress = { benchmarkProgress },
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

        // Benchmark Results Section
        Text(
            text = "BENCHMARK RESULTS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        if (results.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No benchmarks executed yet", color = TextSecondary, fontSize = 13.sp)
                    Text("Select a model and press 'Run Benchmark CUJ'", color = TextMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(results) { res ->
                    BenchmarkResultCard(result = res)
                }
            }
        }
    }
}

@Composable
fun BenchmarkResultCard(result: BenchmarkResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(result.modelName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    GgufTag(result.quantization)
                }
                Text("${result.threadCount} Threads", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Generation Speed
                Column {
                    Text("Generation Speed", color = TextMuted, fontSize = 11.sp)
                    Text(
                        "${result.genSpeedTokPerSec} tok/s",
                        color = EmeraldGlow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Prompt Eval Speed
                Column {
                    Text("Prompt Eval", color = TextMuted, fontSize = 11.sp)
                    Text(
                        "${result.promptEvalSpeedTokPerSec} tok/s",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // TTFT
                Column {
                    Text("TTFT", color = TextMuted, fontSize = 11.sp)
                    Text(
                        "${result.timeToFirstTokenMs} ms",
                        color = Color(0xFFFBBF24),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Peak RAM
                Column {
                    Text("RAM", color = TextMuted, fontSize = 11.sp)
                    Text(
                        "${result.memoryUsageMb} MB",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
