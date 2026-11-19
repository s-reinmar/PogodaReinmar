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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DailyForecastItem
import com.example.domain.util.PolishDateTimeUtils
import com.example.domain.util.WeatherCodeMapper
import com.example.ui.theme.RainCyan
import com.example.ui.theme.SkyPalette
import com.example.ui.theme.TempCool
import com.example.ui.theme.TempFreezing
import com.example.ui.theme.TempHot
import com.example.ui.theme.TempMild
import com.example.ui.theme.TempWarm
import kotlin.math.roundToInt

/**
 * 7-day Open-Meteo forecast card featuring a custom Compose Canvas thermal temperature range bar.
 */
@Composable
fun DailyForecastSection(
    days: List<DailyForecastItem>,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return

    val globalMin = remember(days) { days.minOf { it.tempMinC } }
    val globalMax = remember(days) { days.maxOf { it.tempMaxC } }

    BentoGlassCard(
        palette = palette,
        contentPadding = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_forecast_section"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = appString(R.string.section_daily_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            Text(
                text = appString(R.string.section_hourly_source),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
            )
        }

        Spacer(Modifier.height(10.dp))

        days.forEachIndexed { index, day ->
            DailyForecastRow(
                item = day,
                dayIndex = index,
                globalMin = globalMin,
                globalMax = globalMax,
            )
            if (index < days.lastIndex) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    item: DailyForecastItem,
    dayIndex: Int,
    globalMin: Double,
    globalMax: Double,
) {
    val condition = WeatherCodeMapper.fromWmoCode(item.weatherCode)
    val activeLocale = LocalAppLanguage.current.toLocale()
    val isToday = dayIndex == 0
    val displayDayLabel = when (dayIndex) {
        0 -> appString(R.string.day_today)
        1 -> appString(R.string.day_tomorrow)
        else -> remember(item.isoDate, dayIndex, activeLocale) {
            PolishDateTimeUtils.formatDailyLabels(item.isoDate, dayIndex, activeLocale).first
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Day name + date
        Column(modifier = Modifier.width(62.dp)) {
            Text(
                text = displayDayLabel,
                style = if (isToday) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = Color.White,
            )
            Text(
                text = item.dateShortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.56f),
            )
        }

        // Weather icon + rain probability
        Row(
            modifier = Modifier.width(74.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WeatherConditionIcon(
                condition = condition,
                isDay = true,
                size = 24.dp,
            )
            if (item.precipitationProbabilityMaxPct >= 10) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = RainCyan,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "${item.precipitationProbabilityMaxPct}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = RainCyan,
                    )
                }
            }
        }

        // Min temperature
        Text(
            text = "${item.tempMinC.roundToInt()}°",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.70f),
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )

        Spacer(Modifier.width(10.dp))

        // Visual thermal range bar drawn in Compose Canvas
        TemperatureRangeBar(
            dayMin = item.tempMinC,
            dayMax = item.tempMaxC,
            weekMin = globalMin,
            weekMax = globalMax,
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
        )

        Spacer(Modifier.width(10.dp))

        // Max temperature
        Text(
            text = "${item.tempMaxC.roundToInt()}°",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
    }
}

@Composable
private fun TemperatureRangeBar(
    dayMin: Double,
    dayMax: Double,
    weekMin: Double,
    weekMax: Double,
    modifier: Modifier = Modifier,
) {
    val startColor = thermalColorForTemp(dayMin)
    val endColor = thermalColorForTemp(dayMax)

    Canvas(modifier = modifier) {
        val trackCorner = CornerRadius(size.height / 2f, size.height / 2f)

        // Full-week neutral background track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = trackCorner,
        )

        val span = (weekMax - weekMin).coerceAtLeast(1.0)
        val startFraction = ((dayMin - weekMin) / span).toFloat().coerceIn(0f, 1f)
        val endFraction = ((dayMax - weekMin) / span).toFloat().coerceIn(startFraction, 1f)

        val barLeft = size.width * startFraction
        val rawWidth = size.width * (endFraction - startFraction)
        val barWidth = rawWidth.coerceAtLeast(size.height * 1.4f)
            .coerceAtMost(size.width - barLeft)

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(startColor, endColor),
                startX = barLeft,
                endX = barLeft + barWidth,
            ),
            topLeft = Offset(barLeft, 0f),
            size = Size(barWidth, size.height),
            cornerRadius = trackCorner,
        )
    }
}

private fun thermalColorForTemp(tempC: Double): Color = when {
    tempC <= 0.0 -> TempFreezing
    tempC <= 8.0 -> TempCool
    tempC <= 16.0 -> TempMild
    tempC <= 24.0 -> TempWarm
    else -> TempHot
}
