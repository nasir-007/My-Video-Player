package com.example.ui.player

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.example.data.local.KinoXDatabase
import com.example.data.local.PlayerPreferencesStore
import com.example.data.model.AspectRatioMode
import com.example.data.model.CodecTelemetry
import com.example.data.model.DecoderMode
import com.example.data.model.MediaTrackInfo
import com.example.data.model.OrientationMode
import com.example.data.model.SleepTimerOption
import com.example.data.model.SubtitleStylePrefs
import com.example.data.repository.MediaRepository
import com.example.player.BatteryOptimizer
import com.example.player.KinoXPlayerHolder
import com.example.player.SleepTimerState
import com.example.player.SubtitleCue
import com.example.player.SubtitleParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface GestureHudState {
    data class Brightness(val percent: Int) : GestureHudState
    data class Volume(val percent: Int) : GestureHudState
    data class Seek(val targetMs: Long, val deltaMs: Long) : GestureHudState
    data class DoubleTapSeek(val isForward: Boolean, val seconds: Int) : GestureHudState
    data class Zoom(val scale: Float) : GestureHudState
    data class Toast(val message: String) : GestureHudState
}

enum class PlayerSheetType {
    NONE,
    SUBTITLES,
    DECODER_INFO,
    AUDIO_SPEED,
    SLEEP_TIMER
}

data class PlayerUiState(
    val uriString: String = "",
    val title: String = "",
    val folderUri: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val decoderMode: DecoderMode = DecoderMode.HW_PLUS,
    val backgroundPlayEnabled: Boolean = true,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val aspectMode: AspectRatioMode = AspectRatioMode.FIT,
    val orientationMode: OrientationMode = OrientationMode.AUTO,
    val playbackSpeed: Float = 1.0f,
    val seekStepSeconds: Int = 10,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val seekGestureEnabled: Boolean = true,
    val pinchZoomEnabled: Boolean = true,
    val doubleTapSeekEnabled: Boolean = true,
    val isLocked: Boolean = false,
    val controlsVisible: Boolean = true,
    val unlockHintVisible: Boolean = false,
    val isInPip: Boolean = false,
    val zoomScale: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val gestureHud: GestureHudState? = null,
    val activeSheet: PlayerSheetType = PlayerSheetType.NONE,
    val audioTracks: List<MediaTrackInfo> = emptyList(),
    val subtitleTracks: List<MediaTrackInfo> = emptyList(),
    val externalSubtitleUri: String? = null,
    val externalSubtitleName: String? = null,
    val subtitlesEnabled: Boolean = true,
    val subtitleDelayMs: Long = 0L,
    val subtitleStyle: SubtitleStylePrefs = SubtitleStylePrefs(),
    val activeSubtitleText: String? = null,
    val hasPreviousVideo: Boolean = false,
    val hasNextVideo: Boolean = false,
    val nextVideoTitle: String? = null,
    val codecTelemetry: CodecTelemetry = CodecTelemetry(),
    val errorMessage: String? = null,
    val playerInstanceVersion: Int = 0
)

@OptIn(UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KinoXDatabase.getInstance(application)
    private val prefsStore = PlayerPreferencesStore(application)
    private val repository = MediaRepository(application, database.kinoXDao(), prefsStore)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var currentExoPlayer: ExoPlayer? = null
        private set

    private var currentTracks: Tracks = Tracks.EMPTY
    private var externalCues: List<SubtitleCue> = emptyList()
    private var embeddedCueText: String? = null

    private var positionTickerJob: Job? = null
    private var hudHideJob: Job? = null
    private var gestureClearJob: Job? = null
    private var unlockHintJob: Job? = null
    private var lastSavedAtMs: Long = 0L

    init {
        KinoXPlayerHolder.registerProgressSaver { persistProgress() }
        viewModelScope.launch {
            repository.playbackPrefs.collect { prefs ->
                KinoXPlayerHolder.backgroundPlayEnabled = prefs.backgroundPlayEnabled
                _uiState.update {
                    it.copy(
                        subtitleStyle = prefs.subtitleStyle,
                        backgroundPlayEnabled = prefs.backgroundPlayEnabled,
                        seekStepSeconds = prefs.seekStepSeconds,
                        brightnessGestureEnabled = prefs.brightnessGestureEnabled,
                        volumeGestureEnabled = prefs.volumeGestureEnabled,
                        seekGestureEnabled = prefs.seekGestureEnabled,
                        pinchZoomEnabled = prefs.pinchZoomEnabled,
                        doubleTapSeekEnabled = prefs.doubleTapSeekEnabled
                    )
                }
            }
        }
        viewModelScope.launch {
            KinoXPlayerHolder.sleepTimer.collect { sleep ->
                _uiState.update { it.copy(sleepTimer = sleep) }
            }
        }
        viewModelScope.launch {
            KinoXPlayerHolder.playlist.collect { queue ->
                _uiState.update {
                    it.copy(
                        hasPreviousVideo = queue.hasPrevious,
                        hasNextVideo = queue.hasNext,
                        nextVideoTitle = queue.nextTitle
                    )
                }
            }
        }
    }

    fun initializeMedia(
        uriString: String,
        title: String,
        subtitleUri: String?,
        folderUri: String?
    ) {
        val existingHolderPlayer = KinoXPlayerHolder.exoPlayer
        if (KinoXPlayerHolder.currentUriString == uriString && existingHolderPlayer != null) {
            // Re-attaching to an active background/mini-player session without restarting!
            if (currentExoPlayer !== existingHolderPlayer) {
                attachListenersToPlayer(existingHolderPlayer)
            }
            val currentDuration = existingHolderPlayer.duration.coerceAtLeast(0L)
            val currentPos = existingHolderPlayer.currentPosition.coerceAtLeast(0L)
            val currentSpeed = existingHolderPlayer.playbackParameters.speed
            _uiState.update {
                it.copy(
                    uriString = uriString,
                    title = title.ifBlank { KinoXPlayerHolder.currentTitle },
                    folderUri = folderUri ?: it.folderUri,
                    isPlaying = existingHolderPlayer.isPlaying,
                    isBuffering = existingHolderPlayer.playbackState == Player.STATE_BUFFERING,
                    positionMs = currentPos,
                    durationMs = currentDuration,
                    playbackSpeed = currentSpeed,
                    decoderMode = KinoXPlayerHolder.currentDecoderMode,
                    videoWidth = existingHolderPlayer.videoSize.width,
                    videoHeight = existingHolderPlayer.videoSize.height,
                    playerInstanceVersion = it.playerInstanceVersion + 1
                )
            }
            playerListener.onTracksChanged(existingHolderPlayer.currentTracks)
            viewModelScope.launch {
                val history = repository.getVideoHistoryByUri(uriString)
                if (history != null) {
                    val subUri = subtitleUri ?: history.externalSubtitleUri
                    _uiState.update {
                        it.copy(
                            folderUri = it.folderUri ?: history.folderUri,
                            aspectMode = history.aspectMode,
                            subtitleDelayMs = history.subtitleDelayMs,
                            externalSubtitleUri = subUri,
                            externalSubtitleName = subUri?.let(::resolveDisplayName)
                        )
                    }
                    if (subUri != null && externalCues.isEmpty()) {
                        externalCues = SubtitleParser.loadFromUri(getApplication(), subUri)
                        refreshActiveSubtitleText(existingHolderPlayer.currentPosition.coerceAtLeast(0L))
                    }
                }
            }
            startPositionTicker()
            scheduleHudAutoHide()
            return
        }

        if (_uiState.value.uriString == uriString && currentExoPlayer != null) return

        viewModelScope.launch {
            val app = getApplication<Application>()
            val prefs = repository.playbackPrefs.first()
            val history = repository.getVideoHistoryByUri(uriString)

            val resolvedDecoder = when {
                BatteryOptimizer.isSystemPowerSaveActive(app) -> DecoderMode.HW
                history != null -> history.decoderMode
                else -> prefs.defaultDecoderMode
            }
            val resolvedAspect = history?.aspectMode ?: prefs.defaultAspectMode
            val resolvedSubUri = subtitleUri ?: history?.externalSubtitleUri
            val resolvedDelay = history?.subtitleDelayMs ?: 0L
            val resumePosition = if (prefs.autoResume && history != null && history.lastPositionMs > 2000L) {
                if (history.durationMs > 0L && history.lastPositionMs > history.durationMs - 5000L) 0L
                else history.lastPositionMs
            } else 0L

            KinoXPlayerHolder.backgroundPlayEnabled = prefs.backgroundPlayEnabled

            _uiState.update {
                it.copy(
                    uriString = uriString,
                    title = title.ifBlank { history?.title ?: "Local Video" },
                    folderUri = folderUri ?: history?.folderUri,
                    decoderMode = resolvedDecoder,
                    backgroundPlayEnabled = prefs.backgroundPlayEnabled,
                    aspectMode = resolvedAspect,
                    orientationMode = prefs.defaultOrientationMode,
                    playbackSpeed = prefs.defaultPlaybackSpeed,
                    seekStepSeconds = prefs.seekStepSeconds,
                    brightnessGestureEnabled = prefs.brightnessGestureEnabled,
                    volumeGestureEnabled = prefs.volumeGestureEnabled,
                    seekGestureEnabled = prefs.seekGestureEnabled,
                    pinchZoomEnabled = prefs.pinchZoomEnabled,
                    doubleTapSeekEnabled = prefs.doubleTapSeekEnabled,
                    externalSubtitleUri = resolvedSubUri,
                    externalSubtitleName = resolvedSubUri?.let(::resolveDisplayName),
                    subtitleDelayMs = resolvedDelay,
                    subtitleStyle = prefs.subtitleStyle,
                    errorMessage = null,
                    zoomScale = 1f,
                    panX = 0f,
                    panY = 0f
                )
            }

            if (resolvedSubUri != null) {
                loadExternalSubtitles(resolvedSubUri)
            } else {
                externalCues = emptyList()
            }

            if (KinoXPlayerHolder.playlist.value.items.none { it.uriString == uriString }) {
                val queue = when {
                    !folderUri.isNullOrBlank() -> repository.scanFolderVideos(folderUri)
                    else -> {
                        val folderVideos = repository.scanAllPinnedFoldersVideos()
                        val hist = repository.getAllHistoryVideos()
                        (hist + folderVideos).distinctBy { it.uriString }
                    }
                }
                if (queue.isNotEmpty()) {
                    KinoXPlayerHolder.setPlaylist(queue, uriString)
                }
            }

            buildAndPreparePlayer(
                uriString = uriString,
                title = title,
                decoderMode = resolvedDecoder,
                seekToMs = resumePosition,
                playWhenReady = true
            )
        }
    }

    private fun buildAndPreparePlayer(
        uriString: String,
        title: String,
        decoderMode: DecoderMode,
        seekToMs: Long,
        playWhenReady: Boolean
    ) {
        detachPlayerListeners()
        val app = getApplication<Application>()
        val player = KinoXPlayerHolder.buildOrReplacePlayer(
            context = app,
            decoderMode = decoderMode,
            seekStepSeconds = _uiState.value.seekStepSeconds
        )
        attachListenersToPlayer(player)

        player.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(uriString))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist("KinoX Local Player")
                    .build()
            )
            .build()

        KinoXPlayerHolder.updateNowPlayingMetadata(uriString, title)
        player.setMediaItem(mediaItem)
        if (seekToMs > 0L) {
            player.seekTo(seekToMs)
        }
        player.playWhenReady = playWhenReady
        player.prepare()

        // Bind MediaSessionService controller for background notification & system transport
        KinoXPlayerHolder.connectBackgroundService(app)

        _uiState.update {
            it.copy(
                decoderMode = decoderMode,
                codecTelemetry = it.codecTelemetry.copy(decoderMode = decoderMode),
                errorMessage = null,
                playerInstanceVersion = it.playerInstanceVersion + 1
            )
        }

        startPositionTicker()
        scheduleHudAutoHide()
    }

    private fun attachListenersToPlayer(player: ExoPlayer) {
        currentExoPlayer?.let { prev ->
            prev.removeListener(playerListener)
            prev.removeAnalyticsListener(analyticsListener)
        }
        player.addListener(playerListener)
        player.addAnalyticsListener(analyticsListener)
        currentExoPlayer = player
    }

    private fun detachPlayerListeners() {
        positionTickerJob?.cancel()
        currentExoPlayer?.let { player ->
            player.removeListener(playerListener)
            player.removeAnalyticsListener(analyticsListener)
        }
        currentExoPlayer = null
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) scheduleHudAutoHide() else hudHideJob?.cancel()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = currentExoPlayer ?: return
            _uiState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = player.duration.coerceAtLeast(0L)
                )
            }
            if (playbackState == Player.STATE_ENDED &&
                KinoXPlayerHolder.sleepTimer.value.option != SleepTimerOption.END_OF_VIDEO &&
                KinoXPlayerHolder.playlist.value.hasNext
            ) {
                playNextVideo()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            currentTracks = tracks
            val audioList = mutableListOf<MediaTrackInfo>()
            val subtitleList = mutableListOf<MediaTrackInfo>()

            tracks.groups.forEachIndexed { groupIdx, group ->
                val trackType = group.type
                if (trackType == C.TRACK_TYPE_AUDIO || trackType == C.TRACK_TYPE_TEXT) {
                    for (trackIdx in 0 until group.length) {
                        val format = group.getTrackFormat(trackIdx)
                        val info = MediaTrackInfo(
                            groupIndex = groupIdx,
                            trackIndex = trackIdx,
                            id = format.id ?: "${groupIdx}_$trackIdx",
                            label = buildTrackLabel(format, trackIdx, trackType),
                            language = format.language ?: "und",
                            mimeType = format.sampleMimeType ?: "",
                            bitrate = format.bitrate,
                            channelCount = format.channelCount,
                            isSelected = group.isTrackSelected(trackIdx)
                        )
                        if (trackType == C.TRACK_TYPE_AUDIO) {
                            audioList += info
                        } else {
                            subtitleList += info
                        }
                    }
                }
            }

            _uiState.update {
                it.copy(audioTracks = audioList, subtitleTracks = subtitleList)
            }
        }

        override fun onCues(cueGroup: CueGroup) {
            val text = cueGroup.cues
                .mapNotNull { it.text?.toString()?.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
                .ifBlank { null }
            embeddedCueText = text
            refreshActiveSubtitleText()
        }

        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            _uiState.update { state ->
                state.copy(
                    videoWidth = videoSize.width,
                    videoHeight = videoSize.height,
                    codecTelemetry = state.codecTelemetry.copy(
                        width = videoSize.width,
                        height = videoSize.height
                    )
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "Playback failed (${error.errorCodeName}): ${error.cause?.localizedMessage ?: error.localizedMessage ?: "Unsupported format or inaccessible local file"}"
                )
            }
        }
    }

    private val analyticsListener = object : AnalyticsListener {
        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            val isHw = inferHardwareAcceleration(decoderName)
            _uiState.update { state ->
                state.copy(
                    codecTelemetry = state.codecTelemetry.copy(
                        videoCodecName = decoderName,
                        isHardwareAccelerated = isHw
                    )
                )
            }
        }

        override fun onVideoInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?
        ) {
            _uiState.update { state ->
                state.copy(
                    codecTelemetry = state.codecTelemetry.copy(
                        mimeType = format.sampleMimeType ?: state.codecTelemetry.mimeType,
                        width = if (format.width > 0) format.width else state.codecTelemetry.width,
                        height = if (format.height > 0) format.height else state.codecTelemetry.height,
                        frameRate = if (format.frameRate > 0f) format.frameRate else state.codecTelemetry.frameRate
                    )
                )
            }
        }

        override fun onAudioDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            _uiState.update { state ->
                state.copy(
                    codecTelemetry = state.codecTelemetry.copy(audioCodecName = decoderName)
                )
            }
        }

        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?
        ) {
            _uiState.update { state ->
                state.copy(
                    codecTelemetry = state.codecTelemetry.copy(
                        audioChannels = if (format.channelCount > 0) format.channelCount else state.codecTelemetry.audioChannels,
                        audioSampleRateHz = if (format.sampleRate > 0) format.sampleRate else state.codecTelemetry.audioSampleRateHz
                    )
                )
            }
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            _uiState.update { state ->
                state.copy(
                    codecTelemetry = state.codecTelemetry.copy(
                        droppedFrames = state.codecTelemetry.droppedFrames + droppedFrames
                    )
                )
            }
        }
    }

    private fun inferHardwareAcceleration(decoderName: String): Boolean {
        val videoMimes = listOf("video/avc", "video/hevc", "video/x-vnd.on2.vp9", "video/av01")
        for (mime in videoMimes) {
            val match = runCatching {
                MediaCodecUtil.getDecoderInfos(mime, false, false)
                    .firstOrNull { it.name.equals(decoderName, ignoreCase = true) }
            }.getOrNull()
            if (match != null) return match.hardwareAccelerated
        }
        val lower = decoderName.lowercase()
        return !lower.startsWith("c2.android.") &&
            !lower.startsWith("omx.google.") &&
            !lower.contains("ffmpeg")
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = viewModelScope.launch {
            while (isActive) {
                val player = currentExoPlayer
                if (player != null) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val buf = player.bufferedPosition.coerceAtLeast(0L)
                    _uiState.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = dur,
                            bufferedPositionMs = buf
                        )
                    }
                    refreshActiveSubtitleText(pos)

                    val now = System.currentTimeMillis()
                    if (player.isPlaying && now - lastSavedAtMs >= 5000L) {
                        lastSavedAtMs = now
                        persistProgress()
                    }
                }
                delay(BatteryOptimizer.optimalTickerIntervalMs(getApplication(), player?.isPlaying == true))
            }
        }
    }

    private fun refreshActiveSubtitleText(posMs: Long = _uiState.value.positionMs) {
        val state = _uiState.value
        val nextText = if (!state.subtitlesEnabled) {
            null
        } else if (externalCues.isNotEmpty()) {
            SubtitleParser.activeTextAt(externalCues, posMs, state.subtitleDelayMs)
        } else {
            embeddedCueText
        }
        if (nextText != state.activeSubtitleText) {
            _uiState.update { it.copy(activeSubtitleText = nextText) }
        }
    }

    fun togglePlayPause() {
        val player = currentExoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            persistProgress()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
        scheduleHudAutoHide()
    }

    fun play() {
        currentExoPlayer?.play()
    }

    fun pause(saveNow: Boolean = true) {
        currentExoPlayer?.pause()
        if (saveNow) persistProgress()
    }

    fun seekTo(positionMs: Long) {
        val player = currentExoPlayer ?: return
        val dur = player.duration.coerceAtLeast(0L)
        val clamped = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
        player.seekTo(clamped)
        _uiState.update { it.copy(positionMs = clamped) }
        refreshActiveSubtitleText(clamped)
        scheduleHudAutoHide()
    }

    fun seekBy(deltaMs: Long, showHud: Boolean = true) {
        val player = currentExoPlayer ?: return
        val target = player.currentPosition + deltaMs
        seekTo(target)
        if (showHud) {
            showTransientGestureHud(
                GestureHudState.DoubleTapSeek(
                    isForward = deltaMs >= 0L,
                    seconds = (kotlin.math.abs(deltaMs) / 1000L).toInt().coerceAtLeast(1)
                )
            )
        }
    }

    fun playNextVideo() {
        val nextItem = KinoXPlayerHolder.stepPlaylist(1) ?: return
        switchToPlaylistItem(nextItem)
    }

    fun playPreviousVideo() {
        val prevItem = KinoXPlayerHolder.stepPlaylist(-1) ?: return
        switchToPlaylistItem(prevItem)
    }

    private fun switchToPlaylistItem(target: com.example.data.model.VideoItem) {
        persistProgress()
        viewModelScope.launch {
            val app = getApplication<Application>()
            val prefs = repository.playbackPrefs.first()
            val history = repository.getVideoHistoryByUri(target.uriString)
            val resolvedDecoder = when {
                BatteryOptimizer.isSystemPowerSaveActive(app) -> DecoderMode.HW
                history != null -> history.decoderMode
                else -> target.decoderMode
            }
            val resolvedAspect = history?.aspectMode ?: target.aspectMode
            val resolvedSubUri = target.externalSubtitleUri ?: history?.externalSubtitleUri
            val resolvedDelay = history?.subtitleDelayMs ?: target.subtitleDelayMs
            val resumePosition = if (prefs.autoResume && history != null && history.lastPositionMs > 2000L) {
                if (history.durationMs > 0L && history.lastPositionMs > history.durationMs - 5000L) 0L
                else history.lastPositionMs
            } else 0L

            embeddedCueText = null
            _uiState.update {
                it.copy(
                    uriString = target.uriString,
                    title = target.title,
                    folderUri = target.folderUri ?: it.folderUri,
                    decoderMode = resolvedDecoder,
                    aspectMode = resolvedAspect,
                    externalSubtitleUri = resolvedSubUri,
                    externalSubtitleName = resolvedSubUri?.let(::resolveDisplayName),
                    subtitleDelayMs = resolvedDelay,
                    activeSubtitleText = null,
                    errorMessage = null,
                    zoomScale = 1.0f,
                    panX = 0f,
                    panY = 0f
                )
            }

            if (resolvedSubUri != null) {
                loadExternalSubtitles(resolvedSubUri)
            } else {
                externalCues = emptyList()
            }

            buildAndPreparePlayer(
                uriString = target.uriString,
                title = target.title,
                decoderMode = resolvedDecoder,
                seekToMs = resumePosition,
                playWhenReady = true
            )
            showTransientGestureHud(GestureHudState.Toast(target.title))
        }
    }

    fun switchDecoderMode(newMode: DecoderMode) {
        val state = _uiState.value
        if (state.decoderMode == newMode && state.errorMessage == null) return
        val player = currentExoPlayer
        val resumePos = player?.currentPosition?.coerceAtLeast(0L) ?: state.positionMs
        val wasPlaying = player?.playWhenReady ?: true

        buildAndPreparePlayer(
            uriString = state.uriString,
            title = state.title,
            decoderMode = newMode,
            seekToMs = resumePos,
            playWhenReady = wasPlaying
        )
        showTransientGestureHud(GestureHudState.Toast("Decoder: ${newMode.shortLabel} (${newMode.fullName})"))
        persistProgress(decoderOverride = newMode)
    }

    fun toggleBackgroundPlay() {
        val next = !_uiState.value.backgroundPlayEnabled
        setBackgroundPlayEnabled(next)
        showTransientGestureHud(
            GestureHudState.Toast(if (next) "Background Play: ON" else "Background Play: OFF")
        )
    }

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        KinoXPlayerHolder.backgroundPlayEnabled = enabled
        _uiState.update { it.copy(backgroundPlayEnabled = enabled) }
        viewModelScope.launch { repository.updateBackgroundPlayEnabled(enabled) }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        KinoXPlayerHolder.setSleepTimer(option)
        val toastMsg = when (option) {
            SleepTimerOption.OFF -> "Sleep Mode: Off"
            SleepTimerOption.END_OF_VIDEO -> "Sleep Mode: Auto-close at end"
            else -> "Sleep Mode: Auto-close in ${option.label}"
        }
        showTransientGestureHud(GestureHudState.Toast(toastMsg))
    }

    fun setCustomSleepMinutes(minutes: Int) {
        KinoXPlayerHolder.setCustomSleepMinutes(minutes)
        val toastMsg = if (minutes <= 0) {
            "Sleep Mode: Off"
        } else {
            "Sleep Mode: Auto-close in $minutes min"
        }
        showTransientGestureHud(GestureHudState.Toast(toastMsg))
    }

    fun retryPlayback() {
        val state = _uiState.value
        buildAndPreparePlayer(
            uriString = state.uriString,
            title = state.title,
            decoderMode = state.decoderMode,
            seekToMs = state.positionMs,
            playWhenReady = true
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        currentExoPlayer?.playbackParameters = PlaybackParameters(clamped)
        _uiState.update { it.copy(playbackSpeed = clamped) }
        scheduleHudAutoHide()
    }

    fun cycleAspectRatio() {
        val next = _uiState.value.aspectMode.next()
        _uiState.update {
            it.copy(
                aspectMode = next,
                zoomScale = 1.0f,
                panX = 0f,
                panY = 0f
            )
        }
        showTransientGestureHud(GestureHudState.Toast("Aspect: ${next.label}"))
        scheduleHudAutoHide()
        persistProgress()
    }

    fun cycleOrientation() {
        val next = _uiState.value.orientationMode.next()
        _uiState.update { it.copy(orientationMode = next) }
        showTransientGestureHud(GestureHudState.Toast("Rotation: ${next.label}"))
        scheduleHudAutoHide()
    }

    fun toggleScreenLock() {
        val nextLocked = !_uiState.value.isLocked
        _uiState.update {
            it.copy(
                isLocked = nextLocked,
                controlsVisible = !nextLocked,
                unlockHintVisible = nextLocked,
                activeSheet = PlayerSheetType.NONE
            )
        }
        if (nextLocked) {
            scheduleUnlockHintHide()
        } else {
            scheduleHudAutoHide()
        }
    }

    fun onLockedScreenTap() {
        _uiState.update { it.copy(unlockHintVisible = !it.unlockHintVisible) }
        if (_uiState.value.unlockHintVisible) {
            scheduleUnlockHintHide()
        }
    }

    fun toggleControlsVisibility() {
        val next = !_uiState.value.controlsVisible
        _uiState.update { it.copy(controlsVisible = next) }
        if (next) scheduleHudAutoHide() else hudHideJob?.cancel()
    }

    fun openSheet(sheet: PlayerSheetType) {
        hudHideJob?.cancel()
        _uiState.update { it.copy(activeSheet = sheet, controlsVisible = true) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = PlayerSheetType.NONE) }
        scheduleHudAutoHide()
    }

    fun setInPip(inPip: Boolean) {
        _uiState.update {
            it.copy(
                isInPip = inPip,
                controlsVisible = if (inPip) false else it.controlsVisible,
                activeSheet = if (inPip) PlayerSheetType.NONE else it.activeSheet
            )
        }
    }

    fun updateZoomAndPan(scaleDelta: Float, panDeltaX: Float, panYDelta: Float) {
        _uiState.update { state ->
            val newScale = (state.zoomScale * scaleDelta).coerceIn(1.0f, 4.0f)
            val newPanX = if (newScale <= 1.02f) 0f else (state.panX + panDeltaX).coerceIn(-2200f, 2200f)
            val newPanY = if (newScale <= 1.02f) 0f else (state.panY + panYDelta).coerceIn(-2200f, 2200f)
            state.copy(
                zoomScale = newScale,
                panX = newPanX,
                panY = newPanY,
                gestureHud = GestureHudState.Zoom(newScale)
            )
        }
        gestureClearJob?.cancel()
        gestureClearJob = viewModelScope.launch {
            delay(800L)
            _uiState.update { it.copy(gestureHud = null) }
        }
    }

    fun resetZoom() {
        _uiState.update {
            it.copy(
                zoomScale = 1.0f,
                panX = 0f,
                panY = 0f,
                gestureHud = GestureHudState.Zoom(1.0f)
            )
        }
        clearGestureHudSoon(700L)
    }

    fun showBrightnessHud(percent: Int) {
        showTransientGestureHud(GestureHudState.Brightness(percent.coerceIn(0, 100)), durationMs = 700L)
    }

    fun getCurrentVolumeFraction(): Float {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol.toFloat()
    }

    fun setVolumeFraction(fraction: Float) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val clamped = fraction.coerceIn(0f, 1f)
        val targetStep = kotlin.math.round(clamped * maxVol).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0)
        val percent = kotlin.math.round(clamped * 100f).toInt().coerceIn(0, 100)
        showTransientGestureHud(GestureHudState.Volume(percent), durationMs = 700L)
    }

    fun showSeekHud(targetMs: Long, deltaMs: Long) {
        gestureClearJob?.cancel()
        _uiState.update { it.copy(gestureHud = GestureHudState.Seek(targetMs, deltaMs)) }
    }

    fun clearGestureHudSoon(delayMs: Long = 500L) {
        gestureClearJob?.cancel()
        gestureClearJob = viewModelScope.launch {
            delay(delayMs)
            _uiState.update { it.copy(gestureHud = null) }
        }
    }

    private fun showTransientGestureHud(hud: GestureHudState, durationMs: Long = 950L) {
        gestureClearJob?.cancel()
        _uiState.update { it.copy(gestureHud = hud) }
        gestureClearJob = viewModelScope.launch {
            delay(durationMs)
            _uiState.update { it.copy(gestureHud = null) }
        }
    }

    fun selectAudioTrack(track: MediaTrackInfo) {
        val player = currentExoPlayer ?: return
        val group = currentTracks.groups.getOrNull(track.groupIndex) ?: return
        val params = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            .build()
        player.trackSelectionParameters = params
    }

    fun selectEmbeddedSubtitleTrack(track: MediaTrackInfo) {
        val player = currentExoPlayer ?: return
        val group = currentTracks.groups.getOrNull(track.groupIndex) ?: return
        externalCues = emptyList()
        val params = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            .build()
        player.trackSelectionParameters = params
        _uiState.update {
            it.copy(
                subtitlesEnabled = true,
                externalSubtitleUri = null,
                externalSubtitleName = null
            )
        }
        refreshActiveSubtitleText()
    }

    fun disableSubtitles() {
        val player = currentExoPlayer
        if (player != null) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        }
        _uiState.update { it.copy(subtitlesEnabled = false, activeSubtitleText = null) }
    }

    fun loadExternalSubtitles(uriString: String) {
        viewModelScope.launch {
            val cues = SubtitleParser.loadFromUri(getApplication(), uriString)
            if (cues.isEmpty()) {
                showTransientGestureHud(GestureHudState.Toast("No cues found in subtitle file"))
                return@launch
            }
            externalCues = cues
            currentExoPlayer?.let { player ->
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            }
            val displayName = resolveDisplayName(uriString)
            _uiState.update {
                it.copy(
                    subtitlesEnabled = true,
                    externalSubtitleUri = uriString,
                    externalSubtitleName = displayName
                )
            }
            refreshActiveSubtitleText()
            showTransientGestureHud(GestureHudState.Toast("Loaded ${cues.size} cues"))
            persistProgress()
        }
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        val next = (_uiState.value.subtitleDelayMs + deltaMs).coerceIn(-15_000L, 15_000L)
        _uiState.update { it.copy(subtitleDelayMs = next) }
        refreshActiveSubtitleText()
        persistProgress()
    }

    fun resetSubtitleDelay() {
        _uiState.update { it.copy(subtitleDelayMs = 0L) }
        refreshActiveSubtitleText()
        persistProgress()
    }

    fun updateSubtitleStyle(style: SubtitleStylePrefs) {
        _uiState.update { it.copy(subtitleStyle = style) }
        viewModelScope.launch {
            repository.updateSubtitleStyle(style)
        }
    }

    fun persistProgress(decoderOverride: DecoderMode? = null) {
        val state = _uiState.value
        if (state.uriString.isBlank()) return
        val pos = currentExoPlayer?.currentPosition?.coerceAtLeast(0L) ?: state.positionMs
        val dur = currentExoPlayer?.duration?.coerceAtLeast(0L) ?: state.durationMs
        viewModelScope.launch {
            repository.saveVideoProgress(
                uriString = state.uriString,
                title = state.title,
                folderUri = state.folderUri,
                durationMs = dur,
                lastPositionMs = pos,
                decoderMode = decoderOverride ?: state.decoderMode,
                externalSubtitleUri = state.externalSubtitleUri,
                subtitleDelayMs = state.subtitleDelayMs,
                aspectMode = state.aspectMode,
                width = state.videoWidth,
                height = state.videoHeight
            )
        }
    }

    private fun scheduleHudAutoHide() {
        hudHideJob?.cancel()
        if (_uiState.value.activeSheet != PlayerSheetType.NONE) return
        hudHideJob = viewModelScope.launch {
            delay(3600L)
            if (_uiState.value.isPlaying && _uiState.value.activeSheet == PlayerSheetType.NONE) {
                _uiState.update { it.copy(controlsVisible = false) }
            }
        }
    }

    private fun scheduleUnlockHintHide() {
        unlockHintJob?.cancel()
        unlockHintJob = viewModelScope.launch {
            delay(2500L)
            _uiState.update { it.copy(unlockHintVisible = false) }
        }
    }

    private fun buildTrackLabel(format: Format, index: Int, trackType: Int): String {
        val lang = format.language?.uppercase()?.takeIf { it != "UND" }
        val base = format.label?.takeIf { it.isNotBlank() }
            ?: if (trackType == C.TRACK_TYPE_AUDIO) "Audio ${index + 1}" else "Subtitle ${index + 1}"
        return if (lang != null && !base.contains(lang, ignoreCase = true)) "$base ($lang)" else base
    }

    private fun resolveDisplayName(uriString: String): String {
        val resolver = getApplication<Application>().contentResolver
        return runCatching {
            resolver.query(Uri.parse(uriString), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull() ?: Uri.parse(uriString).lastPathSegment ?: "External Subtitles"
    }

    override fun onCleared() {
        persistProgress()
        KinoXPlayerHolder.registerProgressSaver(null)
        val keepPlayingInBackground =
            _uiState.value.backgroundPlayEnabled && currentExoPlayer?.isPlaying == true
        detachPlayerListeners()
        if (!keepPlayingInBackground) {
            KinoXPlayerHolder.stopAndClearSession()
        }
        super.onCleared()
    }
}
