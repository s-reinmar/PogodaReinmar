package com.example.ui.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.FilterDrama
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.WeatherCondition
import com.example.ui.theme.IceBlue
import com.example.ui.theme.RainCyan
import com.example.ui.theme.SkyPalette
import com.example.ui.theme.SunAmber

/**
 * Translucent Material 3 Bento Grid card with luminous glass border.
 */
@Composable
fun BentoGlassCard(
    palette: SkyPalette,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = if (highlighted) palette.cardHighlight else palette.cardContainer,
        border = BorderStroke(1.dp, palette.cardBorder),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Resolves Material Icons Extended icon & vibrant meteorological tint for a [WeatherCondition].
 */
fun weatherIconSpec(condition: WeatherCondition, isDay: Boolean): Pair<ImageVector, Color> =
    when (condition) {
        WeatherCondition.CLEAR -> if (isDay) {
            Icons.Rounded.WbSunny to SunAmber
        } else {
            Icons.Rounded.NightsStay to Color(0xFFE0E7FF)
        }
        WeatherCondition.MAINLY_CLEAR,
        WeatherCondition.PARTLY_CLOUDY -> if (isDay) {
            Icons.Rounded.FilterDrama to Color(0xFFFDE68A)
        } else {
            Icons.Rounded.NightsStay to IceBlue
        }
        WeatherCondition.OVERCAST -> Icons.Rounded.Cloud to Color(0xFFE2E8F0)
        WeatherCondition.FOG -> Icons.Rounded.Dehaze to Color(0xFFCBD5E1)
        WeatherCondition.DRIZZLE -> Icons.Rounded.Grain to RainCyan
        WeatherCondition.RAIN,
        WeatherCondition.HEAVY_RAIN -> Icons.Rounded.WaterDrop to Color(0xFF38BDF8)
        WeatherCondition.FREEZING_RAIN -> Icons.Rounded.AcUnit to Color(0xFF7DD3FC)
        WeatherCondition.SNOW -> Icons.Rounded.AcUnit to Color(0xFFF0F9FF)
        WeatherCondition.THUNDERSTORM -> Icons.Rounded.Thunderstorm to Color(0xFFFBBF24)
    }

/**
 * Renders a crisp weather condition icon, optionally backed by a soft atmospheric halo.
 */
@Composable
fun WeatherConditionIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    withHalo: Boolean = false,
) {
    val (vector, tint) = weatherIconSpec(condition, isDay)
    val localizedDescription = localizedConditionLabel(condition)
    if (!withHalo) {
        Icon(
            imageVector = vector,
            contentDescription = localizedDescription,
            tint = tint,
            modifier = modifier.size(size),
        )
    } else {
        Box(
            modifier = modifier
                .size(size + 24.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.28f),
                            tint.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = vector,
                contentDescription = localizedDescription,
                tint = tint,
                modifier = Modifier.size(size),
            )
        }
    }
}
