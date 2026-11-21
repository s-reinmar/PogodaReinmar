package com.example.presentation

import com.example.domain.model.AppLanguage
import com.example.domain.model.FavoriteCityAlert
import com.example.domain.model.GeoLocation
import com.example.domain.model.HybridWeatherBundle
import com.example.domain.model.WeatherAlertSettings
import com.example.domain.model.WeatherWarning

/**
 * Explicit sealed finite-state model for the main weather content area.
 */
sealed interface WeatherContentState {
    data object Loading : WeatherContentState
    data class Success(val bundle: HybridWeatherBundle) : WeatherContentState
    data class Error(val message: String) : WeatherContentState
}

/**
 * Search bar & debounced Open-Meteo geocoding dropdown state.
 */
data class SearchUiState(
    val query: String = "",
    val isDropdownOpen: Boolean = false,
    val isSearching: Boolean = false,
    val results: List<GeoLocation> = emptyList(),
    val errorMessage: String? = null,
)

/**
 * GPS location acquisition state.
 */
data class GpsUiState(
    val isLocating: Boolean = false,
    val showFallbackNotice: Boolean = false,
    val isLocationDisabled: Boolean = false,
    val hasRequestedInitialPermission: Boolean = false,
)

/**
 * Tabs inside the aggregated Favorites & Alert Settings Hub panel.
 */
enum class WeatherHubTab {
    FAVORITES,
    ALERTS,
}

/**
 * IMGW meteorological alert monitoring state & persisted preferences.
 */
data class AlertsUiState(
    val settings: WeatherAlertSettings = WeatherAlertSettings(),
    val isChecking: Boolean = false,
    val lastCheckedTimeLabel: String? = null,
    val checkError: Boolean = false,
    val allPolandWarningsCount: Int = 0,
    val favoriteCityAlerts: List<FavoriteCityAlert> = emptyList(),
    val notificationPermissionDenied: Boolean = false,
    val lastNotificationDelivered: Boolean = false,
)

/**
 * Single immutable source of truth rendered by WeatherScreen.
 */
data class WeatherUiState(
    val selectedLanguage: AppLanguage = AppLanguage.defaultForSystem(),
    val selectedLocation: GeoLocation = GeoLocation.WARSAW,
    val favoriteCities: List<GeoLocation> = emptyList(),
    val contentState: WeatherContentState = WeatherContentState.Loading,
    val isRefreshing: Boolean = false,
    val search: SearchUiState = SearchUiState(),
    val gps: GpsUiState = GpsUiState(),
    val alerts: AlertsUiState = AlertsUiState(),
) {
    val isSelectedLocationFavorite: Boolean
        get() = favoriteCities.any { it.matchesLocation(selectedLocation) }

    /**
     * Warnings for the currently displayed location filtered by the user's minimum severity threshold.
     */
    val visibleSelectedLocationWarnings: List<WeatherWarning>
        get() {
            val content = contentState as? WeatherContentState.Success ?: return emptyList()
            val minLevel = alerts.settings.minSeverityLevel
            return content.bundle.warnings.filter { it.level >= minLevel }
        }

    /**
     * Active alerts affecting *other* pinned Polish favorite cities (excluding the currently viewed city).
     */
    val otherFavoriteCityAlerts: List<FavoriteCityAlert>
        get() {
            if (!alerts.settings.checkFavoriteCities) return emptyList()
            val minLevel = alerts.settings.minSeverityLevel
            return alerts.favoriteCityAlerts.mapNotNull { entry ->
                if (entry.city.matchesLocation(selectedLocation)) return@mapNotNull null
                val filtered = entry.warnings.filter { it.level >= minLevel }
                if (filtered.isEmpty()) null else entry.copy(warnings = filtered)
            }
        }

    /**
     * Total number of relevant active IMGW warnings across the selected city and monitored favorites.
     */
    val totalRelevantActiveWarningsCount: Int
        get() = visibleSelectedLocationWarnings.size +
            otherFavoriteCityAlerts.sumOf { it.warnings.size }
}
