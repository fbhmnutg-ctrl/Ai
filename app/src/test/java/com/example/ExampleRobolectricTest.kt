package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  fun `ampr and deep reasoning mode are mutually exclusive in SettingsManager`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = SettingsManager.getInstance(context)

    // Enable AMPR
    settingsManager.setAmprEnabled(true)
    assertTrue("AMPR should be enabled", settingsManager.isAmprEnabled.value)
    assertFalse("Deep Reasoning should be disabled", settingsManager.isDeepReasoningEnabled.value)

    // Enable Deep Reasoning -> should automatically disable AMPR
    settingsManager.setDeepReasoningEnabled(true)
    assertTrue("Deep Reasoning should be enabled", settingsManager.isDeepReasoningEnabled.value)
    assertFalse("AMPR should now be disabled", settingsManager.isAmprEnabled.value)

    // Re-enable AMPR -> should automatically disable Deep Reasoning
    settingsManager.setAmprEnabled(true)
    assertTrue("AMPR should be enabled", settingsManager.isAmprEnabled.value)
    assertFalse("Deep Reasoning should now be disabled", settingsManager.isDeepReasoningEnabled.value)

    // Disabling AMPR leaves both off (standard single-pass)
    settingsManager.setAmprEnabled(false)
    assertFalse("AMPR should be disabled", settingsManager.isAmprEnabled.value)
    assertFalse("Deep Reasoning should remain disabled", settingsManager.isDeepReasoningEnabled.value)
  }
}
