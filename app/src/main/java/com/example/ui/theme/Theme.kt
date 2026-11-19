package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BezelDarkColorScheme = darkColorScheme(
    primary = TitaniumCyan,
    onPrimary = OnTitaniumCyan,
    primaryContainer = TitaniumCyanContainer,
    onPrimaryContainer = TitaniumCyan,
    secondary = SignalAmber,
    onSecondary = ObsidianVoid,
    secondaryContainer = SignalAmberContainer,
    onSecondaryContainer = SignalAmber,
    tertiary = EmeraldActive,
    onTertiary = ObsidianVoid,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldActive,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianElevated,
    surfaceContainerHigh = ObsidianCardHighlight,
    surfaceContainerHighest = HairlineBorder,
    outline = HairlineBorder,
    outlineVariant = ObsidianCardHighlight,
    error = PlasmaCoral,
    onError = ObsidianVoid
)

val BezelShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BezelPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BezelDarkColorScheme,
        typography = BezelTypography,
        shapes = BezelShapes,
        content = content
    )
}
