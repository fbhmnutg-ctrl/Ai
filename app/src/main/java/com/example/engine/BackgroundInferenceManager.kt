package com.example.engine

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatMessage
import com.example.data.local.entity.LocalModelEntity
import com.example.data.repository.ChatRepository
import com.example.ui.viewmodel.ActiveGenerationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

object BackgroundInferenceManager {
    private const val TAG = "BackgroundInference"

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null
    private val localEngine = LocalInferenceEngine()

    private val _generationState = MutableStateFlow(ActiveGenerationState())
    val generationState: StateFlow<ActiveGenerationState> = _generationState.asStateFlow()

    private var activeSessionId: Long? = null
    private var activeModelEntity: LocalModelEntity? = null
    private var appContext: Context? = null
    private var chatRepository: ChatRepository? = null

    fun initialize(application: Application) {
        appContext = application.applicationContext
        val db = AppDatabase.getDatabase(application, applicationScope)
        chatRepository = ChatRepository(db.chatDao())
    }

    fun startGeneration(
        context: Context,
        sessionId: Long,
        model: LocalModelEntity,
        messages: List<ChatMessage>,
        userPrompt: String,
        systemPrompt: String,
        temperature: Float,
        topP: Float,
        cpuThreads: Int,
        isStreaming: Boolean,
        isIntegratedThinkEnabled: Boolean,
        isGpuOffloadEnabled: Boolean,
        gpuOffloadLayers: Int,
        isOomGuardEnabled: Boolean,
        isOllama: Boolean = false,
        ollamaClient: com.example.data.ollama.OllamaClient? = null
    ) {
        stopCurrentGeneration()

        appContext = context.applicationContext
        activeSessionId = sessionId
        activeModelEntity = model

        if (chatRepository == null) {
            val db = AppDatabase.getDatabase(context.applicationContext, applicationScope)
            chatRepository = ChatRepository(db.chatDao())
        }

        _generationState.value = ActiveGenerationState(
            isGenerating = true,
            streamingContent = "",
            isComputingFullResponse = !isStreaming,
            engineSource = if (isOllama) "OLLAMA" else "LOCAL_GGUF"
        )

        activeJob = applicationScope.launch {
            val startTime = System.currentTimeMillis()
            val accumulated = StringBuilder()
            var tokensCount = 0
            var ttft = 0L
            var lastNotificationUpdate = 0L

            if (!isOllama) {
                // Local GGUF Native Inference
                try {
                    localEngine.generateLocalStream(
                        model = model,
                        messages = messages,
                        userPromptOverride = userPrompt,
                        systemPrompt = systemPrompt,
                        temperature = temperature,
                        topP = topP,
                        numThreads = cpuThreads,
                        isIntegratedThinkEnabled = isIntegratedThinkEnabled,
                        context = appContext,
                        isGpuOffloadEnabled = isGpuOffloadEnabled,
                        gpuOffloadLayers = gpuOffloadLayers,
                        isOomGuardEnabled = isOomGuardEnabled
                    ).catch { e ->
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e(TAG, "Local inference error in background: ${e.message}", e)
                            accumulated.append("\n\n*Error: ${e.localizedMessage}*")
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
                                    peakRamMb = model.requiredRamMb,
                                    engineSource = "LOCAL_GGUF"
                                )
                            }
                        } else {
                            // Final chunk completed
                            val totalDuration = System.currentTimeMillis() - startTime
                            val speed = if (totalDuration > 0) (tokensCount * 1000f) / totalDuration else 20f
                            val cleaned = ChatTemplateEngine.cleanModelResponse(accumulated.toString())

                            _generationState.value = ActiveGenerationState(
                                isGenerating = false,
                                isComputingFullResponse = false,
                                streamingContent = cleaned,
                                tokensGenerated = tokensCount,
                                tokensPerSecond = (speed * 10).toInt() / 10f,
                                timeToFirstTokenMs = ttft,
                                durationMs = totalDuration,
                                peakRamMb = chunk.metrics?.peakRamUsageMb ?: model.requiredRamMb,
                                isNativeEngine = true,
                                engineDescription = chunk.metrics?.engineDescription ?: "llama.cpp ARM64 NEON",
                                engineSource = "LOCAL_GGUF"
                            )

                            // Persist to Room
                            saveCompletedAssistantMessage(
                                sessionId = sessionId,
                                content = cleaned,
                                tokensCount = tokensCount,
                                tokensPerSecond = (speed * 10).toInt() / 10f,
                                durationMs = totalDuration,
                                timeToFirstTokenMs = ttft,
                                modelTag = model.name
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (e !is kotlinx.coroutines.CancellationException) {
                        Log.e(TAG, "Fatal inference exception: ${e.message}", e)
                    }
                } finally {
                    finishBackgroundService()
                }
            } else if (ollamaClient != null) {
                // Ollama Remote Host Inference
                try {
                    val formattedMessages = messages.map {
                        com.example.data.ollama.OllamaChatMessagePayload(role = it.role, content = it.content)
                    } + com.example.data.ollama.OllamaChatMessagePayload(role = "user", content = userPrompt)

                    val request = com.example.data.ollama.OllamaChatRequest(
                        model = model.name.lowercase().replace(" ", "-"),
                        messages = formattedMessages,
                        stream = isStreaming,
                        options = com.example.data.ollama.OllamaChatOptions(
                            temperature = temperature,
                            topP = topP,
                            numThread = cpuThreads
                        )
                    )

                    ollamaClient.streamChat(request).catch { e ->
                        accumulated.append("\n\n*Ollama Error: ${e.localizedMessage}*")
                        _generationState.value = _generationState.value.copy(
                            isGenerating = false,
                            streamingContent = accumulated.toString()
                        )
                    }.collect { chunk: com.example.data.ollama.OllamaChatResponse ->
                        val tokenPart = chunk.message?.content ?: ""
                        if (tokenPart.isNotEmpty()) {
                            if (tokensCount == 0) ttft = System.currentTimeMillis() - startTime
                            tokensCount++
                            accumulated.append(tokenPart)
                            val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                            val currentSpeed = if (elapsedSec > 0.1f) tokensCount / elapsedSec else 15f

                            if (isStreaming) {
                                _generationState.value = _generationState.value.copy(
                                    isGenerating = true,
                                    streamingContent = accumulated.toString(),
                                    tokensGenerated = tokensCount,
                                    tokensPerSecond = (currentSpeed * 10).toInt() / 10f,
                                    timeToFirstTokenMs = ttft,
                                    durationMs = System.currentTimeMillis() - startTime,
                                    engineSource = "OLLAMA"
                                )
                            }
                        }

                        if (chunk.done) {
                            val totalDuration = System.currentTimeMillis() - startTime
                            val speed = if (totalDuration > 0) (tokensCount * 1000f) / totalDuration else 15f
                            val cleaned = accumulated.toString().trim()

                            _generationState.value = ActiveGenerationState(
                                isGenerating = false,
                                isComputingFullResponse = false,
                                streamingContent = cleaned,
                                tokensGenerated = tokensCount,
                                tokensPerSecond = (speed * 10).toInt() / 10f,
                                timeToFirstTokenMs = ttft,
                                durationMs = totalDuration,
                                engineSource = "OLLAMA",
                                engineDescription = "Ollama Remote API"
                            )

                            saveCompletedAssistantMessage(
                                sessionId = sessionId,
                                content = cleaned,
                                tokensCount = tokensCount,
                                tokensPerSecond = (speed * 10).toInt() / 10f,
                                durationMs = totalDuration,
                                timeToFirstTokenMs = ttft,
                                modelTag = "Ollama (${model.name})"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ollama chat error: ${e.message}", e)
                } finally {
                    finishBackgroundService()
                }
            }
        }
    }

    private suspend fun saveCompletedAssistantMessage(
        sessionId: Long,
        content: String,
        tokensCount: Int,
        tokensPerSecond: Float,
        durationMs: Long,
        timeToFirstTokenMs: Long,
        modelTag: String
    ) {
        if (content.isBlank()) return
        val repo = chatRepository ?: return
        try {
            val assistantMsg = ChatMessage(
                sessionId = sessionId,
                role = "assistant",
                content = content,
                tokensCount = tokensCount,
                tokensPerSecond = tokensPerSecond,
                generationDurationMs = durationMs,
                timeToFirstTokenMs = timeToFirstTokenMs,
                modelTag = modelTag,
                timestamp = System.currentTimeMillis()
            )
            repo.insertMessage(assistantMsg)
            repo.touchSession(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save assistant message: ${e.message}", e)
        }
    }

    fun stopCurrentGeneration() {
        activeJob?.cancel()
        activeJob = null

        val currentStream = _generationState.value.streamingContent
        val sId = activeSessionId
        val model = activeModelEntity

        if (sId != null && currentStream.isNotBlank()) {
            val cleanedStream = ChatTemplateEngine.cleanModelResponse(currentStream)
            if (cleanedStream.isNotBlank()) {
                val metrics = _generationState.value
                applicationScope.launch {
                    saveCompletedAssistantMessage(
                        sessionId = sId,
                        content = "$cleanedStream ⏹",
                        tokensCount = metrics.tokensGenerated,
                        tokensPerSecond = metrics.tokensPerSecond,
                        durationMs = metrics.durationMs,
                        timeToFirstTokenMs = metrics.timeToFirstTokenMs,
                        modelTag = model?.name ?: "Local GGUF"
                    )
                }
            }
        }

        _generationState.value = ActiveGenerationState(isGenerating = false)
        finishBackgroundService()
    }

    private fun finishBackgroundService() {
        // Background service removed
    }
}
