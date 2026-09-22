package com.example.data.remote

import android.content.Context
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

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeCalls = ConcurrentHashMap<String, Call>()

    fun cancel(modelId: String) {
        activeCalls.remove(modelId)?.cancel()
    }

    suspend fun downloadModel(
        model: LocalModelEntity,
        onProgress: suspend (progress: Float, transferredMb: Float, totalMb: Float, speedMbPerSec: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val url = model.downloadUrl ?: when (model.id) {
            "hf-smollm2-135m-test" -> "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf"
            "qwen-2.5-0.5b-q4" -> "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
            "llama-3.2-1b-instruct-q4" -> "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
            else -> return@withContext Result.failure(IllegalArgumentException("No download URL available for model ${model.name}"))
        }

        val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val targetFile = File(modelsDir, model.filename)
        val tempFile = File(modelsDir, "${model.filename}.tmp")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PocketOllama-Android/1.0")
                .build()

            val call = client.newCall(request)
            activeCalls[model.id] = call

            val response = call.execute()
            if (!response.isSuccessful) {
                activeCalls.remove(model.id)
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body ?: run {
                activeCalls.remove(model.id)
                return@withContext Result.failure(Exception("Empty response body"))
            }

            val totalBytes = if (body.contentLength() > 0) body.contentLength() else model.sizeBytes
            val totalMb = totalBytes / (1024f * 1024f)

            var bytesCopied = 0L
            var lastUpdateBytes = 0L
            var lastUpdateTime = System.currentTimeMillis()
            val buffer = ByteArray(32 * 1024) // 32 KB buffer for faster transfers

            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(tempFile)

            outputStream.use { out ->
                inputStream.use { inStream ->
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) {
                            tempFile.delete()
                            activeCalls.remove(model.id)
                            return@withContext Result.failure(Exception("Download cancelled"))
                        }

                        out.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (elapsed >= 250 || bytesCopied == totalBytes) {
                            val bytesDelta = bytesCopied - lastUpdateBytes
                            val speed = if (elapsed > 0) {
                                (bytesDelta / (1024f * 1024f)) / (elapsed / 1000f)
                            } else 0f

                            val progress = (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            val transferredMb = bytesCopied / (1024f * 1024f)

                            onProgress(progress, transferredMb, totalMb, speed)

                            lastUpdateTime = now
                            lastUpdateBytes = bytesCopied
                        }
                    }
                }
            }

            activeCalls.remove(model.id)

            // Rename temp to real target
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(1.0f, totalMb, totalMb, 0f)
            Result.success(targetFile)
        } catch (e: Exception) {
            activeCalls.remove(model.id)
            if (tempFile.exists()) tempFile.delete()
            Result.failure(e)
        }
    }
}
