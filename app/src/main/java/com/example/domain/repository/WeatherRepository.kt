package com.example.domain.repository

import com.example.domain.model.GeoLocation
import com.example.domain.model.HybridWeatherBundle

/**
 * Contract for hybrid weather data retrieval (IMGW-PIB + Open-Meteo) and Polish city geocoding.
 */
interface WeatherRepository {
    suspend fun fetchHybridWeather(location: GeoLocation): HybridWeatherBundle
    suspend fun searchLocations(query: String, languageCode: String = "pl"): Result<List<GeoLocation>>
}

/**
 * Explicit result union for device GPS lookup.
 */
sealed interface LocationLookupResult {
    data class Success(val location: GeoLocation) : LocationLookupResult
    data object PermissionMissing : LocationLookupResult
    data object LocationDisabled : LocationLookupResult
    data class Unavailable(val reason: String) : LocationLookupResult
}

/**
 * Contract for device geolocation and reverse geocoding.
 */
interface LocationGateway {
    fun hasLocationPermission(): Boolean
    suspend fun fetchCurrentLocation(languageCode: String = "pl"): LocationLookupResult
    suspend fun reverseGeocodeLocationName(
        latitude: Double,
        longitude: Double,
        languageCode: String,
    ): Pair<String, String?>?
}
