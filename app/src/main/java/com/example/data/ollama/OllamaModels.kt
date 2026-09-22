package com.example.data.ollama

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaVersionResponse(
    @Json(name = "version") val version: String = ""
)

@JsonClass(generateAdapter = true)
data class OllamaTagsResponse(
    @Json(name = "models") val models: List<OllamaModelTag> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OllamaModelTag(
    @Json(name = "name") val name: String,
    @Json(name = "model") val model: String? = null,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "digest") val digest: String? = null,
    @Json(name = "details") val details: OllamaModelDetails? = null
)

@JsonClass(generateAdapter = true)
data class OllamaModelDetails(
    @Json(name = "format") val format: String? = "gguf",
    @Json(name = "family") val family: String? = "llama",
    @Json(name = "parameter_size") val parameterSize: String? = "3B",
    @Json(name = "quantization_level") val quantizationLevel: String? = "Q4_K_M"
)

@JsonClass(generateAdapter = true)
data class OllamaChatMessagePayload(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OllamaChatOptions(
    @Json(name = "temperature") val temperature: Float? = 0.7f,
    @Json(name = "top_p") val topP: Float? = 0.9f,
    @Json(name = "num_thread") val numThread: Int? = 4
)

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OllamaChatMessagePayload>,
    @Json(name = "stream") val stream: Boolean = true,
    @Json(name = "options") val options: OllamaChatOptions? = null
)

@JsonClass(generateAdapter = true)
data class OllamaChatResponse(
    @Json(name = "model") val model: String? = null,
    @Json(name = "message") val message: OllamaChatMessagePayload? = null,
    @Json(name = "done") val done: Boolean = false,
    @Json(name = "total_duration") val totalDuration: Long? = null,
    @Json(name = "load_duration") val loadDuration: Long? = null,
    @Json(name = "prompt_eval_count") val promptEvalCount: Int? = null,
    @Json(name = "eval_count") val evalCount: Int? = null,
    @Json(name = "eval_duration") val evalDuration: Long? = null
)

@JsonClass(generateAdapter = true)
data class OllamaPullRequest(
    @Json(name = "name") val name: String,
    @Json(name = "stream") val stream: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OllamaPullResponse(
    @Json(name = "status") val status: String? = "",
    @Json(name = "digest") val digest: String? = null,
    @Json(name = "total") val total: Long? = null,
    @Json(name = "completed") val completed: Long? = null
)
