package com.example.ui.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.catalog.ImgwStationCatalog
import com.example.domain.model.AppLanguage
import com.example.domain.model.GeoLocation
import com.example.presentation.GpsUiState
import com.example.presentation.SearchUiState
import com.example.ui.theme.SkyPalette
import com.example.ui.theme.SunAmber
import java.util.Locale
import kotlin.math.abs

/**
 * Top brand & controls header (PogodaReinmar title, inline expandable PL/EN/DE display language
 * switcher without separate PopupWindow surfaces, GPS trigger, manual refresh) paired with a
 * full-width glass Polish city search bar.
 */
@Composable
fun WeatherSearchHeader(
    selectedLanguage: AppLanguage,
    searchState: SearchUiState,
    gpsState: GpsUiState,
    favoriteCities: List<GeoLocation> = emptyList(),
    isRefreshing: Boolean,
    palette: SkyPalette,
    onLanguageSelected: (AppLanguage) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectLocation: (GeoLocation) -> Unit,
    onToggleFavorite: (GeoLocation) -> Unit = {},
    onGpsClick: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var languageBarExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Row 1: Brand name + Language Selector + GPS + Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = appString(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )

            // Display language switcher pill (toggles inline glass language bar)
            val selectorCd = appString(R.string.language_selector_cd)
            Surface(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = selectorCd }
                    .testTag("language_selector_button"),
                shape = CircleShape,
                color = if (languageBarExpanded) palette.cardHighlight else palette.cardContainer,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (languageBarExpanded) palette.accent.copy(alpha = 0.7f) else palette.cardBorder,
                ),
                onClick = {
                    focusManager.clearFocus()
                    languageBarExpanded = !languageBarExpanded
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = selectedLanguage.shortTag,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = if (languageBarExpanded) {
                            Icons.Rounded.KeyboardArrowUp
                        } else {
                            Icons.Rounded.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // GPS Locate button (minimum 48.dp touch target)
            Surface(
                shape = CircleShape,
                color = palette.cardContainer,
                border = BorderStroke(1.dp, palette.cardBorder),
            ) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        languageBarExpanded = false
                        onGpsClick()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("gps_refresh_button"),
                ) {
                    if (gpsState.isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            color = palette.accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = appString(R.string.gps_refresh_cd),
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }

            // Manual data refresh button (minimum 48.dp touch target)
            Surface(
                shape = CircleShape,
                color = palette.cardContainer,
                border = BorderStroke(1.dp, palette.cardBorder),
            ) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        languageBarExpanded = false
                        onRefreshClick()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("weather_refresh_button"),
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            color = palette.accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = appString(R.string.manual_refresh_cd),
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
        }

        // Inline animated language selector bar (no PopupWindow / BLASTBufferQueue teardown)
        AnimatedVisibility(
            visible = languageBarExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            InlineLanguageSelectorBar(
                selectedLanguage = selectedLanguage,
                palette = palette,
                onLanguageSelected = { language ->
                    languageBarExpanded = false
                    onLanguageSelected(language)
                },
            )
        }

        // Row 2: Full-width spacious glass city search input
        OutlinedTextField(
            value = searchState.query,
            onValueChange = {
                languageBarExpanded = false
                onQueryChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("city_search_input"),
            singleLine = true,
            placeholder = {
                Text(
                    text = appString(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.68f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = appString(R.string.search_icon_cd),
                    tint = Color.White.copy(alpha = 0.85f),
                )
            },
            trailingIcon = {
                if (searchState.query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onClearSearch()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.testTag("search_clear_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = appString(R.string.search_clear_cd),
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.cardHighlight,
                unfocusedContainerColor = palette.cardContainer,
                focusedBorderColor = Color.White.copy(alpha = 0.65f),
                unfocusedBorderColor = palette.cardBorder,
                cursorColor = palette.accent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )

        // Animated search results dropdown
        AnimatedVisibility(
            visible = searchState.isDropdownOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_results_dropdown"),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xEC0B1A30),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when {
                        searchState.isSearching -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = palette.accent,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = appString(R.string.search_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                            }
                        }

                        searchState.errorMessage != null -> {
                            Text(
                                text = searchState.errorMessage.ifBlank {
                                    appString(R.string.search_error_generic)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp),
                            )
                        }

                        searchState.results.isEmpty() -> {
                            Text(
                                text = appString(R.string.search_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.padding(16.dp),
                            )
                        }

                        else -> {
                            searchState.results.take(6).forEachIndexed { index, city ->
                                val isFavorite = favoriteCities.any { it.matchesLocation(city) }
                                SearchResultRow(
                                    city = city,
                                    isFavorite = isFavorite,
                                    accentColor = palette.accent,
                                    onClick = {
                                        focusManager.clearFocus()
                                        onSelectLocation(city)
                                    },
                                    onToggleFavorite = { onToggleFavorite(city) },
                                )
                                if (index < searchState.results.take(6).lastIndex) {
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.10f),
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Actionable GPS notice when permission was declined or device GPS is turned off
        val showGpsBanner = (gpsState.showFallbackNotice || gpsState.isLocationDisabled) &&
            !searchState.isDropdownOpen
        AnimatedVisibility(visible = showGpsBanner) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gps_status_banner"),
                shape = RoundedCornerShape(16.dp),
                color = palette.cardContainer,
                border = BorderStroke(1.dp, palette.cardBorder),
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (gpsState.isLocationDisabled) {
                            appString(R.string.gps_disabled_notice)
                        } else {
                            appString(R.string.gps_fallback_notice)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = if (gpsState.isLocationDisabled) onOpenLocationSettings else onGpsClick,
                        modifier = Modifier.testTag("gps_banner_action_button"),
                    ) {
                        Text(
                            text = if (gpsState.isLocationDisabled) {
                                appString(R.string.gps_action_settings)
                            } else {
                                appString(R.string.gps_action_enable)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineLanguageSelectorBar(
    selectedLanguage: AppLanguage,
    palette: SkyPalette,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("language_selector_menu"),
        shape = RoundedCornerShape(20.dp),
        color = palette.cardHighlight,
        border = BorderStroke(1.dp, palette.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = appString(R.string.language_menu_title),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppLanguage.entries.forEach { language ->
                    val isSelected = language == selectedLanguage
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("language_option_${language.code}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            palette.accent.copy(alpha = 0.24f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                palette.accent.copy(alpha = 0.78f)
                            } else {
                                Color.White.copy(alpha = 0.14f)
                            },
                        ),
                        onClick = { onLanguageSelected(language) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
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
private fun SearchResultRow(
    city: GeoLocation,
    isFavorite: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val isPoland = ImgwStationCatalog.isPolishLocation(city)
    val subtitle = city.formattedSubtitle()
    val latHemisphere = if (city.latitude >= 0.0) "N" else "S"
    val lonHemisphere = if (city.longitude >= 0.0) "E" else "W"

    val countryTagSuffix = city.countryCode?.lowercase(Locale.US)?.let { "_$it" }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
            .testTag("search_result_${city.name.lowercase(Locale.US)}$countryTagSuffix"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = if (isPoland) accentColor else Color.White.copy(alpha = 0.78f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = city.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Surface(
                shape = CircleShape,
                color = if (isPoland) {
                    accentColor.copy(alpha = 0.20f)
                } else {
                    Color.White.copy(alpha = 0.12f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isPoland) {
                        accentColor.copy(alpha = 0.55f)
                    } else {
                        Color.White.copy(alpha = 0.22f)
                    },
                ),
            ) {
                Text(
                    text = if (isPoland) {
                        appString(R.string.search_source_imgw)
                    } else {
                        appString(R.string.search_source_open_meteo)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPoland) accentColor else Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "%.2f°%s, %.2f°%s",
                    abs(city.latitude),
                    latHemisphere,
                    abs(city.longitude),
                    lonHemisphere,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.52f),
            )
        }
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(48.dp)
                .testTag("search_favorite_toggle_${city.name.lowercase(Locale.US)}$countryTagSuffix"),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = if (isFavorite) {
                    appString(R.string.favorite_remove_cd, city.name)
                } else {
                    appString(R.string.favorite_add_cd, city.name)
                },
                tint = if (isFavorite) SunAmber else Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(21.dp),
            )
        }
    }
}
