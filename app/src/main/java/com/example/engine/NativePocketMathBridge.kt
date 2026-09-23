package com.example.engine

import android.util.Log

data class PocketMathConfig(
    val enableFractionalEntropy: Boolean = false,
    val enablePoincareAttention: Boolean = false,
    val enableRiemannianEKF: Boolean = false,
    val enableSpectralFFT: Boolean = false,
    val fractionalAlpha: Float = 0.5f
)

object NativePocketMathBridge {
    private const val TAG = "NativePocketMathBridge"
    private var isLibraryLoaded = false
    private var engineHandle: Long = 0L

    init {
        try {
            System.loadLibrary("pocket_math_core")
            isLibraryLoaded = true
            engineHandle = nativeInitEngine()
            Log.i(TAG, "Native C++ PocketMath library loaded successfully. Handle=$engineHandle")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library 'pocket_math_core' not loaded: ${e.message}")
            isLibraryLoaded = false
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize NativePocketMathBridge", t)
            isLibraryLoaded = false
        }
    }

    private external fun nativeInitEngine(): Long
    private external fun nativeDestroyEngine(handle: Long)
    private external fun nativeUpdateConfig(
        handle: Long,
        enableFractionalEntropy: Boolean,
        enablePoincareAttention: Boolean,
        enableRiemannianEKF: Boolean,
        enableSpectralFFT: Boolean,
        fractionalAlpha: Float
    )
    private external fun nativeProcessLogits(handle: Long, logits: FloatArray): FloatArray
    private external fun nativeComputeFractionalEntropy(handle: Long, logits: FloatArray): Float
    private external fun nativeComputePoincareDistance(handle: Long, u: FloatArray, v: FloatArray, c: Float): Float
    private external fun nativeApplyRiemannianEKF(handle: Long, logits: FloatArray): FloatArray
    private external fun nativeApplySpectralFFT(handle: Long, probs: FloatArray, cutoff: Float): FloatArray

    fun updateConfig(config: PocketMathConfig) {
        if (isLibraryLoaded && engineHandle != 0L) {
            try {
                nativeUpdateConfig(
                    engineHandle,
                    config.enableFractionalEntropy,
                    config.enablePoincareAttention,
                    config.enableRiemannianEKF,
                    config.enableSpectralFFT,
                    config.fractionalAlpha
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Error calling nativeUpdateConfig", t)
            }
        }
    }

    fun processLogitStream(logits: FloatArray): FloatArray {
        if (!isLibraryLoaded || engineHandle == 0L || logits.isEmpty()) return logits
        return try {
            nativeProcessLogits(engineHandle, logits)
        } catch (t: Throwable) {
            Log.e(TAG, "Error in nativeProcessLogits", t)
            logits
        }
    }

    fun computeFractionalEntropy(logits: FloatArray): Float {
        if (!isLibraryLoaded || engineHandle == 0L || logits.isEmpty()) return 0f
        return try {
            nativeComputeFractionalEntropy(engineHandle, logits)
        } catch (t: Throwable) {
            0f
        }
    }

    fun computePoincareDistance(u: FloatArray, v: FloatArray, c: Float = 1.0f): Float {
        if (!isLibraryLoaded || engineHandle == 0L) return 0f
        return try {
            nativeComputePoincareDistance(engineHandle, u, v, c)
        } catch (t: Throwable) {
            0f
        }
    }

    fun applyRiemannianEKF(logits: FloatArray): FloatArray {
        if (!isLibraryLoaded || engineHandle == 0L || logits.isEmpty()) return logits
        return try {
            nativeApplyRiemannianEKF(engineHandle, logits)
        } catch (t: Throwable) {
            logits
        }
    }

    fun applySpectralFFT(probs: FloatArray, cutoff: Float = 0.8f): FloatArray {
        if (!isLibraryLoaded || engineHandle == 0L || probs.isEmpty()) return probs
        return try {
            nativeApplySpectralFFT(engineHandle, probs, cutoff)
        } catch (t: Throwable) {
            probs
        }
    }
}
