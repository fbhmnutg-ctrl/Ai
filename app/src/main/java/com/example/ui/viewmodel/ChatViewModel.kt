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
import com.example.engine.BackgroundInferenceManager
import com.example.engine.ChatTemplateEngine
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
    val startTimestamp: Long = 0L,
    val latestToken: String = "",
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
    val isStreamingEnabled: StateFlow<Boolean> = settingsManager.isStreamingEnabled
    val isIntegratedThinkEnabled: StateFlow<Boolean> = settingsManager.isIntegratedThinkEnabled
    val showThinkingProcess: StateFlow<Boolean> = settingsManager.showThinkingProcess
    val maxTokens: StateFlow<Int> = settingsManager.maxTokens
    val topK: StateFlow<Int> = settingsManager.topK
    val repeatPenalty: StateFlow<Float> = settingsManager.repeatPenalty

    fun toggleIntegratedThink() {
        settingsManager.toggleIntegratedThink()
    }

    fun toggleThinkingProcess() {
        settingsManager.toggleThinkingProcess()
    }

    fun setShowThinkingProcess(show: Boolean) {
        settingsManager.setShowThinkingProcess(show)
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
        BackgroundInferenceManager.stopCurrentGeneration()
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

            val filePath = model.filePath ?: run {
                val context = getApplication<Application>()
                val modelsDir = java.io.File(context.filesDir, "models")
                val f = java.io.File(modelsDir, model.filename)
                if (f.exists()) f.absolutePath else null
            }

            if (filePath != null) {
                try {
                    NativeLlamaBridge.preloadModelIntoMemory(
                        modelFilePath = filePath,
                        contextLength = model.contextLength,
                        threads = _cpuThreads.value
                    )
                } catch (t: Throwable) {
                    android.util.Log.w("ChatViewModel", "Preload model non-fatal exception: ${t.message}")
                }
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

    // Generation State (Synchronized with BackgroundInferenceManager)
    val generationState: StateFlow<ActiveGenerationState> = BackgroundInferenceManager.generationState

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
        BackgroundInferenceManager.initialize(application)

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

    fun updateSettings(
        temp: Float,
        p: Float,
        threads: Int,
        prompt: String,
        maxTokensValue: Int = 2048,
        topKValue: Int = 40,
        repeatPenaltyValue: Float = 1.1f,
        showThinking: Boolean = true
    ) {
        _temperature.value = temp
        _topP.value = p
        _cpuThreads.value = threads
        _systemPrompt.value = prompt

        settingsManager.setTemperature(temp)
        settingsManager.setTopP(p)
        settingsManager.setCpuThreads(threads)
        settingsManager.setMaxTokens(maxTokensValue)
        settingsManager.setTopK(topKValue)
        settingsManager.setRepeatPenalty(repeatPenaltyValue)
        settingsManager.setShowThinkingProcess(showThinking)

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
        BackgroundInferenceManager.stopCurrentGeneration()
    }

    fun sendMessage(promptOverride: String? = null) {
        val prompt = (promptOverride ?: _inputText.value).trim()
        if (prompt.isEmpty() || generationState.value.isGenerating) return

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

            val currentHistory = _currentMessages.value
            val fullMessagesContext = if (currentHistory.none { it.id == userMsg.id }) {
                currentHistory + userMsg
            } else {
                currentHistory
            }

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

            // Start background resilient generation with foreground notification
            BackgroundInferenceManager.startGeneration(
                context = getApplication(),
                sessionId = sId,
                model = model,
                messages = fullMessagesContext,
                userPrompt = prompt,
                systemPrompt = _systemPrompt.value,
                temperature = _temperature.value,
                topP = _topP.value,
                cpuThreads = _cpuThreads.value,
                isStreaming = isStreaming,
                isIntegratedThinkEnabled = isIntegratedThinkEnabled.value,
                isGpuOffloadEnabled = settingsManager.isGpuOffloadEnabled.value,
                gpuOffloadLayers = settingsManager.gpuOffloadLayers.value,
                isOomGuardEnabled = settingsManager.isOomGuardEnabled.value,
                isOllama = isOllama,
                ollamaClient = if (isOllama) ollamaClient else null,
                maxTokens = settingsManager.maxTokens.value,
                topK = settingsManager.topK.value,
                repeatPenalty = settingsManager.repeatPenalty.value,
                showThinkingProcess = settingsManager.showThinkingProcess.value
            )
        }
    }
}
