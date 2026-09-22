package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

data class DeviceHardwareInfo(
    val totalRamMb: Int,
    val availableRamMb: Int,
    val usedRamPercent: Int,
    val cpuCores: Int,
    val supportedAbis: String,
    val recommendedMaxModelParam: String,
    val batteryPercent: Int,
    val isThermalThrottling: Boolean,
    val deviceName: String
)

object DeviceHardwareManager {

    fun getHardwareInfo(context: Context): DeviceHardwareInfo {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt().coerceAtLeast(2048)
        val availableRamMb = (memInfo.availMem / (1024 * 1024)).toInt().coerceAtLeast(1024)
        val usedRamPercent = (((totalRamMb - availableRamMb).toFloat() / totalRamMb) * 100).toInt().coerceIn(0, 100)

        val cpuCores = Runtime.getRuntime().availableProcessors()
        val abis = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        val recommendedModel = when {
            totalRamMb >= 11000 -> "Up to 8B Models (Q4_K_M / Q8_0)"
            totalRamMb >= 7000 -> "Up to 3B - 7B Models (Q4_K_M)"
            totalRamMb >= 5000 -> "Up to 3B Models (Q4_K_M)"
            else -> "Up to 1.5B Models (Q4_K_M)"
        }

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isThrottling = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (powerManager?.currentThermalStatus ?: 0) >= PowerManager.THERMAL_STATUS_MODERATE
        } else {
            false
        }

        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        return DeviceHardwareInfo(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            usedRamPercent = usedRamPercent,
            cpuCores = cpuCores,
            supportedAbis = abis,
            recommendedMaxModelParam = recommendedModel,
            batteryPercent = batteryLevel,
            isThermalThrottling = isThrottling,
            deviceName = deviceName
        )
    }
}
