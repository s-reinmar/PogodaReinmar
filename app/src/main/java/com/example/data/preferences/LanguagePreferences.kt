package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pogoda_reinmar_preferences",
)

interface LanguagePreferencesGateway {
    val selectedLanguage: Flow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage)
}

/**
 * Persists user-selected display language via Jetpack DataStore Preferences.
 */
class DataStoreLanguagePreferences(
    private val context: Context,
) : LanguagePreferencesGateway {

    private val languageKey = stringPreferencesKey("app_language_code")

    override val selectedLanguage: Flow<AppLanguage> =
        context.languageDataStore.data.map { prefs ->
            val savedCode = prefs[languageKey]
            if (savedCode != null) {
                AppLanguage.fromCode(savedCode)
            } else {
                AppLanguage.defaultForSystem()
            }
        }

    override suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { prefs ->
            prefs[languageKey] = language.code
        }
    }
}
