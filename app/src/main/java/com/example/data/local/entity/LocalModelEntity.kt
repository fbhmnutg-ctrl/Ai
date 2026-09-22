package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_models")
data class LocalModelEntity(
    @PrimaryKey
    val id: String, // e.g., "llama-3.2-1b-q4", "deepseek-r1-1.5b-q4", "custom-model-file"
    val name: String, // Display Name
    val filename: String,
    val architecture: String, // "llama", "qwen2", "phi3", "gemma2"
    val quantization: String, // "Q4_K_M", "Q5_K_M", "Q8_0", "FP16"
    val parameterCount: String, // "1.24B", "3.21B", "1.54B"
    val sizeBytes: Long,
    val requiredRamMb: Int,
    val contextLength: Int,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f, // 0.0 to 1.0
    val filePath: String? = null,
    val source: String = "LOCAL_GGUF", // "LOCAL_GGUF" or "OLLAMA"
    val description: String = "",
    val isFavorite: Boolean = false,
    val lastUsedTimestamp: Long = 0L,
    val downloadUrl: String? = null
)
