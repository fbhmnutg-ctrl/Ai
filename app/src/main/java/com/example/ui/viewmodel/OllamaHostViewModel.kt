package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LocalModelEntity
import com.example.data.ollama.OllamaClient
import com.example.data.ollama.OllamaModelTag
import com.example.data.repository.ModelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class OllamaConnectionState {
    object Idle : OllamaConnectionState()
    object Connecting : OllamaConnectionState()
    data class Connected(val version: String, val latencyMs: Long) : OllamaConnectionState()
    data class Error(val message: String) : OllamaConnectionState()
}

data class OllamaPullProgress(
    val modelName: String,
    val isPulling: Boolean = false,
    val status: String = "",
    val completedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Float = 0f,
    val error: String? = null
)

class OllamaHostViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val modelRepository = ModelRepository(database.modelDao())

    val client = OllamaClient("http://10.0.2.2:11434/")

    private val _hostUrl = MutableStateFlow("http://10.0.2.2:11434")
    val hostUrl: StateFlow<String> = _hostUrl.asStateFlow()

    private val _connectionState = MutableStateFlow<OllamaConnectionState>(OllamaConnectionState.Idle)
    val connectionState: StateFlow<OllamaConnectionState> = _connectionState.asStateFlow()

    private val _remoteModels = MutableStateFlow<List<OllamaModelTag>>(emptyList())
    val remoteModels: StateFlow<List<OllamaModelTag>> = _remoteModels.asStateFlow()

    private val _pullProgress = MutableStateFlow<OllamaPullProgress?>(null)
    val pullProgress: StateFlow<OllamaPullProgress?> = _pullProgress.asStateFlow()

    private var pullJob: Job? = null

    init {
        testConnection()
    }

    fun setHostUrl(url: String) {
        _hostUrl.value = url
        client.updateBaseUrl(url)
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionState.value = OllamaConnectionState.Connecting
            val result = client.checkHealth()
            if (result.isSuccess) {
                val (version, latency) = result.getOrThrow()
                _connectionState.value = OllamaConnectionState.Connected(version, latency)
                fetchRemoteModels()
            } else {
                _connectionState.value = OllamaConnectionState.Error(
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to connect to Ollama host"
                )
            }
        }
    }

    fun fetchRemoteModels() {
        viewModelScope.launch {
            val result = client.listModels()
            if (result.isSuccess) {
                val list = result.getOrThrow()
                _remoteModels.value = list

                // Sync each into local database as an Ollama-backed model
                list.forEach { tag ->
                    val paramSize = tag.details?.parameterSize ?: "3B"
                    val quant = tag.details?.quantizationLevel ?: "Q4_K_M"
                    val arch = tag.details?.family ?: "llama"
                    val modelEntity = LocalModelEntity(
                        id = tag.name,
                        name = tag.name,
                        filename = tag.name,
                        architecture = arch,
                        quantization = quant,
                        parameterCount = paramSize,
                        sizeBytes = tag.size,
                        requiredRamMb = 2000,
                        contextLength = 4096,
                        isDownloaded = true,
                        downloadProgress = 1.0f,
                        source = "OLLAMA",
                        description = "Served from remote Ollama host at ${_hostUrl.value}"
                    )
                    modelRepository.insertModel(modelEntity)
                }
            }
        }
    }

    fun pullModel(modelName: String) {
        pullJob?.cancel()
        _pullProgress.value = OllamaPullProgress(
            modelName = modelName,
            isPulling = true,
            status = "Initiating pull..."
        )

        pullJob = viewModelScope.launch {
            client.streamPull(modelName).catch { e ->
                _pullProgress.value = _pullProgress.value?.copy(
                    isPulling = false,
                    error = e.localizedMessage
                )
            }.collect { response ->
                val total = response.total ?: 0L
                val completed = response.completed ?: 0L
                val percent = if (total > 0) completed.toFloat() / total.toFloat() else 0f
                val statusText = response.status ?: "Pulling..."

                _pullProgress.value = OllamaPullProgress(
                    modelName = modelName,
                    isPulling = statusText != "success",
                    status = statusText,
                    completedBytes = completed,
                    totalBytes = total,
                    progressPercent = percent
                )

                if (statusText == "success") {
                    fetchRemoteModels()
                }
            }
        }
    }

    fun cancelPull() {
        pullJob?.cancel()
        pullJob = null
        _pullProgress.value = null
    }
}
