package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.WeatherAlertSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.alertSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pogoda_reinmar_alert_settings",
)

interface AlertPreferencesGateway {
    val settings: Flow<WeatherAlertSettings>
    val notifiedWarningIds: Flow<Set<String>>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setBannerEnabled(enabled: Boolean)
    suspend fun setMinSeverityLevel(level: Int)
    suspend fun setCheckFavoriteCities(enabled: Boolean)
    suspend fun markWarningsNotified(ids: Set<String>)
    suspend fun clearNotifiedHistory()
}

/**
 * Persists IMGW meteorological alert preferences and deduplicated notification history
 * via Jetpack DataStore Preferences.
 */
class DataStoreAlertPreferences(
    private val dataStore: DataStore<Preferences>,
) : AlertPreferencesGateway {

    constructor(context: Context) : this(context.alertSettingsDataStore)

    private val notificationsEnabledKey = booleanPreferencesKey("alerts_notifications_enabled")
    private val bannerEnabledKey = booleanPreferencesKey("alerts_banner_enabled")
    private val minSeverityLevelKey = intPreferencesKey("alerts_min_severity_level")
    private val checkFavoritesKey = booleanPreferencesKey("alerts_check_favorites")
    private val notifiedIdsKey = stringSetPreferencesKey("alerts_notified_ids")

    override val settings: Flow<WeatherAlertSettings> =
        dataStore.data.map { prefs ->
            WeatherAlertSettings(
                notificationsEnabled = prefs[notificationsEnabledKey] ?: true,
                bannerEnabled = prefs[bannerEnabledKey] ?: true,
                minSeverityLevel = (prefs[minSeverityLevelKey] ?: 1).coerceIn(1, 3),
                checkFavoriteCities = prefs[checkFavoritesKey] ?: true,
            )
        }

    override val notifiedWarningIds: Flow<Set<String>> =
        dataStore.data.map { prefs ->
            prefs[notifiedIdsKey] ?: emptySet()
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[notificationsEnabledKey] = enabled
        }
    }

    override suspend fun setBannerEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[bannerEnabledKey] = enabled
        }
    }

    override suspend fun setMinSeverityLevel(level: Int) {
        dataStore.edit { prefs ->
            prefs[minSeverityLevelKey] = level.coerceIn(1, 3)
        }
    }

    override suspend fun setCheckFavoriteCities(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[checkFavoritesKey] = enabled
        }
    }

    override suspend fun markWarningsNotified(ids: Set<String>) {
        if (ids.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[notifiedIdsKey] ?: emptySet()
            // Keep bounded history to avoid unbounded growth over months of usage
            val merged = (current + ids).toList().takeLast(64).toSet()
            prefs[notifiedIdsKey] = merged
        }
    }

    override suspend fun clearNotifiedHistory() {
        dataStore.edit { prefs ->
            prefs.remove(notifiedIdsKey)
        }
    }
}
