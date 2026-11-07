package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AutoPulseColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004E59),
    onPrimaryContainer = Color(0xFF9CF0FF),
    secondary = NeonEmerald,
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF005234),
    onSecondaryContainer = Color(0xFF8CF7BD),
    tertiary = NeonIndigo,
    background = CockpitBackground,
    onBackground = CockpitOnSurface,
    surface = CockpitSurface,
    onSurface = CockpitOnSurface,
    surfaceVariant = CockpitSurfaceVariant,
    onSurfaceVariant = CockpitOnSurfaceDim,
    error = NeonRose,
    onError = Color(0xFF4C0012)
)

@Composable
fun AutoPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AutoPulseColorScheme,
        typography = Typography,
        content = content
    )
}
