package com.example.ui.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.catalog.ImgwStationCatalog
import com.example.domain.model.FavoriteCityAlert
import com.example.domain.model.GeoLocation
import com.example.domain.model.WeatherWarning
import com.example.presentation.AlertsUiState
import com.example.presentation.WeatherHubTab
import com.example.ui.theme.SkyPalette
import com.example.ui.theme.SunAmber
import com.example.ui.theme.WarningLevel1Accent
import com.example.ui.theme.WarningLevel2Accent
import com.example.ui.theme.WarningLevel3Accent
import java.util.Locale

/**
 * Aggregated inline glass Hub panel combining:
 * 1. Favorites Manager (quick city switch, pin current city, remove, live IMGW alert indicators).
 * 2. IMGW Alert Settings & Live Feed Checker (banner toggle, system notifications toggle with
 *    runtime permission handling, favorite cities monitoring, minimum severity filter, manual check).
 */
@Composable
fun WeatherFavoritesAndAlertsHub(
    activeTab: WeatherHubTab,
    favoriteCities: List<GeoLocation>,
    selectedLocation: GeoLocation,
    isSelectedFavorite: Boolean,
    alertsState: AlertsUiState,
    selectedWarnings: List<WeatherWarning>,
    otherFavoriteAlerts: List<FavoriteCityAlert>,
    palette: SkyPalette,
    onSelectTab: (WeatherHubTab) -> Unit,
    onSelectFavorite: (GeoLocation) -> Unit,
    onToggleCurrentFavorite: () -> Unit,
    onRemoveFavorite: (GeoLocation) -> Unit,
    onToggleBannerAlerts: (Boolean) -> Unit,
    onToggleNotificationAlerts: (Boolean) -> Unit,
    onToggleCheckFavoriteAlerts: (Boolean) -> Unit,
    onMinAlertLevelChanged: (Int) -> Unit,
    onCheckAlertsNow: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val relevantAlertsCount = selectedWarnings.size + otherFavoriteAlerts.sumOf { it.warnings.size }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("favorites_alerts_hub_panel"),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xEB0B1B32),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Segmented 2-tab switcher: Favorites | IMGW Alerts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HubTabPill(
                    title = appString(R.string.hub_tab_favorites),
                    countBadge = favoriteCities.size.takeIf { it > 0 },
                    badgeAccent = SunAmber,
                    icon = Icons.Rounded.Star,
                    iconTint = SunAmber,
                    selected = activeTab == WeatherHubTab.FAVORITES,
                    palette = palette,
                    testTag = "hub_tab_favorites",
                    onClick = { onSelectTab(WeatherHubTab.FAVORITES) },
                    modifier = Modifier.weight(1f),
                )
                HubTabPill(
                    title = appString(R.string.hub_tab_alerts),
                    countBadge = relevantAlertsCount.takeIf { it > 0 },
                    badgeAccent = WarningLevel2Accent,
                    icon = Icons.Rounded.NotificationsActive,
                    iconTint = if (relevantAlertsCount > 0) WarningLevel2Accent else palette.accent,
                    selected = activeTab == WeatherHubTab.ALERTS,
                    palette = palette,
                    testTag = "hub_tab_alerts",
                    onClick = { onSelectTab(WeatherHubTab.ALERTS) },
                    modifier = Modifier.weight(1f),
                )
            }

            when (activeTab) {
                WeatherHubTab.FAVORITES -> {
                    HubFavoritesTabContent(
                        favoriteCities = favoriteCities,
                        selectedLocation = selectedLocation,
                        isSelectedFavorite = isSelectedFavorite,
                        favoriteAlerts = alertsState.favoriteCityAlerts,
                        minLevel = alertsState.settings.minSeverityLevel,
                        palette = palette,
                        onSelectFavorite = onSelectFavorite,
                        onToggleCurrentFavorite = onToggleCurrentFavorite,
                        onRemoveFavorite = onRemoveFavorite,
                    )
                }

                WeatherHubTab.ALERTS -> {
                    HubAlertsTabContent(
                        alertsState = alertsState,
                        relevantAlertsCount = relevantAlertsCount,
                        palette = palette,
                        onToggleBannerAlerts = onToggleBannerAlerts,
                        onToggleNotificationAlerts = onToggleNotificationAlerts,
                        onToggleCheckFavoriteAlerts = onToggleCheckFavoriteAlerts,
                        onMinAlertLevelChanged = onMinAlertLevelChanged,
                        onCheckAlertsNow = onCheckAlertsNow,
                        onOpenAppSettings = onOpenAppSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun HubTabPill(
    title: String,
    countBadge: Int?,
    badgeAccent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    selected: Boolean,
    palette: SkyPalette,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) palette.accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) palette.accent.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.14f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (countBadge != null) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = badgeAccent.copy(alpha = 0.24f),
                ) {
                    Text(
                        text = countBadge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HubFavoritesTabContent(
    favoriteCities: List<GeoLocation>,
    selectedLocation: GeoLocation,
    isSelectedFavorite: Boolean,
    favoriteAlerts: List<FavoriteCityAlert>,
    minLevel: Int,
    palette: SkyPalette,
    onSelectFavorite: (GeoLocation) -> Unit,
    onToggleCurrentFavorite: () -> Unit,
    onRemoveFavorite: (GeoLocation) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isSelectedFavorite) {
            Surface(
                onClick = onToggleCurrentFavorite,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("hub_pin_current_city_button"),
                shape = RoundedCornerShape(14.dp),
                color = palette.accent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.50f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = appString(R.string.favorites_add_current, selectedLocation.name),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (favoriteCities.isEmpty()) {
            Text(
                text = appString(R.string.hub_favorites_empty),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        } else {
            favoriteCities.forEachIndexed { index, city ->
                val isSelected = city.matchesLocation(selectedLocation)
                val isPoland = ImgwStationCatalog.isPolishLocation(city)
                val activeCityAlert = favoriteAlerts
                    .firstOrNull { it.city.matchesLocation(city) }
                    ?.let { entry ->
                        val filtered = entry.warnings.filter { it.level >= minLevel }
                        if (filtered.isEmpty()) null else entry.copy(warnings = filtered)
                    }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(
                            role = Role.Button,
                            onClick = { onSelectFavorite(city) },
                        )
                        .padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp)
                        .testTag("hub_favorite_row_${city.name.lowercase(Locale.US)}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (isSelected) palette.accent else SunAmber,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        city.formattedSubtitle()?.let { subtitle ->
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.64f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (activeCityAlert != null) {
                        val badgeColor = severityAccentColor(activeCityAlert.maxLevel)
                        Surface(
                            shape = CircleShape,
                            color = badgeColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.65f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "${activeCityAlert.warnings.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Text(
                            text = if (isPoland) {
                                appString(R.string.search_source_imgw)
                            } else {
                                appString(R.string.search_source_open_meteo)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.50f),
                        )
                    }

                    val removeCd = appString(R.string.favorite_remove_cd, city.name)
                    IconButton(
                        onClick = { onRemoveFavorite(city) },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = removeCd }
                            .testTag("hub_favorite_remove_${city.name.lowercase(Locale.US)}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                if (index < favoriteCities.lastIndex) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
private fun HubAlertsTabContent(
    alertsState: AlertsUiState,
    relevantAlertsCount: Int,
    palette: SkyPalette,
    onToggleBannerAlerts: (Boolean) -> Unit,
    onToggleNotificationAlerts: (Boolean) -> Unit,
    onToggleCheckFavoriteAlerts: (Boolean) -> Unit,
    onMinAlertLevelChanged: (Int) -> Unit,
    onCheckAlertsNow: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val settings = alertsState.settings

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Live IMGW feed status summary & manual Check Now action
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (relevantAlertsCount > 0) {
                WarningLevel2Accent.copy(alpha = 0.14f)
            } else {
                Color.White.copy(alpha = 0.07f)
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (relevantAlertsCount > 0) {
                    WarningLevel2Accent.copy(alpha = 0.50f)
                } else {
                    Color.White.copy(alpha = 0.14f)
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hub_alerts_status_card"),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val statusText = when {
                    alertsState.checkError -> appString(R.string.hub_alerts_check_error)
                    relevantAlertsCount > 0 -> appString(
                        R.string.hub_alerts_status_active,
                        relevantAlertsCount,
                        alertsState.allPolandWarningsCount,
                    )
                    alertsState.allPolandWarningsCount > 0 -> appString(
                        R.string.hub_alerts_status_poland_other,
                        alertsState.allPolandWarningsCount,
                    )
                    else -> appString(R.string.hub_alerts_status_none)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                )

                if (alertsState.lastNotificationDelivered) {
                    Text(
                        text = appString(R.string.hub_alerts_notified_toast),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = alertsState.lastCheckedTimeLabel?.let {
                            appString(R.string.hub_alerts_last_checked, it)
                        }.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.56f),
                    )

                    Surface(
                        onClick = onCheckAlertsNow,
                        enabled = !alertsState.isChecking,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("check_imgw_alerts_button"),
                        shape = CircleShape,
                        color = palette.accent.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.65f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (alertsState.isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    color = palette.accent,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = if (alertsState.isChecking) {
                                    appString(R.string.hub_alerts_checking)
                                } else {
                                    appString(R.string.hub_alerts_check_now)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // Toggle 1: On-screen warning banner
        AlertSettingSwitchRow(
            label = appString(R.string.hub_alert_banner_toggle),
            checked = settings.bannerEnabled,
            accent = palette.accent,
            testTag = "alert_banner_switch",
            onCheckedChange = onToggleBannerAlerts,
        )

        // Toggle 2: Android system notifications
        AlertSettingSwitchRow(
            label = appString(R.string.hub_alert_notifications_toggle),
            checked = settings.notificationsEnabled,
            accent = palette.accent,
            testTag = "alert_notifications_switch",
            onCheckedChange = onToggleNotificationAlerts,
        )

        if (alertsState.notificationPermissionDenied) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = appString(R.string.hub_alert_permission_denied),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningLevel1Accent,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.testTag("alert_permission_settings_button"),
                ) {
                    Text(
                        text = appString(R.string.gps_action_settings),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accent,
                    )
                }
            }
        }

        // Toggle 3: Monitor Polish favorite cities
        AlertSettingSwitchRow(
            label = appString(R.string.hub_alert_favorites_toggle),
            checked = settings.checkFavoriteCities,
            accent = palette.accent,
            testTag = "alert_favorites_switch",
            onCheckedChange = onToggleCheckFavoriteAlerts,
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        // Minimum severity level selector (1+, 2+, 3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = appString(R.string.hub_alert_min_level_label),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1, 2, 3).forEach { level ->
                    val isSelected = settings.minSeverityLevel == level
                    val levelAccent = severityAccentColor(level)
                    val label = if (level == 3) {
                        appString(R.string.hub_alert_level_3_only)
                    } else {
                        appString(R.string.hub_alert_level_option, level)
                    }
                    Surface(
                        onClick = { onMinAlertLevelChanged(level) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("alert_min_level_$level"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            levelAccent.copy(alpha = 0.24f)
                        } else {
                            Color.White.copy(alpha = 0.07f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                levelAccent.copy(alpha = 0.85f)
                            } else {
                                Color.White.copy(alpha = 0.14f)
                            },
                        ),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) levelAccent else Color.White.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertSettingSwitchRow(
    label: String,
    checked: Boolean,
    accent: Color,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.90f),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF061527),
                checkedTrackColor = accent,
                uncheckedThumbColor = Color.White.copy(alpha = 0.78f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.16f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.28f),
            ),
        )
    }
}

private fun severityAccentColor(level: Int): Color = when (level) {
    3 -> WarningLevel3Accent
    2 -> WarningLevel2Accent
    else -> WarningLevel1Accent
}
