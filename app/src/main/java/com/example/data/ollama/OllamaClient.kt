package com.example.data.ollama

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OllamaClient(
    private var baseUrl: String = "http://10.0.2.2:11434/"
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val chatResponseAdapter = moshi.adapter(OllamaChatResponse::class.java)
    private val pullResponseAdapter = moshi.adapter(OllamaPullResponse::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var api: OllamaApi = buildApi(sanitizeUrl(baseUrl))

    private fun sanitizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        return clean
    }

    private fun buildApi(url: String): OllamaApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(OllamaApi::class.java)
    }

    fun updateBaseUrl(newUrl: String) {
        val formatted = sanitizeUrl(newUrl)
        this.baseUrl = formatted
        this.api = buildApi(formatted)
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun checkHealth(): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val response = api.getVersion()
            val latency = System.currentTimeMillis() - startTime
            if (response.isSuccessful && response.body() != null) {
                Result.success(Pair(response.body()!!.version, latency))
            } else {
                Result.failure(Exception("Ollama responded with HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listModels(): Result<List<OllamaModelTag>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTags()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.models)
            } else {
                Result.failure(Exception("Failed to fetch models: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChat(request: OllamaChatRequest): Flow<OllamaChatResponse> = flow {
        val response = api.streamChat(request)
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Failed to start chat stream: HTTP ${response.code()}")
        }

        val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim()
                if (!trimmed.isNullOrEmpty()) {
                    val parsed = chatResponseAdapter.fromJson(trimmed)
                    if (parsed != null) {
                        emit(parsed)
                    }
                }
            }
        } finally {
            reader.close()
        }
    }.flowOn(Dispatchers.IO)

    fun streamPull(modelName: String): Flow<OllamaPullResponse> = flow {
        val response = api.streamPull(OllamaPullRequest(name = modelName, stream = true))
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Failed to pull model: HTTP ${response.code()}")
        }

        val reader = BufferedReader(InputStreamReader(response.body()!!.byteStream()))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim()
                if (!trimmed.isNullOrEmpty()) {
                    val parsed = pullResponseAdapter.fromJson(trimmed)
                    if (parsed != null) {
                        emit(parsed)
                    }
                }
            }
        } finally {
            reader.close()
        }
    }.flowOn(Dispatchers.IO)
}
