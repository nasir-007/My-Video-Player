package com.example.player

import android.content.Context
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

object BatteryOptimizer {

    fun isSystemPowerSaveActive(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode ?: false
    }

    fun optimalTickerIntervalMs(context: Context, isPlaying: Boolean): Long {
        return when {
            !isPlaying -> 400L
            isSystemPowerSaveActive(context) -> 220L
            else -> 110L
        }
    }

    @OptIn(UnstableApi::class)
    fun buildAlwaysOptimizedLoadControl(context: Context): DefaultLoadControl {
        val saverActive = isSystemPowerSaveActive(context)
        val minBufferMs = if (saverActive) 8_000 else 12_000
        val maxBufferMs = if (saverActive) 18_000 else 28_000
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                200,
                400
            )
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()
    }
}
