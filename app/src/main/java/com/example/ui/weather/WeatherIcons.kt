package com.example.ui.weather

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.WeatherCondition
import com.example.domain.util.WeatherCodeMapper
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
 * Maps a [WeatherCondition] and day/night flag to the corresponding custom WebP drawable resource.
 */
@DrawableRes
fun weatherConditionDrawableRes(condition: WeatherCondition, isDay: Boolean): Int =
    when (condition) {
        WeatherCondition.CLEAR -> if (isDay) {
            R.drawable.ic_weather_clear_day
        } else {
            R.drawable.ic_weather_clear_night
        }
        WeatherCondition.MAINLY_CLEAR,
        WeatherCondition.PARTLY_CLOUDY -> if (isDay) {
            R.drawable.ic_weather_partly_cloudy_day
        } else {
            R.drawable.ic_weather_clear_night
        }
        WeatherCondition.OVERCAST -> R.drawable.ic_weather_overcast
        WeatherCondition.FOG -> R.drawable.ic_weather_fog
        WeatherCondition.DRIZZLE,
        WeatherCondition.RAIN,
        WeatherCondition.HEAVY_RAIN -> R.drawable.ic_weather_rain
        WeatherCondition.FREEZING_RAIN -> R.drawable.ic_weather_sleet
        WeatherCondition.HAIL -> R.drawable.ic_weather_hail
        WeatherCondition.SNOW -> R.drawable.ic_weather_snow
        WeatherCondition.THUNDERSTORM -> R.drawable.ic_weather_thunderstorm
    }

/**
 * Resolves the atmospheric glow color paired with each [WeatherCondition] for hero halos.
 */
fun weatherHaloColor(condition: WeatherCondition, isDay: Boolean): Color =
    when (condition) {
        WeatherCondition.CLEAR -> if (isDay) SunAmber else Color(0xFFFDE68A)
        WeatherCondition.MAINLY_CLEAR,
        WeatherCondition.PARTLY_CLOUDY -> if (isDay) Color(0xFFFDE68A) else IceBlue
        WeatherCondition.OVERCAST -> Color(0xFFE2E8F0)
        WeatherCondition.FOG -> Color(0xFFCBD5E1)
        WeatherCondition.DRIZZLE,
        WeatherCondition.RAIN,
        WeatherCondition.HEAVY_RAIN -> RainCyan
        WeatherCondition.FREEZING_RAIN -> Color(0xFF7DD3FC)
        WeatherCondition.HAIL -> Color(0xFF38BDF8)
        WeatherCondition.SNOW -> Color(0xFFF0F9FF)
        WeatherCondition.THUNDERSTORM -> Color(0xFFFBBF24)
    }

/**
 * Maps wind direction degrees (0..360) to one of the 8 custom compass drawable resources.
 */
@DrawableRes
fun windDirectionDrawableRes(degrees: Float?): Int? = when (WeatherCodeMapper.windDirectionLabel(degrees)) {
    "N" -> R.drawable.ic_wind_dir_n
    "NE" -> R.drawable.ic_wind_dir_ne
    "E" -> R.drawable.ic_wind_dir_e
    "SE" -> R.drawable.ic_wind_dir_se
    "S" -> R.drawable.ic_wind_dir_s
    "SW" -> R.drawable.ic_wind_dir_sw
    "W" -> R.drawable.ic_wind_dir_w
    "NW" -> R.drawable.ic_wind_dir_nw
    else -> null
}

/**
 * Renders the custom 8-direction wind compass graphic framed in a subtle frosted glass ring.
 */
@Composable
fun WindDirectionCompassIcon(
    degrees: Float?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
) {
    val drawableRes = windDirectionDrawableRes(degrees)

    Surface(
        modifier = modifier
            .size(size)
            .testTag("wind_direction_compass_icon"),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
    ) {
        Crossfade(
            targetState = drawableRes,
            animationSpec = tween(durationMillis = 240),
            label = "wind_direction_icon_crossfade",
        ) { resId ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (resId != null) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Air,
                        contentDescription = contentDescription,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * Selects the low or high UV custom graphic based on the WHO high-UV threshold (UV >= 6.0).
 */
@DrawableRes
fun uvIndexDrawableRes(uvIndex: Double?): Int =
    if (uvIndex != null && uvIndex >= 6.0) {
        R.drawable.ic_metric_uv_high
    } else {
        R.drawable.ic_metric_uv_low
    }

/**
 * Renders a custom meteorological metric graphic (pressure, humidity, precipitation, UV)
 * inside a subtle frosted circular glass frame consistent with the wind compass badge.
 */
@Composable
fun MetricGraphicBadge(
    @DrawableRes drawableRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    containerAlpha: Float = 0.14f,
    borderColor: Color = Color.White.copy(alpha = 0.24f),
    testTag: String? = null,
) {
    Surface(
        modifier = modifier
            .size(size)
            .let { if (testTag != null) it.testTag(testTag) else it },
        shape = CircleShape,
        color = Color.White.copy(alpha = containerAlpha),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Crossfade(
            targetState = drawableRes,
            animationSpec = tween(durationMillis = 240),
            label = "metric_graphic_crossfade",
        ) { resId ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Renders the custom weather condition illustration, optionally backed by a soft atmospheric halo.
 */
@Composable
fun WeatherConditionIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    withHalo: Boolean = false,
) {
    val drawableRes = weatherConditionDrawableRes(condition, isDay)
    val haloColor = weatherHaloColor(condition, isDay)
    val localizedDescription = localizedConditionLabel(condition)

    if (!withHalo) {
        Box(
            modifier = modifier
                .size(size)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = localizedDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size + 22.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.26f),
                            haloColor.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = drawableRes,
                animationSpec = tween(durationMillis = 260),
                label = "weather_hero_icon_crossfade",
            ) { resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = localizedDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(size)
                        .testTag("hero_weather_condition_icon"),
                )
            }
        }
    }
}
