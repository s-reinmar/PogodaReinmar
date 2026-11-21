package com.example.ui.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.WeatherWarning
import com.example.ui.theme.WarningLevel1Accent
import com.example.ui.theme.WarningLevel1Bg
import com.example.ui.theme.WarningLevel1Border
import com.example.ui.theme.WarningLevel2Accent
import com.example.ui.theme.WarningLevel2Bg
import com.example.ui.theme.WarningLevel2Border
import com.example.ui.theme.WarningLevel3Accent
import com.example.ui.theme.WarningLevel3Bg
import com.example.ui.theme.WarningLevel3Border

/**
 * Prominent IMGW-PIB meteorological warning card displayed at the top of the dashboard.
 */
@Composable
fun ImgwWarningBannerCard(
    warning: WeatherWarning,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(warning.id) { mutableStateOf(false) }

    val (bgColor, borderColor, accentColor) = when (warning.level) {
        3 -> Triple(WarningLevel3Bg, WarningLevel3Border, WarningLevel3Accent)
        2 -> Triple(WarningLevel2Bg, WarningLevel2Border, WarningLevel2Accent)
        else -> Triple(WarningLevel1Bg, WarningLevel1Border, WarningLevel1Accent)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("imgw_warning_banner_${warning.level}"),
        shape = RoundedCornerShape(22.dp),
        color = bgColor.copy(alpha = 0.90f),
        border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.85f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = borderColor.copy(alpha = 0.22f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = appString(R.string.warning_icon_cd),
                        tint = accentColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = borderColor.copy(alpha = 0.25f),
                        ) {
                            Text(
                                text = appString(R.string.warning_level_prefix, warning.level),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        if (warning.probabilityPct != null) {
                            Text(
                                text = "${warning.probabilityPct}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = warning.eventName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = appString(
                            R.string.warning_validity,
                            warning.validFrom,
                            warning.validTo,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.80f),
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = appString(R.string.warning_expand_cd),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = warning.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                    if (!warning.comment.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = warning.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor.copy(alpha = 0.9f),
                        )
                    }
                    if (!warning.office.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = warning.office,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact banner notifying the user about active IMGW warnings affecting their pinned Polish
 * favorite cities (other than the currently viewed city), with 1-tap navigation to each city.
 */
@Composable
fun FavoriteCitiesAlertBanner(
    favoriteAlerts: List<com.example.domain.model.FavoriteCityAlert>,
    onSelectCity: (com.example.domain.model.GeoLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (favoriteAlerts.isEmpty()) return

    val maxLevel = favoriteAlerts.maxOfOrNull { it.maxLevel } ?: 1
    val (bgColor, borderColor, accentColor) = when (maxLevel) {
        3 -> Triple(WarningLevel3Bg, WarningLevel3Border, WarningLevel3Accent)
        2 -> Triple(WarningLevel2Bg, WarningLevel2Border, WarningLevel2Accent)
        else -> Triple(WarningLevel1Bg, WarningLevel1Border, WarningLevel1Accent)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("favorite_cities_alert_banner"),
        shape = RoundedCornerShape(20.dp),
        color = bgColor.copy(alpha = 0.86f),
        border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.80f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = appString(R.string.warning_icon_cd),
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = appString(R.string.favorite_alerts_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                )
            }

            favoriteAlerts.forEach { entry ->
                val topWarning = entry.warnings.maxByOrNull { it.level } ?: return@forEach
                Surface(
                    onClick = { onSelectCity(entry.city) },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorite_alert_jump_${entry.city.name.lowercase()}"),
                ) {
                    Text(
                        text = appString(
                            R.string.favorite_alerts_banner_item,
                            entry.city.name,
                            topWarning.level,
                            topWarning.eventName,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
