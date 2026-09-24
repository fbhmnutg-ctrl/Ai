package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SettingsManager
import com.example.data.local.entity.ChatMessage
import com.example.data.local.entity.LocalModelEntity
import com.example.engine.LocalInferenceEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.ui.components.CodeBlockView
import com.example.ui.theme.AllDevThemes
import com.example.ui.theme.DevThemeColors
import com.example.ui.theme.LocalDevTheme
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToModels: () -> Unit,
    onNavigateToOllama: () -> Unit,
    onNavigateToUploaded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val devTheme = LocalDevTheme.current
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val currentAppTheme by settingsManager.appTheme.collectAsStateWithLifecycle()

    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val activeModel by viewModel.activeModel.collectAsStateWithLifecycle()
    val selectedEngine by viewModel.selectedEngine.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val templateSelectionList by viewModel.templateSelectionList.collectAsStateWithLifecycle()
    val isIntegratedThinkEnabled by viewModel.isIntegratedThinkEnabled.collectAsStateWithLifecycle()
    val modelLoadingState by viewModel.modelLoadingState.collectAsStateWithLifecycle()

    val temperature by viewModel.temperature.collectAsStateWithLifecycle()
    val topP by viewModel.topP.collectAsStateWithLifecycle()
    val cpuThreads by viewModel.cpuThreads.collectAsStateWithLifecycle()
    val systemPrompt by viewModel.systemPrompt.collectAsStateWithLifecycle()
    val showThinkingProcess by viewModel.showThinkingProcess.collectAsStateWithLifecycle()
    val maxTokens by viewModel.maxTokens.collectAsStateWithLifecycle()
    val topK by viewModel.topK.collectAsStateWithLifecycle()
    val repeatPenalty by viewModel.repeatPenalty.collectAsStateWithLifecycle()

    var showParamsSheet by remember { mutableStateOf(false) }
    var showSessionsMenu by remember { mutableStateOf(false) }
    var showModelSelectorMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var userManuallyScrolledUp by remember { mutableStateOf(false) }

    val isAtBottom by remember {
        derivedStateOf {
            val totalItems = messages.size + (if (generationState.isGenerating) 1 else 0)
            if (totalItems <= 1) true
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= totalItems - 2
            }
        }
    }

    // Detect if user is manually scrolling up
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            val totalItems = messages.size + (if (generationState.isGenerating) 1 else 0)
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (totalItems > 1 && lastVisible < totalItems - 2) {
                userManuallyScrolledUp = true
            } else if (lastVisible >= totalItems - 1) {
                userManuallyScrolledUp = false
            }
        }
    }

    // Auto-scroll when new user message is submitted
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastUserMsg = messages.lastOrNull()?.role == "user"
            if (lastUserMsg) {
                userManuallyScrolledUp = false
            }
            if (!userManuallyScrolledUp) {
                val totalItems = messages.size + (if (generationState.isGenerating) 1 else 0)
                listState.scrollToItem((totalItems - 1).coerceAtLeast(0))
            }
        }
    }

    // While streaming tokens, keep scrolled to bottom ONLY if user hasn't scrolled up
    LaunchedEffect(generationState.tokensGenerated) {
        if (generationState.isGenerating && !userManuallyScrolledUp) {
            val totalItems = messages.size + 1
            listState.scrollToItem((totalItems - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(devTheme.bg)
    ) {
        // Minimalist Top Developer Header with Thinking Process Toggle
        MinimalTopChatHeader(
            activeModel = activeModel,
            selectedEngine = selectedEngine,
            isLoadedInMemory = modelLoadingState.isLoadedInMemory,
            currentThemeIcon = devTheme.icon,
            showThinkingProcess = showThinkingProcess,
            onModelClick = { showModelSelectorMenu = true },
            onThemeClick = { showThemeMenu = true },
            onToggleThinkingProcess = { viewModel.toggleThinkingProcess() },
            onNewChat = { viewModel.createNewSession() },
            onOpenSettings = { showParamsSheet = true },
            onToggleSessions = { showSessionsMenu = true }
        )

        // Dropdown Menu for Quick Theme Switching
        DropdownMenu(
            expanded = showThemeMenu,
            onDismissRequest = { showThemeMenu = false },
            modifier = Modifier
                .background(devTheme.card)
                .border(0.5.dp, devTheme.border, RoundedCornerShape(12.dp))
                .widthIn(min = 240.dp)
        ) {
            Text(
                text = "DEVELOPER THEME",
                color = devTheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            AllDevThemes.forEach { preset ->
                val isSelected = preset.id.equals(currentAppTheme, ignoreCase = true)
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(preset.icon, fontSize = 15.sp)
                            Text(
                                text = preset.name,
                                color = if (isSelected) devTheme.primary else devTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = devTheme.primary, modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        settingsManager.setAppTheme(preset.id)
                        showThemeMenu = false
                    }
                )
            }
        }

        // Dropdown Menu for Model Selection
        DropdownMenu(
            expanded = showModelSelectorMenu,
            onDismissRequest = { showModelSelectorMenu = false },
            modifier = Modifier
                .background(devTheme.card)
                .border(0.5.dp, devTheme.border, RoundedCornerShape(12.dp))
                .widthIn(min = 280.dp, max = 340.dp)
        ) {
            Text(
                text = "SELECT MODEL",
                color = devTheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val displayTemplates = templateSelectionList.take(5)
            displayTemplates.forEach { model ->
                val isSelected = activeModel?.id == model.id
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = model.name,
                                    color = if (isSelected) devTheme.primary else devTheme.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(devTheme.secondary)
                                    )
                                }
                            }
                            Text(
                                text = "${model.architecture.uppercase()} • ${model.quantization} • ${model.parameterCount}",
                                color = devTheme.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = devTheme.primary, modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        viewModel.setActiveModel(model)
                        showModelSelectorMenu = false
                    }
                )
            }

            if (displayTemplates.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text("No downloaded models yet", color = devTheme.textMuted, fontSize = 12.sp)
                    },
                    onClick = {}
                )
            }

            HorizontalDivider(color = devTheme.border, modifier = Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Text("Unload Model from RAM", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                },
                onClick = {
                    viewModel.unloadModel()
                    showModelSelectorMenu = false
                }
            )

            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = devTheme.primary, modifier = Modifier.size(16.dp))
                        Text("Manage Uploaded Models...", color = devTheme.textPrimary, fontSize = 12.sp)
                    }
                },
                onClick = {
                    showModelSelectorMenu = false
                    onNavigateToUploaded()
                }
            )

            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = devTheme.tertiary, modifier = Modifier.size(16.dp))
                        Text("Browse GGUF Hub...", color = devTheme.textPrimary, fontSize = 12.sp)
                    }
                },
                onClick = {
                    showModelSelectorMenu = false
                    onNavigateToModels()
                }
            )
        }

        // Sessions Menu
        DropdownMenu(
            expanded = showSessionsMenu,
            onDismissRequest = { showSessionsMenu = false },
            modifier = Modifier
                .background(devTheme.card)
                .border(0.5.dp, devTheme.border, RoundedCornerShape(12.dp))
        ) {
            Text(
                text = "Conversations",
                color = devTheme.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            sessions.forEach { session ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = session.title,
                                color = if (session.id == currentSessionId) devTheme.primary else devTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (session.id == currentSessionId) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${session.modelName} • ${session.engineType}",
                                color = devTheme.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    },
                    onClick = {
                        viewModel.selectSession(session.id)
                        showSessionsMenu = false
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Text("Clear Current Chat", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                },
                onClick = {
                    viewModel.clearMessages()
                    showSessionsMenu = false
                }
            )
        }

        // Model Memory Loading Bar (Sleek Developer Progress)
        AnimatedVisibility(
            visible = modelLoadingState.isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(devTheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
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
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = devTheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Loading ${modelLoadingState.modelName} into RAM...",
                            color = devTheme.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${(modelLoadingState.progress * 100).toInt()}%",
                        color = devTheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { modelLoadingState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = devTheme.primary,
                    trackColor = devTheme.border
                )
            }
        }


        // Messages List or Simple Beautiful Developer Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !generationState.isGenerating) {
                BeautifulChatEmptyState(
                    activeModel = activeModel,
                    devTheme = devTheme,
                    onPromptSelected = { prompt ->
                        viewModel.onInputTextChanged(prompt)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        CleanMessageItem(
                            message = msg,
                            devTheme = devTheme,
                            showThinkingProcess = showThinkingProcess
                        )
                    }

                    // Live streaming bubble if active
                    if (generationState.isGenerating) {
                        item {
                            CleanStreamingBubble(
                                state = generationState,
                                devTheme = devTheme,
                                showThinkingProcess = showThinkingProcess,
                                onStop = { viewModel.stopGeneration() }
                            )
                        }
                    }
                }

                // Floating Jump-to-Bottom button
                androidx.compose.animation.AnimatedVisibility(
                    visible = userManuallyScrolledUp && (generationState.isGenerating || !isAtBottom),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = devTheme.card,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.borderLight),
                        shadowElevation = 4.dp,
                        modifier = Modifier.clickable {
                            userManuallyScrolledUp = false
                            coroutineScope.launch {
                                val totalItems = messages.size + (if (generationState.isGenerating) 1 else 0)
                                listState.animateScrollToItem((totalItems - 1).coerceAtLeast(0))
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = devTheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Scroll to bottom",
                                color = devTheme.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Floating Minimalist Developer Input Bar
        CleanChatInputBar(
            text = inputText,
            isGenerating = generationState.isGenerating,
            isThinkActive = isIntegratedThinkEnabled,
            devTheme = devTheme,
            onTextChanged = { viewModel.onInputTextChanged(it) },
            onSend = { viewModel.sendMessage() },
            onStop = { viewModel.stopGeneration() },
            onToggleThink = { viewModel.toggleIntegratedThink() },
            modifier = Modifier.imePadding()
        )
    }

    // Parameters Bottom Sheet with full runtime model controls
    if (showParamsSheet) {
        CleanParametersBottomSheet(
            temperature = temperature,
            topP = topP,
            topK = topK,
            maxTokens = maxTokens,
            repeatPenalty = repeatPenalty,
            cpuThreads = cpuThreads,
            showThinkingProcess = showThinkingProcess,
            systemPrompt = systemPrompt,
            devTheme = devTheme,
            onDismiss = { showParamsSheet = false },
            onSave = { temp, p, k, maxTok, repPen, threads, showThink, prompt ->
                viewModel.updateSettings(
                    temp = temp,
                    p = p,
                    threads = threads,
                    prompt = prompt,
                    maxTokensValue = maxTok,
                    topKValue = k,
                    repeatPenaltyValue = repPen,
                    showThinking = showThink
                )
                showParamsSheet = false
            }
        )
    }
}

/**
 * Minimal top developer header with quick theme switcher.
 */
@Composable
fun MinimalTopChatHeader(
    activeModel: LocalModelEntity?,
    selectedEngine: String,
    isLoadedInMemory: Boolean,
    currentThemeIcon: String,
    showThinkingProcess: Boolean = true,
    onModelClick: () -> Unit,
    onThemeClick: () -> Unit,
    onToggleThinkingProcess: () -> Unit = {},
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSessions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val devTheme = LocalDevTheme.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = devTheme.bg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Model Selector Capsule
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(devTheme.surface)
                    .border(0.5.dp, devTheme.border, RoundedCornerShape(20.dp))
                    .clickable { onModelClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isLoadedInMemory) devTheme.secondary else devTheme.primary)
                )

                Text(
                    text = activeModel?.name ?: "Select Model",
                    color = devTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Switch Model",
                    tint = devTheme.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Clean Developer Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Theme Switcher
                IconButton(
                    onClick = onThemeClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(devTheme.surface)
                ) {
                    Text(text = currentThemeIcon, fontSize = 14.sp)
                }

                // Quick Thinking Process Demonstration Toggle
                IconButton(
                    onClick = onToggleThinkingProcess,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (showThinkingProcess) devTheme.tertiary.copy(alpha = 0.15f) else Color.Transparent)
                        .testTag("toggle_thinking_process_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = if (showThinkingProcess) "Thinking Process Visible" else "Thinking Process Hidden",
                        tint = if (showThinkingProcess) devTheme.tertiary else devTheme.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSessions,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("conversations_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = devTheme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("chat_params_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tuning",
                        tint = devTheme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(devTheme.surface)
                        .testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = devTheme.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Developer Empty State with programming prompt starters.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BeautifulChatEmptyState(
    activeModel: LocalModelEntity?,
    devTheme: DevThemeColors,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Developer Terminal Glyph
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(devTheme.surface)
                .border(0.5.dp, devTheme.primary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = devTheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Local Developer AI Assistant",
            color = devTheme.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Private on-device inference • GGUF & libllama.so native bridge",
            color = devTheme.textMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Developer Starter Chips
        val suggestions = listOf(
            "💻 Write a concurrent Kotlin Coroutine flow",
            "🐍 Implement a FastAPI async backend service",
            "🦀 Explain Rust ownership and lifetime borrow rules",
            "✍️ شرح خوارزمية البحث الثنائي وكتابة الكود"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = devTheme.surface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPromptSelected(prompt.substring(3).trim()) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prompt,
                            color = devTheme.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = devTheme.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Message item layout matching Developer IDE syntax styling.
 */
@Composable
fun CleanMessageItem(
    message: ChatMessage,
    devTheme: DevThemeColors,
    showThinkingProcess: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    val isArabic = LocalInferenceEngine.isArabicText(message.content)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Message (Developer bubble)
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = devTheme.userBubbleBg,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.userBubbleBorder),
                modifier = Modifier.padding(start = 48.dp)
            ) {
                Text(
                    text = message.content,
                    color = devTheme.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    style = LocalTextStyle.current.copy(
                        textDirection = TextDirection.ContentOrLtr,
                        textAlign = if (isArabic) TextAlign.End else TextAlign.Start
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        } else {
            // Assistant Message
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, top = 2.dp)
            ) {
                // Header (Model identity & Copy)
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
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = devTheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = message.modelTag.ifEmpty { "llama.cpp" },
                            color = devTheme.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy message",
                            tint = devTheme.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Formatted Content with Thinking Process support
                CleanFormattedAssistantContent(
                    content = message.content,
                    devTheme = devTheme,
                    showThinkingProcess = showThinkingProcess
                )

                // Stats and duration footer (discreet monospace)
                if (message.tokensPerSecond > 0f || message.generationDurationMs > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.generationDurationMs > 0L) {
                            val durationSec = message.generationDurationMs / 1000f
                            Text(
                                text = String.format(java.util.Locale.US, "⏱ %.1fs", durationSec),
                                color = devTheme.primary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (message.tokensPerSecond > 0f) {
                            Text(
                                text = "${message.tokensPerSecond} tok/s",
                                color = devTheme.secondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (message.timeToFirstTokenMs > 0) {
                            Text(
                                text = "TTFT: ${message.timeToFirstTokenMs}ms",
                                color = devTheme.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (message.tokensCount > 0) {
                            Text(
                                text = "${message.tokensCount} tokens",
                                color = devTheme.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Assistant content renderer with optional collapsible thinking process.
 */
@Composable
fun CleanFormattedAssistantContent(
    content: String,
    devTheme: DevThemeColors,
    showThinkingProcess: Boolean = true
) {
    var isThinkingExpanded by remember { mutableStateOf(false) }

    if (content.contains("<think>") && content.contains("</think>")) {
        val thinkStart = content.indexOf("<think>") + 7
        val thinkEnd = content.indexOf("</think>")
        val thinkContent = content.substring(thinkStart, thinkEnd).trim()
        val restContent = content.substring(thinkEnd + 8).trim()

        if (showThinkingProcess) {
            val isArabicThink = LocalInferenceEngine.isArabicText(thinkContent)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = devTheme.surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isThinkingExpanded = !isThinkingExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = devTheme.tertiary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isArabicThink) "مسار التفكير والتحليل (Chain-of-Thought)" else "Reasoning Trace",
                                color = devTheme.tertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = devTheme.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isThinkingExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = thinkContent,
                            color = devTheme.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = if (isArabicThink) FontFamily.Default else FontFamily.Monospace,
                            lineHeight = 18.sp,
                            style = LocalTextStyle.current.copy(
                                textDirection = TextDirection.ContentOrLtr,
                                textAlign = if (isArabicThink) TextAlign.End else TextAlign.Start
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            CleanRenderMarkdownContent(restContent, devTheme)
        } else {
            // Thinking process hidden: display cleaned response
            CleanRenderMarkdownContent(restContent.ifBlank { content }, devTheme)
        }
    } else {
        CleanRenderMarkdownContent(content, devTheme)
    }
}

/**
 * Clean markdown and code block renderer with developer syntax highlighting.
 */
@Composable
fun CleanRenderMarkdownContent(text: String, devTheme: DevThemeColors) {
    val isArabic = LocalInferenceEngine.isArabicText(text)
    if (text.contains("```")) {
        val parts = text.split("```")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                val lines = part.lines()
                val language = lines.firstOrNull()?.trim() ?: ""
                val code = if (lines.size > 1) lines.drop(1).joinToString("\n") else part
                CodeBlockView(code = code.trim(), language = language)
            } else {
                if (part.trim().isNotEmpty()) {
                    Text(
                        text = part.trim(),
                        color = devTheme.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        style = LocalTextStyle.current.copy(
                            textDirection = TextDirection.ContentOrLtr,
                            textAlign = if (isArabic) TextAlign.End else TextAlign.Start
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    } else {
        Text(
            text = text,
            color = devTheme.textPrimary,
            fontSize = 14.sp,
            lineHeight = 23.sp,
            style = LocalTextStyle.current.copy(
                textDirection = TextDirection.ContentOrLtr,
                textAlign = if (isArabic) TextAlign.End else TextAlign.Start
            )
        )
    }
}

/**
 * Clean streaming bubble with live stopwatch timer, token stream telemetry, and cursor pulse.
 */
@Composable
fun CleanStreamingBubble(
    state: com.example.ui.viewmodel.ActiveGenerationState,
    devTheme: DevThemeColors,
    showThinkingProcess: Boolean = true,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )
    val isArabic = LocalInferenceEngine.isArabicText(state.streamingContent)

    // Dynamic Live Stopwatch Timer that updates continuously from generation start
    var liveElapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(state.isGenerating, state.startTimestamp) {
        if (state.isGenerating && state.startTimestamp > 0L) {
            while (isActive) {
                liveElapsedMs = (System.currentTimeMillis() - state.startTimestamp).coerceAtLeast(0L)
                delay(50)
            }
        } else {
            liveElapsedMs = state.durationMs
        }
    }

    val totalSec = liveElapsedMs / 1000f
    val timerFormatted = String.format(java.util.Locale.US, "⏱ %02d:%04.1fs", (totalSec / 60).toInt(), totalSec % 60)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 12.dp, top = 2.dp)
    ) {
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
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(devTheme.primary.copy(alpha = alpha))
                )
                Text(
                    text = if (isArabic) "جاري التوليد المباشر..." else "Evaluating response...",
                    color = devTheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )

                // Live generation timer badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = devTheme.primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.primary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = timerFormatted,
                        color = devTheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            TextButton(
                onClick = onStop,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text(
                    text = if (isArabic) "إيقاف" else "Stop",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (state.isComputingFullResponse) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = devTheme.surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = devTheme.primary,
                        trackColor = devTheme.border
                    )
                    Text(
                        text = if (isArabic) "جاري البث الفوري..." else "Streaming response live...",
                        color = devTheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            if (state.streamingContent.contains("<think>") || state.streamingContent.contains("```")) {
                CleanFormattedAssistantContent(
                    content = state.streamingContent + " ▋",
                    devTheme = devTheme,
                    showThinkingProcess = showThinkingProcess
                )
            } else {
                Text(
                    text = state.streamingContent.ifEmpty { if (isArabic) "جاري التفكير..." else "Thinking..." } + " ▋",
                    color = devTheme.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 23.sp,
                    style = LocalTextStyle.current.copy(
                        textDirection = TextDirection.ContentOrLtr,
                        textAlign = if (isArabic) TextAlign.End else TextAlign.Start
                    )
                )
            }
        }

        // Live streaming token telemetry telemetry chip
        if (state.tokensGenerated > 0 || state.tokensPerSecond > 0f) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚡ ${state.tokensGenerated} tokens streamed",
                        color = devTheme.secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (state.tokensPerSecond > 0f) {
                        Text(
                            text = "• ${state.tokensPerSecond} tok/s",
                            color = devTheme.textMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                if (state.timeToFirstTokenMs > 0) {
                    Text(
                        text = "TTFT: ${state.timeToFirstTokenMs}ms",
                        color = devTheme.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Modern floating capsule input bar with integrated Think button.
 * Unrestricted user input: screen can be fully controlled anytime.
 */
@Composable
fun CleanChatInputBar(
    text: String,
    isGenerating: Boolean,
    isThinkActive: Boolean,
    devTheme: DevThemeColors,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onToggleThink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = Color.Transparent
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = devTheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                if (isThinkActive) 1.dp else 0.5.dp,
                if (isThinkActive) devTheme.primary.copy(alpha = 0.7f) else devTheme.border
            ),
            shadowElevation = if (isThinkActive) 3.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // "Think" Feature Toggle Button - Unrestricted interaction
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isThinkActive) devTheme.primary.copy(alpha = 0.18f) else devTheme.card.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isThinkActive) devTheme.primary else devTheme.border
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onToggleThink() }
                        .testTag("think_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = if (isThinkActive) "Deactivate Think Mode" else "Activate Think Mode",
                            tint = if (isThinkActive) devTheme.primary else devTheme.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Think",
                            color = if (isThinkActive) devTheme.primary else devTheme.textMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isThinkActive) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        if (isThinkActive) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(devTheme.primary)
                            )
                        }
                    }
                }

                // Text field always unrestricted: user can type, prepare, or paste anytime
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = {
                        Text(
                            text = if (isGenerating) "Type while streaming..."
                                   else if (isThinkActive) "Message (Think active)..."
                                   else "Message...",
                            color = devTheme.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        textDirection = TextDirection.ContentOrLtr,
                        fontSize = 14.sp,
                        color = devTheme.textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = devTheme.textPrimary,
                        unfocusedTextColor = devTheme.textPrimary,
                        cursorColor = devTheme.primary
                    ),
                    maxLines = 4,
                    enabled = true
                )

                // Unrestricted Action Controls: Stop button and Send button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isGenerating) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .testTag("stop_generation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSend,
                        enabled = text.isNotBlank(),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (text.isNotBlank()) devTheme.primary
                                else devTheme.border.copy(alpha = 0.5f)
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (text.isNotBlank()) devTheme.bg else devTheme.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean parameters sheet with full developer and model controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanParametersBottomSheet(
    temperature: Float,
    topP: Float,
    topK: Int,
    maxTokens: Int,
    repeatPenalty: Float,
    cpuThreads: Int,
    showThinkingProcess: Boolean,
    systemPrompt: String,
    devTheme: DevThemeColors,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Int, Int, Float, Int, Boolean, String) -> Unit
) {
    var temp by remember { mutableStateOf(temperature) }
    var p by remember { mutableStateOf(topP) }
    var k by remember { mutableStateOf(topK) }
    var maxTok by remember { mutableStateOf(maxTokens) }
    var repPen by remember { mutableStateOf(repeatPenalty) }
    var threads by remember { mutableStateOf(cpuThreads) }
    var showThinking by remember { mutableStateOf(showThinkingProcess) }
    var prompt by remember { mutableStateOf(systemPrompt) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = devTheme.card,
        contentColor = devTheme.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Runtime Hyperparameters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = devTheme.textPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Local llama.cpp",
                    fontSize = 11.sp,
                    color = devTheme.primary,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Demonstrate Thinking Process Toggle Switch
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = devTheme.surface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, devTheme.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = devTheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Show Thinking Process",
                                color = devTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Display model reasoning (<think> tags) in an expandable trace",
                            color = devTheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = showThinking,
                        onCheckedChange = { showThinking = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = devTheme.bg,
                            checkedTrackColor = devTheme.primary,
                            uncheckedThumbColor = devTheme.textMuted,
                            uncheckedTrackColor = devTheme.surface
                        )
                    )
                }
            }

            // Max Tokens
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Max Output Tokens", color = devTheme.textSecondary, fontSize = 12.sp)
                    Text("$maxTok tokens", color = devTheme.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = maxTok.toFloat(),
                    onValueChange = { maxTok = it.toInt() },
                    valueRange = 128f..4096f,
                    steps = 30,
                    colors = SliderDefaults.colors(
                        thumbColor = devTheme.primary,
                        activeTrackColor = devTheme.primary,
                        inactiveTrackColor = devTheme.border
                    )
                )
                // Quick preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(256, 512, 1024, 2048, 4096).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (maxTok == preset) devTheme.primary.copy(alpha = 0.2f) else devTheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (maxTok == preset) devTheme.primary else devTheme.border
                            ),
                            modifier = Modifier
                                .clickable { maxTok = preset }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "$preset",
                                color = if (maxTok == preset) devTheme.primary else devTheme.textSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Temperature
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temperature (Creativity)", color = devTheme.textSecondary, fontSize = 12.sp)
                    Text(String.format(java.util.Locale.US, "%.2f", temp), color = devTheme.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = temp,
                    onValueChange = { temp = it },
                    valueRange = 0.0f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = devTheme.primary,
                        activeTrackColor = devTheme.primary,
                        inactiveTrackColor = devTheme.border
                    )
                )
            }

            // Top P
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Top-P (Nucleus Sampling)", color = devTheme.textSecondary, fontSize = 12.sp)
                    Text(String.format(java.util.Locale.US, "%.2f", p), color = devTheme.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = p,
                    onValueChange = { p = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = devTheme.primary,
                        activeTrackColor = devTheme.primary,
                        inactiveTrackColor = devTheme.border
                    )
                )
            }

            // Top K
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Top-K (Vocabulary Candidate Pool)", color = devTheme.textSecondary, fontSize = 12.sp)
                    Text("$k candidates", color = devTheme.secondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = k.toFloat(),
                    onValueChange = { k = it.toInt() },
                    valueRange = 1f..100f,
                    steps = 98,
                    colors = SliderDefaults.colors(
                        thumbColor = devTheme.secondary,
                        activeTrackColor = devTheme.secondary,
                        inactiveTrackColor = devTheme.border
                    )
                )
            }

            // Repetition Penalty
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repetition Penalty", color = devTheme.textSecondary, fontSize = 12.sp)
                    Text(String.format(java.util.Locale.US, "%.2f", repPen), color = devTheme.secondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = repPen,
                    onValueChange = { repPen = it },
                    valueRange = 1.0f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = devTheme.secondary,
                        activeTrackColor = devTheme.secondary,
                        inactiveTrackColor = devTheme.border
                    )
                )
            }

            // CPU Threads
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CPU Compute Threads", color = devTheme.textSecondary, fontSize = 12.sp)
                    Text("$threads Cores", color = devTheme.secondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = threads.toFloat(),
                    onValueChange = { threads = it.toInt() },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = devTheme.secondary,
                        activeTrackColor = devTheme.secondary,
                        inactiveTrackColor = devTheme.border
                    )
                )
            }

            // System Prompt
            Column {
                Text("System Prompt", color = devTheme.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = devTheme.primary,
                        unfocusedBorderColor = devTheme.border,
                        focusedContainerColor = devTheme.surface,
                        unfocusedContainerColor = devTheme.surface,
                        focusedTextColor = devTheme.textPrimary,
                        unfocusedTextColor = devTheme.textPrimary
                    )
                )
            }

            Button(
                onClick = { onSave(temp, p, k, maxTok, repPen, threads, showThinking, prompt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = devTheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply Parameters", color = devTheme.bg, fontWeight = FontWeight.Bold)
            }
        }
    }
}
