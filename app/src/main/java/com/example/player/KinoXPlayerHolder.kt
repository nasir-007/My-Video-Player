package com.example.player

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.example.MainActivity
import com.example.data.model.DecoderMode
import com.example.data.model.PowerProfile
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlayingSnapshot(
    val uriString: String = "",
    val title: String = "",
    val isPlaying: Boolean = false,
    val isActive: Boolean = false
)

object KinoXPlayerHolder {

    @Volatile
    var exoPlayer: ExoPlayer? = null
        private set

    @Volatile
    var mediaSession: MediaSession? = null
        private set

    @Volatile
    var currentDecoderMode: DecoderMode = DecoderMode.HW_PLUS
        private set

    @Volatile
    var currentPowerProfile: PowerProfile = PowerProfile.BALANCED
        private set

    @Volatile
    var backgroundPlayEnabled: Boolean = true

    @Volatile
    var currentUriString: String = ""

    @Volatile
    var currentTitle: String = ""

    private val _nowPlaying = MutableStateFlow(NowPlayingSnapshot())
    val nowPlaying: StateFlow<NowPlayingSnapshot> = _nowPlaying.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null

    fun updateNowPlayingMetadata(uriString: String, title: String) {
        currentUriString = uriString
        currentTitle = title
        _nowPlaying.value = NowPlayingSnapshot(
            uriString = uriString,
            title = title,
            isPlaying = exoPlayer?.isPlaying == true,
            isActive = uriString.isNotBlank() && exoPlayer != null
        )
    }

    @OptIn(UnstableApi::class)
    @Synchronized
    fun buildOrReplacePlayer(
        context: Context,
        decoderMode: DecoderMode,
        powerProfile: PowerProfile
    ): ExoPlayer {
        val appContext = context.applicationContext
        val oldPlayer = exoPlayer

        val renderersFactory = DecoderFactory.createRenderersFactory(appContext, decoderMode)
        val loadControl = BatteryOptimizer.buildLoadControl(powerProfile)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val newPlayer = ExoPlayer.Builder(appContext, renderersFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .setWakeMode(C.WAKE_MODE_NONE)
            .build()

        newPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                newPlayer.setWakeMode(if (isPlaying) C.WAKE_MODE_LOCAL else C.WAKE_MODE_NONE)
                _nowPlaying.value = NowPlayingSnapshot(
                    uriString = currentUriString,
                    title = currentTitle,
                    isPlaying = isPlaying,
                    isActive = currentUriString.isNotBlank()
                )
            }
        })

        exoPlayer = newPlayer
        currentDecoderMode = decoderMode
        currentPowerProfile = powerProfile

        val existingSession = mediaSession
        if (existingSession != null) {
            existingSession.player = newPlayer
        } else {
            createMediaSession(appContext, newPlayer)
        }

        oldPlayer?.release()
        return newPlayer
    }

    @Synchronized
    fun getOrCreateMediaSession(context: Context): MediaSession {
        mediaSession?.let { return it }
        val appContext = context.applicationContext
        val player = exoPlayer ?: buildOrReplacePlayer(appContext, currentDecoderMode, currentPowerProfile)
        return mediaSession ?: createMediaSession(appContext, player)
    }

    private fun createMediaSession(appContext: Context, player: ExoPlayer): MediaSession {
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            appContext,
            101,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return MediaSession.Builder(appContext, player)
            .setId("kinox_background_session")
            .setSessionActivity(sessionActivity)
            .build()
            .also { mediaSession = it }
    }

    fun connectBackgroundService(context: Context) {
        val appContext = context.applicationContext
        if (controllerFuture != null) return
        val sessionToken = SessionToken(appContext, ComponentName(appContext, KinoXPlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({}, MoreExecutors.directExecutor())
    }

    fun togglePlayPauseFromMiniBar() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stopAndClearSession() {
        exoPlayer?.pause()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        currentUriString = ""
        currentTitle = ""
        _nowPlaying.value = NowPlayingSnapshot()
    }

    fun disconnectBackgroundService() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    @Synchronized
    fun releaseAll() {
        disconnectBackgroundService()
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        currentUriString = ""
        currentTitle = ""
        _nowPlaying.value = NowPlayingSnapshot()
    }
}
