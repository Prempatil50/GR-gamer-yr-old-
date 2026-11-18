package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorBorderSubtle = BorderCyan.copy(alpha = 0.45f)

private val GrGamerDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = IceCyan,
    secondary = ElectricBlue,
    onSecondary = TextPrimary,
    secondaryContainer = ElectricBlueContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = GamingEmerald,
    onTertiary = ObsidianBlack,
    tertiaryContainer = GamingEmeraldContainer,
    onTertiaryContainer = GamingEmerald,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = DeepSpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = DeepSpaceSurface,
    surfaceContainer = CarbonCard,
    surfaceContainerHigh = ElevatedSlate,
    surfaceContainerHighest = ElevatedSlate,
    outline = BorderCyan,
    outlineVariant = ColorBorderSubtle,
    error = CrimsonAlert,
    onError = TextPrimary,
    errorContainer = CrimsonAlertContainer,
    onErrorContainer = CrimsonAlert
)

val GrGamerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GrGamerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GrGamerDarkColorScheme,
        typography = GrGamerTypography,
        shapes = GrGamerShapes,
        content = content
    )
}
