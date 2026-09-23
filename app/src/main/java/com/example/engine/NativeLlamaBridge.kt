package com.example.engine

import android.os.Build
import android.util.Log
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class NativeInferenceResult(
    val text: String,
    val tokensPerSecond: Float,
    val isNativeExecution: Boolean,
    val engineName: String,
    val errorDetails: String? = null
)

object NativeLlamaBridge {
    private const val TAG = "NativeLlamaBridge"

    @Volatile
    private var loadedModelHandle: LlamaModel? = null
    @Volatile
    private var loadedModelPath: String? = null

    /**
     * Checks whether the current device/emulator architecture is supported by the native binary.
     * The free edition of llama-android supports arm64-v8a.
     */
    fun isNativeAbiSupported(): Boolean {
        val abis = Build.SUPPORTED_ABIS
        return abis.any { it.equals("arm64-v8a", ignoreCase = true) }
    }

    fun getDeviceArchitectureSummary(): String {
        val primary = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val isArm64 = isNativeAbiSupported()
        return if (isArm64) {
            "ARM64 (Native NEON CPU/Vulkan Ready)"
        } else {
            "$primary (Emulation/Compatibility Mode)"
        }
    }

    suspend fun preloadModelIntoMemory(
        modelFilePath: String,
        contextLength: Int = 2048,
        threads: Int = 4
    ): Boolean = withContext(Dispatchers.IO) {
        val file = File(modelFilePath)
        if (!file.exists()) return@withContext false
        try {
            if (loadedModelPath == modelFilePath && loadedModelHandle != null) {
                return@withContext true
            }
            releaseCurrentModel()
            Log.i(TAG, "Pre-loading native GGUF model into memory: $modelFilePath")
            val loaded = Llama.loadModel(
                modelPath = file.absolutePath,
                config = LlamaConfig(
                    contextSize = contextLength.coerceIn(512, 4096),
                    threads = threads.coerceIn(1, 8)
                )
            )
            loadedModelHandle = loaded
            loadedModelPath = modelFilePath
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to preload native model: ${t.message}")
            false
        }
    }

    suspend fun executeInference(
        modelFilePath: String,
        prompt: String,
        systemPrompt: String,
        contextLength: Int = 2048,
        threads: Int = 4,
        maxTokens: Int = 2048
    ): NativeInferenceResult = withContext(Dispatchers.IO) {
        val file = File(modelFilePath)
        if (!file.exists()) {
            return@withContext NativeInferenceResult(
                text = "",
                tokensPerSecond = 0f,
                isNativeExecution = false,
                engineName = "Offline CPU Fallback",
                errorDetails = "Model weight file not found at: $modelFilePath"
            )
        }

        try {
            // Load model or reuse if already loaded
            val model = if (loadedModelPath == modelFilePath && loadedModelHandle != null) {
                loadedModelHandle!!
            } else {
                releaseCurrentModel()
                Log.i(TAG, "Loading native GGUF model via llama.cpp from $modelFilePath...")
                val loaded = Llama.loadModel(
                    modelPath = file.absolutePath,
                    config = LlamaConfig(
                        contextSize = contextLength.coerceIn(512, 4096),
                        threads = threads.coerceIn(1, 8)
                    )
                )
                loadedModelHandle = loaded
                loadedModelPath = modelFilePath
                loaded
            }

            Log.i(TAG, "Executing native completion on llama.cpp...")
            val completionResult = Llama.complete(
                model = model,
                prompt = prompt,
                systemPrompt = systemPrompt,
                maxTokens = maxTokens
            )

            Log.i(TAG, "Native completion finished at ${completionResult.tokensPerSecond} tok/s")
            NativeInferenceResult(
                text = completionResult.text,
                tokensPerSecond = completionResult.tokensPerSecond,
                isNativeExecution = true,
                engineName = "llama.cpp (Native C++ ARM64)"
            )
        } catch (linkError: UnsatisfiedLinkError) {
            Log.w(TAG, "Native llama.so UnsatisfiedLinkError: ${linkError.message}")
            NativeInferenceResult(
                text = "",
                tokensPerSecond = 0f,
                isNativeExecution = false,
                engineName = "llama.cpp (ABI Incompatible)",
                errorDetails = "Native library libllama.so requires ARM64 device (Host is ${Build.SUPPORTED_ABIS.firstOrNull()})."
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Native execution error", t)
            NativeInferenceResult(
                text = "",
                tokensPerSecond = 0f,
                isNativeExecution = false,
                engineName = "llama.cpp (Execution Failed)",
                errorDetails = t.localizedMessage ?: "Unknown native runtime error"
            )
        }
    }

    fun isModelLoaded(): Boolean = loadedModelHandle != null
    fun getLoadedModelPath(): String? = loadedModelPath

    fun releaseCurrentModel(): Boolean {
        return try {
            val handle = loadedModelHandle
            if (handle != null) {
                Llama.releaseModel(handle)
                loadedModelHandle = null
                loadedModelPath = null
                Log.i(TAG, "Native model successfully released and RAM cleared.")
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error releasing model: ${t.message}")
            false
        }
    }
}
