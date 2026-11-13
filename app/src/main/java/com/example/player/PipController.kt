package com.example.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import com.example.R

object PipController {
    const val ACTION_PIP_CONTROL = "com.example.kinox.PIP_CONTROL"
    const val EXTRA_CONTROL_TYPE = "control_type"
    const val CONTROL_REWIND = 1
    const val CONTROL_PLAY_PAUSE = 2
    const val CONTROL_FORWARD = 3

    fun isPipSupported(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun buildParams(
        context: Context,
        isPlaying: Boolean,
        videoWidth: Int,
        videoHeight: Int,
        sourceRectHint: Rect? = null,
        autoEnter: Boolean = false
    ): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val aspectRatio = computeClampedRational(videoWidth, videoHeight)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(buildActions(context, isPlaying))

        if (sourceRectHint != null && !sourceRectHint.isEmpty) {
            builder.setSourceRectHint(sourceRectHint)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun computeClampedRational(width: Int, height: Int): Rational {
        if (width <= 0 || height <= 0) return Rational(16, 9)
        val raw = width.toFloat() / height.toFloat()
        // Android PiP requires aspect ratio between 1/2.39 and 2.39/1
        return when {
            raw < 0.45f -> Rational(9, 20)
            raw > 2.35f -> Rational(21, 9)
            else -> Rational(width, height)
        }
    }

    private fun buildActions(context: Context, isPlaying: Boolean): List<RemoteAction> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()

        val rewindIntent = createPendingIntent(context, CONTROL_REWIND)
        val playPauseIntent = createPendingIntent(context, CONTROL_PLAY_PAUSE)
        val forwardIntent = createPendingIntent(context, CONTROL_FORWARD)

        val rewindAction = RemoteAction(
            Icon.createWithResource(context, android.R.drawable.ic_media_rew),
            context.getString(R.string.pip_action_rewind),
            context.getString(R.string.pip_action_rewind),
            rewindIntent
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (isPlaying) {
            context.getString(R.string.pip_action_pause)
        } else {
            context.getString(R.string.pip_action_play)
        }
        val playPauseAction = RemoteAction(
            Icon.createWithResource(context, playPauseIcon),
            playPauseTitle,
            playPauseTitle,
            playPauseIntent
        )

        val forwardAction = RemoteAction(
            Icon.createWithResource(context, android.R.drawable.ic_media_ff),
            context.getString(R.string.pip_action_forward),
            context.getString(R.string.pip_action_forward),
            forwardIntent
        )

        return listOf(rewindAction, playPauseAction, forwardAction)
    }

    private fun createPendingIntent(context: Context, controlType: Int): PendingIntent {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_CONTROL_TYPE, controlType)
        }
        return PendingIntent.getBroadcast(
            context,
            controlType,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
