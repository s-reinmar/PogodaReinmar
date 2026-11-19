package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.GeoLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.favoriteCitiesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pogoda_reinmar_favorites",
)

interface FavoriteCitiesGateway {
    val favoriteCities: Flow<List<GeoLocation>>
    suspend fun addFavorite(location: GeoLocation)
    suspend fun removeFavorite(location: GeoLocation)
    suspend fun toggleFavorite(location: GeoLocation)
}

@Serializable
private data class PersistedFavoriteCity(
    val name: String,
    val admin1: String? = null,
    val countryName: String? = null,
    val countryCode: String? = null,
    val timezoneId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val terytVoivodeshipCode: String? = null,
) {
    fun toDomain(): GeoLocation = GeoLocation(
        name = name,
        admin1 = admin1,
        countryName = countryName,
        countryCode = countryCode,
        timezoneId = timezoneId,
        latitude = latitude,
        longitude = longitude,
        isCurrentGps = false,
        terytVoivodeshipCode = terytVoivodeshipCode,
    )

    companion object {
        fun fromDomain(location: GeoLocation): PersistedFavoriteCity = PersistedFavoriteCity(
            name = location.name,
            admin1 = location.admin1,
            countryName = location.countryName,
            countryCode = location.countryCode,
            timezoneId = location.timezoneId,
            latitude = location.latitude,
            longitude = location.longitude,
            terytVoivodeshipCode = location.terytVoivodeshipCode,
        )
    }
}

/**
 * Persists user-pinned favorite cities as a JSON array inside Jetpack DataStore Preferences.
 */
class DataStoreFavoriteCitiesPreferences(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : FavoriteCitiesGateway {

    constructor(context: Context) : this(context.favoriteCitiesDataStore)

    private val favoritesKey = stringPreferencesKey("favorite_cities_json")

    override val favoriteCities: Flow<List<GeoLocation>> =
        dataStore.data.map { prefs ->
            decodeFavorites(prefs[favoritesKey])
        }

    override suspend fun addFavorite(location: GeoLocation) {
        val sanitized = location.copy(isCurrentGps = false)
        dataStore.edit { prefs ->
            val current = decodeFavorites(prefs[favoritesKey])
            if (current.none { it.matchesLocation(sanitized) }) {
                val updated = current + sanitized
                prefs[favoritesKey] = encodeFavorites(updated)
            }
        }
    }

    override suspend fun removeFavorite(location: GeoLocation) {
        dataStore.edit { prefs ->
            val current = decodeFavorites(prefs[favoritesKey])
            val updated = current.filterNot { it.matchesLocation(location) }
            prefs[favoritesKey] = encodeFavorites(updated)
        }
    }

    override suspend fun toggleFavorite(location: GeoLocation) {
        val sanitized = location.copy(isCurrentGps = false)
        dataStore.edit { prefs ->
            val current = decodeFavorites(prefs[favoritesKey])
            val exists = current.any { it.matchesLocation(sanitized) }
            val updated = if (exists) {
                current.filterNot { it.matchesLocation(sanitized) }
            } else {
                current + sanitized
            }
            prefs[favoritesKey] = encodeFavorites(updated)
        }
    }

    private fun decodeFavorites(raw: String?): List<GeoLocation> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<PersistedFavoriteCity>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private fun encodeFavorites(locations: List<GeoLocation>): String {
        val payload = locations.map { PersistedFavoriteCity.fromDomain(it) }
        return json.encodeToString(payload)
    }
}
