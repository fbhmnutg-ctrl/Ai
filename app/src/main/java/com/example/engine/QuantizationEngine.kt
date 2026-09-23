package com.example.engine

data class QuantizationInfo(
    val code: String,            // e.g. "Q6_K_P"
    val displayName: String,     // e.g. "Q6_K_P (6-bit Precision)"
    val bitsPerWeight: Float,    // e.g. 6.56f
    val category: QuantCategory, // HIGH_FIDELITY, BALANCED, ULTRA_LIGHT, EXTREME, UNQUANTIZED
    val qualityTier: String,     // e.g. "Near Lossless (99.8%)"
    val description: String,     // e.g. "6-bit K-quant Precision with enhanced importance matrix weighting. Ideal for strict mathematical precision, code generation, and complex logic."
    val recommendedUse: String,  // e.g. "Coding, math reasoning, zero perplexity degradation"
    val ramFactor: Float         // Factor relative to parameter count
)

enum class QuantCategory(val title: String, val badgeColorHex: Long) {
    HIGH_FIDELITY("High Fidelity (Q6/Q8)", 0xFF00E5FF),
    BALANCED("Balanced (Q4/Q5)", 0xFF10B981),
    ULTRA_LIGHT("Ultra Compact (Q3/IQ3)", 0xFFF59E0B),
    EXTREME("Extreme (Q2/IQ1/IQ2)", 0xFFEC4899),
    UNQUANTIZED("Full Float (FP16/BF16)", 0xFF8B5CF6)
}

object QuantizationEngine {

    val ALL_QUANTS = listOf(
        // High Fidelity / Near Lossless
        QuantizationInfo(
            code = "Q6_K_P",
            displayName = "Q6_K_P (6-bit Precision)",
            bitsPerWeight = 6.56f,
            category = QuantCategory.HIGH_FIDELITY,
            qualityTier = "Near-Lossless (99.8%)",
            description = "6-bit K-quant Precision variant with optimized scaling. Minimizes perplexity loss on coding, math calculations, and multi-turn reasoning.",
            recommendedUse = "Strict precision, programming & formal logic",
            ramFactor = 1.65f
        ),
        QuantizationInfo(
            code = "Q6_K",
            displayName = "Q6_K (6-bit Standard)",
            bitsPerWeight = 6.56f,
            category = QuantCategory.HIGH_FIDELITY,
            qualityTier = "Near-Lossless (99.7%)",
            description = "Standard 6-bit K-quant. High quality across all token distributions with minimal loss compared to FP16.",
            recommendedUse = "General high-fidelity reasoning",
            ramFactor = 1.62f
        ),
        QuantizationInfo(
            code = "Q6_K_L",
            displayName = "Q6_K_L (6-bit Large)",
            bitsPerWeight = 6.62f,
            category = QuantCategory.HIGH_FIDELITY,
            qualityTier = "Near-Lossless (99.8%)",
            description = "6-bit K-quant with large attention block weights.",
            recommendedUse = "Complex syntax & long context",
            ramFactor = 1.68f
        ),
        QuantizationInfo(
            code = "Q8_0",
            displayName = "Q8_0 (8-bit Integer)",
            bitsPerWeight = 8.50f,
            category = QuantCategory.HIGH_FIDELITY,
            qualityTier = "Studio Grade (99.9%)",
            description = "8-bit uniform quantization. Pristine fidelity indistinguishable from 16-bit floating point.",
            recommendedUse = "Benchmark baseline & maximum accuracy",
            ramFactor = 2.05f
        ),

        // Balanced 5-bit & 4-bit
        QuantizationInfo(
            code = "Q5_K_M",
            displayName = "Q5_K_M (5-bit Medium)",
            bitsPerWeight = 5.50f,
            category = QuantCategory.BALANCED,
            qualityTier = "Very High (99.2%)",
            description = "5-bit K-quant with medium tensor blocks. The sweet spot between 6-bit quality and 4-bit memory efficiency.",
            recommendedUse = "Balanced daily assistant tasks",
            ramFactor = 1.38f
        ),
        QuantizationInfo(
            code = "Q5_K_S",
            displayName = "Q5_K_S (5-bit Small)",
            bitsPerWeight = 5.30f,
            category = QuantCategory.BALANCED,
            qualityTier = "Very High (98.9%)",
            description = "5-bit K-quant with compact blocks for reduced memory footprint.",
            recommendedUse = "Mid-range devices with 4-6 GB RAM",
            ramFactor = 1.32f
        ),
        QuantizationInfo(
            code = "Q5_0",
            displayName = "Q5_0 (5-bit Standard)",
            bitsPerWeight = 5.50f,
            category = QuantCategory.BALANCED,
            qualityTier = "High (98.5%)",
            description = "Legacy 5-bit uniform quantization with fast SIMD vectorization.",
            recommendedUse = "Fast CPU execution",
            ramFactor = 1.35f
        ),
        QuantizationInfo(
            code = "Q4_K_M",
            displayName = "Q4_K_M (4-bit Medium)",
            bitsPerWeight = 4.50f,
            category = QuantCategory.BALANCED,
            qualityTier = "Optimal Gold Standard (98.0%)",
            description = "The industry standard 4-bit K-quant. High compression ratio with negligible quality loss on 1B-8B parameter models.",
            recommendedUse = "Standard mobile deployment & fast streaming",
            ramFactor = 1.15f
        ),
        QuantizationInfo(
            code = "Q4_K_S",
            displayName = "Q4_K_S (4-bit Small)",
            bitsPerWeight = 4.30f,
            category = QuantCategory.BALANCED,
            qualityTier = "Good (97.3%)",
            description = "4-bit K-quant with reduced attention weight sizing for maximum RAM savings.",
            recommendedUse = "Devices with tight RAM limits",
            ramFactor = 1.08f
        ),
        QuantizationInfo(
            code = "Q4_0",
            displayName = "Q4_0 (4-bit Legacy)",
            bitsPerWeight = 4.50f,
            category = QuantCategory.BALANCED,
            qualityTier = "Good (96.8%)",
            description = "Fastest CPU vector processing with ARM NEON integer dot products.",
            recommendedUse = "Maximum token throughput (tok/s)",
            ramFactor = 1.12f
        ),
        QuantizationInfo(
            code = "IQ4_NL",
            displayName = "IQ4_NL (Importance Matrix 4-bit)",
            bitsPerWeight = 4.50f,
            category = QuantCategory.BALANCED,
            qualityTier = "Superior 4-bit (98.7%)",
            description = "Non-linear 4-bit importance matrix quantization. Outperforms traditional Q4_K_M at identical file sizes.",
            recommendedUse = "SOTA 4-bit accuracy for reasoning",
            ramFactor = 1.16f
        ),
        QuantizationInfo(
            code = "IQ4_XS",
            displayName = "IQ4_XS (Extra Small 4-bit)",
            bitsPerWeight = 4.25f,
            category = QuantCategory.BALANCED,
            qualityTier = "High Efficiency (97.8%)",
            description = "Extra-small importance matrix 4-bit format optimized for mobile cache hierarchy.",
            recommendedUse = "High efficiency mobile reasoning",
            ramFactor = 1.05f
        ),

        // Ultra Compact 3-bit
        QuantizationInfo(
            code = "IQ3_M",
            displayName = "IQ3_M (Importance Matrix 3-bit Medium)",
            bitsPerWeight = 3.70f,
            category = QuantCategory.ULTRA_LIGHT,
            qualityTier = "High Efficiency (96.0%)",
            description = "3-bit importance matrix providing quality comparable to traditional Q4 on larger architectures.",
            recommendedUse = "Running 3B-7B models under 2.5 GB RAM",
            ramFactor = 0.92f
        ),
        QuantizationInfo(
            code = "IQ3_XXS",
            displayName = "IQ3_XXS (Importance Matrix 3-bit Extra Extra Small)",
            bitsPerWeight = 3.06f,
            category = QuantCategory.ULTRA_LIGHT,
            qualityTier = "Ultra Compact (94.5%)",
            description = "Extreme 3-bit compression with importance matrix calibration. Enables larger models on entry-level hardware.",
            recommendedUse = "Low-end devices & tight memory constraint",
            ramFactor = 0.78f
        ),
        QuantizationInfo(
            code = "Q3_K_L",
            displayName = "Q3_K_L (3-bit Large)",
            bitsPerWeight = 3.82f,
            category = QuantCategory.ULTRA_LIGHT,
            qualityTier = "Moderate (95.5%)",
            description = "3-bit K-quant with large tensor allocation for critical layers.",
            recommendedUse = "Memory-constrained multi-tasking",
            ramFactor = 0.95f
        ),
        QuantizationInfo(
            code = "Q3_K_M",
            displayName = "Q3_K_M (3-bit Medium)",
            bitsPerWeight = 3.50f,
            category = QuantCategory.ULTRA_LIGHT,
            qualityTier = "Moderate (94.0%)",
            description = "Standard 3-bit K-quant.",
            recommendedUse = "Lightweight on-device deployment",
            ramFactor = 0.88f
        ),

        // Extreme 2-bit & 1-bit
        QuantizationInfo(
            code = "IQ2_XXS",
            displayName = "IQ2_XXS (Importance Matrix 2-bit)",
            bitsPerWeight = 2.06f,
            category = QuantCategory.EXTREME,
            qualityTier = "Extreme Compression (88.0%)",
            description = "2-bit importance matrix tensor compression for massive parameter architectures.",
            recommendedUse = "Running 7B-8B models on 2GB RAM phones",
            ramFactor = 0.52f
        ),
        QuantizationInfo(
            code = "Q2_K",
            displayName = "Q2_K (2-bit K-quant)",
            bitsPerWeight = 2.56f,
            category = QuantCategory.EXTREME,
            qualityTier = "Extreme (89.5%)",
            description = "2-bit K-quantization for ultra-low memory footprints.",
            recommendedUse = "Research & extreme memory tests",
            ramFactor = 0.65f
        ),

        // Floating Point Precision
        QuantizationInfo(
            code = "FP16",
            displayName = "FP16 / F16 (16-bit Float)",
            bitsPerWeight = 16.0f,
            category = QuantCategory.UNQUANTIZED,
            qualityTier = "Full Precision (100%)",
            description = "Unquantized 16-bit floating point weights. Zero quantization error.",
            recommendedUse = "Reference evaluations & high-end GPUs",
            ramFactor = 3.80f
        ),
        QuantizationInfo(
            code = "BF16",
            displayName = "BF16 (16-bit Bfloat)",
            bitsPerWeight = 16.0f,
            category = QuantCategory.UNQUANTIZED,
            qualityTier = "Full Precision Bfloat (100%)",
            description = "Brain Floating Point 16-bit format with wide dynamic range.",
            recommendedUse = "Neural weights straight from training",
            ramFactor = 3.80f
        )
    )

    fun find(code: String): QuantizationInfo {
        val clean = code.uppercase().replace("-", "_").trim()
        return ALL_QUANTS.find { it.code.equals(clean, ignoreCase = true) }
            ?: ALL_QUANTS.find { clean.contains(it.code, ignoreCase = true) }
            ?: QuantizationInfo(
                code = clean.ifBlank { "Q4_K_M" },
                displayName = clean.ifBlank { "Q4_K_M" },
                bitsPerWeight = 4.5f,
                category = QuantCategory.BALANCED,
                qualityTier = "Standard (98.0%)",
                description = "Quantized GGUF neural weights representation ($clean).",
                recommendedUse = "On-device mobile inference",
                ramFactor = 1.2f
            )
    }

    fun parseFromFilename(filename: String): QuantizationInfo {
        val lower = filename.lowercase()
        return when {
            // High precision & 6-bit / 8-bit
            "q6_k_p" in lower || "q6-k-p" in lower -> find("Q6_K_P")
            "q6_k_l" in lower || "q6-k-l" in lower -> find("Q6_K_L")
            "q6_k" in lower || "q6-k" in lower || "q6_0" in lower -> find("Q6_K")
            "q8_0" in lower || "q8_k" in lower || "q8-0" in lower -> find("Q8_0")

            // 5-bit
            "q5_k_m" in lower || "q5-k-m" in lower -> find("Q5_K_M")
            "q5_k_s" in lower || "q5-k-s" in lower -> find("Q5_K_S")
            "q5_0" in lower || "q5-0" in lower -> find("Q5_0")
            "q5_1" in lower || "q5-1" in lower -> find("Q5_0")

            // I-quants 4-bit
            "iq4_nl" in lower || "iq4-nl" in lower -> find("IQ4_NL")
            "iq4_xs" in lower || "iq4-xs" in lower -> find("IQ4_XS")

            // 4-bit
            "q4_k_m" in lower || "q4-k-m" in lower || "q4_k" in lower -> find("Q4_K_M")
            "q4_k_s" in lower || "q4-k-s" in lower -> find("Q4_K_S")
            "q4_0" in lower || "q4-0" in lower -> find("Q4_0")
            "q4_1" in lower || "q4-1" in lower -> find("Q4_0")

            // I-quants 3-bit
            "iq3_xxs" in lower || "iq3-xxs" in lower -> find("IQ3_XXS")
            "iq3_s" in lower || "iq3-s" in lower || "iq3_m" in lower -> find("IQ3_M")

            // 3-bit
            "q3_k_l" in lower || "q3-k-l" in lower -> find("Q3_K_L")
            "q3_k_m" in lower || "q3-k-m" in lower || "q3_k" in lower -> find("Q3_K_M")
            "q3_k_s" in lower || "q3-k-s" in lower -> find("Q3_K_M")

            // 2-bit & I-quants
            "iq2_xxs" in lower || "iq2-xxs" in lower || "iq2_xs" in lower -> find("IQ2_XXS")
            "q2_k" in lower || "q2-k" in lower -> find("Q2_K")

            // Floats
            "bf16" in lower -> find("BF16")
            "fp16" in lower || "f16" in lower -> find("FP16")
            "f32" in lower || "fp32" in lower -> find("FP16")

            else -> find("Q4_K_M")
        }
    }

    fun extractFromFilename(filename: String): String {
        return parseFromFilename(filename).code
    }
}
