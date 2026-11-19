package com.example.ui.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DataSourceStatus
import com.example.domain.model.HybridWeatherBundle
import com.example.ui.theme.SkyPalette
import com.example.ui.theme.SunAmber
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Hero Bento Card showcasing the selected Polish city, IMGW-PIB station attribution badge,
 * current temperature, dynamic condition icon, and feels-like / high-low summary.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeatherHeroSection(
    bundle: HybridWeatherBundle,
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    val activeLocale = LocalAppLanguage.current.toLocale()
    val todayDaily = bundle.openMeteo?.daily7d?.firstOrNull()
    val formattedTemp = bundle.effectiveTemperatureC?.let { temp ->
        if (bundle.imgwMeasurement?.temperatureC != null) {
            String.format(activeLocale, "%.1f°", temp)
        } else {
            "${temp.roundToInt()}°"
        }
    } ?: "—°"

    BentoGlassCard(
        palette = palette,
        highlighted = true,
        cornerRadius = 28.dp,
        contentPadding = 20.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_hero_card"),
    ) {
        // City header + voivodeship
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (bundle.location.isCurrentGps) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = bundle.location.name,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        if (!bundle.location.admin1.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = bundle.location.admin1,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
            )
        }

        Spacer(Modifier.height(10.dp))

        // IMGW-PIB Station Attribution Badge OR Resilient Fallback Badge
        when (val status = bundle.imgwStatus) {
            is DataSourceStatus.Available -> {
                val imgw = bundle.imgwMeasurement
                if (imgw != null) {
                    val distText = String.format(activeLocale, "%.1f", imgw.distanceKm)
                    StationAttributionPill(
                        text = appString(
                            R.string.imgw_station_badge,
                            imgw.stationName,
                            distText,
                            imgw.measurementHour,
                        ),
                        icon = Icons.Rounded.Sensors,
                        tint = palette.accent,
                        containerColor = Color.White.copy(alpha = 0.12f),
                        borderColor = palette.accent.copy(alpha = 0.40f),
                        testTag = "imgw_station_badge",
                    )
                }
            }
            is DataSourceStatus.Unavailable -> {
                StationAttributionPill(
                    text = appString(R.string.imgw_unavailable_badge),
                    icon = Icons.Rounded.CloudOff,
                    tint = SunAmber,
                    containerColor = SunAmber.copy(alpha = 0.14f),
                    borderColor = SunAmber.copy(alpha = 0.45f),
                    testTag = "imgw_unavailable_badge",
                )
            }
        }

        // Optional notice when Open-Meteo forecast failed while IMGW still succeeded
        if (bundle.openMeteoStatus is DataSourceStatus.Unavailable) {
            Spacer(Modifier.height(6.dp))
            StationAttributionPill(
                text = appString(R.string.open_meteo_unavailable_badge),
                icon = Icons.Rounded.Info,
                tint = SunAmber,
                containerColor = SunAmber.copy(alpha = 0.14f),
                borderColor = SunAmber.copy(alpha = 0.45f),
                testTag = "open_meteo_unavailable_badge",
            )
        }

        Spacer(Modifier.height(18.dp))

        // Main temperature & weather icon row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedTemp,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    modifier = Modifier.testTag("hero_temperature_text"),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = localizedConditionLabel(bundle.resolvedCondition),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }

            WeatherConditionIcon(
                condition = bundle.resolvedCondition,
                isDay = bundle.isDaytime,
                size = 82.dp,
                withHalo = true,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Secondary chips: Feels-like + Daily Min/Max
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bundle.openMeteo?.apparentTempC?.let { feelsLike ->
                HeroMetaChip(
                    label = appString(
                        R.string.metric_feels_like,
                        feelsLike.roundToInt().toString(),
                    ),
                )
            }
            if (todayDaily != null) {
                HeroMetaChip(
                    label = appString(
                        R.string.metric_high_low,
                        todayDaily.tempMaxC.roundToInt(),
                        todayDaily.tempMinC.roundToInt(),
                    ),
                )
            }
            bundle.imgwMeasurement?.measurementDate?.takeIf { it.isNotBlank() }?.let { date ->
                HeroMetaChip(label = "IMGW • $date")
            }
        }
    }
}

@Composable
private fun StationAttributionPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    containerColor: Color,
    borderColor: Color,
    testTag: String,
) {
    Surface(
        modifier = Modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.92f),
            )
        }
    }
}

@Composable
private fun HeroMetaChip(label: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.90f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
