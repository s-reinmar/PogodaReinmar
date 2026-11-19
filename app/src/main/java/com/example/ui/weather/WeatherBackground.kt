package com.example.ui.weather

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SkyPalette

/**
 * Full-bleed animated atmospheric sky background reacting smoothly to day/night & weather state.
 */
@Composable
fun WeatherAtmosphericBackground(
    palette: SkyPalette,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val topColor by animateColorAsState(
        targetValue = palette.top,
        animationSpec = tween(durationMillis = 700),
        label = "sky_top",
    )
    val midColor by animateColorAsState(
        targetValue = palette.middle,
        animationSpec = tween(durationMillis = 700),
        label = "sky_mid",
    )
    val bottomColor by animateColorAsState(
        targetValue = palette.bottom,
        animationSpec = tween(durationMillis = 700),
        label = "sky_bottom",
    )
    val primaryGlow by animateColorAsState(
        targetValue = palette.glowPrimary,
        animationSpec = tween(durationMillis = 700),
        label = "sky_glow_primary",
    )
    val secondaryGlow by animateColorAsState(
        targetValue = palette.glowSecondary,
        animationSpec = tween(durationMillis = 700),
        label = "sky_glow_secondary",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, midColor, bottomColor),
                ),
            )

            // Upper-right solar or lunar atmospheric halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.14f),
                    radius = size.width * 0.68f,
                ),
                radius = size.width * 0.68f,
                center = Offset(size.width * 0.85f, size.height * 0.14f),
            )

            // Lower-left atmospheric horizon bounce
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryGlow, Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.72f),
                    radius = size.width * 0.60f,
                ),
                radius = size.width * 0.60f,
                center = Offset(size.width * 0.12f, size.height * 0.72f),
            )

            // Subtle deterministic starfield for clear nighttime skies
            if (palette.showStars) {
                val starCoords = listOf(
                    Triple(0.11f, 0.08f, 1.8f),
                    Triple(0.24f, 0.15f, 1.2f),
                    Triple(0.38f, 0.06f, 2.0f),
                    Triple(0.52f, 0.12f, 1.4f),
                    Triple(0.69f, 0.07f, 1.7f),
                    Triple(0.82f, 0.19f, 1.3f),
                    Triple(0.91f, 0.10f, 1.9f),
                    Triple(0.16f, 0.25f, 1.1f),
                    Triple(0.45f, 0.22f, 1.5f),
                    Triple(0.76f, 0.28f, 1.2f),
                    Triple(0.08f, 0.34f, 1.4f),
                    Triple(0.61f, 0.18f, 1.1f),
                )
                starCoords.forEach { (fx, fy, radiusDp) ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.52f),
                        radius = radiusDp * density,
                        center = Offset(size.width * fx, size.height * fy),
                    )
                }
            }
        }
        content()
    }
}
