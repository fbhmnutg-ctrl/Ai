package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "pocket_ollama_settings",
        Context.MODE_PRIVATE
    )

    private val _isAmprEnabled = MutableStateFlow(prefs.getBoolean(KEY_AMPR_ENABLED, false))
    val isAmprEnabled: StateFlow<Boolean> = _isAmprEnabled.asStateFlow()

    private val _amprKPaths = MutableStateFlow(prefs.getInt(KEY_AMPR_K_PATHS, 3))
    val amprKPaths: StateFlow<Int> = _amprKPaths.asStateFlow()

    // Deep Reasoning Mode (Mutually exclusive with AMPR)
    private val _isDeepReasoningEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEEP_REASONING_ENABLED, false))
    val isDeepReasoningEnabled: StateFlow<Boolean> = _isDeepReasoningEnabled.asStateFlow()

    private val _deepReasoningEffort = MutableStateFlow(prefs.getString(KEY_DEEP_REASONING_EFFORT, "MEDIUM") ?: "MEDIUM")
    val deepReasoningEffort: StateFlow<String> = _deepReasoningEffort.asStateFlow()

    private val _cpuThreads = MutableStateFlow(prefs.getInt(KEY_CPU_THREADS, 4))
    val cpuThreads: StateFlow<Int> = _cpuThreads.asStateFlow()

    private val _contextLength = MutableStateFlow(prefs.getInt(KEY_CONTEXT_LENGTH, 2048))
    val contextLength: StateFlow<Int> = _contextLength.asStateFlow()

    fun setAmprEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMPR_ENABLED, enabled).apply()
        _isAmprEnabled.value = enabled
        // Crucial: AMPR and Deep Reasoning cannot run simultaneously
        if (enabled && _isDeepReasoningEnabled.value) {
            prefs.edit().putBoolean(KEY_DEEP_REASONING_ENABLED, false).apply()
            _isDeepReasoningEnabled.value = false
        }
    }

    fun setDeepReasoningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEEP_REASONING_ENABLED, enabled).apply()
        _isDeepReasoningEnabled.value = enabled
        // Crucial: AMPR and Deep Reasoning cannot run simultaneously
        if (enabled && _isAmprEnabled.value) {
            prefs.edit().putBoolean(KEY_AMPR_ENABLED, false).apply()
            _isAmprEnabled.value = false
        }
    }

    fun setDeepReasoningEffort(effort: String) {
        prefs.edit().putString(KEY_DEEP_REASONING_EFFORT, effort).apply()
        _deepReasoningEffort.value = effort
    }

    fun setAmprKPaths(k: Int) {
        val clamped = k.coerceIn(2, 4)
        prefs.edit().putInt(KEY_AMPR_K_PATHS, clamped).apply()
        _amprKPaths.value = clamped
    }

    fun setCpuThreads(threads: Int) {
        val clamped = threads.coerceIn(1, 8)
        prefs.edit().putInt(KEY_CPU_THREADS, clamped).apply()
        _cpuThreads.value = clamped
    }

    fun setContextLength(length: Int) {
        val clamped = length.coerceIn(1024, 8192)
        prefs.edit().putInt(KEY_CONTEXT_LENGTH, clamped).apply()
        _contextLength.value = clamped
    }

    companion object {
        private const val KEY_AMPR_ENABLED = "key_ampr_enabled"
        private const val KEY_AMPR_K_PATHS = "key_ampr_k_paths"
        private const val KEY_DEEP_REASONING_ENABLED = "key_deep_reasoning_enabled"
        private const val KEY_DEEP_REASONING_EFFORT = "key_deep_reasoning_effort"
        private const val KEY_CPU_THREADS = "key_cpu_threads"
        private const val KEY_CONTEXT_LENGTH = "key_context_length"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context).also { INSTANCE = it }
            }
        }
    }
}
