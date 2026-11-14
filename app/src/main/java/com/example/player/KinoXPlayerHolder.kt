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
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.example.MainActivity
import com.example.data.model.DecoderMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.VideoItem
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NowPlayingSnapshot(
    val uriString: String = "",
    val title: String = "",
    val isPlaying: Boolean = false,
    val isActive: Boolean = false
)

data class SleepTimerState(
    val option: SleepTimerOption = SleepTimerOption.OFF,
    val remainingSeconds: Long = 0L,
    val customMinutes: Int = 0
) {
    val isActive: Boolean
        get() = option != SleepTimerOption.OFF || customMinutes > 0

    val badgeLabel: String
        get() = when {
            option == SleepTimerOption.END_OF_VIDEO -> "END"
            isActive -> {
                val mins = remainingSeconds / 60L
                val secs = remainingSeconds % 60L
                String.format(java.util.Locale.US, "%d:%02d", mins, secs)
            }
            else -> ""
        }
}

data class PlaylistState(
    val items: List<VideoItem> = emptyList(),
    val currentIndex: Int = -1
) {
    val hasPrevious: Boolean get() = currentIndex > 0 && items.isNotEmpty()
    val hasNext: Boolean get() = currentIndex >= 0 && currentIndex < items.lastIndex
    val nextTitle: String? get() = items.getOrNull(currentIndex + 1)?.title
}

object KinoXPlayerHolder {

    private val holderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
    var backgroundPlayEnabled: Boolean = true

    @Volatile
    var currentUriString: String = ""

    @Volatile
    var currentTitle: String = ""

    @Volatile
    private var progressSaver: (() -> Unit)? = null

    private val _nowPlaying = MutableStateFlow(NowPlayingSnapshot())
    val nowPlaying: StateFlow<NowPlayingSnapshot> = _nowPlaying.asStateFlow()

    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    private val _sleepAutoCloseEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val sleepAutoCloseEvents: SharedFlow<Unit> = _sleepAutoCloseEvents.asSharedFlow()

    private val _playlist = MutableStateFlow(PlaylistState())
    val playlist: StateFlow<PlaylistState> = _playlist.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var sleepTimerJob: Job? = null

    fun setPlaylist(items: List<VideoItem>, activeUriString: String) {
        if (items.isEmpty()) return
        val idx = items.indexOfFirst { it.uriString == activeUriString }
        _playlist.value = PlaylistState(
            items = items,
            currentIndex = if (idx >= 0) idx else 0
        )
    }

    fun stepPlaylist(delta: Int): VideoItem? {
        val current = _playlist.value
        val targetIdx = current.currentIndex + delta
        val target = current.items.getOrNull(targetIdx) ?: return null
        _playlist.value = current.copy(currentIndex = targetIdx)
        return target
    }

    fun registerProgressSaver(saver: (() -> Unit)?) {
        progressSaver = saver
    }

    fun updateNowPlayingMetadata(uriString: String, title: String) {
        currentUriString = uriString
        currentTitle = title
        val currentQueue = _playlist.value
        val matchedIdx = currentQueue.items.indexOfFirst { it.uriString == uriString }
        if (matchedIdx >= 0 && matchedIdx != currentQueue.currentIndex) {
            _playlist.value = currentQueue.copy(currentIndex = matchedIdx)
        }
        _nowPlaying.value = NowPlayingSnapshot(
            uriString = uriString,
            title = title,
            isPlaying = exoPlayer?.isPlaying == true,
            isActive = uriString.isNotBlank() && exoPlayer != null
        )
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        when (option) {
            SleepTimerOption.OFF -> {
                _sleepTimer.value = SleepTimerState()
            }
            SleepTimerOption.END_OF_VIDEO -> {
                _sleepTimer.value = SleepTimerState(
                    option = SleepTimerOption.END_OF_VIDEO,
                    remainingSeconds = 0L,
                    customMinutes = 0
                )
            }
            else -> startCountdown(
                option = option,
                totalMinutes = option.minutes,
                customMinutes = 0
            )
        }
    }

    fun setCustomSleepMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        val clamped = minutes.coerceIn(0, 240)
        if (clamped == 0) {
            _sleepTimer.value = SleepTimerState()
            return
        }
        startCountdown(
            option = SleepTimerOption.OFF,
            totalMinutes = clamped,
            customMinutes = clamped
        )
    }

    private fun startCountdown(
        option: SleepTimerOption,
        totalMinutes: Int,
        customMinutes: Int
    ) {
        val totalSeconds = totalMinutes * 60L
        _sleepTimer.value = SleepTimerState(
            option = option,
            remainingSeconds = totalSeconds,
            customMinutes = customMinutes
        )
        sleepTimerJob = holderScope.launch {
            var remaining = totalSeconds
            while (remaining > 0L && isActive) {
                delay(1000L)
                remaining--
                _sleepTimer.value = _sleepTimer.value.copy(remainingSeconds = remaining)
            }
            if (isActive && remaining <= 0L) {
                triggerSleepAutoClose()
            }
        }
    }

    fun triggerSleepAutoClose() {
        sleepTimerJob?.cancel()
        _sleepTimer.value = SleepTimerState()
        runCatching { progressSaver?.invoke() }
        exoPlayer?.pause()
        stopAndClearSession()
        disconnectBackgroundService()
        _sleepAutoCloseEvents.tryEmit(Unit)
    }

    @OptIn(UnstableApi::class)
    @Synchronized
    fun buildOrReplacePlayer(
        context: Context,
        decoderMode: DecoderMode,
        seekStepSeconds: Int = 10
    ): ExoPlayer {
        val appContext = context.applicationContext
        val oldPlayer = exoPlayer

        val renderersFactory = DecoderFactory.createRenderersFactory(appContext, decoderMode)
        val loadControl = BatteryOptimizer.buildAlwaysOptimizedLoadControl(appContext)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)
        val mediaSourceFactory = DefaultMediaSourceFactory(appContext, extractorsFactory)
        val seekIncrementMs = seekStepSeconds.coerceIn(5, 60) * 1000L

        val newPlayer = ExoPlayer.Builder(appContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(seekIncrementMs)
            .setSeekForwardIncrementMs(seekIncrementMs)
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

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED &&
                    _sleepTimer.value.option == SleepTimerOption.END_OF_VIDEO
                ) {
                    triggerSleepAutoClose()
                }
            }
        })

        exoPlayer = newPlayer
        currentDecoderMode = decoderMode

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
        val player = exoPlayer ?: buildOrReplacePlayer(appContext, currentDecoderMode)
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
        sleepTimerJob?.cancel()
        _sleepTimer.value = SleepTimerState()
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
