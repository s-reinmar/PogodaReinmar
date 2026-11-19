package com.example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.LanguagePreferencesGateway
import com.example.domain.model.AppLanguage
import com.example.domain.model.GeoLocation
import com.example.domain.repository.LocationGateway
import com.example.domain.repository.LocationLookupResult
import com.example.domain.repository.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * Unidirectional MVVM ViewModel orchestrating:
 * 1. Persisted in-app display language selection (PL / EN / DE).
 * 2. Startup GPS detection with safe fallback to Warsaw [52.2297, 21.0122].
 * 3. Debounced (300ms) Polish city search via Open-Meteo Geocoding in the selected language.
 * 4. Race-condition-safe parallel weather fetching (IMGW-PIB + Open-Meteo).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val locationGateway: LocationGateway,
    private val languagePreferences: LanguagePreferencesGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var weatherFetchJob: Job? = null
    private var gpsJob: Job? = null
    private var searchJob: Job? = null
    private var relocalizeJob: Job? = null

    init {
        observeSavedLanguage()
        observeDebouncedSearch()
    }

    private fun observeSavedLanguage() {
        languagePreferences.selectedLanguage
            .distinctUntilChanged()
            .onEach { language ->
                Locale.setDefault(language.toLocale())
                _uiState.update { state ->
                    state.copy(selectedLanguage = language)
                }
                relocalizeActiveLocationName(language)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Switches display language immediately, persists preference, and updates displayed city/region names.
     */
    fun onLanguageSelected(language: AppLanguage) {
        if (_uiState.value.selectedLanguage == language) return
        Locale.setDefault(language.toLocale())
        _uiState.update { state ->
            state.copy(selectedLanguage = language)
        }
        viewModelScope.launch {
            languagePreferences.setLanguage(language)
        }
        relocalizeActiveLocationName(language)

        // If user currently has search suggestions open, re-query in the new language
        val currentQuery = _uiState.value.search.query.trim()
        if (currentQuery.length >= 2) {
            executeLocationSearch(currentQuery, language.code)
        }
    }

    private fun relocalizeActiveLocationName(language: AppLanguage) {
        val currentLoc = _uiState.value.selectedLocation
        val isDefaultWarsaw = !currentLoc.isCurrentGps &&
            abs(currentLoc.latitude - GeoLocation.WARSAW.latitude) < 0.01 &&
            abs(currentLoc.longitude - GeoLocation.WARSAW.longitude) < 0.01

        if (isDefaultWarsaw) {
            val localizedWarsaw = language.localizedWarsaw()
            applyLocalizedLocationMetadata(localizedWarsaw)
            return
        }

        relocalizeJob?.cancel()
        relocalizeJob = viewModelScope.launch {
            val geocoded = locationGateway.reverseGeocodeLocationName(
                latitude = currentLoc.latitude,
                longitude = currentLoc.longitude,
                languageCode = language.code,
            )
            if (geocoded != null) {
                val updated = currentLoc.copy(
                    name = geocoded.first,
                    admin1 = geocoded.second ?: currentLoc.admin1,
                )
                applyLocalizedLocationMetadata(updated)
            }
        }
    }

    private fun applyLocalizedLocationMetadata(updatedLocation: GeoLocation) {
        _uiState.update { state ->
            val updatedContent = when (val content = state.contentState) {
                is WeatherContentState.Success -> content.copy(
                    bundle = content.bundle.copy(location = updatedLocation),
                )
                else -> content
            }
            state.copy(
                selectedLocation = updatedLocation,
                contentState = updatedContent,
            )
        }
    }

    /**
     * Called once the Compose screen reports the result of the runtime location permission flow.
     */
    fun onInitialPermissionEvaluated(granted: Boolean) {
        if (_uiState.value.gps.hasRequestedInitialPermission) return
        _uiState.update { state ->
            state.copy(gps = state.gps.copy(hasRequestedInitialPermission = true))
        }
        if (granted) {
            refreshGpsLocation()
        } else {
            fallbackToWarsaw(showNotice = true)
        }
    }

    /**
     * Triggered when the user taps the GPS refresh button (after permission check in UI).
     */
    fun refreshGpsLocation() {
        gpsJob?.cancel()
        gpsJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    gps = state.gps.copy(
                        isLocating = true,
                        showFallbackNotice = false,
                        isLocationDisabled = false,
                    ),
                )
            }

            val langCode = _uiState.value.selectedLanguage.code
            when (val result = locationGateway.fetchCurrentLocation(languageCode = langCode)) {
                is LocationLookupResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            gps = state.gps.copy(
                                isLocating = false,
                                showFallbackNotice = false,
                                isLocationDisabled = false,
                            ),
                        )
                    }
                    loadWeatherForLocation(result.location, showFullShimmer = true)
                }
                is LocationLookupResult.LocationDisabled -> {
                    _uiState.update { state ->
                        state.copy(
                            gps = state.gps.copy(
                                isLocating = false,
                                isLocationDisabled = true,
                            ),
                        )
                    }
                    if (_uiState.value.contentState !is WeatherContentState.Success) {
                        fallbackToWarsaw(showNotice = false)
                    }
                }
                is LocationLookupResult.PermissionMissing,
                is LocationLookupResult.Unavailable -> {
                    _uiState.update { state ->
                        state.copy(gps = state.gps.copy(isLocating = false))
                    }
                    val currentState = _uiState.value.contentState
                    if (currentState !is WeatherContentState.Success) {
                        fallbackToWarsaw(showNotice = true)
                    } else {
                        _uiState.update { state ->
                            state.copy(gps = state.gps.copy(showFallbackNotice = true))
                        }
                    }
                }
            }
        }
    }

    fun onPermissionDeniedOnGpsButton() {
        val currentState = _uiState.value.contentState
        if (currentState !is WeatherContentState.Success) {
            fallbackToWarsaw(showNotice = true)
        } else {
            _uiState.update { state ->
                state.copy(gps = state.gps.copy(showFallbackNotice = true))
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { state ->
            val openDropdown = newQuery.trim().length >= 2
            state.copy(
                search = state.search.copy(
                    query = newQuery,
                    isDropdownOpen = openDropdown,
                    isSearching = openDropdown,
                    errorMessage = null,
                    results = if (openDropdown) state.search.results else emptyList(),
                ),
            )
        }
        searchQueryFlow.value = newQuery
    }

    fun onClearSearch() {
        searchJob?.cancel()
        searchQueryFlow.value = ""
        _uiState.update { state ->
            state.copy(search = SearchUiState())
        }
    }

    fun onDismissSearchDropdown() {
        _uiState.update { state ->
            state.copy(search = state.search.copy(isDropdownOpen = false))
        }
    }

    fun onLocationSelected(location: GeoLocation) {
        searchJob?.cancel()
        searchQueryFlow.value = ""
        _uiState.update { state ->
            state.copy(
                search = SearchUiState(),
                gps = state.gps.copy(showFallbackNotice = false, isLocationDisabled = false),
            )
        }
        loadWeatherForLocation(location, showFullShimmer = true)
    }

    fun onRefreshWeather() {
        val currentLoc = _uiState.value.selectedLocation
        val hasExistingSuccess = _uiState.value.contentState is WeatherContentState.Success
        loadWeatherForLocation(currentLoc, showFullShimmer = !hasExistingSuccess)
    }

    private fun fallbackToWarsaw(showNotice: Boolean) {
        val localizedWarsaw = _uiState.value.selectedLanguage.localizedWarsaw()
        _uiState.update { state ->
            state.copy(
                gps = state.gps.copy(
                    isLocating = false,
                    showFallbackNotice = showNotice,
                ),
            )
        }
        loadWeatherForLocation(localizedWarsaw, showFullShimmer = true)
    }

    private fun loadWeatherForLocation(location: GeoLocation, showFullShimmer: Boolean) {
        weatherFetchJob?.cancel()
        weatherFetchJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    selectedLocation = location,
                    isRefreshing = !showFullShimmer,
                    contentState = if (showFullShimmer) WeatherContentState.Loading else state.contentState,
                )
            }

            val bundle = weatherRepository.fetchHybridWeather(location)
            _uiState.update { state ->
                val nextContent = if (bundle.hasAnyData) {
                    WeatherContentState.Success(bundle)
                } else {
                    WeatherContentState.Error("")
                }
                state.copy(
                    selectedLocation = location,
                    contentState = nextContent,
                    isRefreshing = false,
                )
            }
        }
    }

    private fun observeDebouncedSearch() {
        searchQueryFlow
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { rawQuery ->
                val query = rawQuery.trim()
                if (query.length < 2) {
                    searchJob?.cancel()
                    _uiState.update { state ->
                        state.copy(
                            search = state.search.copy(
                                isSearching = false,
                                isDropdownOpen = false,
                                results = emptyList(),
                                errorMessage = null,
                            ),
                        )
                    }
                    return@onEach
                }

                executeLocationSearch(query, _uiState.value.selectedLanguage.code)
            }
            .launchIn(viewModelScope)
    }

    private fun executeLocationSearch(query: String, languageCode: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val result = weatherRepository.searchLocations(
                query = query,
                languageCode = languageCode,
            )
            result.fold(
                onSuccess = { cities ->
                    _uiState.update { state ->
                        state.copy(
                            search = state.search.copy(
                                isSearching = false,
                                isDropdownOpen = true,
                                results = cities,
                                errorMessage = null,
                            ),
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        state.copy(
                            search = state.search.copy(
                                isSearching = false,
                                isDropdownOpen = true,
                                results = emptyList(),
                                errorMessage = err.localizedMessage ?: "",
                            ),
                        )
                    }
                },
            )
        }
    }

    class Factory(
        private val weatherRepository: WeatherRepository,
        private val locationGateway: LocationGateway,
        private val languagePreferences: LanguagePreferencesGateway,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return WeatherViewModel(weatherRepository, locationGateway, languagePreferences) as T
        }
    }
}
