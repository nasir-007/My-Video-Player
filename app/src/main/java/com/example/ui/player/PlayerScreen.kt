package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.OrientationMode
import com.example.player.PipController
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    uriString: String,
    title: String,
    subtitleUri: String?,
    folderUri: String?,
    isInPipMode: Boolean,
    onPipStateSync: (isPlaying: Boolean, width: Int, height: Int, sourceRect: Rect?) -> Unit,
    onRequestEnterPip: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var brightnessFraction by remember {
        val current = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (current in 0f..1f) current else 0.55f)
    }
    var videoBoundsRect by remember { mutableStateOf<Rect?>(null) }
    var surfaceAttachNonce by remember { mutableStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.loadExternalSubtitles(pickedUri.toString())
        }
    }

    LaunchedEffect(uriString) {
        viewModel.initializeMedia(
            uriString = uriString,
            title = title,
            subtitleUri = subtitleUri,
            folderUri = folderUri
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            state.backgroundPlayEnabled &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(isInPipMode) {
        viewModel.setInPip(isInPipMode)
    }

    LaunchedEffect(state.isPlaying, state.videoWidth, state.videoHeight, videoBoundsRect) {
        onPipStateSync(state.isPlaying, state.videoWidth, state.videoHeight, videoBoundsRect)
    }

    // Reattach video surface cleanly when returning from background playback or screen unlock
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                surfaceAttachNonce++
                viewModel.refreshBatteryStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keep screen on during active video playback
    DisposableEffect(state.isPlaying) {
        if (state.isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Immersive full-screen system bars + orientation mode
    DisposableEffect(state.orientationMode) {
        val requested = when (state.orientationMode) {
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        activity?.requestedOrientation = requested

        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.persistProgress()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val attrs = window.attributes
                attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = attrs
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        if (state.activeSheet != PlayerSheetType.NONE) {
            viewModel.closeSheet()
        } else if (state.isLocked) {
            viewModel.onLockedScreenTap()
        } else {
            viewModel.persistProgress()
            onNavigateBack()
        }
    }

    val pipSupported = remember(context) { PipController.isPipSupported(context) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                videoBoundsRect = Rect(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                    bounds.right.roundToInt(),
                    bounds.bottom.roundToInt()
                )
            }
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val containerRatio = if (containerHeight > 0.dp) containerWidth / containerHeight else (16f / 9f)
        val rawVideoRatio = if (state.videoWidth > 0 && state.videoHeight > 0) {
            state.videoWidth.toFloat() / state.videoHeight.toFloat()
        } else {
            16f / 9f
        }

        val (surfaceWidth, surfaceHeight) = remember(
            containerWidth,
            containerHeight,
            rawVideoRatio,
            state.aspectMode,
            state.zoomScale
        ) {
            val baseSize = when (state.aspectMode) {
                AspectRatioMode.STRETCH -> containerWidth to containerHeight
                AspectRatioMode.FIT -> {
                    if (rawVideoRatio > containerRatio) {
                        containerWidth to (containerWidth / rawVideoRatio)
                    } else {
                        (containerHeight * rawVideoRatio) to containerHeight
                    }
                }
                AspectRatioMode.FILL -> {
                    if (rawVideoRatio > containerRatio) {
                        (containerHeight * rawVideoRatio) to containerHeight
                    } else {
                        containerWidth to (containerWidth / rawVideoRatio)
                    }
                }
                AspectRatioMode.RATIO_16_9 -> {
                    val targetRatio = 16f / 9f
                    if (targetRatio > containerRatio) {
                        containerWidth to (containerWidth / targetRatio)
                    } else {
                        (containerHeight * targetRatio) to containerHeight
                    }
                }
                AspectRatioMode.RATIO_4_3 -> {
                    val targetRatio = 4f / 3f
                    if (targetRatio > containerRatio) {
                        containerWidth to (containerWidth / targetRatio)
                    } else {
                        (containerHeight * targetRatio) to containerHeight
                    }
                }
            }
            (baseSize.first * state.zoomScale) to (baseSize.second * state.zoomScale)
        }

        val useSurfaceView = state.decoderMode == DecoderMode.HW && state.zoomScale <= 1.02f

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(surfaceWidth, surfaceHeight)
                .offset { IntOffset(state.panX.roundToInt(), state.panY.roundToInt()) }
        ) {
            key(state.playerInstanceVersion, useSurfaceView, surfaceAttachNonce) {
                val player = viewModel.currentExoPlayer
                if (useSurfaceView) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).also { sv ->
                                player?.setVideoSurfaceView(sv)
                            }
                        },
                        update = { sv ->
                            player?.setVideoSurfaceView(sv)
                        },
                        onRelease = { sv ->
                            player?.clearVideoSurfaceView(sv)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).also { tv ->
                                player?.setVideoTextureView(tv)
                            }
                        },
                        update = { tv ->
                            player?.setVideoTextureView(tv)
                        },
                        onRelease = { tv ->
                            player?.clearVideoTextureView(tv)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (!state.isInPip) {
            SubtitleOverlay(
                text = state.activeSubtitleText,
                style = state.subtitleStyle,
                modifier = Modifier.fillMaxSize()
            )

            PlayerGestureOverlay(
                isLocked = state.isLocked,
                durationMs = state.durationMs,
                currentPositionMs = state.positionMs,
                currentBrightnessFraction = { brightnessFraction },
                currentVolumeFraction = { viewModel.getCurrentVolumeFraction() },
                onSetBrightness = { next ->
                    brightnessFraction = next
                    activity?.window?.let { window ->
                        val attrs = window.attributes
                        attrs.screenBrightness = next.coerceIn(0.02f, 1f)
                        window.attributes = attrs
                    }
                    viewModel.showBrightnessHud((next * 100f).roundToInt())
                },
                onSetVolume = { next ->
                    viewModel.setVolumeFraction(next)
                },
                onSeekPreview = viewModel::showSeekHud,
                onCommitSeek = viewModel::seekTo,
                onClearGestureHud = { viewModel.clearGestureHudSoon() },
                onSingleTap = viewModel::toggleControlsVisibility,
                onLockedTap = viewModel::onLockedScreenTap,
                onDoubleTapLeft = { viewModel.seekBy(-10_000L) },
                onDoubleTapCenter = viewModel::togglePlayPause,
                onDoubleTapRight = { viewModel.seekBy(10_000L) },
                onZoomPan = viewModel::updateZoomAndPan,
                modifier = Modifier.fillMaxSize()
            )

            PlayerHudOverlays(
                state = state,
                pipSupported = pipSupported,
                onBack = {
                    viewModel.persistProgress()
                    onNavigateBack()
                },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekTo = viewModel::seekTo,
                onSeekBy = { delta -> viewModel.seekBy(delta, showHud = true) },
                onCycleDecoder = { viewModel.switchDecoderMode(state.decoderMode.next()) },
                onToggleBackgroundPlay = viewModel::toggleBackgroundPlay,
                onOpenSubtitles = { viewModel.openSheet(PlayerSheetType.SUBTITLES) },
                onOpenAudioSpeed = { viewModel.openSheet(PlayerSheetType.AUDIO_SPEED) },
                onOpenDecoderInfo = { viewModel.openSheet(PlayerSheetType.DECODER_INFO) },
                onOpenPowerBattery = { viewModel.openSheet(PlayerSheetType.POWER_BATTERY) },
                onEnterPip = onRequestEnterPip,
                onToggleLock = viewModel::toggleScreenLock,
                onCycleAspectRatio = viewModel::cycleAspectRatio,
                onCycleOrientation = viewModel::cycleOrientation,
                onRetry = viewModel::retryPlayback,
                onSwitchToSw = { viewModel.switchDecoderMode(DecoderMode.SW) },
                modifier = Modifier.fillMaxSize()
            )

            PlayerBottomSheets(
                state = state,
                onDismiss = viewModel::closeSheet,
                onSelectSubtitleTrack = viewModel::selectEmbeddedSubtitleTrack,
                onDisableSubtitles = viewModel::disableSubtitles,
                onPickExternalSubtitle = {
                    subtitlePickerLauncher.launch(
                        arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*")
                    )
                },
                onAdjustDelay = viewModel::adjustSubtitleDelay,
                onResetDelay = viewModel::resetSubtitleDelay,
                onUpdateSubtitleStyle = viewModel::updateSubtitleStyle,
                onSwitchDecoder = viewModel::switchDecoderMode,
                onSelectAudioTrack = viewModel::selectAudioTrack,
                onSetSpeed = viewModel::setPlaybackSpeed,
                onToggleBackgroundPlay = viewModel::setBackgroundPlayEnabled,
                onSelectPowerProfile = viewModel::selectPowerProfile,
                onSelectSleepTimer = viewModel::setSleepTimer,
                onOpenBatterySettings = viewModel::openSystemBatterySettings
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
