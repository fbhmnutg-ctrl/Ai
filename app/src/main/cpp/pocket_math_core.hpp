#ifndef POCKET_MATH_CORE_HPP
#define POCKET_MATH_CORE_HPP

#include <vector>
#include <cmath>
#include <complex>
#include <memory>
#include <algorithm>
#include <string>
#include <cstdint>
#include <jni.h>

namespace PocketOllama {

/**
 * Dynamic runtime configuration structure for C++ PocketMath engine.
 */
struct PocketMathConfig {
    bool enableFractionalEntropy = false;
    bool enablePoincareAttention = false;
    bool enableRiemannianEKF     = false;
    bool enableSpectralFFT       = false;
    float fractionalAlpha        = 0.5f; // Caputo order parameter \alpha \in (0, 1)
};

/**
 * High-Performance Native Mathematical Engine for PocketOllama.
 * Implements Fractional-Order Entropy, Poincaré Hyperbolic Attention,
 * Riemannian EKF, and Spectral Quantization (FFT).
 */
class PocketMathEngine {
public:
    PocketMathEngine();
    ~PocketMathEngine();

    void updateConfig(const PocketMathConfig& config);
    PocketMathConfig getConfig() const;

    // 1. Fractional-Order Entropy Predictor (Caputo Fractional Derivative)
    float computeFractionalEntropy(const std::vector<float>& logits);

    // 2. Poincaré Hyperbolic Attention Kernel (Möbius addition & Hyperbolic metric)
    float computePoincareDistance(const std::vector<float>& u, const std::vector<float>& v, float c = 1.0f);
    std::vector<float> mobiusAddition(const std::vector<float>& u, const std::vector<float>& v, float c = 1.0f);
    std::vector<float> applyPoincareAttention(
        const std::vector<float>& query,
        const std::vector<float>& key,
        float c = 1.0f
    );

    // 3. Riemannian Extended Kalman Filtering (Exponential/Logarithmic map on Manifold)
    std::vector<float> applyRiemannianEKF(const std::vector<float>& rawLogits);

    // 4. Spectral Quantization via Fast Fourier Transform (Cooley-Tukey 1D/4D FFT)
    std::vector<float> applySpectralFFT(const std::vector<float>& probabilities, float highFreqCutoff = 0.8f);

    // Pipeline processing hook for logit/probability vectors
    std::vector<float> processLogitStream(const std::vector<float>& logits);

private:
    PocketMathConfig config_;

    // Internal temporal buffer for Caputo fractional derivative calculation
    std::vector<float> entropyHistory_;
    static constexpr size_t MAX_HISTORY = 32;

    // Internal state for Riemannian EKF
    std::vector<float> ekfState_;
    std::vector<float> ekfCovariance_;
    bool ekfInitialized_ = false;

    // Helper math functions
    void radix2FFT(std::vector<std::complex<float>>& x);
    void radix2IFFT(std::vector<std::complex<float>>& x);
    std::vector<float> softmax(const std::vector<float>& input);
    void projectToPoincareBall(std::vector<float>& x, float c, float eps = 1e-5f);
};

} // namespace PocketOllama

extern "C" {
    JNIEXPORT jlong JNICALL Java_com_example_engine_NativePocketMathBridge_nativeInitEngine(JNIEnv* env, jobject obj);
    JNIEXPORT void JNICALL Java_com_example_engine_NativePocketMathBridge_nativeDestroyEngine(JNIEnv* env, jobject obj, jlong handle);
    JNIEXPORT void JNICALL Java_com_example_engine_NativePocketMathBridge_nativeUpdateConfig(
        JNIEnv* env, jobject obj, jlong handle,
        jboolean enableFractionalEntropy,
        jboolean enablePoincareAttention,
        jboolean enableRiemannianEKF,
        jboolean enableSpectralFFT,
        jfloat fractionalAlpha
    );
    JNIEXPORT jfloatArray JNICALL Java_com_example_engine_NativePocketMathBridge_nativeProcessLogits(
        JNIEnv* env, jobject obj, jlong handle, jfloatArray logits
    );
    JNIEXPORT jfloat JNICALL Java_com_example_engine_NativePocketMathBridge_nativeComputeFractionalEntropy(
        JNIEnv* env, jobject obj, jlong handle, jfloatArray logits
    );
    JNIEXPORT jfloat JNICALL Java_com_example_engine_NativePocketMathBridge_nativeComputePoincareDistance(
        JNIEnv* env, jobject obj, jlong handle, jfloatArray u, jfloatArray v, jfloat c
    );
    JNIEXPORT jfloatArray JNICALL Java_com_example_engine_NativePocketMathBridge_nativeApplyRiemannianEKF(
        JNIEnv* env, jobject obj, jlong handle, jfloatArray logits
    );
    JNIEXPORT jfloatArray JNICALL Java_com_example_engine_NativePocketMathBridge_nativeApplySpectralFFT(
        JNIEnv* env, jobject obj, jlong handle, jfloatArray probs, jfloat cutoff
    );
}

#endif // POCKET_MATH_CORE_HPP
