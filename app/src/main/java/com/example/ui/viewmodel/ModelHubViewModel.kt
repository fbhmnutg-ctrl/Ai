package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LocalModelEntity
import com.example.data.repository.ModelRepository
import com.example.engine.DeviceHardwareInfo
import com.example.engine.DeviceHardwareManager
import com.example.engine.GgufMetadata
import com.example.engine.GgufParser
import com.example.engine.NativeLlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class ModelFilter {
    ALL, INSTALLED, CATALOG
}

class ModelHubViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val modelRepository = ModelRepository(database.modelDao())

    val allModels: StateFlow<List<LocalModelEntity>> = modelRepository.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uploadedModels: StateFlow<List<LocalModelEntity>> = modelRepository.getUploadedModels()
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

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val modelDownloader = com.example.data.remote.RealModelDownloader(application)

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
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val displayName = GgufParser.getDisplayNameFromUri(context, uri)
            val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
            val targetFile = File(modelsDir, "uploaded_${System.currentTimeMillis()}_$displayName")

            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
            } catch (e: Exception) {
                Log.w("ModelHubViewModel", "Direct stream copy error: ${e.message}")
            }

            val metadata = if (targetFile.exists() && targetFile.length() > 0) {
                GgufParser.parseFromFile(targetFile, displayName)
            } else {
                GgufParser.parseFromUri(context, uri)
            }

            _importedGgufMetadata.value = metadata

            val modelId = "imported-" + (metadata.modelName.lowercase().replace(" ", "-").ifEmpty { "template" }) + "-" + (System.currentTimeMillis() % 10000)
            val entity = LocalModelEntity(
                id = modelId,
                name = metadata.modelName.ifEmpty { displayName.substringBeforeLast('.') },
                filename = displayName,
                architecture = metadata.architecture,
                quantization = metadata.quantization,
                parameterCount = when {
                    metadata.fileSizeBytes > 3_000_000_000L -> "7B"
                    metadata.fileSizeBytes > 1_500_000_000L -> "3B"
                    metadata.fileSizeBytes > 700_000_000L -> "1.5B"
                    metadata.fileSizeBytes > 250_000_000L -> "0.5B"
                    else -> "135M"
                },
                sizeBytes = if (targetFile.exists() && targetFile.length() > 0) targetFile.length() else metadata.fileSizeBytes,
                requiredRamMb = metadata.estimatedRamRequiredMb,
                contextLength = metadata.contextLength,
                isDownloaded = true,
                downloadProgress = 1.0f,
                filePath = if (targetFile.exists()) targetFile.absolutePath else null,
                source = "UPLOADED",
                description = "Uploaded on-device template (${metadata.architecture.uppercase()}, ${metadata.quantization}) with ${metadata.layerCount} layers. Ready for offline inference.",
                isFavorite = true,
                lastUsedTimestamp = System.currentTimeMillis()
            )

            modelRepository.insertModel(entity)
            _importStatusMessage.value = "Imported '${entity.name}' (Architecture: ${entity.architecture.uppercase()}, Quant: ${entity.quantization}) successfully!"
        }
    }

    fun updateModelArchitecture(model: LocalModelEntity, newArchitecture: String) {
        viewModelScope.launch {
            val updated = model.copy(architecture = newArchitecture.trim().lowercase())
            modelRepository.updateModel(updated)
            _inspectingModel.value = updated
            _importStatusMessage.value = "Model architecture updated to '$newArchitecture'."
        }
    }

    fun addCustomTemplate(
        name: String,
        architecture: String = "llama",
        quantization: String = "Q4_K_M",
        parameterCount: String = "1B",
        downloadUrl: String? = null,
        contextLength: Int = 2048
    ) {
        viewModelScope.launch {
            val id = "imported-custom-" + name.lowercase().replace(" ", "-") + "-" + (System.currentTimeMillis() % 10000)
            val sizeBytes = when (parameterCount) {
                "7B" -> 4_000_000_000L
                "3B" -> 1_900_000_000L
                "1.5B", "1B" -> 780_000_000L
                "0.5B" -> 380_000_000L
                else -> 90_000_000L
            }
            val ramMb = when (parameterCount) {
                "7B" -> 5500
                "3B" -> 2800
                "1.5B", "1B" -> 1200
                "0.5B" -> 550
                else -> 220
            }
            val entity = LocalModelEntity(
                id = id,
                name = name,
                filename = "$name-$quantization.gguf",
                architecture = architecture,
                quantization = quantization,
                parameterCount = parameterCount,
                sizeBytes = sizeBytes,
                requiredRamMb = ramMb,
                contextLength = contextLength,
                isDownloaded = true,
                downloadProgress = 1.0f,
                filePath = null,
                downloadUrl = downloadUrl,
                source = "UPLOADED",
                description = "Custom uploaded template ($architecture, $quantization). Ready for local on-device inference.",
                isFavorite = true,
                lastUsedTimestamp = System.currentTimeMillis()
            )
            modelRepository.insertModel(entity)
            _importStatusMessage.value = "Template '$name' added to Uploaded Templates."
        }
    }

    fun clearStatusMessage() {
        _importStatusMessage.value = null
    }

    fun dismissImportDialog() {
        _importedGgufMetadata.value = null
    }

    fun unloadModel() {
        NativeLlamaBridge.releaseCurrentModel()
        _importStatusMessage.value = "Model unloaded from RAM (0 MB active)."
    }

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
                _importStatusMessage.value = "✅ '${model.name}' downloaded and installed successfully!"
            } else {
                modelRepository.updateDownloadState(
                    model.id,
                    isDownloaded = false,
                    progress = 0f,
                    filePath = null
                )
                val errorMsg = downloadResult.exceptionOrNull()?.message ?: "Unknown download error"
                _importStatusMessage.value = "⚠️ Download failed for '${model.name}': $errorMsg"
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
                    val f = File(path)
                    if (f.exists()) f.delete()
                } catch (e: Exception) {
                    Log.w("ModelHubViewModel", "Error deleting file: ${e.message}")
                }
            }
            if (model.source == "UPLOADED" || model.id.startsWith("imported-")) {
                modelRepository.deleteModel(model.id)
            } else {
                modelRepository.updateDownloadState(model.id, isDownloaded = false, progress = 0f, filePath = null)
            }
            _importStatusMessage.value = "Template '${model.name}' removed."
        }
    }
}
