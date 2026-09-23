package com.example.engine

import com.example.data.local.entity.ChatMessage

enum class TemplateFormat {
    CHATML,      // Qwen, DeepSeek, SmolLM, etc. <|im_start|>...<|im_end|>
    LLAMA3,      // Llama 3 / 3.1 / 3.2 <|start_header_id|>...<|end_header_id|>...<|eot_id|>
    GEMMA,       // <start_of_turn>...<end_of_turn>
    MISTRAL,     // [INST] ... [/INST]
    ALPACA       // ### Instruction:\n...\n### Response:
}

object ChatTemplateEngine {

    fun detectFormat(architecture: String, modelName: String, filename: String): TemplateFormat {
        val lower = "$architecture $modelName $filename".lowercase()
        return when {
            "llama-3" in lower || "llama 3" in lower || "llama3" in lower -> TemplateFormat.LLAMA3
            "gemma" in lower -> TemplateFormat.GEMMA
            "mistral" in lower || "mixtral" in lower -> TemplateFormat.MISTRAL
            "qwen" in lower || "deepseek" in lower || "smol" in lower || "chatml" in lower -> TemplateFormat.CHATML
            else -> TemplateFormat.CHATML
        }
    }

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

        return when (format) {
            TemplateFormat.LLAMA3 -> {
                buildString {
                    append("<|begin_of_text|>")
                    if (systemPrompt.isNotBlank()) {
                        append("<|start_header_id|>system<|end_header_id|>\n\n")
                        append(systemPrompt.trim())
                        append("<|eot_id|>")
                    }
                    for (msg in cleanHistory) {
                        append("<|start_header_id|>${msg.role}<|end_header_id|>\n\n")
                        append(msg.content.trim())
                        append("<|eot_id|>")
                    }
                    append("<|start_header_id|>user<|end_header_id|>\n\n")
                    append(currentUserPrompt.trim())
                    append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
                }
            }
            TemplateFormat.CHATML -> {
                buildString {
                    if (systemPrompt.isNotBlank()) {
                        append("<|im_start|>system\n")
                        append(systemPrompt.trim())
                        append("\n<|im_end|>\n")
                    }
                    for (msg in cleanHistory) {
                        append("<|im_start|>${msg.role}\n")
                        append(msg.content.trim())
                        append("\n<|im_end|>\n")
                    }
                    append("<|im_start|>user\n")
                    append(currentUserPrompt.trim())
                    append("\n<|im_end|>\n")
                    append("<|im_start|>assistant\n")
                }
            }
            TemplateFormat.GEMMA -> {
                buildString {
                    for (msg in cleanHistory) {
                        val role = if (msg.role == "assistant") "model" else "user"
                        append("<start_of_turn>$role\n")
                        append(msg.content.trim())
                        append("<end_of_turn>\n")
                    }
                    append("<start_of_turn>user\n")
                    if (systemPrompt.isNotBlank() && cleanHistory.isEmpty()) {
                        append(systemPrompt.trim()).append("\n\n")
                    }
                    append(currentUserPrompt.trim())
                    append("<end_of_turn>\n")
                    append("<start_of_turn>model\n")
                }
            }
            TemplateFormat.MISTRAL -> {
                buildString {
                    val sys = if (systemPrompt.isNotBlank()) "<<SYS>>\n${systemPrompt.trim()}\n<</SYS>>\n\n" else ""
                    append("[INST] $sys${currentUserPrompt.trim()} [/INST]")
                }
            }
            TemplateFormat.ALPACA -> {
                buildString {
                    if (systemPrompt.isNotBlank()) {
                        append("${systemPrompt.trim()}\n\n")
                    }
                    for (msg in cleanHistory) {
                        val prefix = if (msg.role == "assistant") "### Response:\n" else "### Instruction:\n"
                        append(prefix).append(msg.content.trim()).append("\n\n")
                    }
                    append("### Instruction:\n").append(currentUserPrompt.trim()).append("\n\n")
                    append("### Response:\n")
                }
            }
        }
    }
}
