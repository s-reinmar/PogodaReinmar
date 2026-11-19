package com.example.ui.weather

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.DailyForecastItem
import com.example.domain.model.DataSourceStatus
import com.example.domain.model.GeoLocation
import com.example.domain.model.HourlyForecastItem
import com.example.domain.model.HybridWeatherBundle
import com.example.domain.model.ImgwMeasurement
import com.example.domain.model.OpenMeteoForecast
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherWarning
import com.example.presentation.GpsUiState
import com.example.presentation.SearchUiState
import com.example.presentation.WeatherContentState
import com.example.presentation.WeatherUiState
import com.example.presentation.WeatherViewModel
import com.example.ui.theme.PogodaPlTheme
import com.example.ui.theme.SkyPalette
import com.example.ui.theme.SkyPalettes

/**
 * Stateful entry screen connecting [WeatherViewModel] and runtime GPS permission launchers.
 */
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    hasLocationPermission: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val startupPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onInitialPermissionEvaluated(granted)
    }

    val manualGpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshGpsLocation()
        } else {
            viewModel.onPermissionDeniedOnGpsButton()
        }
    }

    LaunchedEffect(Unit) {
        if (!uiState.gps.hasRequestedInitialPermission) {
            if (hasLocationPermission()) {
                viewModel.onInitialPermissionEvaluated(granted = true)
            } else {
                startupPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ),
                )
            }
        }
    }

    val context = LocalContext.current

    WeatherScreenContent(
        uiState = uiState,
        onLanguageSelected = viewModel::onLanguageSelected,
        onQueryChange = viewModel::onSearchQueryChanged,
        onClearSearch = viewModel::onClearSearch,
        onSelectLocation = viewModel::onLocationSelected,
        onSelectFavorite = viewModel::onSelectFavorite,
        onToggleFavorite = viewModel::onToggleFavorite,
        onRemoveFavorite = viewModel::onRemoveFavorite,
        onGpsClick = {
            if (hasLocationPermission()) {
                viewModel.refreshGpsLocation()
            } else {
                manualGpsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ),
                )
            }
        },
        onOpenLocationSettings = {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        },
        onRefreshClick = viewModel::onRefreshWeather,
        modifier = modifier,
    )
}

/**
 * Pure stateless Compose UI for PogodaReinmar Bento Grid dashboard (used by production & @Preview).
 */
@Composable
fun WeatherScreenContent(
    uiState: WeatherUiState,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectLocation: (GeoLocation) -> Unit,
    onSelectFavorite: (GeoLocation) -> Unit = {},
    onToggleFavorite: (GeoLocation) -> Unit = {},
    onRemoveFavorite: (GeoLocation) -> Unit = {},
    onGpsClick: () -> Unit,
    onOpenLocationSettings: () -> Unit = {},
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette: SkyPalette = when (val content = uiState.contentState) {
        is WeatherContentState.Success -> SkyPalettes.resolve(
            condition = content.bundle.resolvedCondition,
            isDaytime = content.bundle.isDaytime,
        )
        else -> SkyPalettes.resolve(WeatherCondition.PARTLY_CLOUDY, isDaytime = true)
    }

    ProvideLocalizedAppContext(language = uiState.selectedLanguage) {
        WeatherAtmosphericBackground(
            palette = palette,
            modifier = modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val bentoColumns = if (maxWidth >= 600.dp) 3 else 2

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp)
                        .testTag("weather_dashboard_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Brand + language switcher + search bar + GPS + manual refresh header
                    item(key = "search_header") {
                        WeatherSearchHeader(
                            selectedLanguage = uiState.selectedLanguage,
                            searchState = uiState.search,
                            gpsState = uiState.gps,
                            favoriteCities = uiState.favoriteCities,
                            isRefreshing = uiState.isRefreshing,
                            palette = palette,
                            onLanguageSelected = onLanguageSelected,
                            onQueryChange = onQueryChange,
                            onClearSearch = onClearSearch,
                            onSelectLocation = onSelectLocation,
                            onToggleFavorite = onToggleFavorite,
                            onGpsClick = onGpsClick,
                            onOpenLocationSettings = onOpenLocationSettings,
                            onRefreshClick = onRefreshClick,
                        )
                    }

                    // Quick-switch Favorites bar (hidden while search dropdown is open)
                    if (!uiState.search.isDropdownOpen) {
                        item(key = "favorites_bar") {
                            WeatherFavoritesBar(
                                favoriteCities = uiState.favoriteCities,
                                selectedLocation = uiState.selectedLocation,
                                isSelectedFavorite = uiState.isSelectedLocationFavorite,
                                palette = palette,
                                onSelectFavorite = onSelectFavorite,
                                onToggleCurrentFavorite = {
                                    onToggleFavorite(uiState.selectedLocation)
                                },
                                onRemoveFavorite = onRemoveFavorite,
                            )
                        }
                    }

                    when (val content = uiState.contentState) {
                        is WeatherContentState.Loading -> {
                            item(key = "loading_skeleton") {
                                WeatherLoadingSkeleton(palette = palette)
                            }
                        }

                        is WeatherContentState.Error -> {
                            item(key = "error_card") {
                                WeatherErrorCard(
                                    message = content.message,
                                    palette = palette,
                                    onRetry = onRefreshClick,
                                )
                            }
                        }

                        is WeatherContentState.Success -> {
                            val bundle = content.bundle

                            // 1. IMGW Meteorological Warning Banner(s) at the very top when active
                            if (bundle.warnings.isNotEmpty()) {
                                items(
                                    items = bundle.warnings,
                                    key = { "warning_${it.id}" },
                                ) { warning ->
                                    ImgwWarningBannerCard(warning = warning)
                                }
                            }

                            // 2. Hero section (IMGW-PIB primary temperature & station badge + favorite toggle)
                            item(key = "hero_section") {
                                WeatherHeroSection(
                                    bundle = bundle,
                                    palette = palette,
                                    isFavorite = uiState.isSelectedLocationFavorite,
                                    onToggleFavorite = { onToggleFavorite(bundle.location) },
                                )
                            }

                            // 3. 24-hour Open-Meteo horizontal forecast
                            val hourly = bundle.openMeteo?.hourly24h.orEmpty()
                            if (hourly.isNotEmpty()) {
                                item(key = "hourly_section") {
                                    HourlyForecastSection(
                                        items = hourly,
                                        palette = palette,
                                    )
                                }
                            }

                            // 4. 7-day Open-Meteo forecast with thermal range bar
                            val daily = bundle.openMeteo?.daily7d.orEmpty()
                            if (daily.isNotEmpty()) {
                                item(key = "daily_section") {
                                    DailyForecastSection(
                                        days = daily,
                                        palette = palette,
                                    )
                                }
                            }

                            // 5. Hybrid Bento Grid detail tiles (IMGW-PIB + Open-Meteo)
                            item(key = "details_bento_grid") {
                                WeatherDetailsBentoGrid(
                                    bundle = bundle,
                                    palette = palette,
                                    columns = bentoColumns,
                                )
                            }

                            // 6. Subtle attribution footer
                            item(key = "source_footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (bundle.imgwStatus is DataSourceStatus.NotApplicable) {
                                            appString(R.string.source_footer_international)
                                        } else {
                                            appString(R.string.source_footer)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.54f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherErrorCard(
    message: String,
    palette: SkyPalette,
    onRetry: () -> Unit,
) {
    BentoGlassCard(
        palette = palette,
        highlighted = true,
        contentPadding = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_error_card"),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = appString(R.string.error_both_failed_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message.ifBlank { appString(R.string.error_both_failed_desc) },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = Color(0xFF061527),
                ),
                modifier = Modifier.testTag("retry_weather_button"),
            ) {
                Text(
                    text = appString(R.string.action_retry),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// =========================================================================
// Jetpack Compose Previews
// =========================================================================

private fun samplePreviewBundle(
    isDaytime: Boolean,
    includeWarning: Boolean,
    imgwOnline: Boolean,
): HybridWeatherBundle {
    val imgw = if (imgwOnline) {
        ImgwMeasurement(
            stationId = "12375",
            stationName = "Warszawa",
            distanceKm = 4.2,
            measurementDate = "2026-11-19",
            measurementHour = "14",
            temperatureC = 11.4,
            windSpeedMs = 5.0,
            windSpeedKmh = 18.0,
            windDirectionDeg = 235f,
            relativeHumidityPct = 68.0,
            precipitationMm = 0.2,
            pressureHpa = 1016.4,
        )
    } else {
        null
    }

    val warnings = if (includeWarning) {
        listOf(
            WeatherWarning(
                id = "w1",
                eventName = "Silny wiatr",
                level = 2,
                probabilityPct = 85,
                validFrom = "19.11, 18:00",
                validTo = "20.11, 09:00",
                description = "Prognozuje się wystąpienie silnego wiatru o średniej prędkości do 45 km/h, w porywach do 85 km/h, z zachodu.",
                comment = null,
                office = "Centralne Biuro Prognoz Meteorologicznych w Warszawie",
                terytCodes = listOf("1465"),
            ),
        )
    } else {
        emptyList()
    }

    val openMeteo = OpenMeteoForecast(
        currentTempC = 11.0,
        apparentTempC = 9.2,
        currentWeatherCode = if (isDaytime) 2 else 61,
        isDayNow = isDaytime,
        currentHumidityPct = 71.0,
        currentPressureHpa = 1015.8,
        currentWindSpeedKmh = 19.4,
        currentWindDirectionDeg = 240f,
        currentPrecipitationMm = 0.4,
        todaySunriseLabel = "07:04",
        todaySunsetLabel = "15:39",
        todaySunriseIso = "2026-11-19T07:04",
        todaySunsetIso = "2026-11-19T15:39",
        todayUvIndexMax = 2.4,
        hourly24h = listOf(
            HourlyForecastItem("2026-11-19T14:00", "Teraz", 11.4, 2, 15, isDaytime, true),
            HourlyForecastItem("2026-11-19T15:00", "15:00", 10.8, 2, 20, isDaytime, false),
            HourlyForecastItem("2026-11-19T16:00", "16:00", 9.6, 3, 35, false, false),
            HourlyForecastItem("2026-11-19T17:00", "17:00", 8.9, 61, 65, false, false),
            HourlyForecastItem("2026-11-19T18:00", "18:00", 8.1, 61, 70, false, false),
            HourlyForecastItem("2026-11-19T19:00", "19:00", 7.5, 3, 25, false, false),
        ),
        daily7d = listOf(
            DailyForecastItem("2026-11-19", "Dziś", "19.11", 2, 5.0, 12.0, 35, 2.4, "2026-11-19T07:04", "2026-11-19T15:39"),
            DailyForecastItem("2026-11-20", "Jutro", "20.11", 61, 4.0, 9.5, 80, 1.5, null, null),
            DailyForecastItem("2026-11-21", "Sob.", "21.11", 1, 2.0, 10.2, 10, 2.8, null, null),
            DailyForecastItem("2026-11-22", "Niedz.", "22.11", 0, 1.0, 11.8, 5, 3.1, null, null),
            DailyForecastItem("2026-11-23", "Pon.", "23.11", 3, 3.0, 8.4, 25, 1.8, null, null),
            DailyForecastItem("2026-11-24", "Wt.", "24.11", 71, -1.0, 3.5, 70, 1.1, null, null),
            DailyForecastItem("2026-11-25", "Śr.", "25.11", 2, -2.0, 4.8, 15, 1.9, null, null),
        ),
    )

    return HybridWeatherBundle(
        location = GeoLocation.WARSAW.copy(isCurrentGps = true),
        imgwMeasurement = imgw,
        imgwStatus = if (imgwOnline) DataSourceStatus.Available else DataSourceStatus.Unavailable("Timeout"),
        warnings = warnings,
        openMeteo = openMeteo,
        openMeteoStatus = DataSourceStatus.Available,
        isDaytime = isDaytime,
        resolvedCondition = if (isDaytime) WeatherCondition.PARTLY_CLOUDY else WeatherCondition.RAIN,
        effectiveTemperatureC = imgw?.temperatureC ?: openMeteo.currentTempC,
        effectiveWindSpeedKmh = imgw?.windSpeedKmh ?: openMeteo.currentWindSpeedKmh,
        effectiveWindSpeedMs = imgw?.windSpeedMs ?: openMeteo.currentWindSpeedKmh?.div(3.6),
        effectiveWindDirectionDeg = imgw?.windDirectionDeg ?: openMeteo.currentWindDirectionDeg,
        effectivePressureHpa = imgw?.pressureHpa ?: openMeteo.currentPressureHpa,
        effectiveHumidityPct = imgw?.relativeHumidityPct ?: openMeteo.currentHumidityPct,
        effectivePrecipitationMm = imgw?.precipitationMm ?: openMeteo.currentPrecipitationMm,
        sunriseProgress = 0.62f,
    )
}

@Preview(showBackground = true, heightDp = 960, name = "Pogoda PL — Dzień (IMGW + Open-Meteo)")
@Composable
private fun WeatherScreenDayPreview() {
    PogodaPlTheme {
        WeatherScreenContent(
            uiState = WeatherUiState(
                selectedLocation = GeoLocation.WARSAW,
                favoriteCities = listOf(
                    GeoLocation.WARSAW,
                    GeoLocation(
                        name = "Kraków",
                        admin1 = "Województwo małopolskie",
                        countryName = "Polska",
                        countryCode = "PL",
                        latitude = 50.0647,
                        longitude = 19.9450,
                    ),
                    GeoLocation(
                        name = "Tokio",
                        countryName = "Japonia",
                        countryCode = "JP",
                        timezoneId = "Asia/Tokyo",
                        latitude = 35.6762,
                        longitude = 139.6503,
                    ),
                ),
                contentState = WeatherContentState.Success(
                    samplePreviewBundle(isDaytime = true, includeWarning = false, imgwOnline = true),
                ),
            ),
            onQueryChange = {},
            onClearSearch = {},
            onSelectLocation = {},
            onGpsClick = {},
            onRefreshClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 960, name = "Pogoda PL — Noc + Alert IMGW + Fallback")
@Composable
private fun WeatherScreenNightWarningPreview() {
    PogodaPlTheme {
        WeatherScreenContent(
            uiState = WeatherUiState(
                selectedLocation = GeoLocation.WARSAW,
                contentState = WeatherContentState.Success(
                    samplePreviewBundle(isDaytime = false, includeWarning = true, imgwOnline = false),
                ),
                gps = GpsUiState(showFallbackNotice = false),
                search = SearchUiState(),
            ),
            onQueryChange = {},
            onClearSearch = {},
            onSelectLocation = {},
            onGpsClick = {},
            onRefreshClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 840, name = "Pogoda PL — Shimmer Loading")
@Composable
private fun WeatherScreenLoadingPreview() {
    PogodaPlTheme {
        WeatherScreenContent(
            uiState = WeatherUiState(contentState = WeatherContentState.Loading),
            onQueryChange = {},
            onClearSearch = {},
            onSelectLocation = {},
            onGpsClick = {},
            onRefreshClick = {},
        )
    }
}
