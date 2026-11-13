package com.example.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatioMode
import com.example.data.model.DecoderMode
import com.example.data.model.OrientationMode
import com.example.data.model.PlaybackPrefs
import com.example.data.model.SleepTimerOption
import com.example.data.model.SubtitleStylePrefs
import com.example.player.SleepTimerState
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CoralError
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SoftCyanContainer
import com.example.ui.theme.TextMuted
import kotlin.math.roundToInt

private val SUBTITLE_COLOR_SWATCHES = listOf(
    0xFFFFFFFFL to "White",
    0xFFFFD166L to "Gold",
    0xFF00E5FFL to "Cyan",
    0xFF2CE59BL to "Mint",
    0xFFFF8FA3L to "Coral"
)

private val SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val SEEK_STEP_PRESETS = listOf(5, 10, 15, 30)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsCustomizationSection(
    prefs: PlaybackPrefs,
    sleepTimer: SleepTimerState,
    onSelectDecoder: (DecoderMode) -> Unit,
    onSelectAspect: (AspectRatioMode) -> Unit,
    onSelectOrientation: (OrientationMode) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSeekStep: (Int) -> Unit,
    onToggleBackgroundPlay: (Boolean) -> Unit,
    onToggleAutoResume: (Boolean) -> Unit,
    onToggleBrightnessGesture: (Boolean) -> Unit,
    onToggleVolumeGesture: (Boolean) -> Unit,
    onToggleSeekGesture: (Boolean) -> Unit,
    onTogglePinchZoom: (Boolean) -> Unit,
    onToggleDoubleTapSeek: (Boolean) -> Unit,
    onUpdateSubtitleStyle: (SubtitleStylePrefs) -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onOpenSleepModal: () -> Unit,
    onClearWatchHistory: () -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Playback & Engine Defaults
        SettingsCardSection(
            title = "Playback & Engine",
            subtitle = "Customize decoder, speed, aspect ratio, and seek timing",
            icon = Icons.Filled.Tune,
            modifier = Modifier.testTag("settings_decoder_section")
        ) {
            SettingsSubLabel("Default Decoder")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecoderMode.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.defaultDecoderMode == mode,
                        onClick = { onSelectDecoder(mode) },
                        label = { Text("${mode.shortLabel} • ${mode.fullName}") },
                        colors = settingsChipColors(),
                        modifier = Modifier.testTag("setting_decoder_${mode.name.lowercase()}")
                    )
                }
            }
            Text(
                text = prefs.defaultDecoderMode.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            HorizontalDivider(color = DividerSubtle)

            SettingsSubLabel("Default Aspect Ratio")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatioMode.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.defaultAspectMode == mode,
                        onClick = { onSelectAspect(mode) },
                        label = { Text(mode.label) },
                        colors = settingsChipColors(),
                        modifier = Modifier.testTag("setting_aspect_${mode.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider(color = DividerSubtle)

            SettingsSubLabel("Default Orientation")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrientationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.defaultOrientationMode == mode,
                        onClick = { onSelectOrientation(mode) },
                        label = { Text(mode.label) },
                        colors = settingsChipColors(),
                        modifier = Modifier.testTag("setting_orientation_${mode.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider(color = DividerSubtle)

            SettingsSubLabel("Default Playback Speed")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SPEED_PRESETS.forEach { speed ->
                    val label = if (speed == 1.0f) "1.0x" else "${speed}x"
                    FilterChip(
                        selected = kotlin.math.abs(prefs.defaultPlaybackSpeed - speed) < 0.01f,
                        onClick = { onSelectSpeed(speed) },
                        label = { Text(label) },
                        colors = settingsChipColors(),
                        modifier = Modifier.testTag("setting_speed_${label.replace(".", "_")}")
                    )
                }
            }

            HorizontalDivider(color = DividerSubtle)

            SettingsSubLabel("Skip / Double-Tap Seek Step")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SEEK_STEP_PRESETS.forEach { sec ->
                    FilterChip(
                        selected = prefs.seekStepSeconds == sec,
                        onClick = { onSelectSeekStep(sec) },
                        label = { Text("${sec}s") },
                        colors = settingsChipColors(),
                        modifier = Modifier.testTag("setting_seek_${sec}s")
                    )
                }
            }

            HorizontalDivider(color = DividerSubtle)

            SettingToggleRow(
                title = "Background Playback",
                subtitle = "Keep audio playing when locking screen or switching apps",
                checked = prefs.backgroundPlayEnabled,
                onCheckedChange = onToggleBackgroundPlay,
                testTag = "setting_toggle_background_play"
            )

            SettingToggleRow(
                title = "Auto-Resume Last Position",
                subtitle = "Continue local videos from your last timestamp",
                checked = prefs.autoResume,
                onCheckedChange = onToggleAutoResume,
                testTag = "setting_toggle_auto_resume"
            )
        }

        // Section 2: Gestures & Touch Controls
        SettingsCardSection(
            title = "Gestures & Touch",
            subtitle = "Fine-tune MX-style swipe, tap, and zoom interactions",
            icon = Icons.Filled.TouchApp,
            modifier = Modifier.testTag("settings_gestures_section")
        ) {
            SettingToggleRow(
                title = "Left-Side Brightness Swipe",
                subtitle = "Drag vertically on left half to adjust screen brightness",
                checked = prefs.brightnessGestureEnabled,
                onCheckedChange = onToggleBrightnessGesture,
                testTag = "setting_toggle_brightness_gesture"
            )
            SettingToggleRow(
                title = "Right-Side Volume Swipe",
                subtitle = "Drag vertically on right half to adjust media volume",
                checked = prefs.volumeGestureEnabled,
                onCheckedChange = onToggleVolumeGesture,
                testTag = "setting_toggle_volume_gesture"
            )
            SettingToggleRow(
                title = "Horizontal Scrub Seek",
                subtitle = "Swipe left or right anywhere to scrub timeline smoothly",
                checked = prefs.seekGestureEnabled,
                onCheckedChange = onToggleSeekGesture,
                testTag = "setting_toggle_seek_gesture"
            )
            SettingToggleRow(
                title = "Pinch-to-Zoom & Pan",
                subtitle = "Two-finger zoom up to 4.0x and pan inside HW+ / SW video",
                checked = prefs.pinchZoomEnabled,
                onCheckedChange = onTogglePinchZoom,
                testTag = "setting_toggle_pinch_zoom"
            )
            SettingToggleRow(
                title = "Double-Tap Left / Right Seek",
                subtitle = "Double-tap screen sides to jump ${prefs.seekStepSeconds}s backward or forward",
                checked = prefs.doubleTapSeekEnabled,
                onCheckedChange = onToggleDoubleTapSeek,
                testTag = "setting_toggle_double_tap"
            )
        }

        // Section 3: Subtitle Studio Appearance
        SettingsCardSection(
            title = "Subtitle Studio",
            subtitle = "Customize global font size, color, shadow, and backdrop",
            icon = Icons.Filled.Subtitles
        ) {
            val style = prefs.subtitleStyle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B1320), Color(0xFF04060A))
                        )
                    )
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("settings_subtitle_preview"),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = style.backgroundOpacity)
                ) {
                    Text(
                        text = "KinoX Cinema Subtitle Preview",
                        color = Color(style.textColorArgb),
                        fontSize = style.fontSizeSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            shadow = if (style.hasOutline) {
                                Shadow(color = Color.Black, blurRadius = 8f)
                            } else null
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            SettingsSubLabel("Subtitle Color")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SUBTITLE_COLOR_SWATCHES.forEach { (argb, label) ->
                    val selected = style.textColorArgb == argb
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                onUpdateSubtitleStyle(style.copy(textColorArgb = argb))
                            }
                            .testTag("setting_sub_color_${label.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) ElectricCyan else GlassBorder,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            SettingsSliderRow(
                label = "Font Size",
                valueLabel = "${style.fontSizeSp.roundToInt()} sp",
                value = style.fontSizeSp,
                valueRange = 14f..32f,
                onValueChange = { onUpdateSubtitleStyle(style.copy(fontSizeSp = it)) }
            )

            SettingsSliderRow(
                label = "Backdrop Opacity",
                valueLabel = "${(style.backgroundOpacity * 100).roundToInt()}%",
                value = style.backgroundOpacity,
                valueRange = 0f..0.85f,
                onValueChange = { onUpdateSubtitleStyle(style.copy(backgroundOpacity = it)) }
            )

            SettingsSliderRow(
                label = "Bottom Margin",
                valueLabel = "${style.bottomMarginPercent.roundToInt()}%",
                value = style.bottomMarginPercent,
                valueRange = 4f..24f,
                onValueChange = { onUpdateSubtitleStyle(style.copy(bottomMarginPercent = it)) }
            )

            SettingToggleRow(
                title = "Edge Shadow & Outline",
                subtitle = "Keep subtitle text crisp against bright cinema scenes",
                checked = style.hasOutline,
                onCheckedChange = { onUpdateSubtitleStyle(style.copy(hasOutline = it)) },
                testTag = "setting_toggle_subtitle_outline"
            )
        }

        // Section 4: Sleeping Time Mode
        SettingsCardSection(
            title = "Sleeping Time Mode",
            subtitle = "Automatically stops playback and closes KinoX when timer ends",
            icon = Icons.Filled.Bedtime
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (sleepTimer.isActive) {
                        "Active countdown: ${sleepTimer.badgeLabel}"
                    } else {
                        "Timer is currently off"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (sleepTimer.isActive) MintSuccess else TextMuted,
                    fontFamily = if (sleepTimer.isActive) JetBrainsMonoFontFamily else null
                )
                OutlinedButton(
                    onClick = onOpenSleepModal,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_open_sleep_button")
                ) {
                    Text("Custom Timer", color = ElectricCyan)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    SleepTimerOption.OFF,
                    SleepTimerOption.MIN_15,
                    SleepTimerOption.MIN_30,
                    SleepTimerOption.MIN_60,
                    SleepTimerOption.END_OF_VIDEO
                ).forEach { option ->
                    val selected = sleepTimer.option == option && sleepTimer.customMinutes == 0
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectSleepTimer(option) },
                        label = { Text(option.label) },
                        colors = settingsChipColors()
                    )
                }
            }
        }

        // Section 5: Always-On Optimization & Maintenance
        SettingsCardSection(
            title = "Storage & Reset",
            subtitle = "Manage watch history or restore factory preferences",
            icon = Icons.Filled.Memory
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardSlate,
                border = BorderStroke(1.dp, MintSuccess.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Always-On Battery Optimizer & Delay-Less Local Buffer active (Android 10–16).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MintSuccess,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onClearWatchHistory,
                    border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CleaningServices,
                        contentDescription = null,
                        tint = CoralError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Clear History", color = CoralError, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onResetSettings,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("reset_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Reset All", color = ElectricCyan, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SettingsCardSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftCyanContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
            HorizontalDivider(color = DividerSubtle)
            content()
        }
    }
}

@Composable
private fun SettingsSubLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = ElectricCyan
    )
}

@Composable
private fun SettingsSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFontFamily,
                color = ElectricCyan
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = CardSlate
            )
        )
    }
}

@Composable
private fun settingsChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = SoftCyanContainer,
    selectedLabelColor = ElectricCyan,
    containerColor = CardSlate,
    labelColor = TextMuted
)
