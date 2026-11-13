package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KinoXDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepCyan,
    primaryContainer = SoftCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = SkyCyan,
    onSecondary = DeepCyan,
    secondaryContainer = ElevatedSlate,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarm,
    onTertiary = AmberContainer,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberWarm,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = TheaterSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSlate,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = TheaterSurface,
    surfaceContainer = ElevatedSlate,
    surfaceContainerHigh = CardSlate,
    surfaceContainerHighest = Color(0xFF223047),
    outline = GlassBorder,
    outlineVariant = DividerSubtle,
    error = CoralError,
    onError = TextPrimary,
    errorContainer = CoralContainer,
    onErrorContainer = CoralError
)

private val KinoXShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KinoXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KinoXDarkColorScheme,
        typography = Typography,
        shapes = KinoXShapes,
        content = content
    )
}
