#include "pocket_math_core.hpp"
#include <numeric>
#include <android/log.h>

#define LOG_TAG "PocketMathCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifndef M_PI
#define M_PI 3.14159265358979323846f
#endif

namespace PocketOllama {

PocketMathEngine::PocketMathEngine() {
    LOGI("PocketMathEngine C++ instance initialized.");
}

PocketMathEngine::~PocketMathEngine() {
    LOGI("PocketMathEngine C++ instance destroyed.");
}

void PocketMathEngine::updateConfig(const PocketMathConfig& config) {
    config_ = config;
    LOGI("PocketMathEngine config updated: FracEntropy=%d, PoincareAttn=%d, RiemEKF=%d, SpectralFFT=%d, Alpha=%.2f",
         config_.enableFractionalEntropy, config_.enablePoincareAttention,
         config_.enableRiemannianEKF, config_.enableSpectralFFT, config_.fractionalAlpha);
}

PocketMathConfig PocketMathEngine::getConfig() const {
    return config_;
}

// ============================================================================
// 1. Fractional-Order Entropy Predictor (Caputo Fractional Derivative)
// ============================================================================
float PocketMathEngine::computeFractionalEntropy(const std::vector<float>& logits) {
    if (logits.empty()) return 0.0f;

    // Calculate current Shannon Entropy S(t) = - \sum p_i \ln p_i
    std::vector<float> probs = softmax(logits);
    float currentEntropy = 0.0f;
    for (float p : probs) {
        if (p > 1e-9f) {
            currentEntropy -= p * std::log(p);
        }
    }

    entropyHistory_.push_back(currentEntropy);
    if (entropyHistory_.size() > MAX_HISTORY) {
        entropyHistory_.erase(entropyHistory_.begin());
    }

    size_t N = entropyHistory_.size();
    if (N < 2) return currentEntropy;

    // Caputo Fractional Derivative calculation:
    // {}^C D^\alpha_t f(t) = \frac{1}{\Gamma(1-\alpha)} \int_0^t (t-\tau)^{-\alpha} f'(\tau) d\tau
    float alpha = config_.fractionalAlpha;
    if (alpha <= 0.0f || alpha >= 1.0f) alpha = 0.5f;

    float gammaTerm = std::tgamma(1.0f - alpha);
    if (gammaTerm == 0.0f || std::isnan(gammaTerm)) gammaTerm = 1.77245385f; // fallback sqrt(pi)

    float caputoDerivative = 0.0f;
    float dt = 1.0f; // Discrete step size

    for (size_t i = 1; i < N; ++i) {
        float fPrime = (entropyHistory_[i] - entropyHistory_[i - 1]) / dt;
        float t = static_cast<float>(N - 1);
        float tau = static_cast<float>(i);
        float weight = std::pow(std::max(0.001f, t - tau), -alpha);
        caputoDerivative += weight * fPrime * dt;
    }

    caputoDerivative /= gammaTerm;

    // Predict future trajectory uncertainty = S(t) + \alpha * {}^C D^\alpha_t S(t)
    float predictedEntropy = currentEntropy + alpha * caputoDerivative;
    return predictedEntropy;
}

// ============================================================================
// 2. Poincaré Hyperbolic Attention Kernel
// ============================================================================
void PocketMathEngine::projectToPoincareBall(std::vector<float>& x, float c, float eps) {
    float norm = 0.0f;
    for (float v : x) norm += v * v;
    norm = std::sqrt(norm);

    float maxNorm = (1.0f / std::sqrt(c)) - eps;
    if (norm >= maxNorm && norm > 0.0f) {
        float scale = maxNorm / norm;
        for (float& v : x) v *= scale;
    }
}

std::vector<float> PocketMathEngine::mobiusAddition(const std::vector<float>& u, const std::vector<float>& v, float c) {
    size_t dim = std::min(u.size(), v.size());
    std::vector<float> result(dim, 0.0f);

    float u2 = 0.0f, v2 = 0.0f, uv = 0.0f;
    for (size_t i = 0; i < dim; ++i) {
        u2 += u[i] * u[i];
        v2 += v[i] * v[i];
        uv += u[i] * v[i];
    }

    float num_u = 1.0f + 2.0f * c * uv + c * v2;
    float num_v = 1.0f - c * u2;
    float denom = 1.0f + 2.0f * c * uv + c * c * u2 * v2;

    if (std::abs(denom) < 1e-7f) denom = 1e-7f;

    for (size_t i = 0; i < dim; ++i) {
        result[i] = (num_u * u[i] + num_v * v[i]) / denom;
    }

    projectToPoincareBall(result, c);
    return result;
}

float PocketMathEngine::computePoincareDistance(const std::vector<float>& u, const std::vector<float>& v, float c) {
    if (u.empty() || v.empty()) return 0.0f;

    std::vector<float> u_proj = u;
    std::vector<float> v_proj = v;
    projectToPoincareBall(u_proj, c);
    projectToPoincareBall(v_proj, c);

    // Negative u for Möbius subtraction: -u \oplus_c v
    std::vector<float> neg_u(u_proj.size());
    for (size_t i = 0; i < u_proj.size(); ++i) neg_u[i] = -u_proj[i];

    std::vector<float> diff = mobiusAddition(neg_u, v_proj, c);

    float diffNorm = 0.0f;
    for (float x : diff) diffNorm += x * x;
    diffNorm = std::sqrt(diffNorm);

    float sqrt_c = std::sqrt(c);
    float arg = std::min(0.99999f, sqrt_c * diffNorm);

    // d_\mathbb{B}(u, v) = \frac{2}{\sqrt{c}} \text{artanh}(\sqrt{c} \| -u \oplus_c v \|)
    float dist = (2.0f / sqrt_c) * std::atanh(arg);
    return dist;
}

std::vector<float> PocketMathEngine::applyPoincareAttention(
    const std::vector<float>& query,
    const std::vector<float>& key,
    float c
) {
    size_t dim = std::min(query.size(), key.size());
    std::vector<float> projected(dim, 0.0f);

    float dist = computePoincareDistance(query, key, c);
    float weight = std::exp(-dist);

    for (size_t i = 0; i < dim; ++i) {
        projected[i] = query[i] * weight + key[i] * (1.0f - weight);
    }
    return projected;
}

// ============================================================================
// 3. Riemannian Extended Kalman Filtering
// ============================================================================
std::vector<float> PocketMathEngine::applyRiemannianEKF(const std::vector<float>& rawLogits) {
    if (rawLogits.empty()) return rawLogits;

    size_t dim = rawLogits.size();
    if (!ekfInitialized_ || ekfState_.size() != dim) {
        ekfState_ = rawLogits;
        ekfCovariance_ = std::vector<float>(dim, 1.0f);
        ekfInitialized_ = true;
        return rawLogits;
    }

    float processNoiseQ = 0.01f;
    float measurementNoiseR = 0.1f;

    std::vector<float> filteredState(dim);

    for (size_t i = 0; i < dim; ++i) {
        // Predict
        float p_minus = ekfCovariance_[i] + processNoiseQ;

        // Innovation on Riemannian Tangent Space: v_k = Log_{\hat{x}_k^-}(z_k) \approx z_k - \hat{x}_k^-
        float v_k = rawLogits[i] - ekfState_[i];

        // Kalman Gain: K_k = P_k^- / (P_k^- + R_k)
        float K_k = p_minus / (p_minus + measurementNoiseR);

        // State Update via Riemannian Exponential Map: Exp_x(v) \approx x + v
        ekfState_[i] = ekfState_[i] + K_k * v_k;
        ekfCovariance_[i] = (1.0f - K_k) * p_minus;

        filteredState[i] = ekfState_[i];
    }

    return filteredState;
}

// ============================================================================
// 4. Spectral Quantization via 4D/1D Fast Fourier Transform
// ============================================================================
void PocketMathEngine::radix2FFT(std::vector<std::complex<float>>& x) {
    size_t n = x.size();
    if (n <= 1) return;

    // Bit-reversal permutation
    for (size_t i = 1, j = 0; i < n; ++i) {
        size_t bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) std::swap(x[i], x[j]);
    }

    for (size_t len = 2; len <= n; len <<= 1) {
        float angle = -2.0f * M_PI / static_cast<float>(len);
        std::complex<float> wlen(std::cos(angle), std::sin(angle));
        for (size_t i = 0; i < n; i += len) {
            std::complex<float> w(1.0f, 0.0f);
            for (size_t j = 0; j < len / 2; ++j) {
                std::complex<float> u = x[i + j];
                std::complex<float> v = x[i + j + len / 2] * w;
                x[i + j] = u + v;
                x[i + j + len / 2] = u - v;
                w *= wlen;
            }
        }
    }
}

void PocketMathEngine::radix2IFFT(std::vector<std::complex<float>>& x) {
    size_t n = x.size();
    for (auto& c : x) c = std::conj(c);
    radix2FFT(x);
    for (auto& c : x) c = std::conj(c) / static_cast<float>(n);
}

std::vector<float> PocketMathEngine::applySpectralFFT(const std::vector<float>& probabilities, float highFreqCutoff) {
    if (probabilities.empty()) return probabilities;

    // Pad to nearest power of 2 for Radix-2 FFT
    size_t origSize = probabilities.size();
    size_t n = 1;
    while (n < origSize) n <<= 1;

    std::vector<std::complex<float>> fftBuffer(n, 0.0f);
    for (size_t i = 0; i < origSize; ++i) {
        fftBuffer[i] = std::complex<float>(probabilities[i], 0.0f);
    }

    // Forward FFT: \mathcal{F}\{f\}(\omega)
    radix2FFT(fftBuffer);

    // Apply spectral frequency filter: attenuate high-frequency stochastic noise
    size_t cutoffIdx = static_cast<size_t>(static_cast<float>(n) * highFreqCutoff);
    for (size_t i = cutoffIdx; i < n - cutoffIdx; ++i) {
        fftBuffer[i] *= 0.1f; // Attenuate high frequency noise
    }

    // Inverse FFT
    radix2IFFT(fftBuffer);

    std::vector<float> filteredProbs(origSize);
    for (size_t i = 0; i < origSize; ++i) {
        filteredProbs[i] = std::max(0.0f, fftBuffer[i].real());
    }

    return softmax(filteredProbs);
}

// ============================================================================
// Pipeline Hook & Helpers
// ============================================================================
std::vector<float> PocketMathEngine::softmax(const std::vector<float>& input) {
    if (input.empty()) return {};
    float maxVal = *std::max_element(input.begin(), input.end());
    std::vector<float> output(input.size());
    float sum = 0.0f;
    for (size_t i = 0; i < input.size(); ++i) {
        output[i] = std::exp(input[i] - maxVal);
        sum += output[i];
    }
    if (sum > 0.0f) {
        for (size_t i = 0; i < output.size(); ++i) output[i] /= sum;
    }
    return output;
}

std::vector<float> PocketMathEngine::processLogitStream(const std::vector<float>& logits) {
    if (logits.empty()) return logits;

    std::vector<float> processed = logits;

    // 1. Riemannian EKF
    if (config_.enableRiemannianEKF) {
        processed = applyRiemannianEKF(processed);
    }

    // 2. Fractional Entropy Predictor
    if (config_.enableFractionalEntropy) {
        float fracEntropy = computeFractionalEntropy(processed);
        // Subtle entropy temperature adjustment if uncertainty spikes
        if (fracEntropy > 3.0f) {
            for (float& val : processed) val *= 0.95f; // sharpen logits
        }
    }

    // 3. Poincaré Hyperbolic Attention Adjustment
    if (config_.enablePoincareAttention) {
        std::vector<float> probs = softmax(processed);
        processed = applyPoincareAttention(processed, probs, 1.0f);
    }

    // 4. Spectral FFT Filter
    if (config_.enableSpectralFFT) {
        std::vector<float> probs = softmax(processed);
        std::vector<float> cleanProbs = applySpectralFFT(probs, 0.75f);
        for (size_t i = 0; i < processed.size(); ++i) {
            processed[i] = std::log(std::max(1e-7f, cleanProbs[i]));
        }
    }

    return processed;
}

} // namespace PocketOllama

// ============================================================================
// JNI Export Implementations
// ============================================================================
extern "C" {

JNIEXPORT jlong JNICALL Java_com_example_engine_NativePocketMathBridge_nativeInitEngine(JNIEnv* env, jobject obj) {
    auto* engine = new PocketOllama::PocketMathEngine();
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL Java_com_example_engine_NativePocketMathBridge_nativeDestroyEngine(JNIEnv* env, jobject obj, jlong handle) {
    if (handle != 0) {
        auto* engine = reinterpret_cast<PocketOllama::PocketMathEngine*>(handle);
        delete engine;
    }
}

JNIEXPORT void JNICALL Java_com_example_engine_NativePocketMathBridge_nativeUpdateConfig(
    JNIEnv* env, jobject obj, jlong handle,
    jboolean enableFractionalEntropy,
    jboolean enablePoincareAttention,
    jboolean enableRiemannianEKF,
    jboolean enableSpectralFFT,
    jfloat fractionalAlpha
) {
    if (handle != 0) {
        auto* engine = reinterpret_cast<PocketOllama::PocketMathEngine*>(handle);
        PocketOllama::PocketMathConfig cfg;
        cfg.enableFractionalEntropy = enableFractionalEntropy;
        cfg.enablePoincareAttention = enablePoincareAttention;
        cfg.enableRiemannianEKF     = enableRiemannianEKF;
        cfg.enableSpectralFFT       = enableSpectralFFT;
        cfg.fractionalAlpha        = fractionalAlpha;
        engine->updateConfig(cfg);
    }
}

JNIEXPORT jfloatArray JNICALL Java_com_example_engine_NativePocketMathBridge_nativeProcessLogits(
    JNIEnv* env, jobject obj, jlong handle, jfloatArray logits
) {
    if (handle == 0 || logits == nullptr) return logits;
    auto* engine = reinterpret_cast<PocketOllama::PocketMathEngine*>(handle);

    jsize len = env->GetArrayLength(logits);
    jfloat* body = env->GetFloatArrayElements(logits, nullptr);
    std::vector<float> inLogits(body, body + len);
    env->ReleaseFloatArrayElements(logits, body, JNI_ABORT);

    std::vector<float> outLogits = engine->processLogitStream(inLogits);

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(outLogits.size()));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(outLogits.size()), outLogits.data());
    return result;
}

JNIEXPORT jfloat JNICALL Java_com_example_engine_NativePocketMathBridge_nativeComputeFractionalEntropy(
    JNIEnv* env, jobject obj, jlong handle, jfloatArray logits
) {
    if (handle == 0 || logits == nullptr) return 0.0f;
    auto* engine = reinterpret_cast<PocketOllama::PocketMathEngine*>(handle);

    jsize len = env->GetArrayLength(logits);
    jfloat* body = env->GetFloatArrayElements(logits, nullptr);
    std::vector<float> inLogits(body, body + len);
    env->ReleaseFloatArrayElements(logits, body, JNI_ABORT);

    return engine->computeFractionalEntropy(inLogits);
}

JNIEXPORT jfloat JNICALL Java_com_example_engine_NativePocketMathBridge_nativeComputePoincareDistance(
    JNIEnv* env, jobject obj, jlong handle, jfloatArray u, jfloatArray v, jfloat c
) {
    if (handle == 0 || u == nullptr || v == nullptr) return 0.0f;
    auto* engine = reinterpret_cast<PocketOllama::PocketMathEngine*>(handle);

    jsize lenU = env->GetArrayLength(u);
    jfloat* bodyU = env->GetFloatArrayElements(u, nullptr);
    std::vector<float> uVec(bodyU, bodyU + lenU);
    env->ReleaseFloatArrayElements(u, bodyU, JNI_ABORT);

    jsize lenV = env->GetArrayLength(v);
    jfloat* bodyV = env->GetFloatArrayElements(v, nullptr);
    std::vector<float> vVec(bodyV, bodyV + lenV);
    env->ReleaseFloatArrayElements(v, bodyV, JNI_ABORT);

    return engine->computePoincareDistance(uVec, vVec, c);
}

JNIEXPORT jfloatArray JNICALL Java_com_example_engine_NativePocketMathBridge_nativeApplyRiemannianEKF(
    JNIEnv* env, jobject obj, jlong handle, jfloatArray logits
) {
    if (handle == 0 || logits == nullptr) return logits;
    auto* engine = reinterpret_cast<PocketOllama::PocketMathEngine*>(handle);

    jsize len = env->GetArrayLength(logits);
    jfloat* body = env->GetFloatArrayElements(logits, nullptr);
    std::vector<float> inLogits(body, body + len);
    env->ReleaseFloatArrayElements(logits, body, JNI_ABORT);

    std::vector<float> outLogits = engine->applyRiemannianEKF(inLogits);

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(outLogits.size()));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(outLogits.size()), outLogits.data());
    return result;
}

JNIEXPORT jfloatArray JNICALL Java_com_example_engine_NativePocketMathBridge_nativeApplySpectralFFT(
    JNIEnv* env, jobject obj, jlong handle, jfloatArray probs, jfloat cutoff
) {
    if (handle == 0 || probs == nullptr) return probs;
    auto* engine = reinterpret_cast<PocketOllama::PocketMathEngine*>(handle);

    jsize len = env->GetArrayLength(probs);
    jfloat* body = env->GetFloatArrayElements(probs, nullptr);
    std::vector<float> inProbs(body, body + len);
    env->ReleaseFloatArrayElements(probs, body, JNI_ABORT);

    std::vector<float> outProbs = engine->applySpectralFFT(inProbs, cutoff);

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(outProbs.size()));
    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(outProbs.size()), outProbs.data());
    return result;
}

} // extern "C"
