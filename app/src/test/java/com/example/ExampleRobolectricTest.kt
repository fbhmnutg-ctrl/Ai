package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.QuantizationEngine
import com.example.engine.QuantCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PocketOllama", appName)
  }

  @Test
  fun `quantization engine parses high precision Q6_K_P and Q8_0 correctly`() {
    val q6kp = QuantizationEngine.find("Q6_K_P")
    assertEquals("Q6_K_P", q6kp.code)
    assertEquals(QuantCategory.HIGH_FIDELITY, q6kp.category)
    assertEquals(6.56f, q6kp.bitsPerWeight, 0.01f)
    assertTrue(q6kp.qualityTier.contains("Near-Lossless"))

    val q8 = QuantizationEngine.find("Q8_0")
    assertEquals("Q8_0", q8.code)
    assertEquals(QuantCategory.HIGH_FIDELITY, q8.category)
    assertEquals(8.50f, q8.bitsPerWeight, 0.01f)
  }

  @Test
  fun `quantization engine parses importance matrix and balanced quants`() {
    val iq4nl = QuantizationEngine.find("IQ4_NL")
    assertEquals("IQ4_NL", iq4nl.code)
    assertEquals(QuantCategory.BALANCED, iq4nl.category)
    assertEquals(4.50f, iq4nl.bitsPerWeight, 0.01f)

    val q5km = QuantizationEngine.find("Q5_K_M")
    assertEquals("Q5_K_M", q5km.code)
    assertEquals(QuantCategory.BALANCED, q5km.category)
  }

  @Test
  fun `quantization engine extracts quant from GGUF filenames`() {
    val extracted1 = QuantizationEngine.extractFromFilename("deepseek-r1-distill-qwen-1.5b-q6_k_p.gguf")
    assertEquals("Q6_K_P", extracted1)

    val extracted2 = QuantizationEngine.extractFromFilename("llama-3.2-1b-instruct-q8_0.gguf")
    assertEquals("Q8_0", extracted2)

    val extracted3 = QuantizationEngine.extractFromFilename("qwen2.5-coder-1.5b-iq4_nl.gguf")
    assertEquals("IQ4_NL", extracted3)
  }

  @Test
  fun `gemma dynamic chat template formats correctly`() {
    val prompt = "Explain quantum computing."
    val formatted = com.example.engine.ChatTemplateEngine.formatPrompt(
      com.example.engine.ModelArchitecture.GEMMA,
      prompt
    )
    val expected = "<start_of_turn>user\nExplain quantum computing.<end_of_turn>\n<start_of_turn>model\n"
    assertEquals(expected, formatted)
  }

  @Test
  fun `llama 3 dynamic chat template formats correctly`() {
    val prompt = "Write a quicksort in Kotlin."
    val formatted = com.example.engine.ChatTemplateEngine.formatPrompt(
      com.example.engine.ModelArchitecture.LLAMA_3,
      prompt
    )
    val expected = "<|begin_of_text|><|start_header_id|>user<|end_header_id|>\n\nWrite a quicksort in Kotlin.<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
    assertEquals(expected, formatted)
  }

  @Test
  fun `qwen 2 dynamic chat template formats correctly`() {
    val prompt = "Hello Qwen"
    val formatted = com.example.engine.ChatTemplateEngine.formatPrompt(
      com.example.engine.ModelArchitecture.QWEN,
      prompt
    )
    val expected = "<|im_start|>system\nYou are a helpful assistant.<|im_end|>\n<|im_start|>user\nHello Qwen<|im_end|>\n<|im_start|>assistant\n"
    assertEquals(expected, formatted)
  }

  @Test
  fun `mistral dynamic chat template formats correctly`() {
    val prompt = "What is the speed of light?"
    val formatted = com.example.engine.ChatTemplateEngine.formatPrompt(
      com.example.engine.ModelArchitecture.MISTRAL,
      prompt
    )
    val expected = "<s>[INST] What is the speed of light? [/INST]"
    assertEquals(expected, formatted)
  }

  @Test
  fun `deepseek dynamic chat template formats correctly`() {
    val prompt = "Solve 2+2"
    val formatted = com.example.engine.ChatTemplateEngine.formatPrompt(
      com.example.engine.ModelArchitecture.DEEPSEEK,
      prompt
    )
    val expected = "<|begin_of_sentence|>User: Solve 2+2\n\nAssistant:"
    assertEquals(expected, formatted)
  }

  @Test
  fun `unified stop tokens contains required hallucination prevention tokens`() {
    val requiredTokens = listOf(
      "<end_of_turn>", "<start_of_turn>", "<|eot_id|>", "<|start_header_id|>",
      "<|im_end|>", "<|im_start|>", "[/INST]", "</s>", "<eos>",
      "user:", "assistant:", "User:", "Assistant:"
    )
    val actualList = com.example.engine.ChatTemplateEngine.UNIFIED_STOP_TOKENS.toList()
    for (token in requiredTokens) {
      assertTrue("Missing stop token: $token", actualList.contains(token))
    }
  }

  @Test
  fun `cleanModelResponse strips residual control tokens and role headers`() {
    // Test Llama 3 residual tokens & hallucinated user turn
    val rawLlama = "Kotlin is a statically typed language.<|eot_id|><|start_header_id|>user<|end_header_id|>\n\nWhat about Java?"
    val cleanedLlama = com.example.engine.ChatTemplateEngine.cleanModelResponse(rawLlama)
    assertEquals("Kotlin is a statically typed language.", cleanedLlama)

    // Test Gemma turn tokens
    val rawGemma = "<start_of_turn>model\nHello! How can I assist you today?<end_of_turn>"
    val cleanedGemma = com.example.engine.ChatTemplateEngine.cleanModelResponse(rawGemma)
    assertEquals("Hello! How can I assist you today?", cleanedGemma)

    // Test Qwen tokens
    val rawQwen = "<|im_start|>assistant\nI am Qwen 2.5.<|im_end|>"
    val cleanedQwen = com.example.engine.ChatTemplateEngine.cleanModelResponse(rawQwen)
    assertEquals("I am Qwen 2.5.", cleanedQwen)

    // Test Mistral INST tokens
    val rawMistral = "[INST] How's the weather? [/INST] It is sunny and pleasant today!</s>"
    val cleanedMistral = com.example.engine.ChatTemplateEngine.cleanModelResponse(rawMistral)
    assertEquals("It is sunny and pleasant today!", cleanedMistral)

    // Test DeepSeek format and hallucinated user turn
    val rawDeepSeek = "<|begin_of_sentence|>User: Solve 2+2\n\nAssistant: The answer is 4.\nUser: Solve 3+3\nAssistant: 6."
    val cleanedDeepSeek = com.example.engine.ChatTemplateEngine.cleanModelResponse(rawDeepSeek)
    assertEquals("The answer is 4.", cleanedDeepSeek)
  }

  @Test
  fun `gemma and gamma model classification detects GEMMA architecture accurately`() {
    val gammaArch = com.example.engine.ModelArchitecture.fromModel(
      architecture = "gamma",
      modelName = "gamma-2b-it",
      filename = "gamma-2b-it-q4.gguf"
    )
    assertEquals(com.example.engine.ModelArchitecture.GEMMA, gammaArch)

    val gemmaArch = com.example.engine.ModelArchitecture.fromModel(
      architecture = "gemma2",
      modelName = "gemma-2-2b-it",
      filename = "gemma-2-2b-it-Q4_K_M.gguf"
    )
    assertEquals(com.example.engine.ModelArchitecture.GEMMA, gemmaArch)

    val paligemmaArch = com.example.engine.ModelArchitecture.fromModel(
      architecture = "",
      modelName = "paligemma-3b",
      filename = "paligemma-3b.gguf"
    )
    assertEquals(com.example.engine.ModelArchitecture.GEMMA, paligemmaArch)

    val gammaTemplateFormat = com.example.engine.ChatTemplateEngine.detectFormat(
      architecture = "gamma",
      modelName = "gamma-2b-it",
      filename = "gamma-2b.gguf"
    )
    assertEquals(com.example.engine.TemplateFormat.GEMMA, gammaTemplateFormat)

    val prompt = "Explain gravity"
    val formatted = com.example.engine.ChatTemplateEngine.formatPrompt(gammaArch, prompt)
    assertEquals("<start_of_turn>user\nExplain gravity<end_of_turn>\n<start_of_turn>model\n", formatted)
  }
}

