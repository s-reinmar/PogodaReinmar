package com.example.ui.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.HourlyForecastItem
import com.example.domain.util.WeatherCodeMapper
import com.example.ui.theme.RainCyan
import com.example.ui.theme.SkyPalette
import kotlin.math.roundToInt

/**
 * Horizontal 24-hour Open-Meteo forecast strip inside a Bento glass card.
 */
@Composable
fun HourlyForecastSection(
    items: List<HourlyForecastItem>,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    BentoGlassCard(
        palette = palette,
        contentPadding = 16.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_forecast_section"),
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
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = appString(R.string.section_hourly_title),
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

        Spacer(Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("hourly_forecast_row"),
        ) {
            items(
                items = items,
                key = { it.isoTime },
            ) { item ->
                HourlyChip(item = item, palette = palette)
            }
        }
    }
}

@Composable
private fun HourlyChip(
    item: HourlyForecastItem,
    palette: SkyPalette,
) {
    val condition = WeatherCodeMapper.fromWmoCode(item.weatherCode)
    val bgColor = if (item.isCurrentHour) {
        palette.accent.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val borderColor = if (item.isCurrentHour) {
        palette.accent.copy(alpha = 0.70f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.width(68.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (item.isCurrentHour) appString(R.string.time_now) else item.hourLabel,
                style = if (item.isCurrentHour) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = if (item.isCurrentHour) palette.accent else Color.White.copy(alpha = 0.82f),
            )

            WeatherConditionIcon(
                condition = condition,
                isDay = item.isDay,
                size = 26.dp,
            )

            Text(
                text = "${item.temperatureC.roundToInt()}°",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = if (item.precipitationProbabilityPct >= 20) {
                        RainCyan
                    } else {
                        Color.White.copy(alpha = 0.32f)
                    },
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = "${item.precipitationProbabilityPct}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.precipitationProbabilityPct >= 20) {
                        RainCyan
                    } else {
                        Color.White.copy(alpha = 0.50f)
                    },
                )
            }
        }
    }
}
