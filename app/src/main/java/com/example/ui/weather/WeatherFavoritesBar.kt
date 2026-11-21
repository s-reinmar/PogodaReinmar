package com.example.ui.weather

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.FavoriteCityAlert
import com.example.domain.model.GeoLocation
import com.example.ui.theme.SkyPalette
import com.example.ui.theme.SunAmber
import com.example.ui.theme.WarningLevel1Accent
import com.example.ui.theme.WarningLevel2Accent
import com.example.ui.theme.WarningLevel3Accent
import java.util.Locale

/**
 * Horizontal quick-switcher bar for user-pinned favorite cities persisted in Jetpack DataStore.
 * Displayed when at least one favorite city is pinned; displays a live warning dot on any Polish
 * favorite city that currently has an active IMGW alert.
 */
@Composable
fun WeatherFavoritesBar(
    favoriteCities: List<GeoLocation>,
    selectedLocation: GeoLocation,
    isSelectedFavorite: Boolean,
    favoriteCityAlerts: List<FavoriteCityAlert> = emptyList(),
    minAlertLevel: Int = 1,
    palette: SkyPalette,
    onSelectFavorite: (GeoLocation) -> Unit,
    onToggleCurrentFavorite: () -> Unit,
    onRemoveFavorite: (GeoLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (favoriteCities.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("favorites_bar"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("favorites_chips_row"),
            ) {
                items(
                    items = favoriteCities,
                    key = { it.stableId() },
                ) { city ->
                    val isSelected = city.matchesLocation(selectedLocation)
                    val activeAlertLevel = favoriteCityAlerts
                        .firstOrNull { it.city.matchesLocation(city) }
                        ?.warnings
                        ?.filter { it.level >= minAlertLevel }
                        ?.maxOfOrNull { it.level }
                    FavoriteCityChip(
                        city = city,
                        isSelected = isSelected,
                        activeAlertLevel = activeAlertLevel,
                        palette = palette,
                        onSelect = { onSelectFavorite(city) },
                        onRemove = { onRemoveFavorite(city) },
                        modifier = Modifier.animateItem(),
                    )
                }

                if (!isSelectedFavorite) {
                    item(key = "pin_current_chip") {
                        Surface(
                            onClick = onToggleCurrentFavorite,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .animateItem()
                                .testTag("favorites_quick_add_current"),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = appString(
                                        R.string.favorite_add_cd,
                                        selectedLocation.name,
                                    ),
                                    tint = palette.accent,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    text = selectedLocation.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.88f),
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
private fun FavoriteCityChip(
    city: GeoLocation,
    isSelected: Boolean,
    activeAlertLevel: Int?,
    palette: SkyPalette,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alertAccent = when (activeAlertLevel) {
        3 -> WarningLevel3Accent
        2 -> WarningLevel2Accent
        1 -> WarningLevel1Accent
        else -> null
    }
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            palette.accent.copy(alpha = 0.26f)
        } else {
            palette.cardContainer
        },
        animationSpec = spring(),
        label = "favoriteChipContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            alertAccent != null -> alertAccent.copy(alpha = 0.78f)
            isSelected -> palette.accent.copy(alpha = 0.85f)
            else -> palette.cardBorder
        },
        animationSpec = spring(),
        label = "favoriteChipBorder",
    )

    val selectCd = appString(R.string.favorite_chip_select_cd, city.name)
    val removeCd = appString(R.string.favorite_remove_cd, city.name)
    val chipTagSuffix = city.name.lowercase(Locale.US)
    val countryTag = city.countryCode?.uppercase(Locale.US)?.takeIf { it != "PL" }

    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("favorite_chip_$chipTagSuffix"),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = onSelect,
                    )
                    .semantics { contentDescription = selectCd }
                    .padding(start = 13.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = alertAccent ?: if (isSelected) SunAmber else SunAmber.copy(alpha = 0.72f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (countryTag != null) {
                    Text(
                        text = countryTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                }
            }

            Spacer(Modifier.width(2.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = onRemove,
                    )
                    .semantics { contentDescription = removeCd }
                    .testTag("favorite_remove_$chipTagSuffix"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
