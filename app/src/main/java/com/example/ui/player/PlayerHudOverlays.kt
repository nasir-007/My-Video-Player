package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DecoderMode
import com.example.data.model.SubtitleStylePrefs
import com.example.ui.common.formatDuration
import com.example.ui.common.formatSignedDelta
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassOverlay
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SoftCyanContainer
import com.example.ui.theme.TextMuted
import java.util.Locale

@Composable
fun SubtitleOverlay(
    text: String?,
    style: SubtitleStylePrefs,
    modifier: Modifier = Modifier
) {
    if (text.isNullOrBlank()) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val bottomPadding = (style.bottomMarginPercent * 3.2f).dp
        Box(
            modifier = Modifier
                .padding(bottom = bottomPadding)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = style.backgroundOpacity.coerceIn(0f, 0.9f)))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                color = Color(style.textColorArgb.toInt()),
                fontSize = style.fontSizeSp.sp,
                lineHeight = (style.fontSizeSp * 1.28f).sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(
                    shadow = if (style.hasOutline) {
                        Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    } else null
                ),
                modifier = Modifier.testTag("subtitle_overlay_text")
            )
        }
    }
}

@Composable
fun PlayerHudOverlays(
    state: PlayerUiState,
    pipSupported: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    onResetZoom: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onCycleDecoder: () -> Unit,
    onToggleBackgroundPlay: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudioSpeed: () -> Unit,
    onOpenDecoderInfo: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleLock: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onCycleOrientation: () -> Unit,
    onRetry: () -> Unit,
    onSwitchToSw: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Buffering Spinner
        if (state.isBuffering && state.errorMessage == null) {
            CircularProgressIndicator(
                color = ElectricCyan,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
            )
        }

        // Error Banner
        if (state.errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ElevatedSlate.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Playback Interrupted",
                        style = MaterialTheme.typography.titleMedium,
                        color = CoralError
                    )
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianBackground
                            )
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                        if (state.decoderMode != DecoderMode.SW) {
                            OutlinedButton(onClick = onSwitchToSw) {
                                Text("Try SW Decoder", color = AmberWarm)
                            }
                        }
                    }
                }
            }
        }

        // Gesture HUD Feedback
        GestureHudDisplay(
            hud = state.gestureHud,
            modifier = Modifier.fillMaxSize()
        )

        // Screen Locked Unlock Pill
        AnimatedVisibility(
            visible = state.isLocked && state.unlockHintVisible && !state.isInPip,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
        ) {
            Surface(
                onClick = onToggleLock,
                shape = RoundedCornerShape(50),
                color = GlassOverlay,
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f)),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("unlock_controls_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = stringResource(R.string.cd_unlock_screen),
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.player_unlock_hint),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Top & Bottom Controls HUD
        AnimatedVisibility(
            visible = state.controlsVisible && !state.isLocked && !state.isInPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar Gradient + Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xDD070A0F), Color.Transparent)
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White
                        )
                    }

                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Decoder Mode Pill (HW / HW+ / SW)
                    Surface(
                        onClick = onOpenDecoderInfo,
                        shape = RoundedCornerShape(50),
                        color = SoftCyanContainer.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_decoder_pill")
                    ) {
                        Text(
                            text = state.decoderMode.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleBackgroundPlay,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_bg_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = stringResource(R.string.cd_bg_play),
                            tint = if (state.backgroundPlayEnabled) MintSuccess else Color.White.copy(alpha = 0.65f)
                        )
                    }

                    IconButton(
                        onClick = onOpenSleepTimer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_sleep_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = stringResource(R.string.cd_sleep_timer),
                            tint = if (state.sleepTimer.isActive) MintSuccess else Color.White
                        )
                    }

                    IconButton(
                        onClick = onOpenAudioSpeed,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Audiotrack,
                            contentDescription = stringResource(R.string.cd_audio_tracks),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onOpenSubtitles,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_subtitle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Subtitles,
                            contentDescription = stringResource(R.string.cd_subtitles),
                            tint = if (state.subtitlesEnabled) ElectricCyan else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    if (pipSupported) {
                        IconButton(
                            onClick = onEnterPip,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureInPictureAlt,
                                contentDescription = stringResource(R.string.cd_pip),
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenDecoderInfo,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.cd_codec_info),
                            tint = Color.White
                        )
                    }
                }

                // Center Transport Cluster
                val skipDeltaMs = state.seekStepSeconds.coerceIn(5, 60) * 1000L
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onPreviousVideo,
                        enabled = state.hasPreviousVideo,
                        shape = CircleShape,
                        color = if (state.hasPreviousVideo) GlassOverlay else GlassOverlay.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (state.hasPreviousVideo) GlassBorder else GlassBorder.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_previous_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = stringResource(R.string.cd_previous_video),
                                tint = if (state.hasPreviousVideo) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { onSeekBy(-skipDeltaMs) },
                        shape = CircleShape,
                        color = GlassOverlay,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("player_rewind_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = stringResource(R.string.cd_rewind),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        color = ElectricCyan,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("player_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(
                                    if (state.isPlaying) R.string.cd_pause else R.string.cd_play
                                ),
                                tint = ObsidianBackground,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { onSeekBy(skipDeltaMs) },
                        shape = CircleShape,
                        color = GlassOverlay,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("player_forward_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = stringResource(R.string.cd_forward),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onNextVideo,
                        enabled = state.hasNextVideo,
                        shape = CircleShape,
                        color = if (state.hasNextVideo) GlassOverlay else GlassOverlay.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (state.hasNextVideo) GlassBorder else GlassBorder.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_next_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = stringResource(R.string.cd_next_video),
                                tint = if (state.hasNextVideo) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Bottom Bar Gradient + Timeline + Action Row
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xE6070A0F))
                            )
                        )
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.hasNextVideo && !state.nextVideoTitle.isNullOrBlank()) {
                        Surface(
                            onClick = onNextVideo,
                            shape = RoundedCornerShape(50),
                            color = GlassOverlay,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .align(Alignment.End)
                                .minimumInteractiveComponentSize()
                                .testTag("player_up_next_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "NEXT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.nextVideoTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(160.dp)
                                )
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = stringResource(R.string.cd_next_video),
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    var isDraggingSlider by remember { mutableStateOf(false) }
                    var sliderValueMs by remember { mutableFloatStateOf(0f) }
                    var showRemainingTime by remember { mutableStateOf(false) }
                    val durationFloat = state.durationMs.coerceAtLeast(1L).toFloat()
                    val displayPosMs = if (isDraggingSlider) sliderValueMs.toLong() else state.positionMs
                    val rightTimeLabel = if (showRemainingTime && state.durationMs > 0L) {
                        "-${formatDuration((state.durationMs - displayPosMs).coerceAtLeast(0L))}"
                    } else {
                        formatDuration(state.durationMs)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatDuration(displayPosMs),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color.White
                        )
                        Slider(
                            value = displayPosMs.toFloat().coerceIn(0f, durationFloat),
                            onValueChange = {
                                isDraggingSlider = true
                                sliderValueMs = it
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                onSeekTo(sliderValueMs.toLong())
                            },
                            valueRange = 0f..durationFloat,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_seek_slider")
                        )
                        Text(
                            text = rightTimeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = if (showRemainingTime) ElectricCyan else TextMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showRemainingTime = !showRemainingTime }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("player_duration_toggle")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onToggleLock,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_lock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = stringResource(R.string.cd_lock_screen),
                                    tint = Color.White
                                )
                            }

                            Surface(
                                onClick = onOpenAudioSpeed,
                                shape = RoundedCornerShape(50),
                                color = GlassOverlay,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_speed_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Speed,
                                        contentDescription = stringResource(R.string.cd_speed),
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2gx", state.playbackSpeed),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }

                            if (state.sleepTimer.isActive) {
                                Surface(
                                    onClick = onOpenSleepTimer,
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF0E2F26),
                                    border = BorderStroke(1.dp, MintSuccess.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = state.sleepTimer.badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MintSuccess,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (state.zoomScale > 1.05f) {
                                Surface(
                                    onClick = onResetZoom,
                                    shape = RoundedCornerShape(50),
                                    color = SoftCyanContainer,
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("player_reset_zoom_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ZoomOutMap,
                                            contentDescription = stringResource(R.string.cd_reset_zoom),
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1fx • Reset", state.zoomScale),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = onCycleDecoder,
                                shape = RoundedCornerShape(50),
                                color = GlassOverlay,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_quick_decoder_toggle")
                            ) {
                                Text(
                                    text = "DEC ${state.decoderMode.shortLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = onCycleOrientation,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_rotate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ScreenRotation,
                                    contentDescription = stringResource(R.string.cd_rotate_screen),
                                    tint = Color.White
                                )
                            }

                            Surface(
                                onClick = onCycleAspectRatio,
                                shape = RoundedCornerShape(50),
                                color = GlassOverlay,
                                border = BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("player_aspect_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AspectRatio,
                                        contentDescription = stringResource(R.string.cd_aspect_ratio),
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = state.aspectMode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureHudDisplay(
    hud: GestureHudState?,
    modifier: Modifier = Modifier
) {
    if (hud == null) return

    Box(modifier = modifier) {
        when (hud) {
            is GestureHudState.Brightness -> {
                VerticalMeterPill(
                    icon = { Icon(Icons.Filled.WbSunny, contentDescription = null, tint = AmberWarm) },
                    percent = hud.percent,
                    barColor = AmberWarm,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp)
                )
            }
            is GestureHudState.Volume -> {
                VerticalMeterPill(
                    icon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = ElectricCyan) },
                    percent = hud.percent,
                    barColor = ElectricCyan,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 28.dp)
                )
            }
            is GestureHudState.Seek -> {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GlassOverlay,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = formatDuration(hud.targetMs),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color.White
                        )
                        Text(
                            text = formatSignedDelta(hud.deltaMs),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = ElectricCyan
                        )
                    }
                }
            }
            is GestureHudState.DoubleTapSeek -> {
                val alignment = if (hud.isForward) Alignment.CenterEnd else Alignment.CenterStart
                Surface(
                    shape = CircleShape,
                    color = GlassOverlay,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(alignment)
                        .padding(horizontal = 42.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (hud.isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "${if (hud.isForward) "+" else "-"}${hud.seconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
            is GestureHudState.Zoom -> {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GlassOverlay,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = ElectricCyan)
                        Text(
                            text = String.format(Locale.US, "%.0f%%", hud.scale * 100f),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color.White
                        )
                    }
                }
            }
            is GestureHudState.Toast -> {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GlassOverlay,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 64.dp)
                ) {
                    Text(
                        text = hud.message,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalMeterPill(
    icon: @Composable () -> Unit,
    percent: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = GlassOverlay,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon()
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = (percent / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = Color.White
            )
        }
    }
}
