package com.example.engine

import com.example.data.local.entity.ChatMessage

/**
 * Model architectures supported for dynamic chat templating.
 */
enum class ModelArchitecture {
    GEMMA,
    LLAMA_3,
    QWEN,
    MISTRAL,
    DEEPSEEK,
    OTHER;

    companion object {
        fun fromModel(architecture: String, modelName: String = "", filename: String = ""): ModelArchitecture {
            val lower = "$architecture $modelName $filename".lowercase()
            return when {
                "gemma" in lower -> GEMMA
                "llama-3" in lower || "llama 3" in lower || "llama3" in lower || "llama" in lower -> LLAMA_3
                "qwen" in lower -> QWEN
                "mistral" in lower || "mixtral" in lower -> MISTRAL
                "deepseek" in lower -> DEEPSEEK
                else -> LLAMA_3
            }
        }
    }
}

/**
 * Chat template formats with exact delimiter specs.
 */
enum class TemplateFormat {
    GEMMA,       // <start_of_turn>user\n{prompt}<end_of_turn>\n<start_of_turn>model\n
    LLAMA3,      // <|begin_of_text|><|start_header_id|>user<|end_header_id|>\n\n{prompt}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n
    QWEN,        // <|im_start|>system\nYou are a helpful assistant.<|im_end|>\n<|im_start|>user\n{prompt}<|im_end|>\n<|im_start|>assistant\n
    CHATML,      // Alias for QWEN / ChatML
    MISTRAL,     // <s>[INST] {prompt} [/INST]
    DEEPSEEK,    // <|begin_of_sentence|>User: {prompt}\n\nAssistant:
    ALPACA       // ### Instruction:\n...\n### Response:
}

object ChatTemplateEngine {

    /**
     * Unified stop-sequence array passed to the inference engine to prevent hallucinations.
     */
    val UNIFIED_STOP_TOKENS: Array<String> = arrayOf(
        "<end_of_turn>",
        "<start_of_turn>",
        "<|eot_id|>",
        "<|start_header_id|>",
        "<|im_end|>",
        "<|im_start|>",
        "[/INST]",
        "</s>",
        "<eos>",
        "user:",
        "assistant:",
        "User:",
        "Assistant:"
    )

    /**
     * Detects appropriate template format from model architecture and filename metadata.
     */
    fun detectFormat(architecture: String, modelName: String, filename: String): TemplateFormat {
        val lower = "$architecture $modelName $filename".lowercase()
        return when {
            "gemma" in lower -> TemplateFormat.GEMMA
            "llama-3" in lower || "llama 3" in lower || "llama3" in lower -> TemplateFormat.LLAMA3
            "qwen" in lower -> TemplateFormat.QWEN
            "mistral" in lower || "mixtral" in lower -> TemplateFormat.MISTRAL
            "deepseek" in lower -> TemplateFormat.DEEPSEEK
            "alpaca" in lower -> TemplateFormat.ALPACA
            "smol" in lower || "chatml" in lower -> TemplateFormat.QWEN
            "llama" in lower -> TemplateFormat.LLAMA3
            else -> TemplateFormat.LLAMA3
        }
    }

    /**
     * Dynamic prompt formatting for a single user turn with optional system prompt.
     */
    fun formatPrompt(
        modelType: ModelArchitecture,
        prompt: String,
        systemPrompt: String = ""
    ): String {
        val trimmedPrompt = prompt.trim()
        val trimmedSystem = systemPrompt.trim()

        return when (modelType) {
            ModelArchitecture.GEMMA -> {
                buildString {
                    if (trimmedSystem.isNotBlank()) {
                        append("<start_of_turn>system\n").append(trimmedSystem).append("<end_of_turn>\n")
                    }
                    append("<start_of_turn>user\n").append(trimmedPrompt).append("<end_of_turn>\n<start_of_turn>model\n")
                }
            }
            ModelArchitecture.LLAMA_3 -> {
                buildString {
                    append("<|begin_of_text|>")
                    if (trimmedSystem.isNotBlank()) {
                        append("<|start_header_id|>system<|end_header_id|>\n\n").append(trimmedSystem).append("<|eot_id|>")
                    }
                    append("<|start_header_id|>user<|end_header_id|>\n\n").append(trimmedPrompt)
                    append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
                }
            }
            ModelArchitecture.QWEN -> {
                val sys = if (trimmedSystem.isNotBlank()) trimmedSystem else "You are a helpful assistant."
                "<|im_start|>system\n$sys<|im_end|>\n<|im_start|>user\n$trimmedPrompt<|im_end|>\n<|im_start|>assistant\n"
            }
            ModelArchitecture.MISTRAL -> {
                if (trimmedSystem.isNotBlank()) {
                    "<s>[INST] $trimmedSystem\n\n$trimmedPrompt [/INST]"
                } else {
                    "<s>[INST] $trimmedPrompt [/INST]"
                }
            }
            ModelArchitecture.DEEPSEEK -> {
                if (trimmedSystem.isNotBlank()) {
                    "<|begin_of_sentence|>$trimmedSystem\n\nUser: $trimmedPrompt\n\nAssistant:"
                } else {
                    "<|begin_of_sentence|>User: $trimmedPrompt\n\nAssistant:"
                }
            }
            ModelArchitecture.OTHER -> {
                "<|begin_of_text|><|start_header_id|>user<|end_header_id|>\n\n$trimmedPrompt<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
            }
        }
    }

    /**
     * Dynamic prompt formatting by TemplateFormat enum.
     */
    fun formatPrompt(
        format: TemplateFormat,
        prompt: String,
        systemPrompt: String = ""
    ): String {
        return when (format) {
            TemplateFormat.GEMMA -> formatPrompt(ModelArchitecture.GEMMA, prompt, systemPrompt)
            TemplateFormat.LLAMA3 -> formatPrompt(ModelArchitecture.LLAMA_3, prompt, systemPrompt)
            TemplateFormat.QWEN, TemplateFormat.CHATML -> formatPrompt(ModelArchitecture.QWEN, prompt, systemPrompt)
            TemplateFormat.MISTRAL -> formatPrompt(ModelArchitecture.MISTRAL, prompt, systemPrompt)
            TemplateFormat.DEEPSEEK -> formatPrompt(ModelArchitecture.DEEPSEEK, prompt, systemPrompt)
            TemplateFormat.ALPACA -> {
                val sys = if (systemPrompt.isNotBlank()) "${systemPrompt.trim()}\n\n" else ""
                "${sys}### Instruction:\n${prompt.trim()}\n\n### Response:\n"
            }
        }
    }

    /**
     * Builds full multi-turn conversational prompt history with dynamic chat templates.
     */
    fun buildPrompt(
        format: TemplateFormat,
        systemPrompt: String,
        messages: List<ChatMessage>,
        currentUserPrompt: String
    ): String {
        val history = messages.filter { it.role == "user" || it.role == "assistant" }
        // Filter out duplicate if the last message in history is already the currentUserPrompt
        val cleanHistory = if (history.isNotEmpty() && history.last().role == "user" && history.last().content.trim() == currentUserPrompt.trim()) {
            history.dropLast(1)
        } else {
            history
        }

        val trimmedUserPrompt = currentUserPrompt.trim()
        val trimmedSystem = systemPrompt.trim()

        // If history is empty, fall back directly to single-turn formatting
        if (cleanHistory.isEmpty()) {
            return formatPrompt(format, trimmedUserPrompt, trimmedSystem)
        }

        return when (format) {
            TemplateFormat.GEMMA -> {
                buildString {
                    if (trimmedSystem.isNotBlank()) {
                        append("<start_of_turn>system\n").append(trimmedSystem).append("<end_of_turn>\n")
                    }
                    for (msg in cleanHistory) {
                        val role = if (msg.role == "assistant") "model" else "user"
                        append("<start_of_turn>$role\n")
                        append(msg.content.trim())
                        append("<end_of_turn>\n")
                    }
                    append("<start_of_turn>user\n")
                    append(trimmedUserPrompt)
                    append("<end_of_turn>\n")
                    append("<start_of_turn>model\n")
                }
            }
            TemplateFormat.LLAMA3 -> {
                buildString {
                    append("<|begin_of_text|>")
                    if (trimmedSystem.isNotBlank()) {
                        append("<|start_header_id|>system<|end_header_id|>\n\n")
                        append(trimmedSystem)
                        append("<|eot_id|>")
                    }
                    for (msg in cleanHistory) {
                        append("<|start_header_id|>${msg.role}<|end_header_id|>\n\n")
                        append(msg.content.trim())
                        append("<|eot_id|>")
                    }
                    append("<|start_header_id|>user<|end_header_id|>\n\n")
                    append(trimmedUserPrompt)
                    append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
                }
            }
            TemplateFormat.QWEN, TemplateFormat.CHATML -> {
                buildString {
                    val sys = if (trimmedSystem.isNotBlank()) trimmedSystem else "You are a helpful assistant."
                    append("<|im_start|>system\n")
                    append(sys)
                    append("<|im_end|>\n")

                    for (msg in cleanHistory) {
                        append("<|im_start|>${msg.role}\n")
                        append(msg.content.trim())
                        append("<|im_end|>\n")
                    }
                    append("<|im_start|>user\n")
                    append(trimmedUserPrompt)
                    append("<|im_end|>\n")
                    append("<|im_start|>assistant\n")
                }
            }
            TemplateFormat.MISTRAL -> {
                buildString {
                    append("<s>")
                    val sysPrefix = if (trimmedSystem.isNotBlank()) "$trimmedSystem\n\n" else ""
                    var firstTurn = true
                    var pendingUser: String? = null

                    for (msg in cleanHistory) {
                        if (msg.role == "user") {
                            pendingUser = msg.content.trim()
                        } else if (msg.role == "assistant" && pendingUser != null) {
                            val promptWithSys = if (firstTurn) "$sysPrefix$pendingUser" else pendingUser
                            append("[INST] ").append(promptWithSys).append(" [/INST] ")
                            append(msg.content.trim()).append("</s>")
                            firstTurn = false
                            pendingUser = null
                        }
                    }

                    val finalPrompt = if (firstTurn) "$sysPrefix$trimmedUserPrompt" else trimmedUserPrompt
                    append("[INST] ").append(finalPrompt).append(" [/INST]")
                }
            }
            TemplateFormat.DEEPSEEK -> {
                buildString {
                    append("<|begin_of_sentence|>")
                    if (trimmedSystem.isNotBlank()) {
                        append(trimmedSystem).append("\n\n")
                    }
                    for (msg in cleanHistory) {
                        val roleTag = if (msg.role == "assistant") "Assistant:" else "User:"
                        append(roleTag).append(" ").append(msg.content.trim()).append("\n\n")
                    }
                    append("User: ").append(trimmedUserPrompt).append("\n\nAssistant:")
                }
            }
            TemplateFormat.ALPACA -> {
                buildString {
                    if (trimmedSystem.isNotBlank()) {
                        append(trimmedSystem).append("\n\n")
                    }
                    for (msg in cleanHistory) {
                        val prefix = if (msg.role == "assistant") "### Response:\n" else "### Instruction:\n"
                        append(prefix).append(msg.content.trim()).append("\n\n")
                    }
                    append("### Instruction:\n").append(trimmedUserPrompt).append("\n\n")
                    append("### Response:\n")
                }
            }
        }
    }

    /**
     * Cleans raw model output by stripping residual special control tokens,
     * role headers, chat delimiters, and hallucinated follow-up turns.
     */
    fun cleanModelResponse(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // Strip leading instruction echo if model repeats [INST] ... [/INST]
        val leadingInstEchoRegex = Regex("^\\s*\\[INST\\].*?\\[/INST\\]\\s*", RegexOption.DOT_MATCHES_ALL)
        text = text.replace(leadingInstEchoRegex, "")

        // Strip leading prompt echo if model repeats <|begin_of_sentence|>User: ... Assistant:
        val leadingUserEchoRegex = Regex("^(?:<\\|begin_of_sentence\\|>)?\\s*user:\\s*.*?assistant:\\s*", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        text = text.replace(leadingUserEchoRegex, "")

        // 1. Strip leading control tokens or echo headers at the very start
        val leadingHeaderRegex = Regex(
            "^(\\s*<\\|[^>]*\\|>|\\s*<start_of_turn>\\s*(?:model|assistant)?|\\s*\\[/?INST\\]|\\s*<\\/?[sb]>|\\s*<eos>|\\s*<bos>|\\s*(?:assistant|model|user|system)(?::|\\s*\\n)\\s*|\\s*(?:assistant|model|user|system):\\s*)+",
            RegexOption.IGNORE_CASE
        )
        text = text.replace(leadingHeaderRegex, "")

        // 2. Truncate at the earliest occurrence of any stop sequence in the generated body
        var earliestStopIndex = -1
        for (stop in UNIFIED_STOP_TOKENS) {
            val idx = text.indexOf(stop, ignoreCase = (stop.equals("user:", true) || stop.equals("assistant:", true)))
            if (idx != -1 && (earliestStopIndex == -1 || idx < earliestStopIndex)) {
                earliestStopIndex = idx
            }
        }
        if (earliestStopIndex != -1) {
            text = text.substring(0, earliestStopIndex)
        }

        // 3. Strip any residual special control tokens using Regex
        val residualControlTokensRegex = Regex(
            "<\\|.*?\\|>|<start_of_turn>|<end_of_turn>|\\[/?INST\\]|<\\/?[sb]>|<eos>",
            RegexOption.IGNORE_CASE
        )
        text = text.replace(residualControlTokensRegex, "")

        // 4. Strip any leading role headers that may remain (with or without colon, newline)
        val leadingRoleRegex = Regex(
            "^\\s*(?:(?:assistant|model|user|system)(?::|\\s*\\n)\\s*|###\\s*response:\\s*)",
            RegexOption.IGNORE_CASE
        )
        text = text.replace(leadingRoleRegex, "")

        // 5. Strip any residual trailing role labels or delimiters
        val trailingRoleRegex = Regex(
            "\\s*(?:(?:user|assistant|model|system)(?::|\\s*\\n)?|###\\s*instruction:?)\\s*$",
            RegexOption.IGNORE_CASE
        )
        text = text.replace(trailingRoleRegex, "")

        return text.trim()
    }
}
