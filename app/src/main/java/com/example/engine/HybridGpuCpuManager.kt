package com.example.engine

import android.content.Context
import android.util.Log

data class HybridOffloadPlan(
    val isGpuAccelerated: Boolean,
    val totalModelLayers: Int,
    val gpuOffloadLayers: Int,
    val cpuThreads: Int,
    val safeContextLength: Int,
    val isOomRiskHigh: Boolean,
    val estimatedRamMbNeeded: Int,
    val memoryStatusSummary: String
)

object HybridGpuCpuManager {
    private const val TAG = "HybridGpuCpuManager"

    /**
     * Computes an optimal, crash-safe hybrid GPU + CPU offload plan for a given model weight size
     * based on active hardware telemetry (total RAM, available RAM, GPU readiness).
     */
    fun calculateOffloadPlan(
        context: Context,
        modelSizeMb: Int,
        totalModelLayers: Int = 32,
        userGpuLayersPreference: Int = -1, // -1 means AUTO
        requestedContextLength: Int = 2048,
        requestedThreads: Int = 4,
        isGpuOffloadEnabled: Boolean = true
    ): HybridOffloadPlan {
        val hwInfo = DeviceHardwareManager.getHardwareInfo(context)
        val availRamMb = hwInfo.availableRamMb
        val totalRamMb = hwInfo.totalRamMb

        // Estimate KV-cache & overhead memory per 1024 context tokens (~150MB - 300MB depending on layers)
        val kvCacheOverheadMb = (requestedContextLength / 1024) * 120
        val totalEstimatedRamNeeded = modelSizeMb + kvCacheOverheadMb + 400 // 400MB base system overhead

        val isOomRiskHigh = totalEstimatedRamNeeded > (availRamMb + 300)

        // Automatically scale safe context length down if memory is tight to prevent Android LMK (Low Memory Killer) crash
        val safeContextLength = when {
            availRamMb < 1200 -> requestedContextLength.coerceAtMost(512)
            availRamMb < 2200 -> requestedContextLength.coerceAtMost(1024)
            else -> requestedContextLength
        }

        // Determine optimal GPU offload layers
        val maxPossibleGpuLayers = totalModelLayers.coerceAtLeast(1)
        val isGpuAvailable = hwInfo.supportedAbis.contains("arm64", ignoreCase = true) && isGpuOffloadEnabled

        val computedGpuLayers = when {
            !isGpuAvailable -> 0
            userGpuLayersPreference >= 0 -> userGpuLayersPreference.coerceIn(0, maxPossibleGpuLayers)
            availRamMb >= 6000 -> maxPossibleGpuLayers // Full offload if plenty of RAM
            availRamMb >= 3500 -> (maxPossibleGpuLayers * 0.75f).toInt() // 75% GPU, 25% CPU
            availRamMb >= 2000 -> (maxPossibleGpuLayers * 0.50f).toInt() // 50% GPU, 50% CPU
            else -> (maxPossibleGpuLayers * 0.25f).toInt() // Minimal GPU offload to avoid GPU VRAM/unified memory crash
        }

        // Clamp CPU threads based on physical cores to prevent thread thrashing
        val safeCpuThreads = requestedThreads.coerceAtMost(hwInfo.cpuCores)

        val memoryStatusSummary = when {
            isOomRiskHigh -> "⚠️ High Memory Load: Hybrid GPU/CPU auto-balancing active ($availRamMb MB RAM free)."
            computedGpuLayers > 0 -> "⚡ Hybrid GPU (${computedGpuLayers}L) + CPU (${totalModelLayers - computedGpuLayers}L) Offloading Active."
            else -> "💻 Pure CPU Execution ($safeCpuThreads Threads, NEON Vectorized)."
        }

        Log.i(TAG, "Calculated Hybrid Plan: GPU Layers=$computedGpuLayers/$totalModelLayers, CPU Threads=$safeCpuThreads, SafeCtx=$safeContextLength, Estimated RAM=$totalEstimatedRamNeeded MB, Avail RAM=$availRamMb MB")

        return HybridOffloadPlan(
            isGpuAccelerated = computedGpuLayers > 0,
            totalModelLayers = totalModelLayers,
            gpuOffloadLayers = computedGpuLayers,
            cpuThreads = safeCpuThreads,
            safeContextLength = safeContextLength,
            isOomRiskHigh = isOomRiskHigh,
            estimatedRamMbNeeded = totalEstimatedRamNeeded,
            memoryStatusSummary = memoryStatusSummary
        )
    }
}
