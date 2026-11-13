package com.example.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DecoderMode
import com.example.data.model.MediaTrackInfo
import com.example.data.model.PowerProfile
import com.example.data.model.SleepTimerOption
import com.example.data.model.SubtitleStylePrefs
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomSheets(
    state: PlayerUiState,
    onDismiss: () -> Unit,
    onSelectSubtitleTrack: (MediaTrackInfo) -> Unit,
    onDisableSubtitles: () -> Unit,
    onPickExternalSubtitle: () -> Unit,
    onAdjustDelay: (Long) -> Unit,
    onResetDelay: () -> Unit,
    onUpdateSubtitleStyle: (SubtitleStylePrefs) -> Unit,
    onSwitchDecoder: (DecoderMode) -> Unit,
    onSelectAudioTrack: (MediaTrackInfo) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onSelectPowerProfile: (PowerProfile) -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    if (state.activeSheet == PlayerSheetType.NONE) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ElevatedSlate,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        when (state.activeSheet) {
            PlayerSheetType.SUBTITLES -> SubtitleStudioContent(
                state = state,
                onSelectTrack = onSelectSubtitleTrack,
                onDisableSubtitles = onDisableSubtitles,
                onPickExternalSubtitle = onPickExternalSubtitle,
                onAdjustDelay = onAdjustDelay,
                onResetDelay = onResetDelay,
                onUpdateStyle = onUpdateSubtitleStyle
            )
            PlayerSheetType.DECODER_INFO -> DecoderAndTelemetryContent(
                state = state,
                onSwitchDecoder = onSwitchDecoder
            )
            PlayerSheetType.AUDIO_SPEED -> AudioAndSpeedContent(
                state = state,
                onSelectAudioTrack = onSelectAudioTrack,
                onSetSpeed = onSetSpeed
            )
            PlayerSheetType.POWER_BATTERY -> PowerAndBackgroundSheetContent(
                state = state,
                onToggleBackgroundPlay = onToggleBackgroundPlay,
                onSelectPowerProfile = onSelectPowerProfile,
                onSelectSleepTimer = onSelectSleepTimer,
                onOpenBatterySettings = onOpenBatterySettings
            )
            PlayerSheetType.NONE -> Unit
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PowerAndBackgroundSheetContent(
    state: PlayerUiState,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onSelectPowerProfile: (PowerProfile) -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Background Play & Battery", style = MaterialTheme.typography.titleLarge)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardSlate,
            border = BorderStroke(1.dp, if (state.backgroundPlayEnabled) MintSuccess.copy(alpha = 0.5f) else GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = null,
                    tint = if (state.backgroundPlayEnabled) MintSuccess else TextMuted
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Background Audio Playback", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Keep playing via MediaSessionService when leaving app or locking screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = state.backgroundPlayEnabled,
                    onCheckedChange = onToggleBackgroundPlay,
                    colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBackground, checkedTrackColor = MintSuccess)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sleep Timer", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SleepTimerOption.entries.forEach { option ->
                    val selected = state.sleepTimerOption == option
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSleepTimer(option) },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoftCyanContainer,
                            selectedLabelColor = ElectricCyan
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = DividerSubtle)
        Text("Power Optimization Profile", style = MaterialTheme.typography.titleSmall)

        PowerProfile.entries.forEach { profile ->
            val selected = state.powerProfile == profile
            Surface(
                onClick = { onSelectPowerProfile(profile) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) SoftCyanContainer else CardSlate,
                border = BorderStroke(1.dp, if (selected) ElectricCyan else GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelectPowerProfile(profile) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) ElectricCyan else Color.White
                        )
                        Text(
                            text = profile.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ObsidianBackground,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BatterySaver,
                        contentDescription = null,
                        tint = if (state.batteryStatus.isIgnoringBatteryOptimizations) MintSuccess else AmberWarm
                    )
                    Text(
                        text = if (state.batteryStatus.isIgnoringBatteryOptimizations) {
                            "Unrestricted • Battery ${state.batteryStatus.batteryPercent}%"
                        } else {
                            "System Doze may restrict background play"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(onClick = onOpenBatterySettings) {
                    Text("Settings", color = ElectricCyan)
                }
            }
        }
    }
}

@Composable
private fun SubtitleStudioContent(
    state: PlayerUiState,
    onSelectTrack: (MediaTrackInfo) -> Unit,
    onDisableSubtitles: () -> Unit,
    onPickExternalSubtitle: () -> Unit,
    onAdjustDelay: (Long) -> Unit,
    onResetDelay: () -> Unit,
    onUpdateStyle: (SubtitleStylePrefs) -> Unit
) {
    val style = state.subtitleStyle
    val colorOptions = listOf(
        0xFFFFFFFFL to "White",
        0xFFFFE066L to "Cinema Yellow",
        0xFF00E5FFL to "Cyan",
        0xFF2CE59BL to "Mint"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Subtitle Studio", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = onPickExternalSubtitle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftCyanContainer,
                    contentColor = ElectricCyan
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("pick_external_subtitle_button")
            ) {
                Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Load .SRT / .VTT")
            }
        }

        if (state.externalSubtitleName != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SoftCyanContainer,
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ElectricCyan)
                    Text(
                        text = "External: ${state.externalSubtitleName}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tracks", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDisableSubtitles)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !state.subtitlesEnabled,
                    onClick = onDisableSubtitles,
                    colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                )
                Text("Subtitles Off", style = MaterialTheme.typography.bodyMedium)
            }
            state.subtitleTracks.forEach { track ->
                val selected = state.subtitlesEnabled && state.externalSubtitleUri == null && track.isSelected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTrack(track) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelectTrack(track) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Text(track.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        HorizontalDivider(color = DividerSubtle)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtitle Sync Delay", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = String.format(Locale.US, "%+.1fs", state.subtitleDelayMs / 1000f),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = ElectricCyan
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAdjustDelay(-500L) },
                    modifier = Modifier.weight(1f)
                ) { Text("-0.5s") }
                OutlinedButton(
                    onClick = { onAdjustDelay(-100L) },
                    modifier = Modifier.weight(1f)
                ) { Text("-0.1s") }
                TextButton(
                    onClick = onResetDelay,
                    modifier = Modifier.weight(0.9f)
                ) { Text("Reset", color = ElectricCyan) }
                OutlinedButton(
                    onClick = { onAdjustDelay(100L) },
                    modifier = Modifier.weight(1f)
                ) { Text("+0.1s") }
                OutlinedButton(
                    onClick = { onAdjustDelay(500L) },
                    modifier = Modifier.weight(1f)
                ) { Text("+0.5s") }
            }
        }

        HorizontalDivider(color = DividerSubtle)

        Text("Appearance", style = MaterialTheme.typography.titleSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianBackground)
                .padding(vertical = 18.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = style.backgroundOpacity))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Sample KinoX Subtitle Line",
                    color = Color(style.textColorArgb.toInt()),
                    fontSize = style.fontSizeSp.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Color", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            colorOptions.forEach { (argb, _) ->
                val isSelected = style.textColorArgb == argb
                Surface(
                    onClick = { onUpdateStyle(style.copy(textColorArgb = argb)) },
                    shape = CircleShape,
                    color = Color(argb.toInt()),
                    border = BorderStroke(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) ElectricCyan else GlassBorder
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(Modifier.size(34.dp))
                }
            }
        }

        Column {
            Text("Font Size: ${style.fontSizeSp.toInt()}sp", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Slider(
                value = style.fontSizeSp,
                onValueChange = { onUpdateStyle(style.copy(fontSizeSp = it)) },
                valueRange = 14f..32f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
        }

        Column {
            Text("Background Box Opacity: ${(style.backgroundOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Slider(
                value = style.backgroundOpacity,
                onValueChange = { onUpdateStyle(style.copy(backgroundOpacity = it)) },
                valueRange = 0f..0.85f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
        }

        Column {
            Text("Bottom Margin: ${style.bottomMarginPercent.toInt()}%", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Slider(
                value = style.bottomMarginPercent,
                onValueChange = { onUpdateStyle(style.copy(bottomMarginPercent = it)) },
                valueRange = 4f..24f,
                colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Drop Shadow / Edge Outline", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = style.hasOutline,
                onCheckedChange = { onUpdateStyle(style.copy(hasOutline = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBackground, checkedTrackColor = ElectricCyan)
            )
        }
    }
}

@Composable
private fun DecoderAndTelemetryContent(
    state: PlayerUiState,
    onSwitchDecoder: (DecoderMode) -> Unit
) {
    val telemetry = state.codecTelemetry
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Decoder & Hardware Acceleration", style = MaterialTheme.typography.titleLarge)

        DecoderMode.entries.forEach { mode ->
            val selected = state.decoderMode == mode
            Surface(
                onClick = { onSwitchDecoder(mode) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) SoftCyanContainer else CardSlate,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) ElectricCyan else GlassBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("decoder_option_${mode.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSwitchDecoder(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${mode.shortLabel} — ${mode.fullName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = mode.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = DividerSubtle)
        Text("Live Codec Telemetry", style = MaterialTheme.typography.titleSmall)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianBackground,
            border = BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryRow("Video Decoder", telemetry.videoCodecName)
                TelemetryRow(
                    "Acceleration",
                    when (telemetry.isHardwareAccelerated) {
                        true -> "Hardware Accelerated"
                        false -> "Software Codec"
                        null -> "Detecting..."
                    },
                    valueColor = when (telemetry.isHardwareAccelerated) {
                        true -> MintSuccess
                        false -> AmberWarm
                        null -> TextMuted
                    }
                )
                TelemetryRow("Video Format", telemetry.mimeType)
                TelemetryRow(
                    "Resolution / FPS",
                    if (telemetry.width > 0) {
                        val fps = if (telemetry.frameRate > 0f) String.format(Locale.US, " @ %.1f fps", telemetry.frameRate) else ""
                        "${telemetry.width}x${telemetry.height}$fps"
                    } else "—"
                )
                TelemetryRow("Dropped Frames", "${telemetry.droppedFrames}")
                TelemetryRow("Audio Decoder", telemetry.audioCodecName)
                TelemetryRow(
                    "Audio Stream",
                    if (telemetry.audioChannels > 0) "${telemetry.audioChannels}ch • ${telemetry.audioSampleRateHz} Hz" else "—"
                )
            }
        }
    }
}

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMonoFontFamily,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioAndSpeedContent(
    state: PlayerUiState,
    onSelectAudioTrack: (MediaTrackInfo) -> Unit,
    onSetSpeed: (Float) -> Unit
) {
    val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Playback Speed", style = MaterialTheme.typography.titleLarge)
            Text(
                text = String.format(Locale.US, "%.2fx", state.playbackSpeed),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                color = ElectricCyan
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            speedPresets.forEach { preset ->
                val selected = kotlin.math.abs(state.playbackSpeed - preset) < 0.01f
                FilterChip(
                    selected = selected,
                    onClick = { onSetSpeed(preset) },
                    label = {
                        Text(
                            text = "${preset}x",
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SoftCyanContainer,
                        selectedLabelColor = ElectricCyan
                    )
                )
            }
        }

        Slider(
            value = state.playbackSpeed,
            onValueChange = onSetSpeed,
            valueRange = 0.25f..3.0f,
            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
        )

        HorizontalDivider(color = DividerSubtle)
        Text("Audio Tracks", style = MaterialTheme.typography.titleSmall)

        if (state.audioTracks.isEmpty()) {
            Text(
                text = "Default audio stream active",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        } else {
            state.audioTracks.forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectAudioTrack(track) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = track.isSelected,
                        onClick = { onSelectAudioTrack(track) },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Column {
                        Text(track.label, style = MaterialTheme.typography.bodyMedium)
                        if (track.channelCount > 0) {
                            Text(
                                text = "${track.channelCount} channels • ${track.mimeType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
