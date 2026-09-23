package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.entity.LocalModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class RealModelDownloader(private val context: Context) {

    private val TAG = "RealModelDownloader"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val activeCalls = ConcurrentHashMap<String, Call>()

    fun cancel(modelId: String) {
        activeCalls.remove(modelId)?.cancel()
    }

    private fun getCandidateUrls(model: LocalModelEntity): List<String> {
        val candidates = mutableListOf<String>()

        // Primary downloadUrl if set
        if (!model.downloadUrl.isNullOrBlank()) {
            candidates.add(model.downloadUrl)
        }

        val name = model.name.lowercase()
        val id = model.id.lowercase()

        // Known high-speed direct GGUF links on HuggingFace for top models
        when {
            name.contains("smollm2-135m") || id.contains("smollm2-135m") || name.contains("smollm") -> {
                candidates.add("https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf")
                candidates.add("https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf")
            }
            name.contains("smollm2-360m") || id.contains("smollm2-360m") -> {
                candidates.add("https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q8_0.gguf")
            }
            name.contains("qwen2.5-0.5b") || id.contains("qwen-2.5-0.5b") || id.contains("qwen2.5-0.5b") || (name.contains("qwen") && name.contains("0.5b")) -> {
                candidates.add("https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf")
                candidates.add("https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf")
            }
            name.contains("qwen2.5-1.5b") || id.contains("qwen-2.5-1.5b") || id.contains("qwen2.5-1.5b") || (name.contains("qwen") && name.contains("1.5b")) -> {
                candidates.add("https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf")
            }
            name.contains("qwen2.5-3b") || id.contains("qwen-2.5-3b") || id.contains("qwen2.5-3b") -> {
                candidates.add("https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf")
            }
            name.contains("llama-3.2-1b") || id.contains("llama-3.2-1b") -> {
                candidates.add("https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf")
                candidates.add("https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q5_K_M.gguf")
            }
            name.contains("llama-3.2-3b") || id.contains("llama-3.2-3b") -> {
                candidates.add("https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf")
            }
            name.contains("gemma-2-2b") || id.contains("gemma-2-2b") || name.contains("gemma") -> {
                candidates.add("https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf")
            }
            name.contains("deepseek-r1") || id.contains("deepseek-r1") || name.contains("deepseek") -> {
                candidates.add("https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-IQ4_NL.gguf")
                candidates.add("https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf")
            }
            name.contains("phi-3.5") || id.contains("phi-3.5") || name.contains("phi") -> {
                candidates.add("https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf")
            }
        }

        // Generic fallback URL attempt
        if (!model.downloadUrl.isNullOrBlank() && !model.downloadUrl.contains("-GGUF")) {
            val cleanModelName = model.name.replace(" ", "-")
            val quant = if (model.quantization.isNotBlank()) model.quantization else "Q4_K_M"
            candidates.add("https://huggingface.co/bartowski/${cleanModelName}-GGUF/resolve/main/${cleanModelName}-${quant}.gguf")
        }

        return candidates.distinct()
    }

    suspend fun downloadModel(
        model: LocalModelEntity,
        onProgress: suspend (progress: Float, transferredMb: Float, totalMb: Float, speedMbPerSec: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val candidates = getCandidateUrls(model)
        if (candidates.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No download candidates available for model ${model.name}"))
        }

        val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val targetFile = File(modelsDir, model.filename)
        val tempFile = File(modelsDir, "${model.filename}.tmp")

        var lastException: Exception? = null

        for (url in candidates) {
            Log.d(TAG, "Attempting download from candidate URL: $url")
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "*/*")
                    .build()

                val call = client.newCall(request)
                activeCalls[model.id] = call

                val response = call.execute()
                if (!response.isSuccessful) {
                    activeCalls.remove(model.id)
                    lastException = Exception("HTTP ${response.code} for $url")
                    Log.w(TAG, "Candidate failed: ${response.code} for $url")
                    response.close()
                    continue
                }

                val body = response.body
                if (body == null) {
                    activeCalls.remove(model.id)
                    lastException = Exception("Empty body for $url")
                    response.close()
                    continue
                }

                val contentType = response.header("Content-Type")?.lowercase() ?: ""
                if (contentType.contains("text/html") || contentType.contains("application/json")) {
                    // HF returned HTML or error JSON page instead of binary GGUF
                    activeCalls.remove(model.id)
                    lastException = Exception("URL returned HTML webpage instead of binary file for $url")
                    Log.w(TAG, "Candidate returned non-binary Content-Type '$contentType' for $url")
                    response.close()
                    continue
                }

                val totalBytes = if (body.contentLength() > 0) body.contentLength() else model.sizeBytes
                val totalMb = (totalBytes / (1024f * 1024f)).coerceAtLeast(10f)

                var bytesCopied = 0L
                var lastUpdateBytes = 0L
                var lastUpdateTime = System.currentTimeMillis()
                val buffer = ByteArray(64 * 1024) // 64 KB buffer for high performance

                val inputStream: InputStream = body.byteStream()
                if (tempFile.exists()) tempFile.delete()
                val outputStream = FileOutputStream(tempFile)

                outputStream.use { out ->
                    inputStream.use { inStream ->
                        var bytesRead: Int
                        while (inStream.read(buffer).also { bytesRead = it } != -1) {
                            if (!isActive) {
                                tempFile.delete()
                                activeCalls.remove(model.id)
                                response.close()
                                return@withContext Result.failure(Exception("Download cancelled by user"))
                            }

                            out.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastUpdateTime
                            if (elapsed >= 200 || bytesCopied == totalBytes) {
                                val bytesDelta = bytesCopied - lastUpdateBytes
                                val speed = if (elapsed > 0) {
                                    (bytesDelta / (1024f * 1024f)) / (elapsed / 1000f)
                                } else 0f

                                val progress = (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0.01f, 0.99f)
                                val transferredMb = bytesCopied / (1024f * 1024f)

                                onProgress(progress, transferredMb, totalMb, speed)

                                lastUpdateTime = now
                                lastUpdateBytes = bytesCopied
                            }
                        }
                    }
                }

                response.close()
                activeCalls.remove(model.id)

                if (tempFile.length() < 100_000) {
                    // Less than 100KB, likely invalid HTML or fragment
                    tempFile.delete()
                    lastException = Exception("Downloaded file too small (${tempFile.length()} bytes)")
                    continue
                }

                // Rename temp to real target
                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                onProgress(1.0f, totalMb, totalMb, 0f)
                return@withContext Result.success(targetFile)

            } catch (e: Exception) {
                activeCalls.remove(model.id)
                if (tempFile.exists()) tempFile.delete()
                if (e.message?.contains("cancelled", ignoreCase = true) == true) {
                    return@withContext Result.failure(e)
                }
                lastException = e
                Log.e(TAG, "Download error for $url: ${e.message}", e)
            }
        }

        Result.failure(lastException ?: Exception("All download URLs failed for ${model.name}"))
    }
}
