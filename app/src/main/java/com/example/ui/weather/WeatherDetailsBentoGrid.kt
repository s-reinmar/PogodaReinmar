package com.example.ui.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.HybridWeatherBundle
import com.example.domain.util.WeatherCodeMapper
import com.example.ui.theme.RainCyan
import com.example.ui.theme.SkyPalette
import com.example.ui.theme.SunAmber
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Adaptive Bento Grid displaying 6 hybrid meteorological detail tiles
 * (Wind + rotating compass, Pressure, Humidity, Precipitation, UV Index, Sunrise/Sunset arc).
 */
@Composable
fun WeatherDetailsBentoGrid(
    bundle: HybridWeatherBundle,
    palette: SkyPalette,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_details_bento_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = appString(R.string.section_details_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Text(
                text = appString(R.string.section_details_source),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }

        val tiles: List<@Composable (Modifier) -> Unit> = listOf(
            { mod -> WindBentoTile(bundle, palette, mod) },
            { mod -> PressureBentoTile(bundle, palette, mod) },
            { mod -> HumidityBentoTile(bundle, palette, mod) },
            { mod -> PrecipitationBentoTile(bundle, palette, mod) },
            { mod -> UvIndexBentoTile(bundle, palette, mod) },
            { mod -> SunCycleBentoTile(bundle, palette, mod) },
        )

        val safeColumns = columns.coerceIn(2, 3)
        tiles.chunked(safeColumns).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowTiles.forEach { renderTile ->
                    renderTile(Modifier.weight(1f))
                }
                repeat(safeColumns - rowTiles.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BentoTileHeader(
    title: String,
    icon: ImageVector,
    tint: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        val activeLocale = LocalAppLanguage.current.toLocale()
        Text(
            text = title.uppercase(activeLocale),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun WindBentoTile(
    bundle: HybridWeatherBundle,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    val windKmh = bundle.effectiveWindSpeedKmh
    val windMs = bundle.effectiveWindSpeedMs
    val directionDeg = bundle.effectiveWindDirectionDeg

    BentoGlassCard(
        palette = palette,
        modifier = modifier
            .height(152.dp)
            .testTag("tile_wind"),
    ) {
        BentoTileHeader(
            title = appString(R.string.metric_wind),
            icon = Icons.Rounded.Air,
            tint = palette.accent,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = windKmh?.let { "${it.roundToInt()} km/h" } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                val activeLocale = LocalAppLanguage.current.toLocale()
                Text(
                    text = windMs?.let {
                        String.format(activeLocale, "%.1f m/s", it)
                    } ?: appString(R.string.metric_no_data),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.68f),
                )
            }

            WindDirectionCompassIcon(
                degrees = directionDeg,
                contentDescription = localizedWindDirectionName(directionDeg),
                size = 50.dp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${localizedWindDirectionName(directionDeg)} (${WeatherCodeMapper.windDirectionLabel(directionDeg)})",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun PressureBentoTile(
    bundle: HybridWeatherBundle,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    val pressure = bundle.effectivePressureHpa
    val normalized = ((pressure ?: 1013.0) - 965.0) / (1050.0 - 965.0)

    BentoGlassCard(
        palette = palette,
        modifier = modifier
            .height(152.dp)
            .testTag("tile_pressure"),
    ) {
        BentoTileHeader(
            title = appString(R.string.metric_pressure),
            icon = Icons.Rounded.Speed,
            tint = palette.accent,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (pressure != null) {
                    buildAnnotatedString {
                        append("${pressure.roundToInt()}")
                        withStyle(
                            SpanStyle(
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.78f),
                            ),
                        ) {
                            append(" hPa")
                        }
                    }
                } else {
                    buildAnnotatedString { append("—") }
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            MetricGraphicBadge(
                drawableRes = R.drawable.ic_metric_pressure,
                contentDescription = appString(R.string.metric_pressure),
                size = 46.dp,
                testTag = "metric_pressure_icon",
            )
        }
        Spacer(Modifier.height(8.dp))
        MetricBarIndicator(
            fraction = normalized.toFloat().coerceIn(0f, 1f),
            colors = listOf(Color(0xFF60A5FA), Color(0xFF34D399), SunAmber),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = localizedPressureDescription(pressure),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun HumidityBentoTile(
    bundle: HybridWeatherBundle,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    val humidity = bundle.effectiveHumidityPct

    BentoGlassCard(
        palette = palette,
        modifier = modifier
            .height(152.dp)
            .testTag("tile_humidity"),
    ) {
        BentoTileHeader(
            title = appString(R.string.metric_humidity),
            icon = Icons.Rounded.WaterDrop,
            tint = RainCyan,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = humidity?.let { "${it.roundToInt()}%" } ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            MetricGraphicBadge(
                drawableRes = R.drawable.ic_metric_humidity,
                contentDescription = appString(R.string.metric_humidity),
                size = 46.dp,
                testTag = "metric_humidity_icon",
            )
        }
        Spacer(Modifier.height(8.dp))
        MetricBarIndicator(
            fraction = ((humidity ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f),
            colors = listOf(Color(0xFF38BDF8), RainCyan),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = localizedHumidityDescription(humidity),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun PrecipitationBentoTile(
    bundle: HybridWeatherBundle,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    val precip = bundle.effectivePrecipitationMm
    val sourceLabel = if (bundle.imgwMeasurement?.precipitationMm != null) {
        appString(R.string.precip_source_imgw)
    } else {
        appString(R.string.precip_source_open_meteo)
    }

    BentoGlassCard(
        palette = palette,
        modifier = modifier
            .height(152.dp)
            .testTag("tile_precipitation"),
    ) {
        BentoTileHeader(
            title = appString(R.string.metric_precipitation),
            icon = Icons.Rounded.Umbrella,
            tint = RainCyan,
        )
        Spacer(Modifier.weight(1f))
        val activeLocale = LocalAppLanguage.current.toLocale()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (precip != null) {
                    buildAnnotatedString {
                        append(String.format(activeLocale, "%.1f", precip))
                        withStyle(
                            SpanStyle(
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.78f),
                            ),
                        ) {
                            append(" mm")
                        }
                    }
                } else {
                    buildAnnotatedString { append("—") }
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            MetricGraphicBadge(
                drawableRes = R.drawable.ic_metric_precipitation,
                contentDescription = appString(R.string.metric_precipitation),
                size = 46.dp,
                testTag = "metric_precipitation_icon",
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = localizedPrecipitationDescription(precip),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = sourceLabel,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.56f),
        )
    }
}

@Composable
private fun UvIndexBentoTile(
    bundle: HybridWeatherBundle,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    val uv = bundle.openMeteo?.todayUvIndexMax
    val isHighUv = uv != null && uv >= 6.0

    BentoGlassCard(
        palette = palette,
        modifier = modifier
            .height(152.dp)
            .testTag("tile_uv"),
    ) {
        BentoTileHeader(
            title = appString(R.string.metric_uv),
            icon = Icons.Rounded.WbSunny,
            tint = SunAmber,
        )
        Spacer(Modifier.weight(1f))
        val activeLocale = LocalAppLanguage.current.toLocale()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = uv?.let { String.format(activeLocale, "%.1f", it) } ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            MetricGraphicBadge(
                drawableRes = uvIndexDrawableRes(uv),
                contentDescription = localizedUvRiskDescription(uv),
                size = 46.dp,
                containerAlpha = if (isHighUv) 0.16f else 0.24f,
                borderColor = if (isHighUv) {
                    SunAmber.copy(alpha = 0.65f)
                } else {
                    Color.White.copy(alpha = 0.30f)
                },
                testTag = "metric_uv_icon",
            )
        }
        Spacer(Modifier.height(8.dp))
        MetricBarIndicator(
            fraction = ((uv ?: 0.0) / 11.0).toFloat().coerceIn(0f, 1f),
            colors = listOf(
                Color(0xFF4ADE80),
                Color(0xFFFACC15),
                Color(0xFFFB923C),
                Color(0xFFF87171),
                Color(0xFFC084FC),
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = localizedUvRiskDescription(uv),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun SunCycleBentoTile(
    bundle: HybridWeatherBundle,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    val sunrise = bundle.openMeteo?.todaySunriseLabel ?: "—"
    val sunset = bundle.openMeteo?.todaySunsetLabel ?: "—"
    val progress = bundle.sunriseProgress

    BentoGlassCard(
        palette = palette,
        modifier = modifier
            .height(152.dp)
            .testTag("tile_sun_cycle"),
    ) {
        BentoTileHeader(
            title = appString(R.string.metric_sun),
            icon = Icons.Rounded.WbTwilight,
            tint = SunAmber,
        )
        Spacer(Modifier.weight(1f))

        // Solar horizon arc drawn in Compose Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            val horizonY = size.height - 6.dp.toPx()
            val arcWidth = size.width - 16.dp.toPx()
            val startX = 8.dp.toPx()
            val arcHeight = size.height - 14.dp.toPx()

            // Horizon baseline
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, horizonY),
                end = Offset(size.width, horizonY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            )

            // Sun trajectory parabolic arc
            val arcPath = Path().apply {
                moveTo(startX, horizonY)
                quadraticTo(
                    startX + arcWidth / 2f,
                    horizonY - arcHeight * 1.9f,
                    startX + arcWidth,
                    horizonY,
                )
            }
            drawPath(
                path = arcPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        SunAmber.copy(alpha = 0.45f),
                        Color(0xFFFDE047),
                        Color(0xFFFB923C).copy(alpha = 0.65f),
                    ),
                ),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )

            // Current sun position dot along the arc
            if (progress != null && bundle.isDaytime) {
                val t = progress.coerceIn(0f, 1f)
                val sunX = startX + arcWidth * t
                val sunY = horizonY - (sin(t * PI).toFloat() * arcHeight)
                drawCircle(
                    color = SunAmber.copy(alpha = 0.35f),
                    radius = 9.dp.toPx(),
                    center = Offset(sunX, sunY),
                )
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = 4.5.dp.toPx(),
                    center = Offset(sunX, sunY),
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = appString(R.string.metric_sunrise),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.58f),
                )
                Text(
                    text = sunrise,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = appString(R.string.metric_sunset),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.58f),
                )
                Text(
                    text = sunset,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun MetricBarIndicator(
    fraction: Float,
    colors: List<Color>,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50)),
    ) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            size = size,
            cornerRadius = corner,
        )
        val fillWidth = (size.width * fraction).coerceAtLeast(size.height)
        drawRoundRect(
            brush = Brush.horizontalGradient(colors = colors),
            size = Size(fillWidth, size.height),
            cornerRadius = corner,
        )
    }
}
