package com.example

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.data.preferences.DataStoreFavoriteCitiesPreferences
import com.example.domain.model.GeoLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FavoriteCitiesPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `persists added cities in DataStore and deduplicates near-identical coordinates`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = File(tempFolder.root, "favorites_test_1.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            val prefs = DataStoreFavoriteCitiesPreferences(dataStore)

            val warsawGps = GeoLocation.WARSAW.copy(
                name = "Warsaw",
                latitude = 52.2310,
                longitude = 21.0150,
                isCurrentGps = true,
            )
            val tokyo = GeoLocation(
                name = "Tokio",
                admin1 = "Tokio",
                countryName = "Japonia",
                countryCode = "JP",
                timezoneId = "Asia/Tokyo",
                latitude = 35.6762,
                longitude = 139.6503,
            )

            assertTrue(prefs.favoriteCities.first().isEmpty())

            prefs.addFavorite(GeoLocation.WARSAW)
            prefs.addFavorite(warsawGps) // Within ~2 km of Warsaw -> should not duplicate
            prefs.addFavorite(tokyo)

            val saved = prefs.favoriteCities.first()
            assertEquals(2, saved.size)
            assertEquals("Warszawa", saved[0].name)
            assertFalse(saved[0].isCurrentGps)
            assertEquals("Tokio", saved[1].name)
            assertEquals("JP", saved[1].countryCode)
            assertEquals("Asia/Tokyo", saved[1].timezoneId)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `toggleFavorite adds when missing and removes when already present`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = File(tempFolder.root, "favorites_test_2.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            val prefs = DataStoreFavoriteCitiesPreferences(dataStore)

            val berlin = GeoLocation(
                name = "Berlin",
                countryName = "Niemcy",
                countryCode = "DE",
                timezoneId = "Europe/Berlin",
                latitude = 52.5200,
                longitude = 13.4050,
            )

            prefs.toggleFavorite(berlin)
            assertEquals(1, prefs.favoriteCities.first().size)
            assertTrue(prefs.favoriteCities.first().first().matchesLocation(berlin))

            prefs.toggleFavorite(berlin)
            assertTrue(prefs.favoriteCities.first().isEmpty())
        } finally {
            scope.cancel()
        }
    }
}
