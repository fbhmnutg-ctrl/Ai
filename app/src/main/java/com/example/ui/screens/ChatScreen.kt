package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.ChatMessage
import com.example.data.local.entity.LocalModelEntity
import com.example.engine.LocalInferenceEngine
import kotlinx.coroutines.launch
import com.example.ui.components.CodeBlockView
import com.example.ui.components.EngineBadge
import com.example.ui.components.GgufTag
import com.example.ui.components.MetricChip
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
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToModels: () -> Unit,
    onNavigateToOllama: () -> Unit,
    onNavigateToUploaded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val activeModel by viewModel.activeModel.collectAsStateWithLifecycle()
    val selectedEngine by viewModel.selectedEngine.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val templateSelectionList by viewModel.templateSelectionList.collectAsStateWithLifecycle()
    val isAmprEnabled by viewModel.isAmprEnabled.collectAsStateWithLifecycle()
    val isDeepReasoningEnabled by viewModel.isDeepReasoningEnabled.collectAsStateWithLifecycle()

    val temperature by viewModel.temperature.collectAsStateWithLifecycle()
    val topP by viewModel.topP.collectAsStateWithLifecycle()
    val cpuThreads by viewModel.cpuThreads.collectAsStateWithLifecycle()
    val systemPrompt by viewModel.systemPrompt.collectAsStateWithLifecycle()

    var showParamsSheet by remember { mutableStateOf(false) }
    var showSessionsMenu by remember { mutableStateOf(false) }
    var showModelSelectorMenu by remember { mutableStateOf(false) }
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

    // Detect if user is manually scrolling up to inspect history
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
            .background(ObsidianBg)
    ) {
        // Top App Bar
        TopChatHeader(
            activeModel = activeModel,
            selectedEngine = selectedEngine,
            onModelClick = { showModelSelectorMenu = true },
            onNewChat = { viewModel.createNewSession() },
            onOpenSettings = { showParamsSheet = true },
            onToggleSessions = { showSessionsMenu = true }
        )

        // Template Selection Dropdown Menu (Max 5 templates + Unload Option)
        DropdownMenu(
            expanded = showModelSelectorMenu,
            onDismissRequest = { showModelSelectorMenu = false },
            modifier = Modifier
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                .widthIn(min = 280.dp, max = 340.dp)
        ) {
            Text(
                text = "SELECT TEMPLATE (MAX 5)",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Strictly maximum 5 templates as requested by user
            val displayTemplates = templateSelectionList.take(5)
            displayTemplates.forEach { model ->
                val isSelected = activeModel?.id == model.id
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name,
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${model.architecture} • ${model.quantization} • ${model.parameterCount}",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        viewModel.setActiveModel(model)
                        showModelSelectorMenu = false
                    }
                )
            }

            HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

            // Unload option to cancel model
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Unload Model",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Unload Model from RAM",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                onClick = {
                    viewModel.unloadModel()
                    showModelSelectorMenu = false
                }
            )

            HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Manage Uploaded Templates...", color = TextPrimary, fontSize = 12.sp)
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
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = VioletNeural,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Browse GGUF Hub...", color = TextPrimary, fontSize = 12.sp)
                    }
                },
                onClick = {
                    showModelSelectorMenu = false
                    onNavigateToModels()
                }
            )
        }

        // Session Selector Dropdown Menu
        DropdownMenu(
            expanded = showSessionsMenu,
            onDismissRequest = { showSessionsMenu = false },
            modifier = Modifier
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "Conversations",
                color = TextSecondary,
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
                                color = if (session.id == currentSessionId) NeonCyan else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (session.id == currentSessionId) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${session.modelName} • ${session.engineType}",
                                color = TextMuted,
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

        // Active Reasoning Mode Sub-Bar
        if (isDeepReasoningEnabled || isAmprEnabled) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, ObsidianBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isDeepReasoningEnabled) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = VioletNeural,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Deep Reasoning AI Active",
                                color = VioletNeural,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "AMPR Multi-Path Active",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = if (isDeepReasoningEnabled) "CoT & Axiomatic Check" else "Minimal Entropy H(S)",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Messages List or Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !generationState.isGenerating) {
                ChatEmptyState(
                    activeModel = activeModel,
                    onNavigateToModels = onNavigateToModels
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageItem(message = msg)
                    }

                    // Live streaming bubble if active
                    if (generationState.isGenerating) {
                        item {
                            StreamingBubble(state = generationState, onStop = { viewModel.stopGeneration() })
                        }
                    }
                }

                // Floating Jump-to-Bottom badge when scrolled up
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
                        color = ObsidianCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.clickable {
                            userManuallyScrolledUp = false
                            coroutineScope.launch {
                                val totalItems = messages.size + (if (generationState.isGenerating) 1 else 0)
                                listState.animateScrollToItem((totalItems - 1).coerceAtLeast(0))
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (generationState.isGenerating) {
                                    "الرد يكتمل الآن (${generationState.tokensPerSecond} tok/s) • اضغط للمتابعة"
                                } else {
                                    "الانتقال لآخر الرسائل ↓"
                                },
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Area
        ChatInputBar(
            text = inputText,
            isGenerating = generationState.isGenerating,
            onTextChanged = { viewModel.onInputTextChanged(it) },
            onSend = { viewModel.sendMessage() },
            onStop = { viewModel.stopGeneration() },
            modifier = Modifier.imePadding()
        )
    }

    // Parameters Bottom Sheet
    if (showParamsSheet) {
        ParametersBottomSheet(
            temperature = temperature,
            topP = topP,
            cpuThreads = cpuThreads,
            systemPrompt = systemPrompt,
            onDismiss = { showParamsSheet = false },
            onSave = { temp, p, threads, prompt ->
                viewModel.updateSettings(temp, p, threads, prompt)
                showParamsSheet = false
            }
        )
    }
}

@Composable
fun TopChatHeader(
    activeModel: LocalModelEntity?,
    selectedEngine: String,
    onModelClick: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSessions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Model Selector Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                    .clickable { onModelClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (selectedEngine == "LOCAL_GGUF") NeonCyan else VioletNeural)
                )
                Column {
                    Text(
                        text = activeModel?.name ?: "Select Model",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = if (selectedEngine == "LOCAL_GGUF") "Local GGUF (${activeModel?.quantization ?: "Q4_K_M"})" else "Ollama Remote",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Switch Model",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Quick Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onToggleSessions,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("conversations_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Conversations",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("chat_params_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tuning Parameters",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyanSubtle)
                        .testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatEmptyState(
    activeModel: LocalModelEntity?,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Minimalist sovereign core indicator
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(ObsidianCard)
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = activeModel?.name ?: "Sovereign AI Engine",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            GgufTag(text = activeModel?.quantization ?: "Q4_K_M")
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "معالجة محلية خاصة 100% • جاهز للمحادثة الفورية",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Zero cloud telemetry • All neural calculations on CPU",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MessageItem(
    message: ChatMessage,
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
            // User Message Bubble
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                color = Color(0xFF0F323D),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                modifier = Modifier.padding(start = 48.dp)
            ) {
                Text(
                    text = message.content,
                    color = Color(0xFFECFEFF),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    style = LocalTextStyle.current.copy(
                        textDirection = TextDirection.ContentOrLtr,
                        textAlign = if (isArabic) TextAlign.End else TextAlign.Start
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        } else {
            // Assistant Message Card
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = ObsidianCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header with Model Tag & Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                            Text(
                                text = message.modelTag.ifEmpty { "GGUF Engine" },
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
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
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Parse potential <think> tags for DeepSeek reasoning models
                    FormattedAssistantContent(content = message.content)

                    // Generation Stats Footer
                    if (message.tokensPerSecond > 0f) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MetricChip(
                                icon = { Icon(Icons.Default.Speed, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(12.dp)) },
                                label = "${message.tokensPerSecond} tok/s"
                            )
                            if (message.timeToFirstTokenMs > 0) {
                                MetricChip(
                                    icon = { Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp)) },
                                    label = "TTFT: ${message.timeToFirstTokenMs}ms"
                                )
                            }
                            Text(
                                text = "${message.tokensCount} tokens",
                                color = TextMuted,
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

@Composable
fun FormattedAssistantContent(content: String) {
    var isThinkingExpanded by remember { mutableStateOf(false) }

    if (content.contains("<think>") && content.contains("</think>")) {
        val thinkStart = content.indexOf("<think>") + 7
        val thinkEnd = content.indexOf("</think>")
        val thinkContent = content.substring(thinkStart, thinkEnd).trim()
        val restContent = content.substring(thinkEnd + 8).trim()

        val isDeepReasoning = thinkContent.contains("Deep Reasoning") || thinkContent.contains("التفكير العميق")
        val isAmpr = thinkContent.contains("AMPR") || thinkContent.contains("AMPR التكيفي")
        val isArabicThink = LocalInferenceEngine.isArabicText(thinkContent)

        val title = when {
            isDeepReasoning -> if (isArabicThink) "مسار التفكير العميق (Chain-of-Thought)" else "Deep Reasoning Chain-of-Thought"
            isAmpr -> if (isArabicThink) "مسارات AMPR التكيفية (Adaptive Paths)" else "AMPR Multi-Path Trajectory"
            else -> if (isArabicThink) "خطوات التحليل والتفكير" else "Reasoning Process"
        }
        val themeColor = if (isAmpr) NeonCyan else VioletNeural

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.3f)),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (isDeepReasoning) Icons.Default.AutoAwesome else Icons.Default.Psychology,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = title,
                            color = themeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isThinkingExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = thinkContent,
                        color = TextSecondary,
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
        RenderMarkdownContent(restContent)
    } else {
        RenderMarkdownContent(content)
    }
}

@Composable
fun RenderMarkdownContent(text: String) {
    val isArabic = LocalInferenceEngine.isArabicText(text)
    // If contains code fences
    if (text.contains("```")) {
        val parts = text.split("```")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    // Code block
                    val lines = part.trim().lines()
                    val lang = if (lines.isNotEmpty() && lines.first().length <= 12 && !lines.first().contains(" ")) lines.first() else "code"
                    val codeContent = if (lines.isNotEmpty() && lines.first() == lang) lines.drop(1).joinToString("\n") else part
                    CodeBlockView(code = codeContent.trim(), language = lang)
                } else if (part.isNotBlank()) {
                    val partIsArabic = LocalInferenceEngine.isArabicText(part)
                    Text(
                        text = part.trim(),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        style = LocalTextStyle.current.copy(
                            textDirection = TextDirection.ContentOrLtr,
                            textAlign = if (partIsArabic) TextAlign.End else TextAlign.Start
                        )
                    )
                }
            }
        }
    } else {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            style = LocalTextStyle.current.copy(
                textDirection = TextDirection.ContentOrLtr,
                textAlign = if (isArabic) TextAlign.End else TextAlign.Start
            )
        )
    }
}

@Composable
fun StreamingBubble(
    state: com.example.ui.viewmodel.ActiveGenerationState,
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

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = alpha))
                    )
                    Text(
                        text = if (isArabic) "جاري التوليد على المعالج (CPU)..." else "Generating on CPU...",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "إيقاف" else "Stop", color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.streamingContent.ifEmpty { if (isArabic) "جاري تحليل المدخلات محلياً..." else "Evaluating prompt..." },
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                style = LocalTextStyle.current.copy(
                    textDirection = TextDirection.ContentOrLtr,
                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricChip(
                    icon = { Icon(Icons.Default.Speed, contentDescription = null, tint = EmeraldGlow, modifier = Modifier.size(12.dp)) },
                    label = "${state.tokensPerSecond} tok/s"
                )
                if (state.timeToFirstTokenMs > 0) {
                    MetricChip(
                        icon = { Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp)) },
                        label = "TTFT: ${state.timeToFirstTokenMs}ms"
                    )
                }
                Text(
                    text = "${state.tokensGenerated} tokens",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    isGenerating: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = ObsidianSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = if (isGenerating) "النموذج يولد الرد حالياً..." else "اسأل النموذج المحلي أو اكتب رسالة...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.ContentOrLtr
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan
                ),
                maxLines = 4,
                enabled = !isGenerating
            )

            // Send or Stop button
            IconButton(
                onClick = { if (isGenerating) onStop() else onSend() },
                enabled = isGenerating || text.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGenerating) Color(0xFFEF4444) else if (text.isNotBlank()) NeonCyan else ObsidianCard)
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = if (isGenerating) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (isGenerating) "Stop" else "Send",
                    tint = if (isGenerating) Color.White else if (text.isNotBlank()) Color(0xFF00363D) else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametersBottomSheet(
    temperature: Float,
    topP: Float,
    cpuThreads: Int,
    systemPrompt: String,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Int, String) -> Unit
) {
    var temp by remember { mutableStateOf(temperature) }
    var p by remember { mutableStateOf(topP) }
    var threads by remember { mutableStateOf(cpuThreads) }
    var prompt by remember { mutableStateOf(systemPrompt) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Model Runtime Tuning",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Temperature
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Temperature", color = TextSecondary, fontSize = 13.sp)
                    Text(String.format("%.2f", temp), color = NeonCyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = temp,
                    onValueChange = { temp = it },
                    valueRange = 0.0f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = ObsidianBorder
                    )
                )
            }

            // Top P
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Top-P (Nucleus Sampling)", color = TextSecondary, fontSize = 13.sp)
                    Text(String.format("%.2f", p), color = NeonCyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = p,
                    onValueChange = { p = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = ObsidianBorder
                    )
                )
            }

            // CPU Threads
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CPU Compute Threads", color = TextSecondary, fontSize = 13.sp)
                    Text("$threads Cores", color = EmeraldGlow, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = threads.toFloat(),
                    onValueChange = { threads = it.toInt() },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldGlow,
                        activeTrackColor = EmeraldGlow,
                        inactiveTrackColor = ObsidianBorder
                    )
                )
            }

            // System Prompt
            Column {
                Text("System Prompt", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Native Engine Architecture Info Card
            val isArm64 = com.example.engine.NativeLlamaBridge.isNativeAbiSupported()
            val archSummary = com.example.engine.NativeLlamaBridge.getDeviceArchitectureSummary()
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ObsidianSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isArm64) EmeraldGlow.copy(alpha = 0.4f) else ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isArm64) Icons.Default.Bolt else Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isArm64) EmeraldGlow else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Native Engine Status",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Engine: llama.cpp b9878 (libllama.so)\nArchitecture: $archSummary",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }

            Button(
                onClick = { onSave(temp, p, threads, prompt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply Parameters", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
            }
        }
    }
}
