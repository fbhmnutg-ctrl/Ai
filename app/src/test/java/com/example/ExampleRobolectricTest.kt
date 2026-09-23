package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.QuantizationEngine
import com.example.engine.QuantCategory
import java.io.File
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

  @Test
  fun `imported gamma model parses metadata with GEMMA architecture`() {
    val tempFile = File.createTempFile("gamma-2-2b-it-Q4_K_M", ".gguf")
    tempFile.writeBytes(ByteArray(64)) // minimal mock file
    
    val parsedMeta = com.example.engine.GgufParser.parseFromFile(tempFile, "gamma-2-2b-it-Q4_K_M.gguf")
    assertEquals("gemma", parsedMeta.architecture)
    assertEquals("Q4_K_M", parsedMeta.quantization)

    val customFile = File.createTempFile("imported_gamma_weights", ".bin")
    val parsedCustom = com.example.engine.GgufParser.parseFromFile(customFile, "my_custom_gamma_weights.gguf")
    assertEquals("gemma", parsedCustom.architecture)

    tempFile.delete()
    customFile.delete()
  }

  @Test
  fun `huggingface api service strictly allows sub 4B models and excludes large models`() {
    val api = com.example.data.remote.HuggingFaceApiService()

    val gemma2b = com.example.data.remote.HuggingFaceModel(
      id = "google/gemma-2-2b-it",
      author = "google",
      modelName = "gemma-2-2b-it",
      parameterCount = "2.6B",
      architectureClass = "gemma",
      downloads = 10000,
      likes = 500,
      pipelineTag = "text-generation",
      tags = listOf("gemma2", "text-generation"),
      hasGguf = true
    )
    assertTrue(api.isModelStrictlySub4B(gemma2b))

    val qwen15b = com.example.data.remote.HuggingFaceModel(
      id = "Qwen/Qwen2.5-1.5B-Instruct",
      author = "Qwen",
      modelName = "Qwen2.5-1.5B-Instruct",
      parameterCount = "1.5B",
      architectureClass = "qwen",
      downloads = 50000,
      likes = 1200,
      pipelineTag = "text-generation",
      tags = listOf("qwen2", "text-generation"),
      hasGguf = true
    )
    assertTrue(api.isModelStrictlySub4B(qwen15b))

    val phi35Mini = com.example.data.remote.HuggingFaceModel(
      id = "microsoft/Phi-3.5-mini-instruct",
      author = "microsoft",
      modelName = "Phi-3.5-mini-instruct",
      parameterCount = "3.8B",
      architectureClass = "phi",
      downloads = 80000,
      likes = 2000,
      pipelineTag = "text-generation",
      tags = listOf("phi3"),
      hasGguf = true
    )
    assertTrue(api.isModelStrictlySub4B(phi35Mini))

    val smol135m = com.example.data.remote.HuggingFaceModel(
      id = "HuggingFaceTB/SmolLM2-135M",
      author = "HuggingFaceTB",
      modelName = "SmolLM2-135M",
      parameterCount = "135M",
      architectureClass = "smollm",
      downloads = 12000,
      likes = 300,
      pipelineTag = "text-generation",
      tags = listOf("smollm"),
      hasGguf = true
    )
    assertTrue(api.isModelStrictlySub4B(smol135m))

    // Disallowed: 7B, 8B, 70B models
    val llama7b = com.example.data.remote.HuggingFaceModel(
      id = "meta-llama/Llama-2-7b-chat-hf",
      author = "meta-llama",
      modelName = "Llama-2-7b-chat-hf",
      parameterCount = "7B",
      architectureClass = "llama",
      downloads = 200000,
      likes = 4000,
      pipelineTag = "text-generation",
      tags = listOf("llama", "7b"),
      hasGguf = true
    )
    org.junit.Assert.assertFalse(api.isModelStrictlySub4B(llama7b))

    val llama70b = com.example.data.remote.HuggingFaceModel(
      id = "meta-llama/Meta-Llama-3-70B-Instruct",
      author = "meta-llama",
      modelName = "Meta-Llama-3-70B-Instruct",
      parameterCount = "70B",
      architectureClass = "llama",
      downloads = 1000000,
      likes = 12000,
      pipelineTag = "text-generation",
      tags = listOf("llama", "70b"),
      hasGguf = true
    )
    org.junit.Assert.assertFalse(api.isModelStrictlySub4B(llama70b))
  }

  @Test
  fun `background inference manager initializes cleanly`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    com.example.engine.BackgroundInferenceManager.initialize(context)
    val state = com.example.engine.BackgroundInferenceManager.generationState.value
    org.junit.Assert.assertFalse(state.isGenerating)
    assertEquals("", state.streamingContent)
  }

  @Test
  fun `local inference generates response without crash for downloaded model`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val engine = com.example.engine.LocalInferenceEngine()
    val model = com.example.data.local.entity.LocalModelEntity(
      id = "test-qwen-0.5b",
      name = "Qwen 2.5 0.5B",
      filename = "qwen-0.5b.gguf",
      architecture = "qwen",
      quantization = "Q4_K_M",
      parameterCount = "0.5B",
      sizeBytes = 350_000_000L,
      requiredRamMb = 550,
      contextLength = 2048,
      isDownloaded = true,
      filePath = null
    )

    val messages = listOf(
      com.example.data.local.entity.ChatMessage(
        sessionId = 1L,
        role = "user",
        content = "Hello! Tell me about Kotlin."
      )
    )

    val collectedTokens = StringBuilder()
    var isFinished = false
    engine.generateLocalStream(
      model = model,
      messages = messages,
      userPromptOverride = "Hello! Tell me about Kotlin.",
      systemPrompt = "You are a helpful assistant.",
      context = context
    ).collect { chunk ->
      if (!chunk.isFinished) {
        collectedTokens.append(chunk.token)
      } else {
        isFinished = true
      }
    }

    assertTrue(isFinished)
    assertTrue(collectedTokens.isNotEmpty())
  }
}

