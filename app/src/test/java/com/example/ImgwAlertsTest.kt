package com.example

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.data.notifications.AlertNotificationGateway
import com.example.data.preferences.DataStoreAlertPreferences
import com.example.data.preferences.FavoriteCitiesGateway
import com.example.data.preferences.LanguagePreferencesGateway
import com.example.domain.model.AppLanguage
import com.example.domain.model.DataSourceStatus
import com.example.domain.model.GeoLocation
import com.example.domain.model.HybridWeatherBundle
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherWarning
import com.example.domain.repository.LocationGateway
import com.example.domain.repository.LocationLookupResult
import com.example.domain.repository.ReverseGeocodedLocation
import com.example.domain.repository.WeatherRepository
import com.example.presentation.WeatherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ImgwAlertsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `persists IMGW alert settings and notified IDs in DataStore`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = File(tempFolder.root, "alerts_settings_test.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
            val prefs = DataStoreAlertPreferences(dataStore)

            val defaults = prefs.settings.first()
            assertTrue(defaults.notificationsEnabled)
            assertTrue(defaults.bannerEnabled)
            assertEquals(1, defaults.minSeverityLevel)
            assertTrue(defaults.checkFavoriteCities)

            prefs.setBannerEnabled(false)
            prefs.setNotificationsEnabled(false)
            prefs.setMinSeverityLevel(2)
            prefs.setCheckFavoriteCities(false)
            prefs.markWarningsNotified(setOf("warn_1", "warn_2"))

            val updated = prefs.settings.first()
            assertFalse(updated.bannerEnabled)
            assertFalse(updated.notificationsEnabled)
            assertEquals(2, updated.minSeverityLevel)
            assertFalse(updated.checkFavoriteCities)
            assertEquals(setOf("warn_1", "warn_2"), prefs.notifiedWarningIds.first())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `viewModel checks IMGW feed, matches favorite Polish city alerts, and deduplicates notifications`() = runTest {
        val dsScope = CoroutineScope(SupervisorJob() + testDispatcher)
        try {
            val file = File(tempFolder.root, "vm_alerts_test.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(
                scope = dsScope,
                produceFile = { file },
            )
            val alertPrefs = DataStoreAlertPreferences(dataStore)

            val krakow = GeoLocation(
                name = "Kraków",
                admin1 = "Województwo małopolskie",
                countryName = "Polska",
                countryCode = "PL",
                latitude = 50.0647,
                longitude = 19.9450,
                terytVoivodeshipCode = "12",
            )
            val krakowStorm = WeatherWarning(
                id = "imgw_storm_12",
                eventName = "Burze z gradem",
                level = 2,
                probabilityPct = 85,
                validFrom = "19.11, 18:00",
                validTo = "20.11, 02:00",
                description = "Prognozowane silne burze z opadami gradu.",
                comment = null,
                office = "IMGW Kraków",
                terytCodes = listOf("1261"),
            )

            val fakeNotifier = object : AlertNotificationGateway {
                val posted = mutableListOf<Pair<String, String>>()
                override fun hasNotificationPermission(): Boolean = true
                override fun postImgwAlertNotification(
                    cityName: String,
                    warning: WeatherWarning,
                    totalActiveCount: Int,
                ): Boolean {
                    posted += cityName to warning.id
                    return true
                }
            }

            val fakeRepo = object : WeatherRepository {
                override suspend fun fetchHybridWeather(location: GeoLocation): HybridWeatherBundle =
                    HybridWeatherBundle(
                        location = location,
                        imgwMeasurement = null,
                        imgwStatus = DataSourceStatus.NotApplicable,
                        warnings = emptyList(),
                        openMeteo = null,
                        openMeteoStatus = DataSourceStatus.Unavailable("none"),
                        isDaytime = true,
                        resolvedCondition = WeatherCondition.CLEAR,
                        effectiveTemperatureC = 12.0,
                        effectiveWindSpeedKmh = 10.0,
                        effectiveWindSpeedMs = 2.8,
                        effectiveWindDirectionDeg = 180f,
                        effectivePressureHpa = 1015.0,
                        effectiveHumidityPct = 55.0,
                        effectivePrecipitationMm = 0.0,
                        sunriseProgress = 0.5f,
                    )

                override suspend fun searchLocations(
                    query: String,
                    languageCode: String,
                ): Result<List<GeoLocation>> = Result.success(emptyList())

                override suspend fun fetchAllActiveImgwWarnings(): Result<List<WeatherWarning>> =
                    Result.success(listOf(krakowStorm))

                override fun filterWarningsForLocation(
                    warnings: List<WeatherWarning>,
                    location: GeoLocation,
                ): List<WeatherWarning> {
                    val teryt = location.terytVoivodeshipCode ?: return emptyList()
                    return warnings.filter { w -> w.terytCodes.any { it.startsWith(teryt) } }
                }
            }

            val fakeLang = object : LanguagePreferencesGateway {
                override val selectedLanguage: Flow<AppLanguage> = MutableStateFlow(AppLanguage.POLISH)
                override suspend fun setLanguage(language: AppLanguage) {}
            }

            val fakeFavorites = object : FavoriteCitiesGateway {
                override val favoriteCities: Flow<List<GeoLocation>> = MutableStateFlow(listOf(krakow))
                override suspend fun addFavorite(location: GeoLocation) {}
                override suspend fun removeFavorite(location: GeoLocation) {}
                override suspend fun toggleFavorite(location: GeoLocation) {}
            }

            val fakeLocation = object : LocationGateway {
                override fun hasLocationPermission(): Boolean = false
                override suspend fun fetchCurrentLocation(languageCode: String): LocationLookupResult =
                    LocationLookupResult.PermissionMissing

                override suspend fun reverseGeocodeLocationName(
                    latitude: Double,
                    longitude: Double,
                    languageCode: String,
                ): ReverseGeocodedLocation? = null
            }

            val viewModel = WeatherViewModel(
                weatherRepository = fakeRepo,
                locationGateway = fakeLocation,
                languagePreferences = fakeLang,
                favoriteCitiesGateway = fakeFavorites,
                alertPreferencesGateway = alertPrefs,
                alertNotifier = fakeNotifier,
            )

            advanceUntilIdle()

            // First silent check detects Krakow warning and sends notification once
            viewModel.onCheckImgwAlertsNow(forceNotifyIfActive = false)
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.otherFavoriteCityAlerts.size)
            assertEquals("Kraków", viewModel.uiState.value.otherFavoriteCityAlerts.first().city.name)
            assertEquals(1, fakeNotifier.posted.size)
            assertEquals("Kraków" to "imgw_storm_12", fakeNotifier.posted.first())

            // Second silent check does NOT duplicate the same warning notification
            viewModel.onCheckImgwAlertsNow(forceNotifyIfActive = false)
            advanceUntilIdle()
            assertEquals(1, fakeNotifier.posted.size)

            // Raising min severity to level 3 hides the level-2 alert from relevant banners
            viewModel.onMinAlertLevelChanged(3)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.otherFavoriteCityAlerts.isEmpty())
            assertEquals(0, viewModel.uiState.value.totalRelevantActiveWarningsCount)
        } finally {
            dsScope.cancel()
        }
    }
}
