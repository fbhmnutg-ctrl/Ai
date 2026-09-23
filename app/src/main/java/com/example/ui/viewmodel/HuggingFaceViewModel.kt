package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LocalModelEntity
import com.example.data.remote.HuggingFaceApiService
import com.example.data.remote.HuggingFaceModel
import com.example.data.remote.ModelArchitectureClass
import com.example.data.repository.ModelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HuggingFaceViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = HuggingFaceApiService()
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val modelRepository = ModelRepository(database.modelDao())
    private val modelDownloader = com.example.data.remote.RealModelDownloader(application)

    val localModels: StateFlow<List<LocalModelEntity>> = modelRepository.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val downloadJobs = mutableMapOf<String, Job>()

    private val _models = MutableStateFlow<List<HuggingFaceModel>>(emptyList())
    val models: StateFlow<List<HuggingFaceModel>> = _models.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedClass = MutableStateFlow(ModelArchitectureClass.ALL)
    val selectedClass: StateFlow<ModelArchitectureClass> = _selectedClass.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _inspectingHfModel = MutableStateFlow<HuggingFaceModel?>(null)
    val inspectingHfModel: StateFlow<HuggingFaceModel?> = _inspectingHfModel.asStateFlow()

    private var searchJob: Job? = null

    init {
        fetchModels()
    }

    fun setSelectedClass(archClass: ModelArchitectureClass) {
        _selectedClass.value = archClass
        fetchModels()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350) // Debounce typing
            fetchModels()
        }
    }

    fun fetchModels() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = apiService.fetchSub4BModels(
                    searchQuery = _searchQuery.value,
                    archClass = _selectedClass.value
                )
                _models.value = results
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load models: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun inspectModel(model: HuggingFaceModel?) {
        _inspectingHfModel.value = model
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun addModelToLocalHub(hfModel: HuggingFaceModel, selectedQuant: String = "Q4_K_M") {
        viewModelScope.launch {
            val cleanName = hfModel.modelName.replace("-GGUF", "").replace("_GGUF", "")
            val arch = when {
                hfModel.architectureClass.contains("gemma") || hfModel.id.contains("gemma") || hfModel.id.contains("gamma") -> "gemma"
                hfModel.architectureClass.contains("qwen") -> "qwen2"
                hfModel.architectureClass.contains("llama") -> "llama"
                hfModel.architectureClass.contains("deepseek") -> "deepseek"
                hfModel.architectureClass.contains("phi") -> "phi3"
                hfModel.architectureClass.contains("smol") -> "llama"
                hfModel.architectureClass.contains("mistral") -> "mistral"
                else -> "llama"
            }

            val estBytes = when {
                hfModel.parameterCount.contains("3") -> 2_100_000_000L
                hfModel.parameterCount.contains("2") -> 1_500_000_000L
                hfModel.parameterCount.contains("1") -> 900_000_000L
                hfModel.parameterCount.contains("0.") || hfModel.parameterCount.contains("M") -> 350_000_000L
                else -> 1_200_000_000L
            }

            val requiredRam = (estBytes / (1024 * 1024) * 1.3f).toInt().coerceAtLeast(400)

            val downloadUrl = when {
                hfModel.id.contains("SmolLM2-135M", ignoreCase = true) -> "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf"
                hfModel.id.contains("SmolLM2-360M", ignoreCase = true) -> "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q8_0.gguf"
                hfModel.id.contains("Qwen2.5-0.5B", ignoreCase = true) -> "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
                hfModel.id.contains("Qwen2.5-1.5B", ignoreCase = true) -> "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
                hfModel.id.contains("Qwen2.5-3B", ignoreCase = true) -> "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
                hfModel.id.contains("Llama-3.2-1B", ignoreCase = true) -> "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
                hfModel.id.contains("Llama-3.2-3B", ignoreCase = true) -> "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
                hfModel.id.contains("gemma-2-2b", ignoreCase = true) -> "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"
                hfModel.id.contains("DeepSeek-R1", ignoreCase = true) -> "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
                hfModel.id.contains("Phi-3.5", ignoreCase = true) -> "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf"
                hfModel.id.contains("-GGUF", ignoreCase = true) -> "https://huggingface.co/${hfModel.id}/resolve/main/$cleanName-$selectedQuant.gguf"
                else -> "https://huggingface.co/bartowski/${cleanName}-GGUF/resolve/main/${cleanName}-${selectedQuant}.gguf"
            }

            val newEntity = LocalModelEntity(
                id = "hf-" + hfModel.id.replace('/', '-').lowercase(),
                name = cleanName,
                filename = "$cleanName-$selectedQuant.gguf",
                architecture = arch,
                quantization = selectedQuant,
                parameterCount = hfModel.parameterCount,
                sizeBytes = estBytes,
                requiredRamMb = requiredRam,
                contextLength = 4096,
                isDownloaded = false,
                downloadProgress = 0f,
                downloadUrl = downloadUrl,
                source = "HUGGINGFACE",
                description = "HuggingFace model by ${hfModel.author} (${hfModel.parameterCount}, $arch). Verified < 4B parameters for mobile.",
                isFavorite = false
            )

            modelRepository.insertModel(newEntity)
            _statusMessage.value = "Added '${hfModel.id}' to your Model Hub for download!"
        }
    }

    fun cancelDownload(modelId: String) {
        modelDownloader.cancel(modelId)
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        viewModelScope.launch {
            modelRepository.updateDownloadState(modelId, isDownloaded = false, progress = 0f, filePath = null)
            _statusMessage.value = "Cancelled download."
        }
    }

    fun downloadDirectlyFromHf(hfModel: HuggingFaceModel, selectedQuant: String = "Q4_K_M") {
        val cleanName = hfModel.modelName.replace("-GGUF", "").replace("_GGUF", "")
        val modelId = "hf-" + hfModel.id.replace('/', '-').lowercase()

        if (downloadJobs.containsKey(modelId)) return

        val job = viewModelScope.launch {
            val arch = when {
                hfModel.architectureClass.contains("gemma") || hfModel.id.contains("gemma") || hfModel.id.contains("gamma") -> "gemma"
                hfModel.architectureClass.contains("qwen") -> "qwen2"
                hfModel.architectureClass.contains("llama") -> "llama"
                hfModel.architectureClass.contains("deepseek") -> "deepseek"
                hfModel.architectureClass.contains("phi") -> "phi3"
                hfModel.architectureClass.contains("smol") -> "llama"
                hfModel.architectureClass.contains("mistral") -> "mistral"
                else -> "llama"
            }

            val estBytes = when {
                hfModel.parameterCount.contains("3") -> 2_100_000_000L
                hfModel.parameterCount.contains("2") -> 1_500_000_000L
                hfModel.parameterCount.contains("1") -> 900_000_000L
                hfModel.parameterCount.contains("0.") || hfModel.parameterCount.contains("M") -> 350_000_000L
                else -> 1_200_000_000L
            }

            val requiredRam = (estBytes / (1024 * 1024) * 1.3f).toInt().coerceAtLeast(400)

            val downloadUrl = when {
                hfModel.id.contains("SmolLM2-135M", ignoreCase = true) -> "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf"
                hfModel.id.contains("SmolLM2-360M", ignoreCase = true) -> "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q8_0.gguf"
                hfModel.id.contains("Qwen2.5-0.5B", ignoreCase = true) -> "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
                hfModel.id.contains("Qwen2.5-1.5B", ignoreCase = true) -> "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
                hfModel.id.contains("Qwen2.5-3B", ignoreCase = true) -> "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
                hfModel.id.contains("Llama-3.2-1B", ignoreCase = true) -> "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
                hfModel.id.contains("Llama-3.2-3B", ignoreCase = true) -> "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
                hfModel.id.contains("gemma-2-2b", ignoreCase = true) -> "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"
                hfModel.id.contains("DeepSeek-R1", ignoreCase = true) -> "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
                hfModel.id.contains("Phi-3.5", ignoreCase = true) -> "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf"
                hfModel.id.contains("-GGUF", ignoreCase = true) -> "https://huggingface.co/${hfModel.id}/resolve/main/$cleanName-$selectedQuant.gguf"
                else -> "https://huggingface.co/bartowski/${cleanName}-GGUF/resolve/main/${cleanName}-${selectedQuant}.gguf"
            }

            val newEntity = LocalModelEntity(
                id = modelId,
                name = cleanName,
                filename = "$cleanName-$selectedQuant.gguf",
                architecture = arch,
                quantization = selectedQuant,
                parameterCount = hfModel.parameterCount,
                sizeBytes = estBytes,
                requiredRamMb = requiredRam,
                contextLength = 4096,
                isDownloaded = false,
                downloadProgress = 0.01f,
                downloadUrl = downloadUrl,
                source = "HUGGINGFACE",
                description = "Direct HuggingFace GGUF download by ${hfModel.author} (${hfModel.parameterCount}, $arch).",
                isFavorite = true,
                lastUsedTimestamp = System.currentTimeMillis()
            )

            modelRepository.insertModel(newEntity)
            _statusMessage.value = "📥 Downloading GGUF directly for '$cleanName'..."

            val result = modelDownloader.downloadModel(newEntity) { progress, _, _, _ ->
                modelRepository.updateDownloadState(
                    modelId,
                    isDownloaded = progress >= 1.0f,
                    progress = progress,
                    filePath = null
                )
            }

            if (result.isSuccess) {
                val file = result.getOrThrow()
                modelRepository.updateDownloadState(
                    modelId,
                    isDownloaded = true,
                    progress = 1.0f,
                    filePath = file.absolutePath
                )
                _statusMessage.value = "✅ '$cleanName' downloaded successfully!"
            } else {
                modelRepository.updateDownloadState(
                    modelId,
                    isDownloaded = false,
                    progress = 0f,
                    filePath = null
                )
                _statusMessage.value = "⚠️ Download failed for '$cleanName': ${result.exceptionOrNull()?.message}"
            }
            downloadJobs.remove(modelId)
        }
        downloadJobs[modelId] = job
    }
}
