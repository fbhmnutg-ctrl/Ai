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

    // Text Display Streaming Toggle (Real-time vs Instant full response)
    private val _isStreamingEnabled = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_ENABLED, true))
    val isStreamingEnabled: StateFlow<Boolean> = _isStreamingEnabled.asStateFlow()

    // Integrated Thinking Mode (Think button next to chat input box)
    private val _isIntegratedThinkEnabled = MutableStateFlow(prefs.getBoolean(KEY_INTEGRATED_THINK_ENABLED, false))
    val isIntegratedThinkEnabled: StateFlow<Boolean> = _isIntegratedThinkEnabled.asStateFlow()

    // Developer Theme Preset ("CATPPUCCIN", "TOKYO_NIGHT", "GITHUB_DARK", "MONOKAI", "VS_CODE")
    private val _appTheme = MutableStateFlow(prefs.getString(KEY_APP_THEME, "CATPPUCCIN") ?: "CATPPUCCIN")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    // Native C++ PocketMath Core Flags
    private val _enableFractionalEntropy = MutableStateFlow(prefs.getBoolean(KEY_FRACTIONAL_ENTROPY, true))
    val enableFractionalEntropy: StateFlow<Boolean> = _enableFractionalEntropy.asStateFlow()

    private val _enablePoincareAttention = MutableStateFlow(prefs.getBoolean(KEY_POINCARE_ATTENTION, true))
    val enablePoincareAttention: StateFlow<Boolean> = _enablePoincareAttention.asStateFlow()

    private val _enableRiemannianEKF = MutableStateFlow(prefs.getBoolean(KEY_RIEMANNIAN_EKF, true))
    val enableRiemannianEKF: StateFlow<Boolean> = _enableRiemannianEKF.asStateFlow()

    private val _enableSpectralFFT = MutableStateFlow(prefs.getBoolean(KEY_SPECTRAL_FFT, true))
    val enableSpectralFFT: StateFlow<Boolean> = _enableSpectralFFT.asStateFlow()

    private val _fractionalAlpha = MutableStateFlow(prefs.getFloat(KEY_FRACTIONAL_ALPHA, 0.5f))
    val fractionalAlpha: StateFlow<Float> = _fractionalAlpha.asStateFlow()

    init {
        syncPocketMathConfig()
    }

    private fun syncPocketMathConfig() {
        com.example.engine.NativePocketMathBridge.updateConfig(
            com.example.engine.PocketMathConfig(
                enableFractionalEntropy = _enableFractionalEntropy.value,
                enablePoincareAttention = _enablePoincareAttention.value,
                enableRiemannianEKF = _enableRiemannianEKF.value,
                enableSpectralFFT = _enableSpectralFFT.value,
                fractionalAlpha = _fractionalAlpha.value
            )
        )
    }

    fun setFractionalEntropy(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FRACTIONAL_ENTROPY, enabled).apply()
        _enableFractionalEntropy.value = enabled
        syncPocketMathConfig()
    }

    fun setPoincareAttention(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_POINCARE_ATTENTION, enabled).apply()
        _enablePoincareAttention.value = enabled
        syncPocketMathConfig()
    }

    fun setRiemannianEKF(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RIEMANNIAN_EKF, enabled).apply()
        _enableRiemannianEKF.value = enabled
        syncPocketMathConfig()
    }

    fun setSpectralFFT(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SPECTRAL_FFT, enabled).apply()
        _enableSpectralFFT.value = enabled
        syncPocketMathConfig()
    }

    fun setFractionalAlpha(alpha: Float) {
        prefs.edit().putFloat(KEY_FRACTIONAL_ALPHA, alpha).apply()
        _fractionalAlpha.value = alpha
        syncPocketMathConfig()
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString(KEY_APP_THEME, theme).apply()
        _appTheme.value = theme
    }

    fun setStreamingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STREAMING_ENABLED, enabled).apply()
        _isStreamingEnabled.value = enabled
    }

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

    fun setIntegratedThinkEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTEGRATED_THINK_ENABLED, enabled).apply()
        _isIntegratedThinkEnabled.value = enabled
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

    companion object {
        private const val KEY_AMPR_ENABLED = "key_ampr_enabled"
        private const val KEY_AMPR_K_PATHS = "key_ampr_k_paths"
        private const val KEY_DEEP_REASONING_ENABLED = "key_deep_reasoning_enabled"
        private const val KEY_DEEP_REASONING_EFFORT = "key_deep_reasoning_effort"
        private const val KEY_CPU_THREADS = "key_cpu_threads"
        private const val KEY_CONTEXT_LENGTH = "key_context_length"
        private const val KEY_STREAMING_ENABLED = "key_streaming_enabled"
        private const val KEY_INTEGRATED_THINK_ENABLED = "key_integrated_think_enabled"
        private const val KEY_APP_THEME = "key_app_theme"

        private const val KEY_FRACTIONAL_ENTROPY = "key_fractional_entropy"
        private const val KEY_POINCARE_ATTENTION = "key_poincare_attention"
        private const val KEY_RIEMANNIAN_EKF = "key_riemannian_ekf"
        private const val KEY_SPECTRAL_FFT = "key_spectral_fft"
        private const val KEY_FRACTIONAL_ALPHA = "key_fractional_alpha"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context).also { INSTANCE = it }
            }
        }
    }
}
