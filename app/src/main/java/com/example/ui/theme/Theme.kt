package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AtmosphericDarkColorScheme = darkColorScheme(
    primary = SkyPrimary,
    onPrimary = SkyOnPrimary,
    primaryContainer = SkyPrimaryContainer,
    onPrimaryContainer = SkyOnPrimaryContainer,
    secondary = SunAmber,
    onSecondary = Color(0xFF2B1B00),
    tertiary = RainCyan,
    onTertiary = Color(0xFF002730),
    background = Color(0xFF0B1628),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF10213A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1C3352),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0x55FFFFFF),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)

val PogodaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PogodaPlTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AtmosphericDarkColorScheme,
        typography = PogodaTypography,
        shapes = PogodaShapes,
        content = content,
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    PogodaPlTheme(content = content)
}
