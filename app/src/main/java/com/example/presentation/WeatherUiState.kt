package com.example.presentation

import com.example.domain.model.AppLanguage
import com.example.domain.model.GeoLocation
import com.example.domain.model.HybridWeatherBundle

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
 * Single immutable source of truth rendered by WeatherScreen.
 */
data class WeatherUiState(
    val selectedLanguage: AppLanguage = AppLanguage.defaultForSystem(),
    val selectedLocation: GeoLocation = GeoLocation.WARSAW,
    val contentState: WeatherContentState = WeatherContentState.Loading,
    val isRefreshing: Boolean = false,
    val search: SearchUiState = SearchUiState(),
    val gps: GpsUiState = GpsUiState(),
)
