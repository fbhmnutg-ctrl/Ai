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
}
