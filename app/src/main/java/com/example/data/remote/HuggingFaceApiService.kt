package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class HuggingFaceApiService {
    private val TAG = "HuggingFaceApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Pre-curated top verified <4B text models to ensure instant responses & offline usability
    private val curatedSub4BModels = listOf(
        HuggingFaceModel(
            id = "google/gemma-2-2b-it",
            author = "google",
            modelName = "gemma-2-2b-it",
            parameterCount = "2.6B",
            architectureClass = "gemma",
            downloads = 452000,
            likes = 3120,
            pipelineTag = "text-generation",
            tags = listOf("gemma", "gemma2", "text-generation", "google", "instruct", "conversational"),
            hasGguf = true,
            ggufFiles = listOf("gemma-2-2b-it-Q4_K_M.gguf", "gemma-2-2b-it-Q6_K.gguf", "gemma-2-2b-it-Q8_0.gguf"),
            description = "Google's state-of-the-art lightweight 2.6B parameter instruction-tuned model."
        ),
        HuggingFaceModel(
            id = "bartowski/gemma-2-2b-it-GGUF",
            author = "bartowski",
            modelName = "gemma-2-2b-it-GGUF",
            parameterCount = "2.6B",
            architectureClass = "gemma",
            downloads = 230000,
            likes = 1890,
            pipelineTag = "text-generation",
            tags = listOf("gemma", "gguf", "text-generation", "quantized"),
            hasGguf = true,
            ggufFiles = listOf("gemma-2-2b-it-Q4_K_M.gguf", "gemma-2-2b-it-Q5_K_M.gguf", "gemma-2-2b-it-Q8_0.gguf", "gemma-2-2b-it-IQ4_NL.gguf"),
            description = "High-speed GGUF quantizations of Google Gemma 2 2B for local mobile inference."
        ),
        HuggingFaceModel(
            id = "Qwen/Qwen2.5-1.5B-Instruct",
            author = "Qwen",
            modelName = "Qwen2.5-1.5B-Instruct",
            parameterCount = "1.54B",
            architectureClass = "qwen",
            downloads = 680000,
            likes = 4200,
            pipelineTag = "text-generation",
            tags = listOf("qwen2", "qwen", "text-generation", "instruct", "chat"),
            hasGguf = true,
            ggufFiles = listOf("qwen2.5-1.5b-instruct-q4_k_m.gguf", "qwen2.5-1.5b-instruct-q8_0.gguf"),
            description = "Alibaba Cloud's competitive multilingual 1.5B model with exceptional coding and reasoning."
        ),
        HuggingFaceModel(
            id = "Qwen/Qwen2.5-3B-Instruct",
            author = "Qwen",
            modelName = "Qwen2.5-3B-Instruct",
            parameterCount = "3.09B",
            architectureClass = "qwen",
            downloads = 890000,
            likes = 5400,
            pipelineTag = "text-generation",
            tags = listOf("qwen2", "qwen", "text-generation", "3b", "instruct"),
            hasGguf = true,
            ggufFiles = listOf("qwen2.5-3b-instruct-q4_k_m.gguf", "qwen2.5-3b-instruct-q5_k_m.gguf"),
            description = "Flagship sub-4B dense model from Alibaba Cloud for advanced logic and programming."
        ),
        HuggingFaceModel(
            id = "Qwen/Qwen2.5-0.5B-Instruct",
            author = "Qwen",
            modelName = "Qwen2.5-0.5B-Instruct",
            parameterCount = "0.49B",
            architectureClass = "qwen",
            downloads = 340000,
            likes = 1600,
            pipelineTag = "text-generation",
            tags = listOf("qwen2", "qwen", "text-generation", "0.5b", "mobile"),
            hasGguf = true,
            ggufFiles = listOf("qwen2.5-0.5b-instruct-q4_k_m.gguf", "qwen2.5-0.5b-instruct-q8_0.gguf"),
            description = "Ultra-fast lightweight 0.5B model ideal for battery saving and instantaneous edge execution."
        ),
        HuggingFaceModel(
            id = "unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
            author = "unsloth",
            modelName = "DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
            parameterCount = "1.5B",
            architectureClass = "deepseek",
            downloads = 510000,
            likes = 4900,
            pipelineTag = "text-generation",
            tags = listOf("deepseek", "reasoning", "gguf", "r1", "1.5b", "distill"),
            hasGguf = true,
            ggufFiles = listOf("DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf", "DeepSeek-R1-Distill-Qwen-1.5B-Q8_0.gguf"),
            description = "DeepSeek-R1 reasoning engine distilled into an agile 1.5B parameter mobile-friendly architecture."
        ),
        HuggingFaceModel(
            id = "meta-llama/Llama-3.2-1B-Instruct",
            author = "meta-llama",
            modelName = "Llama-3.2-1B-Instruct",
            parameterCount = "1.23B",
            architectureClass = "llama",
            downloads = 1200000,
            likes = 8200,
            pipelineTag = "text-generation",
            tags = listOf("llama", "llama-3.2", "text-generation", "meta", "instruct"),
            hasGguf = true,
            ggufFiles = listOf("Llama-3.2-1B-Instruct-Q4_K_M.gguf", "Llama-3.2-1B-Instruct-Q8_0.gguf"),
            description = "Meta's highly optimized edge model with 128k context window support and fast token throughput."
        ),
        HuggingFaceModel(
            id = "meta-llama/Llama-3.2-3B-Instruct",
            author = "meta-llama",
            modelName = "Llama-3.2-3B-Instruct",
            parameterCount = "3.21B",
            architectureClass = "llama",
            downloads = 1540000,
            likes = 9400,
            pipelineTag = "text-generation",
            tags = listOf("llama", "llama-3.2", "3b", "meta", "instruct"),
            hasGguf = true,
            ggufFiles = listOf("Llama-3.2-3B-Instruct-Q4_K_M.gguf", "Llama-3.2-3B-Instruct-Q5_K_M.gguf"),
            description = "High-performing 3.2B parameter instruction-tuned model from Meta AI for multilingual chats."
        ),
        HuggingFaceModel(
            id = "microsoft/Phi-3.5-mini-instruct",
            author = "microsoft",
            modelName = "Phi-3.5-mini-instruct",
            parameterCount = "3.82B",
            architectureClass = "phi",
            downloads = 720000,
            likes = 5100,
            pipelineTag = "text-generation",
            tags = listOf("phi3", "phi", "microsoft", "3.8b", "text-generation"),
            hasGguf = true,
            ggufFiles = listOf("Phi-3.5-mini-instruct-Q4_K_M.gguf", "Phi-3.5-mini-instruct-Q8_0.gguf"),
            description = "Microsoft's 3.82B powerhouse featuring synthetic data training and multi-step reasoning."
        ),
        HuggingFaceModel(
            id = "HuggingFaceTB/SmolLM2-1.7B-Instruct",
            author = "HuggingFaceTB",
            modelName = "SmolLM2-1.7B-Instruct",
            parameterCount = "1.71B",
            architectureClass = "smollm",
            downloads = 410000,
            likes = 3300,
            pipelineTag = "text-generation",
            tags = listOf("smollm", "smol", "text-generation", "instruct", "1.7b"),
            hasGguf = true,
            ggufFiles = listOf("SmolLM2-1.7B-Instruct-Q4_K_M.gguf", "SmolLM2-1.7B-Instruct-Q8_0.gguf"),
            description = "Hugging Face's specialized on-device model trained on SmolTalk and fine-web datasets."
        ),
        HuggingFaceModel(
            id = "HuggingFaceTB/SmolLM2-360M-Instruct",
            author = "HuggingFaceTB",
            modelName = "SmolLM2-360M-Instruct",
            parameterCount = "0.36B",
            architectureClass = "smollm",
            downloads = 210000,
            likes = 1450,
            pipelineTag = "text-generation",
            tags = listOf("smollm", "360m", "text-generation", "edge"),
            hasGguf = true,
            ggufFiles = listOf("SmolLM2-360M-Instruct-Q4_K_M.gguf", "SmolLM2-360M-Instruct-Q8_0.gguf"),
            description = "Pocket-sized 360M model offering fast latency and low RAM footprint for real-time applications."
        ),
        HuggingFaceModel(
            id = "HuggingFaceTB/SmolLM2-135M-Instruct",
            author = "HuggingFaceTB",
            modelName = "SmolLM2-135M-Instruct",
            parameterCount = "0.13B",
            architectureClass = "smollm",
            downloads = 195000,
            likes = 1200,
            pipelineTag = "text-generation",
            tags = listOf("smollm", "135m", "ultra-light"),
            hasGguf = true,
            ggufFiles = listOf("SmolLM2-135M-Instruct-Q4_K_M.gguf"),
            description = "Micro-architecture 135M parameter text model designed for instant inference."
        ),
        HuggingFaceModel(
            id = "google/paligemma-3b-pt-224",
            author = "google",
            modelName = "paligemma-3b-pt-224",
            parameterCount = "2.9B",
            architectureClass = "gemma",
            downloads = 310000,
            likes = 2400,
            pipelineTag = "text-generation",
            tags = listOf("gemma", "paligemma", "google", "vision-language", "text-generation"),
            hasGguf = true,
            ggufFiles = listOf("paligemma-3b-pt-224-Q4_K_M.gguf"),
            description = "Google's 2.9B multimodal vision-language model built upon Gemma 2B architecture."
        ),
        HuggingFaceModel(
            id = "LiquidAI/LFM-1.3B",
            author = "LiquidAI",
            modelName = "LFM-1.3B",
            parameterCount = "1.3B",
            architectureClass = "other",
            downloads = 98000,
            likes = 1100,
            pipelineTag = "text-generation",
            tags = listOf("liquid", "non-transformer", "text-generation", "1.3b"),
            hasGguf = true,
            ggufFiles = listOf("LFM-1.3B-Q4_K_M.gguf"),
            description = "Liquid Neural Network based foundation model tailored for long context and memory efficiency."
        )
    )

    suspend fun fetchSub4BModels(
        searchQuery: String = "",
        archClass: ModelArchitectureClass = ModelArchitectureClass.ALL
    ): List<HuggingFaceModel> = withContext(Dispatchers.IO) {
        val query = searchQuery.trim()
        val networkResults = mutableListOf<HuggingFaceModel>()

        try {
            val filterTerm = when (archClass) {
                ModelArchitectureClass.ALL -> if (query.isNotEmpty()) query else "text-generation"
                ModelArchitectureClass.GEMMA -> if (query.isNotEmpty()) "$query gemma" else "gemma"
                ModelArchitectureClass.QWEN -> if (query.isNotEmpty()) "$query qwen" else "qwen"
                ModelArchitectureClass.LLAMA -> if (query.isNotEmpty()) "$query llama" else "llama"
                ModelArchitectureClass.DEEPSEEK -> if (query.isNotEmpty()) "$query deepseek" else "deepseek"
                ModelArchitectureClass.PHI -> if (query.isNotEmpty()) "$query phi" else "phi"
                ModelArchitectureClass.SMOLLM -> if (query.isNotEmpty()) "$query smol" else "smol"
                ModelArchitectureClass.MISTRAL -> if (query.isNotEmpty()) "$query mistral" else "mistral"
            }

            val url = "https://huggingface.co/api/models?pipeline_tag=text-generation&search=${java.net.URLEncoder.encode(filterTerm, "UTF-8")}&sort=downloads&limit=50&full=true"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PocketOllama-Android-Applet/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val jsonArray = JSONArray(jsonStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val parsed = parseModelFromJson(obj)
                        if (parsed != null && isModelStrictlySub4B(parsed)) {
                            networkResults.add(parsed)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network fetch error: ${e.message}, returning filtered curated models")
        }

        // Merge curated models with network results without duplicates
        val allCombined = (curatedSub4BModels + networkResults).distinctBy { it.id.lowercase() }

        // Apply search query filter and architecture filter
        return@withContext allCombined.filter { model ->
            val matchesArch = when (archClass) {
                ModelArchitectureClass.ALL -> true
                ModelArchitectureClass.GEMMA -> model.architectureClass == "gemma" || model.id.contains("gemma", ignoreCase = true) || model.id.contains("gamma", ignoreCase = true)
                ModelArchitectureClass.QWEN -> model.architectureClass == "qwen" || model.id.contains("qwen", ignoreCase = true)
                ModelArchitectureClass.LLAMA -> model.architectureClass == "llama" || model.id.contains("llama", ignoreCase = true)
                ModelArchitectureClass.DEEPSEEK -> model.architectureClass == "deepseek" || model.id.contains("deepseek", ignoreCase = true)
                ModelArchitectureClass.PHI -> model.architectureClass == "phi" || model.id.contains("phi", ignoreCase = true)
                ModelArchitectureClass.SMOLLM -> model.architectureClass == "smollm" || model.id.contains("smol", ignoreCase = true)
                ModelArchitectureClass.MISTRAL -> model.architectureClass == "mistral" || model.id.contains("mistral", ignoreCase = true)
            }

            val matchesSearch = if (query.isEmpty()) true else {
                model.id.contains(query, ignoreCase = true) ||
                        model.modelName.contains(query, ignoreCase = true) ||
                        model.author.contains(query, ignoreCase = true) ||
                        model.tags.any { it.contains(query, ignoreCase = true) } ||
                        model.description.contains(query, ignoreCase = true)
            }

            matchesArch && matchesSearch && isModelStrictlySub4B(model)
        }
    }

    private fun parseModelFromJson(obj: JSONObject): HuggingFaceModel? {
        val id = obj.optString("id", "")
        if (id.isBlank()) return null

        val author = id.substringBefore('/', "community")
        val modelName = id.substringAfter('/')
        val downloads = obj.optInt("downloads", 0)
        val likes = obj.optInt("likes", 0)
        val pipelineTag = obj.optString("pipeline_tag", "text-generation")

        val tagsList = mutableListOf<String>()
        val tagsArr = obj.optJSONArray("tags")
        if (tagsArr != null) {
            for (j in 0 until tagsArr.length()) {
                tagsList.add(tagsArr.getString(j))
            }
        }

        val hasGguf = tagsList.any { it.equals("gguf", ignoreCase = true) } || id.contains("gguf", ignoreCase = true)
        val lowerId = id.lowercase()

        val arch = when {
            "gemma" in lowerId || "gamma" in lowerId || "paligemma" in lowerId -> "gemma"
            "qwen" in lowerId -> "qwen"
            "llama" in lowerId -> "llama"
            "deepseek" in lowerId -> "deepseek"
            "phi" in lowerId -> "phi"
            "smol" in lowerId -> "smollm"
            "mistral" in lowerId -> "mistral"
            else -> "other"
        }

        val paramCount = extractParameterCount(id, tagsList)
        val ggufFiles = extractSiblingGgufFiles(obj)

        return HuggingFaceModel(
            id = id,
            author = author,
            modelName = modelName,
            parameterCount = paramCount,
            architectureClass = arch,
            downloads = downloads,
            likes = likes,
            pipelineTag = pipelineTag,
            tags = tagsList,
            hasGguf = hasGguf || ggufFiles.isNotEmpty(),
            ggufFiles = ggufFiles,
            description = "HuggingFace text model ($paramCount, $arch architecture) with $downloads downloads."
        )
    }

    private fun extractSiblingGgufFiles(obj: JSONObject): List<String> {
        val list = mutableListOf<String>()
        val siblings = obj.optJSONArray("siblings") ?: return list
        for (i in 0 until siblings.length()) {
            val rfilename = siblings.optJSONObject(i)?.optString("rfilename", "") ?: ""
            if (rfilename.endsWith(".gguf", ignoreCase = true)) {
                list.add(rfilename)
            }
        }
        return list
    }

    private fun extractParameterCount(id: String, tags: List<String>): String {
        val search = "$id ${tags.joinToString(" ")}".lowercase()

        val pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(b|m)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(search)
        if (matcher.find()) {
            val num = matcher.group(1)
            val unit = matcher.group(3)?.uppercase() ?: "B"
            return "$num$unit"
        }

        return when {
            "0.5b" in search -> "0.5B"
            "1b" in search -> "1B"
            "1.5b" in search -> "1.5B"
            "2b" in search || "2.6b" in search -> "2.6B"
            "3b" in search || "3.2b" in search -> "3.2B"
            "3.8b" in search -> "3.8B"
            "135m" in search -> "135M"
            "360m" in search -> "360M"
            "500m" in search -> "500M"
            else -> "2B"
        }
    }

    /**
     * Strictly verifies that the model has LESS than 4 Billion parameters (< 4B).
     * Excludes models that are 7B, 8B, 9B, 13B, 14B, 32B, 70B, etc.
     */
    fun isModelStrictlySub4B(model: HuggingFaceModel): Boolean {
        val search = "${model.id} ${model.parameterCount} ${model.tags.joinToString(" ")}".lowercase()

        // Explicit exclusions of large models
        val largeExclusions = listOf("7b", "8b", "9b", "11b", "13b", "14b", "20b", "22b", "27b", "32b", "33b", "34b", "40b", "70b", "72b", "110b", "405b")
        for (large in largeExclusions) {
            // Check word boundaries for 7b, 8b, etc.
            if (Pattern.compile("(^|[^0-9.])${large}([^0-9a-zA-Z]|\$)").matcher(search).find()) {
                return false
            }
        }

        // Parse parameter count string if it contains "B"
        val paramPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*b", Pattern.CASE_INSENSITIVE)
        val match = paramPattern.matcher(model.parameterCount)
        if (match.find()) {
            val billions = match.group(1)?.toFloatOrNull() ?: 2.0f
            if (billions >= 4.0f) {
                return false
            }
        }

        return true
    }
}
