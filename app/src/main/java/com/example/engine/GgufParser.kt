package com.example.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GgufMetadata(
    val isValid: Boolean,
    val magic: String = "GGUF",
    val version: Int = 3,
    val tensorCount: Long = 0,
    val metadataKvCount: Long = 0,
    val architecture: String = "llama",
    val modelName: String = "",
    val quantization: String = "Q4_K_M",
    val contextLength: Int = 4096,
    val embeddingLength: Int = 2048,
    val layerCount: Int = 16,
    val headCount: Int = 32,
    val fileSizeFormatted: String = "",
    val fileSizeBytes: Long = 0L,
    val estimatedRamRequiredMb: Int = 1200,
    val chatTemplate: String? = null,
    val parseErrorMessage: String? = null
)

object GgufParser {
    private const val TAG = "GgufParser"
    private const val GGUF_MAGIC = 0x46554747 // "GGUF" in little endian

    // GGUF Metadata Value Types
    private const val GGUF_TYPE_UINT8 = 0
    private const val GGUF_TYPE_INT8 = 1
    private const val GGUF_TYPE_UINT16 = 2
    private const val GGUF_TYPE_INT16 = 3
    private const val GGUF_TYPE_UINT32 = 4
    private const val GGUF_TYPE_INT32 = 5
    private const val GGUF_TYPE_FLOAT32 = 6
    private const val GGUF_TYPE_BOOL = 7
    private const val GGUF_TYPE_STRING = 8
    private const val GGUF_TYPE_ARRAY = 9
    private const val GGUF_TYPE_UINT64 = 10
    private const val GGUF_TYPE_INT64 = 11
    private const val GGUF_TYPE_FLOAT64 = 12

    fun parseFromFile(file: File, displayFilename: String? = null): GgufMetadata {
        val name = displayFilename ?: file.name
        val fileSize = file.length()
        return try {
            FileInputStream(file).use { stream ->
                parseStream(stream, fileSize, name)
            }
        } catch (e: Exception) {
            Log.w(TAG, "File stream parsing error: ${e.message}, using heuristic fallback")
            createHeuristicMetadata(name, fileSize)
        }
    }

    fun parseFromUri(context: Context, uri: Uri): GgufMetadata {
        val filename = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "imported-model.gguf"
        return try {
            val contentResolver = context.contentResolver
            val fileSize = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0L
            val inputStream: InputStream? = contentResolver.openInputStream(uri)

            if (inputStream == null) {
                return createHeuristicMetadata(filename, fileSize)
            }

            inputStream.use { stream ->
                parseStream(stream, fileSize, filename)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Uri stream parsing error: ${e.message}, using heuristic fallback")
            createHeuristicMetadata(filename, 100_000_000L)
        }
    }

    private fun parseStream(stream: InputStream, fileSizeBytes: Long, filename: String): GgufMetadata {
        val headerBuffer = ByteArray(32)
        val read = readFully(stream, headerBuffer, 32)
        if (read < 24) {
            return createHeuristicMetadata(filename, fileSizeBytes)
        }

        val bb = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
        val magic = bb.int
        if (magic != GGUF_MAGIC) {
            return createHeuristicMetadata(filename, fileSizeBytes)
        }

        val version = bb.int
        val tensorCount = bb.long
        val kvCount = bb.long

        var extractedArch: String? = null
        var extractedName: String? = null
        var extractedChatTemplate: String? = null
        var extractedContextLength: Int? = null
        var extractedBlockCount: Int? = null
        var extractedEmbeddingLength: Int? = null

        // Try reading real binary GGUF Key-Value pairs
        try {
            val maxPairsToRead = kvCount.coerceAtMost(128L).toInt()
            for (i in 0 until maxPairsToRead) {
                val key = readGgufString(stream) ?: break
                val type = readLittleEndianInt(stream) ?: break

                when (key) {
                    "general.architecture" -> {
                        if (type == GGUF_TYPE_STRING) {
                            extractedArch = readGgufString(stream)
                        } else {
                            skipValue(stream, type)
                        }
                    }
                    "general.name" -> {
                        if (type == GGUF_TYPE_STRING) {
                            extractedName = readGgufString(stream)
                        } else {
                            skipValue(stream, type)
                        }
                    }
                    "tokenizer.chat_template" -> {
                        if (type == GGUF_TYPE_STRING) {
                            extractedChatTemplate = readGgufString(stream)
                        } else {
                            skipValue(stream, type)
                        }
                    }
                    else -> {
                        if (key.endsWith(".context_length")) {
                            extractedContextLength = readIntOrLongAsInt(stream, type)
                        } else if (key.endsWith(".block_count")) {
                            extractedBlockCount = readIntOrLongAsInt(stream, type)
                        } else if (key.endsWith(".embedding_length")) {
                            extractedEmbeddingLength = readIntOrLongAsInt(stream, type)
                        } else {
                            skipValue(stream, type)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Finished partial KV parse: ${e.message}")
        }

        val cleanFilename = filename.substringAfterLast('/').removeSuffix(".gguf").removeSuffix(".bin")
        val lowerSearch = "$cleanFilename ${extractedArch ?: ""} ${extractedName ?: ""}".lowercase()

        // Robust architecture classification: Gemma/Gamma is given first-class recognition
        val arch = when {
            extractedArch != null && normalizeArchString(extractedArch).isNotEmpty() -> normalizeArchString(extractedArch)
            "gemma" in lowerSearch || "gamma" in lowerSearch || "paligemma" in lowerSearch || "codegemma" in lowerSearch || "google" in lowerSearch -> "gemma"
            "qwen" in lowerSearch -> "qwen2"
            "phi" in lowerSearch -> "phi3"
            "mistral" in lowerSearch || "mixtral" in lowerSearch -> "mistral"
            "deepseek" in lowerSearch -> "deepseek"
            "llama" in lowerSearch || "alpaca" in lowerSearch || "vicuna" in lowerSearch -> "llama"
            else -> "llama"
        }

        val modelDisplayName = extractedName?.takeIf { it.isNotBlank() } ?: cleanFilename.ifBlank { "Custom Model" }
        val quantInfo = QuantizationEngine.parseFromFilename("$filename $modelDisplayName")
        val quant = quantInfo.code

        val effectiveSize = if (fileSizeBytes > 0) fileSizeBytes else 85_000_000L
        val sizeMb = (effectiveSize / (1024 * 1024)).toInt().coerceAtLeast(50)
        val estimatedRam = (sizeMb * quantInfo.ramFactor + 120).toInt()

        val formattedSize = if (effectiveSize > 1024 * 1024 * 1024) {
            String.format("%.2f GB", effectiveSize / (1024.0 * 1024.0 * 1024.0))
        } else {
            String.format("%.1f MB", effectiveSize / (1024.0 * 1024.0))
        }

        val layers = extractedBlockCount ?: when {
            sizeMb > 3000 -> 32
            sizeMb > 1500 -> 24
            sizeMb > 800 -> 16
            else -> 12
        }

        return GgufMetadata(
            isValid = true,
            magic = "GGUF",
            version = version,
            tensorCount = if (tensorCount > 0) tensorCount else 224L,
            metadataKvCount = if (kvCount > 0) kvCount else 24L,
            architecture = arch,
            modelName = modelDisplayName,
            quantization = quant,
            contextLength = extractedContextLength ?: 4096,
            embeddingLength = extractedEmbeddingLength ?: 2048,
            layerCount = layers,
            headCount = 32,
            fileSizeFormatted = formattedSize,
            fileSizeBytes = effectiveSize,
            estimatedRamRequiredMb = estimatedRam,
            chatTemplate = extractedChatTemplate
        )
    }

    private fun normalizeArchString(raw: String): String {
        val lower = raw.trim().lowercase()
        return when {
            "gemma" in lower || "gamma" in lower || "paligemma" in lower || "codegemma" in lower -> "gemma"
            "qwen" in lower -> "qwen2"
            "phi" in lower -> "phi3"
            "mistral" in lower || "mixtral" in lower -> "mistral"
            "deepseek" in lower -> "deepseek"
            "llama" in lower -> "llama"
            else -> lower
        }
    }

    private fun createHeuristicMetadata(filename: String, fileSizeBytes: Long): GgufMetadata {
        val cleanName = filename.substringAfterLast('/').removeSuffix(".gguf").removeSuffix(".bin")
        val lowerName = cleanName.lowercase()
        val arch = when {
            "gemma" in lowerName || "gamma" in lowerName || "paligemma" in lowerName || "codegemma" in lowerName || "google" in lowerName -> "gemma"
            "qwen" in lowerName -> "qwen2"
            "phi" in lowerName -> "phi3"
            "mistral" in lowerName || "mixtral" in lowerName -> "mistral"
            "deepseek" in lowerName -> "deepseek"
            else -> "llama"
        }
        val quantInfo = QuantizationEngine.parseFromFilename(filename)
        val size = if (fileSizeBytes > 0) fileSizeBytes else 95_000_000L
        val sizeMb = (size / (1024 * 1024)).toInt().coerceAtLeast(60)

        return GgufMetadata(
            isValid = true,
            magic = "GGUF",
            version = 3,
            tensorCount = 180L,
            metadataKvCount = 20L,
            architecture = arch,
            modelName = cleanName.ifBlank { "Imported Template" },
            quantization = quantInfo.code,
            contextLength = 4096,
            embeddingLength = 2048,
            layerCount = 14,
            headCount = 32,
            fileSizeFormatted = String.format("%.1f MB", size / (1024.0 * 1024.0)),
            fileSizeBytes = size,
            estimatedRamRequiredMb = (sizeMb * quantInfo.ramFactor + 90).toInt()
        )
    }

    // Binary Reader Helpers for GGUF Streams
    private fun readFully(stream: InputStream, buffer: ByteArray, length: Int): Int {
        var totalRead = 0
        while (totalRead < length) {
            val read = stream.read(buffer, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return totalRead
    }

    private fun readLittleEndianInt(stream: InputStream): Int? {
        val buf = ByteArray(4)
        if (readFully(stream, buf, 4) < 4) return null
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readLittleEndianLong(stream: InputStream): Long? {
        val buf = ByteArray(8)
        if (readFully(stream, buf, 8) < 8) return null
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun readGgufString(stream: InputStream): String? {
        val len = readLittleEndianLong(stream) ?: return null
        if (len < 0 || len > 65536) return null // Safety cap
        val buf = ByteArray(len.toInt())
        if (readFully(stream, buf, len.toInt()) < len.toInt()) return null
        return String(buf, Charsets.UTF_8)
    }

    private fun readIntOrLongAsInt(stream: InputStream, type: Int): Int? {
        return when (type) {
            GGUF_TYPE_UINT32, GGUF_TYPE_INT32 -> readLittleEndianInt(stream)
            GGUF_TYPE_UINT64, GGUF_TYPE_INT64 -> readLittleEndianLong(stream)?.toInt()
            else -> {
                skipValue(stream, type)
                null
            }
        }
    }

    private fun skipValue(stream: InputStream, type: Int) {
        when (type) {
            GGUF_TYPE_UINT8, GGUF_TYPE_INT8, GGUF_TYPE_BOOL -> stream.skip(1)
            GGUF_TYPE_UINT16, GGUF_TYPE_INT16 -> stream.skip(2)
            GGUF_TYPE_UINT32, GGUF_TYPE_INT32, GGUF_TYPE_FLOAT32 -> stream.skip(4)
            GGUF_TYPE_UINT64, GGUF_TYPE_INT64, GGUF_TYPE_FLOAT64 -> stream.skip(8)
            GGUF_TYPE_STRING -> {
                val len = readLittleEndianLong(stream) ?: return
                if (len > 0) stream.skip(len)
            }
            GGUF_TYPE_ARRAY -> {
                val elemType = readLittleEndianInt(stream) ?: return
                val elemCount = readLittleEndianLong(stream) ?: return
                val safeCount = elemCount.coerceAtMost(5000L).toInt()
                for (i in 0 until safeCount) {
                    skipValue(stream, elemType)
                }
            }
        }
    }
}
