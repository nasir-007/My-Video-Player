package com.example.ui.library

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.res.painterResource
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
import com.example.data.model.PowerProfile
import com.example.data.model.VideoItem
import com.example.player.BatteryStatusSnapshot
import com.example.player.NowPlayingSnapshot
import com.example.ui.common.formatDuration
import com.example.ui.common.formatFileSize
import com.example.ui.common.formatResolutionTag
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CardSlate
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
    powerProfile: PowerProfile,
    backgroundPlayEnabled: Boolean,
    onCycleDecoder: () -> Unit,
    onOpenPowerTab: () -> Unit,
    onLinkFolder: () -> Unit,
    onPickVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ElevatedSlate)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_cinema_hero),
            contentDescription = stringResource(R.string.cd_hero_banner),
            contentScale = ContentScale.Crop,
            alpha = 0.44f,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x4D070A0F),
                            Color(0xE0070A0F)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoftCyanContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = stringResource(R.string.library_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onOpenPowerTab,
                        shape = RoundedCornerShape(50),
                        color = if (backgroundPlayEnabled) Color(0xFF0E2F26) else CardSlate,
                        border = BorderStroke(
                            1.dp,
                            if (backgroundPlayEnabled) MintSuccess.copy(alpha = 0.6f) else GlassBorder
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hero_power_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BatterySaver,
                                contentDescription = null,
                                tint = if (powerProfile == PowerProfile.ECO) MintSuccess else ElectricCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = powerProfile.label.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
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
                    subtitle = "Scan device folders",
                    onClick = onLinkFolder,
                    isHighlighted = false,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("link_folder_button")
                )
                SourceQuickChip(
                    icon = Icons.Outlined.VideoFile,
                    label = stringResource(R.string.action_pick_video),
                    subtitle = "Instant video picker",
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
    subtitle: String,
    onClick: () -> Unit,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) SoftCyanContainer else CardSlate.copy(alpha = 0.9f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isHighlighted) ElectricCyan.copy(alpha = 0.55f) else GlassBorder
        ),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isHighlighted) ElectricCyan else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun BackgroundMiniPlayerBar(
    nowPlaying: NowPlayingSnapshot,
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
                Text(
                    text = if (nowPlaying.isPlaying) "Playing in background • Tap to return" else "Paused in background",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan
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
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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

@Composable
fun PowerAndBatterySection(
    prefs: PlaybackPrefs,
    batteryStatus: BatteryStatusSnapshot,
    onSelectPowerProfile: (PowerProfile) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onToggleAutoEco: (Boolean) -> Unit,
    onToggleAutoResume: (Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Device Battery & System Optimization Guard Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ElevatedSlate,
            border = BorderStroke(
                1.dp,
                if (batteryStatus.isIgnoringBatteryOptimizations) MintSuccess.copy(alpha = 0.45f)
                else AmberWarm.copy(alpha = 0.55f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            imageVector = if (batteryStatus.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatterySaver,
                            contentDescription = null,
                            tint = if (batteryStatus.isIgnoringBatteryOptimizations) MintSuccess else AmberWarm
                        )
                        Column {
                            Text("Battery & Doze Guard", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Device battery ${batteryStatus.batteryPercent}% • " +
                                    if (batteryStatus.isSystemPowerSaveMode) "System Saver ON" else "Normal thermal state",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Text(
                        text = "${batteryStatus.batteryPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = ElectricCyan
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (batteryStatus.isIgnoringBatteryOptimizations) Color(0xFF0E2F26) else AmberContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (batteryStatus.isIgnoringBatteryOptimizations) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (batteryStatus.isIgnoringBatteryOptimizations) MintSuccess else AmberWarm,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (batteryStatus.isIgnoringBatteryOptimizations) {
                                "Unrestricted: Android will not kill KinoX during background audio playback."
                            } else {
                                "Battery restricted: Android Doze may pause background audio when screen is off."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Button(
                    onClick = onOpenBatterySettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (batteryStatus.isIgnoringBatteryOptimizations) CardSlate else ElectricCyan,
                        contentColor = if (batteryStatus.isIgnoringBatteryOptimizations) ElectricCyan else ObsidianBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("open_battery_settings_button")
                ) {
                    Text(
                        text = if (batteryStatus.isIgnoringBatteryOptimizations) {
                            "Review Android Battery Settings"
                        } else {
                            "Allow Unrestricted Background Play"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Background Play & Engine Toggles
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ElevatedSlate,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Background & Engine Controls", style = MaterialTheme.typography.titleMedium)

                SettingToggleRow(
                    title = "Background Playback System",
                    subtitle = "Continue audio with notification controls when leaving app or locking screen",
                    checked = prefs.backgroundPlayEnabled,
                    onCheckedChange = onToggleBackgroundPlay,
                    testTag = "toggle_background_play"
                )
                SettingToggleRow(
                    title = "Auto-Eco on System Battery Saver",
                    subtitle = "Drop to strict HW decoding & 250ms polling when phone enters Saver mode",
                    checked = prefs.autoEcoOnBatterySaver,
                    onCheckedChange = onToggleAutoEco,
                    testTag = "toggle_auto_eco"
                )
                SettingToggleRow(
                    title = "Auto-Resume Last Position",
                    subtitle = "Automatically restore saved timestamp on local videos",
                    checked = prefs.autoResume,
                    onCheckedChange = onToggleAutoResume,
                    testTag = "toggle_auto_resume"
                )
            }
        }

        // Power Profiles
        Text("Playback Power Profiles", style = MaterialTheme.typography.titleMedium)
        PowerProfile.entries.forEach { profile ->
            val selected = prefs.powerProfile == profile
            Surface(
                onClick = { onSelectPowerProfile(profile) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) SoftCyanContainer else ElevatedSlate,
                border = BorderStroke(1.dp, if (selected) ElectricCyan else GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("power_profile_${profile.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelectPowerProfile(profile) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = if (profile == PowerProfile.ECO) MintSuccess else ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = profile.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) ElectricCyan else Color.White
                            )
                            MetadataBadge(
                                text = "${profile.minBufferMs / 1000}-${profile.maxBufferMs / 1000}s buf",
                                highlight = selected
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = profile.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
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
