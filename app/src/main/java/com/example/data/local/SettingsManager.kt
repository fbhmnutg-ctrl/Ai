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

    private val _cpuThreads = MutableStateFlow(prefs.getInt(KEY_CPU_THREADS, 4))
    val cpuThreads: StateFlow<Int> = _cpuThreads.asStateFlow()

    private val _contextLength = MutableStateFlow(prefs.getInt(KEY_CONTEXT_LENGTH, 2048))
    val contextLength: StateFlow<Int> = _contextLength.asStateFlow()

    // Text Display Streaming Toggle (Real-time vs Instant full response)
    private val _isStreamingEnabled = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_ENABLED, true))
    val isStreamingEnabled: StateFlow<Boolean> = _isStreamingEnabled.asStateFlow()

    // Integrated Thinking Mode (Think button next to chat input box)
    private val _isIntegratedThinkEnabled = MutableStateFlow(prefs.getBoolean(KEY_INTEGRATED_THINK_ENABLED, false))
    val isIntegratedThinkEnabled: StateFlow<Boolean> = _isIntegratedThinkEnabled.asStateFlow()

    // Developer Theme Preset ("CATPPUCCIN", "TOKYO_NIGHT", "GITHUB_DARK", "MONOKAI", "VS_CODE")
    private val _appTheme = MutableStateFlow(prefs.getString(KEY_APP_THEME, "CATPPUCCIN") ?: "CATPPUCCIN")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    // Hybrid GPU + CPU Offloading and Memory OOM Guard Settings
    private val _isGpuOffloadEnabled = MutableStateFlow(prefs.getBoolean(KEY_GPU_OFFLOAD_ENABLED, true))
    val isGpuOffloadEnabled: StateFlow<Boolean> = _isGpuOffloadEnabled.asStateFlow()

    private val _gpuOffloadLayers = MutableStateFlow(prefs.getInt(KEY_GPU_OFFLOAD_LAYERS, -1))
    val gpuOffloadLayers: StateFlow<Int> = _gpuOffloadLayers.asStateFlow()

    private val _isOomGuardEnabled = MutableStateFlow(prefs.getBoolean(KEY_OOM_GUARD_ENABLED, true))
    val isOomGuardEnabled: StateFlow<Boolean> = _isOomGuardEnabled.asStateFlow()

    // Demonstrating the thinking process (optional)
    private val _showThinkingProcess = MutableStateFlow(prefs.getBoolean(KEY_SHOW_THINKING_PROCESS, true))
    val showThinkingProcess: StateFlow<Boolean> = _showThinkingProcess.asStateFlow()

    // Granular Model Hyperparameter Controls
    private val _temperature = MutableStateFlow(prefs.getFloat(KEY_TEMPERATURE, 0.7f))
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _topP = MutableStateFlow(prefs.getFloat(KEY_TOP_P, 0.9f))
    val topP: StateFlow<Float> = _topP.asStateFlow()

    private val _topK = MutableStateFlow(prefs.getInt(KEY_TOP_K, 40))
    val topK: StateFlow<Int> = _topK.asStateFlow()

    private val _maxTokens = MutableStateFlow(prefs.getInt(KEY_MAX_TOKENS, 2048))
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _repeatPenalty = MutableStateFlow(prefs.getFloat(KEY_REPEAT_PENALTY, 1.1f))
    val repeatPenalty: StateFlow<Float> = _repeatPenalty.asStateFlow()

    private val _autoSaveDownloads = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SAVE_DOWNLOADS, true))
    val autoSaveDownloads: StateFlow<Boolean> = _autoSaveDownloads.asStateFlow()

    fun setShowThinkingProcess(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_THINKING_PROCESS, show).apply()
        _showThinkingProcess.value = show
        // Sync integrated think flag
        prefs.edit().putBoolean(KEY_INTEGRATED_THINK_ENABLED, show).apply()
        _isIntegratedThinkEnabled.value = show
    }

    fun toggleThinkingProcess() {
        val next = !_showThinkingProcess.value
        setShowThinkingProcess(next)
    }

    fun setTemperature(temp: Float) {
        val clamped = temp.coerceIn(0.0f, 2.0f)
        prefs.edit().putFloat(KEY_TEMPERATURE, clamped).apply()
        _temperature.value = clamped
    }

    fun setTopP(p: Float) {
        val clamped = p.coerceIn(0.05f, 1.0f)
        prefs.edit().putFloat(KEY_TOP_P, clamped).apply()
        _topP.value = clamped
    }

    fun setTopK(k: Int) {
        val clamped = k.coerceIn(1, 100)
        prefs.edit().putInt(KEY_TOP_K, clamped).apply()
        _topK.value = clamped
    }

    fun setMaxTokens(tokens: Int) {
        val clamped = tokens.coerceIn(64, 8192)
        prefs.edit().putInt(KEY_MAX_TOKENS, clamped).apply()
        _maxTokens.value = clamped
    }

    fun setRepeatPenalty(penalty: Float) {
        val clamped = penalty.coerceIn(1.0f, 2.0f)
        prefs.edit().putFloat(KEY_REPEAT_PENALTY, clamped).apply()
        _repeatPenalty.value = clamped
    }

    fun setAutoSaveDownloads(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SAVE_DOWNLOADS, enabled).apply()
        _autoSaveDownloads.value = enabled
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString(KEY_APP_THEME, theme).apply()
        _appTheme.value = theme
    }

    fun setStreamingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STREAMING_ENABLED, enabled).apply()
        _isStreamingEnabled.value = enabled
    }

    fun setIntegratedThinkEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTEGRATED_THINK_ENABLED, enabled).apply()
        _isIntegratedThinkEnabled.value = enabled
        // Sync show thinking process flag
        prefs.edit().putBoolean(KEY_SHOW_THINKING_PROCESS, enabled).apply()
        _showThinkingProcess.value = enabled
    }

    fun toggleIntegratedThink() {
        val nextState = !_isIntegratedThinkEnabled.value
        setIntegratedThinkEnabled(nextState)
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

    fun setGpuOffloadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GPU_OFFLOAD_ENABLED, enabled).apply()
        _isGpuOffloadEnabled.value = enabled
    }

    fun setGpuOffloadLayers(layers: Int) {
        prefs.edit().putInt(KEY_GPU_OFFLOAD_LAYERS, layers).apply()
        _gpuOffloadLayers.value = layers
    }

    fun setOomGuardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OOM_GUARD_ENABLED, enabled).apply()
        _isOomGuardEnabled.value = enabled
    }

    companion object {
        private const val KEY_CPU_THREADS = "key_cpu_threads"
        private const val KEY_CONTEXT_LENGTH = "key_context_length"
        private const val KEY_STREAMING_ENABLED = "key_streaming_enabled"
        private const val KEY_INTEGRATED_THINK_ENABLED = "key_integrated_think_enabled"
        private const val KEY_APP_THEME = "key_app_theme"

        private const val KEY_GPU_OFFLOAD_ENABLED = "key_gpu_offload_enabled"
        private const val KEY_GPU_OFFLOAD_LAYERS = "key_gpu_offload_layers"
        private const val KEY_OOM_GUARD_ENABLED = "key_oom_guard_enabled"

        private const val KEY_SHOW_THINKING_PROCESS = "key_show_thinking_process"
        private const val KEY_TEMPERATURE = "key_temperature"
        private const val KEY_TOP_P = "key_top_p"
        private const val KEY_TOP_K = "key_top_k"
        private const val KEY_MAX_TOKENS = "key_max_tokens"
        private const val KEY_REPEAT_PENALTY = "key_repeat_penalty"
        private const val KEY_AUTO_SAVE_DOWNLOADS = "key_auto_save_downloads"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context).also { INSTANCE = it }
            }
        }
    }
}
