package com.example

import com.example.data.remote.api.ImgwApiService
import com.example.data.remote.api.OpenMeteoForecastService
import com.example.data.remote.api.OpenMeteoGeocodingService
import com.example.data.remote.dto.ImgwSynopDto
import com.example.data.remote.dto.OpenMeteoCurrentDto
import com.example.data.remote.dto.OpenMeteoDailyDto
import com.example.data.remote.dto.OpenMeteoForecastResponseDto
import com.example.data.remote.dto.OpenMeteoGeocodingResponseDto
import com.example.data.remote.dto.OpenMeteoGeocodingResultDto
import com.example.data.remote.dto.OpenMeteoHourlyDto
import com.example.data.repository.HybridWeatherRepositoryImpl
import com.example.domain.catalog.ImgwStationCatalog
import com.example.domain.model.DataSourceStatus
import com.example.domain.model.GeoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalWeatherRoutingTest {

    private class FakeImgwApi : ImgwApiService {
        var synopCalls = 0
        var warningsCalls = 0

        override suspend fun getSynopStations(): List<ImgwSynopDto> {
            synopCalls++
            return listOf(
                ImgwSynopDto(
                    stationId = "12375",
                    stationName = "Warszawa",
                    measurementDate = "2026-11-19",
                    measurementHour = "14",
                    temperature = "11.4",
                    windSpeedMs = "5.0",
                    windDirectionDeg = "230",
                    relativeHumidity = "68.0",
                    precipitationSum = "0.0",
                    pressureHpa = "1016.2",
                ),
            )
        }

        override suspend fun getWarningsRaw(): ResponseBody {
            warningsCalls++
            return """{"status":false,"message":"No products were found"}"""
                .toResponseBody("application/json".toMediaType())
        }
    }

    private class FakeForecastApi : OpenMeteoForecastService {
        var lastTimezoneArg: String? = null

        override suspend fun getForecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            timezone: String,
            forecastDays: Int,
        ): OpenMeteoForecastResponseDto {
            lastTimezoneArg = timezone
            val resolvedTz = if (longitude > 100.0) "Asia/Tokyo" else "Europe/Warsaw"
            return OpenMeteoForecastResponseDto(
                latitude = latitude,
                longitude = longitude,
                timezone = resolvedTz,
                current = OpenMeteoCurrentDto(
                    time = "2026-11-19T22:00",
                    temperature2m = 16.5,
                    relativeHumidity2m = 62.0,
                    apparentTemperature = 15.8,
                    isDay = 0,
                    precipitation = 0.0,
                    weatherCode = 1,
                    pressureMsl = 1019.3,
                    surfacePressure = 1012.0,
                    windSpeed10m = 14.4,
                    windDirection10m = 90.0,
                ),
                hourly = OpenMeteoHourlyDto(
                    time = listOf("2026-11-19T21:00", "2026-11-19T22:00", "2026-11-19T23:00"),
                    temperature2m = listOf(17.0, 16.5, 16.0),
                    weatherCode = listOf(1, 1, 2),
                    precipitationProbability = listOf(5, 10, 15),
                    isDay = listOf(0, 0, 0),
                ),
                daily = OpenMeteoDailyDto(
                    time = listOf("2026-11-19"),
                    weatherCode = listOf(1),
                    temperature2mMax = listOf(19.0),
                    temperature2mMin = listOf(11.0),
                    sunrise = listOf("2026-11-19T06:21"),
                    sunset = listOf("2026-11-19T16:31"),
                    uvIndexMax = listOf(3.8),
                    precipitationProbabilityMax = listOf(15),
                ),
            )
        }
    }

    private class FakeGeocodingApi : OpenMeteoGeocodingService {
        var lastCountryCode: String? = "UNSET"

        override suspend fun searchCities(
            query: String,
            count: Int,
            language: String,
            countryCode: String?,
            format: String,
        ): OpenMeteoGeocodingResponseDto {
            lastCountryCode = countryCode
            return OpenMeteoGeocodingResponseDto(
                results = listOf(
                    OpenMeteoGeocodingResultDto(
                        id = 1L,
                        name = "Paryż",
                        latitude = 48.8566,
                        longitude = 2.3522,
                        country = "Francja",
                        countryCode = "FR",
                        timezone = "Europe/Paris",
                        admin1 = "Île-de-France",
                    ),
                    OpenMeteoGeocodingResultDto(
                        id = 2L,
                        name = "Paryż",
                        latitude = 52.8850,
                        longitude = 17.5750,
                        country = "Polska",
                        countryCode = "PL",
                        timezone = "Europe/Warsaw",
                        admin1 = "Województwo kujawsko-pomorskie",
                    ),
                ),
            )
        }
    }

    @Test
    fun `searchLocations queries worldwide and preserves both Polish and foreign results`() = runBlocking {
        val fakeGeocoding = FakeGeocodingApi()
        val repository = HybridWeatherRepositoryImpl(
            imgwApi = FakeImgwApi(),
            forecastApi = FakeForecastApi(),
            geocodingApi = fakeGeocoding,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val result = repository.searchLocations("Paryż", languageCode = "pl")
        assertTrue(result.isSuccess)
        assertNull(fakeGeocoding.lastCountryCode)

        val cities = result.getOrThrow()
        assertEquals(2, cities.size)
        assertEquals("FR", cities[0].countryCode)
        assertEquals("Francja", cities[0].countryName)
        assertNull(cities[0].terytVoivodeshipCode)
        assertFalse(ImgwStationCatalog.isPolishLocation(cities[0]))

        assertEquals("PL", cities[1].countryCode)
        assertEquals("04", cities[1].terytVoivodeshipCode)
        assertTrue(ImgwStationCatalog.isPolishLocation(cities[1]))
    }

    @Test
    fun `fetchHybridWeather uses IMGW and Open-Meteo for Poland`() = runBlocking {
        val fakeImgw = FakeImgwApi()
        val fakeForecast = FakeForecastApi()
        val repository = HybridWeatherRepositoryImpl(
            imgwApi = fakeImgw,
            forecastApi = fakeForecast,
            geocodingApi = FakeGeocodingApi(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        val bundle = repository.fetchHybridWeather(GeoLocation.WARSAW)
        assertEquals(1, fakeImgw.synopCalls)
        assertEquals(1, fakeImgw.warningsCalls)
        assertEquals("auto", fakeForecast.lastTimezoneArg)
        assertEquals(DataSourceStatus.Available, bundle.imgwStatus)
        assertNotNull(bundle.imgwMeasurement)
        assertEquals(11.4, bundle.effectiveTemperatureC!!, 0.01)
    }

    @Test
    fun `fetchHybridWeather skips IMGW completely and uses Open-Meteo for foreign cities`() = runBlocking {
        val fakeImgw = FakeImgwApi()
        val fakeForecast = FakeForecastApi()
        val repository = HybridWeatherRepositoryImpl(
            imgwApi = fakeImgw,
            forecastApi = fakeForecast,
            geocodingApi = FakeGeocodingApi(),
            ioDispatcher = Dispatchers.Unconfined,
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

        val bundle = repository.fetchHybridWeather(tokyo)
        assertEquals(0, fakeImgw.synopCalls)
        assertEquals(0, fakeImgw.warningsCalls)
        assertEquals(DataSourceStatus.NotApplicable, bundle.imgwStatus)
        assertNull(bundle.imgwMeasurement)
        assertTrue(bundle.warnings.isEmpty())
        assertEquals(DataSourceStatus.Available, bundle.openMeteoStatus)
        assertEquals(16.5, bundle.effectiveTemperatureC!!, 0.01)
        assertEquals(1019.3, bundle.effectivePressureHpa!!, 0.01)
        assertEquals("22:00", bundle.openMeteo?.currentLocalTimeLabel)
        assertEquals("2026-11-19T22:00", bundle.openMeteo?.hourly24h?.firstOrNull()?.isoTime)
        assertEquals("Japonia", tokyo.formattedSubtitle())
    }
}
