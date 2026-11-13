package com.example.player

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import com.example.data.model.PowerProfile

data class BatteryStatusSnapshot(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val isSystemPowerSaveMode: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false
)

object BatteryOptimizer {

    fun readBatterySnapshot(context: Context): BatteryStatusSnapshot {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            100
        }

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val isPowerSave = powerManager?.isPowerSaveMode ?: false
        val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }

        return BatteryStatusSnapshot(
            batteryPercent = percent,
            isCharging = isCharging,
            isSystemPowerSaveMode = isPowerSave,
            isIgnoringBatteryOptimizations = isIgnoring
        )
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    @OptIn(UnstableApi::class)
    fun buildLoadControl(profile: PowerProfile): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                profile.minBufferMs,
                profile.maxBufferMs,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(profile != PowerProfile.ECO)
            .build()
    }
}
