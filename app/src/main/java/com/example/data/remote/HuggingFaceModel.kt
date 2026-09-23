package com.example.data.remote

data class HuggingFaceModel(
    val id: String, // Full model name, e.g. "google/gemma-2-2b-it"
    val author: String,
    val modelName: String,
    val parameterCount: String,
    val architectureClass: String, // "gemma", "qwen", "llama", "deepseek", "phi", "smollm", "mistral", "other"
    val downloads: Int,
    val likes: Int,
    val pipelineTag: String,
    val tags: List<String>,
    val hasGguf: Boolean,
    val ggufFiles: List<String> = emptyList(),
    val directDownloadUrl: String? = null,
    val description: String = "",
    val isVerified: Boolean = true
)

enum class ModelArchitectureClass(val label: String, val queryKey: String) {
    ALL("All (< 4B)", ""),
    GEMMA("Gemma / Google", "gemma"),
    QWEN("Qwen 2.5", "qwen"),
    LLAMA("Llama 3.2", "llama"),
    DEEPSEEK("DeepSeek", "deepseek"),
    PHI("Phi-3 / 3.5", "phi"),
    SMOLLM("SmolLM2", "smol"),
    MISTRAL("Mistral / Other", "mistral")
}
