package com.example.ui.library

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.R
import com.example.data.model.DecoderMode
import com.example.data.model.FolderItem
import com.example.data.model.PlaybackPrefs
import com.example.data.model.SleepTimerOption
import com.example.data.model.VideoItem
import com.example.player.NowPlayingSnapshot
import com.example.player.SleepTimerState
import com.example.ui.common.formatDuration
import com.example.ui.common.formatFileSize
import com.example.ui.common.formatResolutionTag
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SoftCyanContainer
import com.example.ui.theme.TextMuted

@Composable
fun CinemaHeroHeader(
    defaultDecoder: DecoderMode,
    sleepTimer: SleepTimerState,
    onCycleDecoder: () -> Unit,
    onOpenSleepMode: () -> Unit,
    onLinkFolder: () -> Unit,
    onPickVideo: () -> Unit,
    folderCount: Int = 0,
    videoCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ElevatedSlate,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftCyanContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    val heroSubtitle = if (folderCount > 0 || videoCount > 0) {
                        "$folderCount folder${if (folderCount == 1) "" else "s"} • $videoCount video${if (videoCount == 1) "" else "s"}"
                    } else {
                        stringResource(R.string.library_subtitle)
                    }
                    Text(
                        text = heroSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onOpenSleepMode,
                        shape = RoundedCornerShape(50),
                        color = if (sleepTimer.isActive) Color(0xFF0E2F26) else CardSlate,
                        border = BorderStroke(
                            1.dp,
                            if (sleepTimer.isActive) MintSuccess.copy(alpha = 0.7f) else GlassBorder
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hero_sleep_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = stringResource(R.string.cd_sleep_timer),
                                tint = if (sleepTimer.isActive) MintSuccess else ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (sleepTimer.isActive) sleepTimer.badgeLabel else "Sleep",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = if (sleepTimer.isActive) JetBrainsMonoFontFamily else null,
                                color = if (sleepTimer.isActive) MintSuccess else Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = onCycleDecoder,
                        shape = RoundedCornerShape(50),
                        color = SoftCyanContainer,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("default_decoder_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "DEC",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = defaultDecoder.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SourceQuickChip(
                    icon = Icons.Outlined.CreateNewFolder,
                    label = stringResource(R.string.action_link_folder),
                    onClick = onLinkFolder,
                    isHighlighted = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("link_folder_button")
                )
                SourceQuickChip(
                    icon = Icons.Outlined.VideoFile,
                    label = stringResource(R.string.action_pick_video),
                    onClick = onPickVideo,
                    isHighlighted = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_video_button")
                )
            }
        }
    }
}

@Composable
private fun SourceQuickChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlighted) SoftCyanContainer else CardSlate,
        border = BorderStroke(
            width = 1.dp,
            color = if (isHighlighted) ElectricCyan.copy(alpha = 0.45f) else GlassBorder
        ),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isHighlighted) ElectricCyan else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BackgroundMiniPlayerBar(
    nowPlaying: NowPlayingSnapshot,
    sleepTimer: SleepTimerState,
    onResumeVideo: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!nowPlaying.isActive) return

    Surface(
        onClick = onResumeVideo,
        shape = RoundedCornerShape(18.dp),
        color = ElevatedSlate,
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("background_mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SoftCyanContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nowPlaying.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val statusText = when {
                    sleepTimer.isActive -> "Sleep auto-close: ${sleepTimer.badgeLabel}"
                    nowPlaying.isPlaying -> "Playing in background • Tap to return"
                    else -> "Paused in background"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sleepTimer.isActive) MintSuccess else ElectricCyan
                )
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (nowPlaying.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (nowPlaying.isPlaying) R.string.cd_pause else R.string.cd_play),
                    tint = ElectricCyan
                )
            }
            IconButton(
                onClick = onStop,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingSection(
    items: List<VideoItem>,
    onPlayVideo: (VideoItem) -> Unit,
    onRemoveHistory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.section_continue_watching),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(items, key = { it.uriString }) { video ->
                ContinueWatchingCard(
                    video = video,
                    onClick = { onPlayVideo(video) },
                    onRemove = { onRemoveHistory(video.uriString) }
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    video: VideoItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val progress = if (video.durationMs > 0L) {
        (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSlate),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier
            .width(220.dp)
            .testTag("continue_card_${video.title}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(ObsidianBackground)
            ) {
                VideoThumbnail(
                    uriString = video.uriString,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xAA070A0F))
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xB3070A0F),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = video.decoderMode.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_delete_item),
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = stringResource(R.string.cd_play),
                    tint = ElectricCyan,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                )
                Text(
                    text = if (video.durationMs > 0L) {
                        "${formatDuration(video.lastPositionMs)} / ${formatDuration(video.durationMs)}"
                    } else {
                        formatDuration(video.lastPositionMs)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
                LinearProgressIndicator(
                    progress = { progress },
                    color = ElectricCyan,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun VideoThumbnail(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(uriString) {
        ImageRequest.Builder(context)
            .data(Uri.parse(uriString))
            .decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
            .videoFrameMillis(2000L)
            .crossfade(true)
            .build()
    }
    Box(
        modifier = modifier.background(CardSlate),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.45f),
            modifier = Modifier.size(28.dp)
        )
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun FolderCard(
    folder: FolderItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSlate),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("folder_card_${folder.displayName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftCyanContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = folder.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.videoCount} video${if (folder.videoCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_item),
                    tint = TextMuted
                )
            }
        }
    }
}

@Composable
fun VideoListCard(
    video: VideoItem,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resTag = formatResolutionTag(video.width, video.height)
    val sizeText = formatFileSize(video.sizeBytes)
    val progress = if (video.durationMs > 0L && video.lastPositionMs > 0L) {
        (video.lastPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSlate),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${video.title}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                VideoThumbnail(
                    uriString = video.uriString,
                    modifier = Modifier.fillMaxSize()
                )
                if (video.durationMs > 0L) {
                    Surface(
                        color = Color(0xCC070A0F),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = ElectricCyan,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.5.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resTag != null) {
                        MetadataBadge(text = resTag, highlight = true)
                    }
                    MetadataBadge(text = video.decoderMode.shortLabel, highlight = false)
                    if (video.externalSubtitleUri != null) {
                        Icon(
                            imageVector = Icons.Filled.Subtitles,
                            contentDescription = stringResource(R.string.cd_subtitles),
                            tint = AmberWarm,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (sizeText.isNotEmpty()) {
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_item),
                        tint = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun MetadataBadge(text: String, highlight: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (highlight) SoftCyanContainer else CardSlate,
        border = BorderStroke(
            0.75.dp,
            if (highlight) ElectricCyan.copy(alpha = 0.45f) else GlassBorder
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) ElectricCyan else TextMuted,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepAndPlaybackModalSheet(
    prefs: PlaybackPrefs,
    sleepTimer: SleepTimerState,
    onDismiss: () -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onSetCustomSleepMinutes: (Int) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onToggleAutoResume: (Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ElevatedSlate,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        SleepModePanelContent(
            prefs = prefs,
            sleepTimer = sleepTimer,
            onSelectSleepTimer = onSelectSleepTimer,
            onSetCustomSleepMinutes = onSetCustomSleepMinutes,
            onToggleBackgroundPlay = onToggleBackgroundPlay,
            onToggleAutoResume = onToggleAutoResume,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepModePanelContent(
    prefs: PlaybackPrefs,
    sleepTimer: SleepTimerState,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onSetCustomSleepMinutes: (Int) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onToggleAutoResume: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = if (sleepTimer.isActive) MintSuccess else ElectricCyan
                )
                Column {
                    Text("Sleeping Time Mode", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Stops playback & auto-closes app when timer ends",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
            if (sleepTimer.isActive) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0E2F26),
                    border = BorderStroke(1.dp, MintSuccess.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = sleepTimer.badgeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MintSuccess,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("sleep_timer_countdown_badge")
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SleepTimerOption.entries.forEach { option ->
                val selected = sleepTimer.option == option && sleepTimer.customMinutes == 0
                FilterChip(
                    selected = selected,
                    onClick = { onSelectSleepTimer(option) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SoftCyanContainer,
                        selectedLabelColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("sleep_chip_${option.name.lowercase()}")
                )
            }
        }

        // Custom +/- 5 min adjuster
        val currentMins = if (sleepTimer.isActive && sleepTimer.option != SleepTimerOption.END_OF_VIDEO) {
            ((sleepTimer.remainingSeconds + 59L) / 60L).toInt()
        } else 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onSetCustomSleepMinutes((currentMins - 5).coerceAtLeast(0)) },
                enabled = currentMins > 0,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
            ) {
                Text("-5 min", color = ElectricCyan)
            }
            OutlinedButton(
                onClick = { onSetCustomSleepMinutes((currentMins + 5).coerceAtMost(240)) },
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("sleep_add_5m_button")
            ) {
                Text("+5 min", color = ElectricCyan)
            }
        }

        HorizontalDivider(color = DividerSubtle)

        SettingToggleRow(
            title = "Background Playback",
            subtitle = "Keep audio playing when locking screen or switching apps",
            checked = prefs.backgroundPlayEnabled,
            onCheckedChange = onToggleBackgroundPlay,
            testTag = "toggle_background_play"
        )

        SettingToggleRow(
            title = "Auto-Resume Last Position",
            subtitle = "Resume local videos right where you left off",
            checked = prefs.autoResume,
            onCheckedChange = onToggleAutoResume,
            testTag = "toggle_auto_resume"
        )
    }
}

@Composable
internal fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBackground,
                checkedTrackColor = ElectricCyan
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun SectionHeaderWithBadge(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = SoftCyanContainer,
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}
