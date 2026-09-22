package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LocalModelEntity
import com.example.data.repository.ModelRepository
import com.example.engine.DeviceHardwareInfo
import com.example.engine.DeviceHardwareManager
import com.example.engine.GgufMetadata
import com.example.engine.GgufParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ModelFilter {
    ALL, INSTALLED, CATALOG
}

class ModelHubViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val modelRepository = ModelRepository(database.modelDao())

    val allModels: StateFlow<List<LocalModelEntity>> = modelRepository.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFilter = MutableStateFlow(ModelFilter.ALL)
    val selectedFilter: StateFlow<ModelFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _hardwareInfo = MutableStateFlow(DeviceHardwareManager.getHardwareInfo(application))
    val hardwareInfo: StateFlow<DeviceHardwareInfo> = _hardwareInfo.asStateFlow()

    private val _importedGgufMetadata = MutableStateFlow<GgufMetadata?>(null)
    val importedGgufMetadata: StateFlow<GgufMetadata?> = _importedGgufMetadata.asStateFlow()

    private val _inspectingModel = MutableStateFlow<LocalModelEntity?>(null)
    val inspectingModel: StateFlow<LocalModelEntity?> = _inspectingModel.asStateFlow()

    companion object {
        const val HF_TEST_TEMPLATE_ID = "hf-smollm2-135m-test"
        val HF_TEST_TEMPLATE = LocalModelEntity(
            id = HF_TEST_TEMPLATE_ID,
            name = "SmolLM2 135M (Hugging Face)",
            filename = "SmolLM2-135M-Instruct-Q4_K_M.gguf",
            architecture = "llama",
            quantization = "Q4_K_M",
            parameterCount = "135M",
            sizeBytes = 89128960L, // ~85 MB
            requiredRamMb = 220,
            contextLength = 2048,
            isDownloaded = false,
            downloadProgress = 0f,
            source = "HUGGING_FACE",
            description = "Ultra-lightweight test template from Hugging Face website (huggingface.co/HuggingFaceTB/SmolLM2-135M). Designed for fast mobile CPU testing and instant evaluation.",
            isFavorite = true
        )
    }

    init {
        viewModelScope.launch {
            val existing = modelRepository.getModelByIdDirect(HF_TEST_TEMPLATE_ID)
            if (existing == null) {
                modelRepository.insertModel(HF_TEST_TEMPLATE)
            }
        }
    }

    private val downloadJobs = mutableMapOf<String, Job>()

    fun setFilter(filter: ModelFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun inspectModel(model: LocalModelEntity?) {
        _inspectingModel.value = model
    }

    fun refreshHardware() {
        _hardwareInfo.value = DeviceHardwareManager.getHardwareInfo(getApplication())
    }

    fun importGgufFile(uri: Uri) {
        viewModelScope.launch {
            val metadata = GgufParser.parseFromUri(getApplication(), uri)
            _importedGgufMetadata.value = metadata

            if (metadata.isValid) {
                val modelId = "imported-" + (metadata.modelName.lowercase().replace(" ", "-").ifEmpty { "custom-model" }) + "-" + System.currentTimeMillis() % 10000
                val entity = LocalModelEntity(
                    id = modelId,
                    name = metadata.modelName.ifEmpty { "Imported GGUF Model" },
                    filename = uri.lastPathSegment ?: "custom.gguf",
                    architecture = metadata.architecture,
                    quantization = metadata.quantization,
                    parameterCount = when {
                        metadata.fileSizeBytes > 3_000_000_000L -> "7B"
                        metadata.fileSizeBytes > 1_500_000_000L -> "3B"
                        metadata.fileSizeBytes > 700_000_000L -> "1.5B"
                        else -> "0.5B"
                    },
                    sizeBytes = metadata.fileSizeBytes,
                    requiredRamMb = metadata.estimatedRamRequiredMb,
                    contextLength = metadata.contextLength,
                    isDownloaded = true,
                    downloadProgress = 1.0f,
                    filePath = uri.toString(),
                    source = "LOCAL_GGUF",
                    description = "Custom GGUF model imported from local storage. Parsed ${metadata.tensorCount} tensors across ${metadata.layerCount} layers.",
                    isFavorite = false,
                    lastUsedTimestamp = System.currentTimeMillis()
                )
                modelRepository.insertModel(entity)
            }
        }
    }

    fun dismissImportDialog() {
        _importedGgufMetadata.value = null
    }

    private val modelDownloader = com.example.data.remote.RealModelDownloader(application)

    fun startModelDownload(model: LocalModelEntity) {
        if (downloadJobs.containsKey(model.id)) return

        val job = viewModelScope.launch {
            modelRepository.updateDownloadState(model.id, isDownloaded = false, progress = 0.01f, filePath = null)

            val downloadResult = modelDownloader.downloadModel(model) { progress, _, _, _ ->
                modelRepository.updateDownloadState(
                    model.id,
                    isDownloaded = progress >= 1.0f,
                    progress = progress,
                    filePath = null
                )
            }

            if (downloadResult.isSuccess) {
                val file = downloadResult.getOrThrow()
                modelRepository.updateDownloadState(
                    model.id,
                    isDownloaded = true,
                    progress = 1.0f,
                    filePath = file.absolutePath
                )
            } else {
                modelRepository.updateDownloadState(
                    model.id,
                    isDownloaded = false,
                    progress = 0f,
                    filePath = null
                )
            }
            downloadJobs.remove(model.id)
        }
        downloadJobs[model.id] = job
    }

    fun cancelDownload(modelId: String) {
        modelDownloader.cancel(modelId)
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        viewModelScope.launch {
            modelRepository.updateDownloadState(modelId, isDownloaded = false, progress = 0f, filePath = null)
        }
    }

    fun deleteModel(model: LocalModelEntity) {
        viewModelScope.launch {
            model.filePath?.let { path ->
                try {
                    val f = java.io.File(path)
                    if (f.exists()) f.delete()
                } catch (e: Exception) {
                    android.util.Log.w("ModelHubViewModel", "Error deleting file: ${e.message}")
                }
            }
            modelRepository.updateDownloadState(model.id, isDownloaded = false, progress = 0f, filePath = null)
            if (model.id.startsWith("imported-")) {
                modelRepository.deleteModel(model.id)
            }
        }
    }
}
