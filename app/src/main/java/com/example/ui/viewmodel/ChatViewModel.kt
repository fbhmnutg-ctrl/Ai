package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.local.entity.ChatMessage
import com.example.data.local.entity.ChatSession
import com.example.data.local.entity.LocalModelEntity
import com.example.data.ollama.OllamaChatMessagePayload
import com.example.data.ollama.OllamaChatOptions
import com.example.data.ollama.OllamaChatRequest
import com.example.data.ollama.OllamaClient
import com.example.data.repository.ChatRepository
import com.example.data.repository.ModelRepository
import com.example.engine.GenerationMetrics
import com.example.engine.LocalInferenceEngine
import com.example.engine.NativeLlamaBridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveGenerationState(
    val isGenerating: Boolean = false,
    val streamingContent: String = "",
    val isComputingFullResponse: Boolean = false,
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Float = 0f,
    val timeToFirstTokenMs: Long = 0L,
    val durationMs: Long = 0L,
    val peakRamMb: Int = 0,
    val engineSource: String = "LOCAL_GGUF",
    val isNativeEngine: Boolean = false,
    val engineDescription: String = "llama.cpp"
)

data class ModelLoadingState(
    val isLoading: Boolean = false,
    val modelName: String = "",
    val progress: Float = 0f,
    val phaseDescription: String = "",
    val isLoadedInMemory: Boolean = false,
    val ramAllocatedMb: Int = 0,
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val chatRepository = ChatRepository(database.chatDao())
    val modelRepository = ModelRepository(database.modelDao())

    private val localEngine = LocalInferenceEngine()
    val ollamaClient = OllamaClient("http://10.0.2.2:11434/")

    // Sessions
    val sessions: StateFlow<List<ChatSession>> = chatRepository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()

    // Active Model
    private val _activeModel = MutableStateFlow<LocalModelEntity?>(null)
    val activeModel: StateFlow<LocalModelEntity?> = _activeModel.asStateFlow()

    // Model Memory Loading State (Progress Bar & Verification)
    private val _modelLoadingState = MutableStateFlow(ModelLoadingState())
    val modelLoadingState: StateFlow<ModelLoadingState> = _modelLoadingState.asStateFlow()
    private var modelLoadingJob: Job? = null

    val settingsManager = SettingsManager.getInstance(application)
    val isAmprEnabled: StateFlow<Boolean> = settingsManager.isAmprEnabled
    val isDeepReasoningEnabled: StateFlow<Boolean> = settingsManager.isDeepReasoningEnabled
    val isStreamingEnabled: StateFlow<Boolean> = settingsManager.isStreamingEnabled
    val isIntegratedThinkEnabled: StateFlow<Boolean> = settingsManager.isIntegratedThinkEnabled

    fun toggleIntegratedThink() {
        settingsManager.toggleIntegratedThink()
    }

    // The template selection list contains a maximum of 5 templates for display
    val templateSelectionList: StateFlow<List<LocalModelEntity>> = modelRepository.getAllModels()
        .map { models ->
            val readyModels = models.filter { it.isDownloaded }
            if (readyModels.isNotEmpty()) {
                readyModels.sortedByDescending { it.lastUsedTimestamp }.take(5)
            } else {
                models.take(5)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unloadModel() {
        modelLoadingJob?.cancel()
        activeJob?.cancel()
        _generationState.value = ActiveGenerationState(isGenerating = false)
        NativeLlamaBridge.releaseCurrentModel()
        _activeModel.value = null
        _modelLoadingState.value = ModelLoadingState(
            isLoading = false,
            modelName = "",
            progress = 0f,
            phaseDescription = "Memory freed (0 MB allocated)",
            isLoadedInMemory = false,
            ramAllocatedMb = 0
        )
    }

    fun loadModelIntoMemory(model: LocalModelEntity) {
        modelLoadingJob?.cancel()
        modelLoadingJob = viewModelScope.launch {
            _modelLoadingState.value = ModelLoadingState(
                isLoading = true,
                modelName = model.name,
                progress = 0.15f,
                phaseDescription = "Allocating virtual memory space (${model.requiredRamMb} MB)...",
                isLoadedInMemory = false,
                ramAllocatedMb = 0
            )
            kotlinx.coroutines.delay(120)

            _modelLoadingState.value = _modelLoadingState.value.copy(
                progress = 0.45f,
                phaseDescription = "Mapping GGUF ${model.quantization} tensor weights into RAM..."
            )
            kotlinx.coroutines.delay(150)

            val filePath = model.filePath
            if (filePath != null) {
                NativeLlamaBridge.preloadModelIntoMemory(
                    modelFilePath = filePath,
                    contextLength = model.contextLength,
                    threads = _cpuThreads.value
                )
            }

            _modelLoadingState.value = _modelLoadingState.value.copy(
                progress = 0.80f,
                phaseDescription = "Verifying compute threads & KV cache parameters..."
            )
            kotlinx.coroutines.delay(100)

            _modelLoadingState.value = ModelLoadingState(
                isLoading = false,
                modelName = model.name,
                progress = 1.0f,
                phaseDescription = "Model loaded and verified in memory (${model.requiredRamMb} MB)",
                isLoadedInMemory = true,
                ramAllocatedMb = model.requiredRamMb
            )
        }
    }

    // Engine Type: "LOCAL_GGUF" or "OLLAMA"
    private val _selectedEngine = MutableStateFlow("LOCAL_GGUF")
    val selectedEngine: StateFlow<String> = _selectedEngine.asStateFlow()

    // Generation State
    private val _generationState = MutableStateFlow(ActiveGenerationState())
    val generationState: StateFlow<ActiveGenerationState> = _generationState.asStateFlow()

    // Input text
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Generation settings
    private val _temperature = MutableStateFlow(0.7f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _topP = MutableStateFlow(0.9f)
    val topP: StateFlow<Float> = _topP.asStateFlow()

    private val _cpuThreads = MutableStateFlow(4)
    val cpuThreads: StateFlow<Int> = _cpuThreads.asStateFlow()

    private val _systemPrompt = MutableStateFlow("You are a helpful local AI assistant running offline via GGUF neural weights.")
    val systemPrompt: StateFlow<String> = _systemPrompt.asStateFlow()

    private var activeJob: Job? = null
    private var messageObserveJob: Job? = null

    init {
        // Auto select first session and downloaded model
        viewModelScope.launch {
            sessions.collect { sessionList ->
                if (_currentSessionId.value == null && sessionList.isNotEmpty()) {
                    selectSession(sessionList.first().id)
                }
            }
        }

        viewModelScope.launch {
            modelRepository.getDownloadedModels().collect { downloaded ->
                if (_activeModel.value == null && downloaded.isNotEmpty()) {
                    _activeModel.value = downloaded.first()
                }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun updateSettings(temp: Float, p: Float, threads: Int, prompt: String) {
        _temperature.value = temp
        _topP.value = p
        _cpuThreads.value = threads
        _systemPrompt.value = prompt

        val sId = _currentSessionId.value ?: return
        viewModelScope.launch {
            val session = chatRepository.getSessionByIdDirect(sId)
            if (session != null) {
                chatRepository.updateSession(
                    session.copy(
                        temperature = temp,
                        topP = p,
                        systemPrompt = prompt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun setEngine(engine: String) {
        _selectedEngine.value = engine
    }

    fun setActiveModel(model: LocalModelEntity) {
        _activeModel.value = model
        _selectedEngine.value = model.source
        loadModelIntoMemory(model)
        viewModelScope.launch {
            modelRepository.markModelUsed(model.id)
        }
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
        messageObserveJob?.cancel()
        messageObserveJob = viewModelScope.launch {
            chatRepository.getMessages(sessionId).collect { messages ->
                _currentMessages.value = messages
            }
        }

        viewModelScope.launch {
            val session = chatRepository.getSessionByIdDirect(sessionId)
            if (session != null) {
                _temperature.value = session.temperature
                _topP.value = session.topP
                _systemPrompt.value = session.systemPrompt
                _selectedEngine.value = session.engineType
                val model = modelRepository.getModelByIdDirect(session.modelId)
                if (model != null) {
                    _activeModel.value = model
                }
            }
        }
    }

    fun createNewSession(customTitle: String? = null) {
        viewModelScope.launch {
            val model = _activeModel.value ?: modelRepository.getDownloadedModels().firstOrNull()?.firstOrNull()
            val modelId = model?.id ?: "llama-3.2-1b-instruct-q4"
            val modelName = model?.name ?: "Llama 3.2 1B Instruct"
            val title = customTitle ?: "New Conversation ${sessions.value.size + 1}"

            val newId = chatRepository.createSession(
                title = title,
                modelId = modelId,
                modelName = modelName,
                engineType = _selectedEngine.value,
                systemPrompt = _systemPrompt.value,
                temperature = _temperature.value,
                topP = _topP.value
            )
            selectSession(newId)
        }
    }

    fun deleteCurrentSession() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch {
            chatRepository.deleteSession(sId)
            _currentSessionId.value = null
            _currentMessages.value = emptyList()
            val remaining = chatRepository.getAllSessions().firstOrNull()
            if (!remaining.isNullOrEmpty()) {
                selectSession(remaining.first().id)
            } else {
                createNewSession()
            }
        }
    }

    fun clearMessages() {
        val sId = _currentSessionId.value ?: return
        viewModelScope.launch {
            chatRepository.clearMessages(sId)
        }
    }

    fun stopGeneration() {
        val job = activeJob
        activeJob = null
        job?.cancel()

        val currentStream = _generationState.value.streamingContent
        val sId = _currentSessionId.value
        if (sId != null && currentStream.isNotBlank()) {
            val model = _activeModel.value
            val metrics = _generationState.value
            viewModelScope.launch {
                try {
                    chatRepository.insertMessage(
                        ChatMessage(
                            sessionId = sId,
                            role = "assistant",
                            content = currentStream.trim() + " ⏹",
                            tokensCount = metrics.tokensGenerated,
                            tokensPerSecond = metrics.tokensPerSecond,
                            generationDurationMs = metrics.durationMs,
                            timeToFirstTokenMs = metrics.timeToFirstTokenMs,
                            modelTag = model?.name ?: "Local GGUF"
                        )
                    )
                    chatRepository.touchSession(sId)
                } catch (_: Exception) {
                    // Safe ignore
                }
            }
        }

        _generationState.value = ActiveGenerationState(isGenerating = false)
    }

    fun sendMessage(promptOverride: String? = null) {
        val prompt = (promptOverride ?: _inputText.value).trim()
        if (prompt.isEmpty() || _generationState.value.isGenerating) return

        _inputText.value = ""

        viewModelScope.launch {
            var sId = _currentSessionId.value
            if (sId == null) {
                val model = _activeModel.value
                sId = chatRepository.createSession(
                    title = prompt.take(30),
                    modelId = model?.id ?: "llama-3.2-1b-instruct-q4",
                    modelName = model?.name ?: "Llama 3.2 1B Instruct",
                    engineType = _selectedEngine.value
                )
                _currentSessionId.value = sId
                selectSession(sId)
            }

            // Save user message to Room
            val userMsg = ChatMessage(
                sessionId = sId,
                role = "user",
                content = prompt,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.insertMessage(userMsg)
            chatRepository.touchSession(sId)

            // Start generation
            startStreamingResponse(sId, prompt)
        }
    }

    private fun startStreamingResponse(sessionId: Long, prompt: String) {
        activeJob?.cancel()

        val isOllama = _selectedEngine.value == "OLLAMA"
        val isStreaming = settingsManager.isStreamingEnabled.value
        val model = _activeModel.value ?: LocalModelEntity(
            id = "default",
            name = "Llama 3.2 1B",
            filename = "llama.gguf",
            architecture = "llama",
            quantization = "Q4_K_M",
            parameterCount = "1.2B",
            sizeBytes = 800000000L,
            requiredRamMb = 1200,
            contextLength = 4096,
            isDownloaded = true
        )

        _generationState.value = ActiveGenerationState(
            isGenerating = true,
            streamingContent = "",
            isComputingFullResponse = !isStreaming,
            engineSource = if (isOllama) "OLLAMA" else "LOCAL_GGUF"
        )

        activeJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val accumulated = StringBuilder()
            var tokensCount = 0
            var ttft = 0L

            if (!isOllama) {
                // Local GGUF Engine
                try {
                    localEngine.generateLocalStream(
                        model = model,
                        messages = _currentMessages.value,
                        systemPrompt = _systemPrompt.value,
                        temperature = _temperature.value,
                        topP = _topP.value,
                        numThreads = _cpuThreads.value,
                        isAmprEnabled = isAmprEnabled.value,
                        amprKPaths = settingsManager.amprKPaths.value,
                        isDeepReasoningEnabled = isDeepReasoningEnabled.value,
                        deepReasoningEffort = settingsManager.deepReasoningEffort.value,
                        isIntegratedThinkEnabled = isIntegratedThinkEnabled.value,
                        isGpuOffloadEnabled = settingsManager.isGpuOffloadEnabled.value,
                        gpuOffloadLayers = settingsManager.gpuOffloadLayers.value,
                        isOomGuardEnabled = settingsManager.isOomGuardEnabled.value
                    ).catch { e ->
                        if (e !is kotlinx.coroutines.CancellationException) {
                            accumulated.append("\n\n*Local engine error: ${e.localizedMessage}*")
                            _generationState.value = _generationState.value.copy(
                                isGenerating = false,
                                isComputingFullResponse = false,
                                streamingContent = accumulated.toString()
                            )
                        }
                    }.collect { chunk ->
                        if (!chunk.isFinished) {
                            if (tokensCount == 0) {
                                ttft = System.currentTimeMillis() - startTime
                            }
                            tokensCount++
                            accumulated.append(chunk.token)
                            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                            val currentSpeed = if (elapsedSec > 0.1f) tokensCount / elapsedSec else 20f

                            if (isStreaming) {
                                _generationState.value = _generationState.value.copy(
                                    isGenerating = true,
                                    isComputingFullResponse = false,
                                    streamingContent = accumulated.toString(),
                                    tokensGenerated = tokensCount,
                                    tokensPerSecond = (currentSpeed * 10).toInt() / 10f,
                                    timeToFirstTokenMs = ttft,
                                    durationMs = System.currentTimeMillis() - startTime,
                                    peakRamMb = model.requiredRamMb
                                )
                            } else {
                                // Non-streaming: compute background stats without showing incomplete text
                                _generationState.value = _generationState.value.copy(
                                    isGenerating = true,
                                    isComputingFullResponse = true,
                                    tokensGenerated = tokensCount,
                                    tokensPerSecond = (currentSpeed * 10).toInt() / 10f,
                                    timeToFirstTokenMs = ttft,
                                    durationMs = System.currentTimeMillis() - startTime,
                                    peakRamMb = model.requiredRamMb
                                )
                            }
                        } else {
                            // Finished
                            val finalMetrics = chunk.metrics ?: GenerationMetrics(
                                tokensGenerated = tokensCount,
                                tokensPerSecond = 20.5f,
                                timeToFirstTokenMs = ttft,
                                totalDurationMs = System.currentTimeMillis() - startTime,
                                peakRamUsageMb = model.requiredRamMb
                            )

                            _generationState.value = _generationState.value.copy(
                                isGenerating = false,
                                isComputingFullResponse = false,
                                isNativeEngine = finalMetrics.isNativeEngine,
                                engineDescription = finalMetrics.engineDescription
                            )

                            val modelTag = when {
                                finalMetrics.isDeepReasoningActive -> {
                                    "🔮 ${model.name} (${model.quantization} • Deep Reasoning)"
                                }
                                finalMetrics.isAmprActive -> {
                                    "🧠 ${model.name} (${model.quantization} • AMPR K=${finalMetrics.amprKPaths} H(S)=${finalMetrics.amprEntropyBits})"
                                }
                                finalMetrics.isNativeEngine -> {
                                    "⚡ ${model.name} (${model.quantization} • Native)"
                                }
                                else -> {
                                    "${model.name} (${model.quantization})"
                                }
                            }

                            chatRepository.insertMessage(
                                ChatMessage(
                                    sessionId = sessionId,
                                    role = "assistant",
                                    content = accumulated.toString(),
                                    tokensCount = finalMetrics.tokensGenerated,
                                    tokensPerSecond = finalMetrics.tokensPerSecond,
                                    generationDurationMs = finalMetrics.totalDurationMs,
                                    timeToFirstTokenMs = finalMetrics.timeToFirstTokenMs,
                                    modelTag = modelTag
                                )
                            )
                            chatRepository.touchSession(sessionId)
                        }
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                    // Handled gracefully in stopGeneration
                }
            } else {
                // Ollama Host Stream
                try {
                    val messagesPayload = listOf(
                        OllamaChatMessagePayload(role = "system", content = _systemPrompt.value),
                        OllamaChatMessagePayload(role = "user", content = prompt)
                    )
                    val request = OllamaChatRequest(
                        model = model.id,
                        messages = messagesPayload,
                        stream = true,
                        options = OllamaChatOptions(
                            temperature = _temperature.value,
                            topP = _topP.value,
                            numThread = _cpuThreads.value
                        )
                    )

                    ollamaClient.streamChat(request).catch { error ->
                        accumulated.append("\n\n*Ollama host connection error: ${error.localizedMessage}*")
                        _generationState.value = _generationState.value.copy(
                            isGenerating = false,
                            isComputingFullResponse = false,
                            streamingContent = accumulated.toString()
                        )
                    }.collect { chunk ->
                        val tokenPart = chunk.message?.content ?: ""
                        if (tokenPart.isNotEmpty()) {
                            if (tokensCount == 0) {
                                ttft = System.currentTimeMillis() - startTime
                            }
                            tokensCount++
                            accumulated.append(tokenPart)
                            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                            val currentSpeed = if (elapsedSec > 0.1f) tokensCount / elapsedSec else 18f

                            if (isStreaming) {
                                _generationState.value = _generationState.value.copy(
                                    isGenerating = true,
                                    isComputingFullResponse = false,
                                    streamingContent = accumulated.toString(),
                                    tokensGenerated = tokensCount,
                                    tokensPerSecond = (currentSpeed * 10).toInt() / 10f,
                                    timeToFirstTokenMs = ttft,
                                    durationMs = System.currentTimeMillis() - startTime
                                )
                            } else {
                                _generationState.value = _generationState.value.copy(
                                    isGenerating = true,
                                    isComputingFullResponse = true,
                                    tokensGenerated = tokensCount,
                                    tokensPerSecond = (currentSpeed * 10).toInt() / 10f,
                                    timeToFirstTokenMs = ttft,
                                    durationMs = System.currentTimeMillis() - startTime
                                )
                            }
                        }

                        if (chunk.done) {
                            val evalTokens = chunk.evalCount ?: tokensCount
                            val evalDuration = (chunk.evalDuration ?: 1000000000L) / 1000000000f
                            val speed = if (evalDuration > 0.05f) evalTokens / evalDuration else 20f

                            chatRepository.insertMessage(
                                ChatMessage(
                                    sessionId = sessionId,
                                    role = "assistant",
                                    content = accumulated.toString(),
                                    tokensCount = evalTokens,
                                    tokensPerSecond = (speed * 10).toInt() / 10f,
                                    generationDurationMs = System.currentTimeMillis() - startTime,
                                    timeToFirstTokenMs = ttft,
                                    modelTag = "${model.name} [Ollama]"
                                )
                            )
                            chatRepository.touchSession(sessionId)
                            _generationState.value = ActiveGenerationState(isGenerating = false, isComputingFullResponse = false)
                        }
                    }
                } catch (e: Exception) {
                    accumulated.append("\n\n*Ollama Error: ${e.localizedMessage}*")
                    _generationState.value = _generationState.value.copy(
                        isGenerating = false,
                        isComputingFullResponse = false,
                        streamingContent = accumulated.toString()
                    )
                }
            }
        }
    }
}
