package com.example.data.repository

import com.example.data.remote.NetworkClient
import com.example.data.remote.api.ImgwApiService
import com.example.data.remote.api.OpenMeteoForecastService
import com.example.data.remote.api.OpenMeteoGeocodingService
import com.example.data.remote.dto.ImgwSynopDto
import com.example.data.remote.dto.ImgwWarningDto
import com.example.data.remote.dto.OpenMeteoForecastResponseDto
import com.example.domain.catalog.ImgwStationCatalog
import com.example.domain.model.DailyForecastItem
import com.example.domain.model.DataSourceStatus
import com.example.domain.model.GeoLocation
import com.example.domain.model.HourlyForecastItem
import com.example.domain.model.HybridWeatherBundle
import com.example.domain.model.ImgwMeasurement
import com.example.domain.model.OpenMeteoForecast
import com.example.domain.model.WeatherWarning
import com.example.domain.repository.WeatherRepository
import com.example.domain.util.PolishDateTimeUtils
import com.example.domain.util.WeatherCodeMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.math.roundToInt

/**
 * Hybrid weather repository combining IMGW-PIB ground telemetry & warnings with Open-Meteo
 * forecasts in parallel via [supervisorScope] (fault-tolerant `Promise.allSettled` pattern).
 */
class HybridWeatherRepositoryImpl(
    private val imgwApi: ImgwApiService = NetworkClient.imgwApiService,
    private val forecastApi: OpenMeteoForecastService = NetworkClient.openMeteoForecastService,
    private val geocodingApi: OpenMeteoGeocodingService = NetworkClient.openMeteoGeocodingService,
    private val json: Json = NetworkClient.json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WeatherRepository {

    override suspend fun fetchHybridWeather(location: GeoLocation): HybridWeatherBundle =
        withContext(ioDispatcher) {
            supervisorScope {
                val synopDeferred = async {
                    safeApiCall { imgwApi.getSynopStations() }
                }
                val warningsDeferred = async {
                    safeApiCall { fetchAndParseImgwWarnings() }
                }
                val openMeteoDeferred = async {
                    safeApiCall {
                        forecastApi.getForecast(
                            latitude = location.latitude,
                            longitude = location.longitude,
                        )
                    }
                }

                val synopResult = synopDeferred.await()
                val warningsResult = warningsDeferred.await()
                val openMeteoResult = openMeteoDeferred.await()

                val nearestCatalogStation = ImgwStationCatalog.findNearestCatalogStation(
                    location.latitude,
                    location.longitude,
                ).station

                val imgwMeasurement = synopResult.getOrNull()?.let { stations ->
                    matchNearestImgwMeasurement(location, stations)
                }

                val imgwStatus: DataSourceStatus = when {
                    imgwMeasurement != null -> DataSourceStatus.Available
                    synopResult.isFailure -> DataSourceStatus.Unavailable(
                        synopResult.exceptionOrNull()?.localizedMessage
                            ?: "Brak odpowiedzi serwera IMGW-PIB",
                    )
                    else -> DataSourceStatus.Unavailable("Nie znaleziono aktywnej stacji IMGW")
                }

                val openMeteoForecast = openMeteoResult.getOrNull()?.let { dto ->
                    mapOpenMeteoForecast(dto)
                }

                val openMeteoStatus: DataSourceStatus = when {
                    openMeteoForecast != null -> DataSourceStatus.Available
                    else -> DataSourceStatus.Unavailable(
                        openMeteoResult.exceptionOrNull()?.localizedMessage
                            ?: "Brak odpowiedzi serwera Open-Meteo",
                    )
                }

                val voivodeshipTeryt = ImgwStationCatalog.resolveVoivodeshipTeryt(
                    explicitTeryt = location.terytVoivodeshipCode,
                    admin1 = location.admin1,
                    fallbackStation = nearestCatalogStation,
                )

                val relevantWarnings = filterWarningsForRegion(
                    warnings = warningsResult.getOrDefault(emptyList()),
                    voivodeshipTeryt = voivodeshipTeryt,
                )

                val isDaytime = PolishDateTimeUtils.isCurrentlyDaytime(
                    sunriseIso = openMeteoForecast?.todaySunriseIso,
                    sunsetIso = openMeteoForecast?.todaySunsetIso,
                    fallbackIsDay = openMeteoForecast?.isDayNow ?: true,
                )

                val effectiveTemp = imgwMeasurement?.temperatureC ?: openMeteoForecast?.currentTempC
                val effectivePrecip = imgwMeasurement?.precipitationMm ?: openMeteoForecast?.currentPrecipitationMm
                val resolvedCondition = WeatherCodeMapper.resolveCondition(
                    wmoCode = openMeteoForecast?.currentWeatherCode,
                    imgwPrecipitationMm = imgwMeasurement?.precipitationMm,
                    temperatureC = effectiveTemp,
                )

                val effectiveWindKmh = imgwMeasurement?.windSpeedKmh ?: openMeteoForecast?.currentWindSpeedKmh
                val effectiveWindMs = imgwMeasurement?.windSpeedMs
                    ?: openMeteoForecast?.currentWindSpeedKmh?.div(3.6)

                HybridWeatherBundle(
                    location = location,
                    imgwMeasurement = imgwMeasurement,
                    imgwStatus = imgwStatus,
                    warnings = relevantWarnings,
                    openMeteo = openMeteoForecast,
                    openMeteoStatus = openMeteoStatus,
                    isDaytime = isDaytime,
                    resolvedCondition = resolvedCondition,
                    effectiveTemperatureC = effectiveTemp,
                    effectiveWindSpeedKmh = effectiveWindKmh,
                    effectiveWindSpeedMs = effectiveWindMs,
                    effectiveWindDirectionDeg = imgwMeasurement?.windDirectionDeg
                        ?: openMeteoForecast?.currentWindDirectionDeg,
                    effectivePressureHpa = imgwMeasurement?.pressureHpa
                        ?: openMeteoForecast?.currentPressureHpa,
                    effectiveHumidityPct = imgwMeasurement?.relativeHumidityPct
                        ?: openMeteoForecast?.currentHumidityPct,
                    effectivePrecipitationMm = effectivePrecip,
                    sunriseProgress = PolishDateTimeUtils.computeSolarProgress(
                        sunriseIso = openMeteoForecast?.todaySunriseIso,
                        sunsetIso = openMeteoForecast?.todaySunsetIso,
                    ),
                )
            }
        }

    override suspend fun searchLocations(
        query: String,
        languageCode: String,
    ): Result<List<GeoLocation>> =
        withContext(ioDispatcher) {
            val trimmed = query.trim()
            if (trimmed.length < 2) return@withContext Result.success(emptyList())

            safeApiCall {
                val response = geocodingApi.searchCities(query = trimmed, language = languageCode)
                response.results.orEmpty()
                    .filter { it.countryCode == null || it.countryCode.equals("PL", ignoreCase = true) }
                    .map { item ->
                        GeoLocation(
                            name = item.name,
                            admin1 = item.admin1,
                            latitude = item.latitude,
                            longitude = item.longitude,
                            isCurrentGps = false,
                            terytVoivodeshipCode = ImgwStationCatalog.resolveVoivodeshipTeryt(
                                explicitTeryt = null,
                                admin1 = item.admin1,
                                fallbackStation = null,
                            ),
                        )
                    }
                    .distinctBy { "${it.name}|${it.admin1}" }
            }
        }

    private suspend fun fetchAndParseImgwWarnings(): List<WeatherWarning> {
        val rawBody = imgwApi.getWarningsRaw().use { it.string() }.trim()
        if (rawBody.isBlank()) return emptyList()

        val element = json.parseToJsonElement(rawBody)
        if (element !is JsonArray) {
            // IMGW returns {"status":false,"message":"No products were found"} when no alerts exist.
            return emptyList()
        }

        return element.mapNotNull { item ->
            runCatching { json.decodeFromJsonElement<ImgwWarningDto>(item) }.getOrNull()
        }.mapNotNull { dto ->
            val name = dto.eventName.trim()
            val content = dto.content?.trim().orEmpty()
            if (name.isEmpty() && content.isEmpty()) return@mapNotNull null

            WeatherWarning(
                id = dto.id.ifBlank { "${name}_${dto.validFrom}" },
                eventName = name.ifBlank { "Ostrzeżenie meteorologiczne" },
                level = dto.level?.trim()?.toIntOrNull()?.coerceIn(1, 3) ?: 1,
                probabilityPct = dto.probability?.trim()?.toIntOrNull(),
                validFrom = PolishDateTimeUtils.formatImgwTimestamp(dto.validFrom),
                validTo = PolishDateTimeUtils.formatImgwTimestamp(dto.validTo),
                description = content.ifBlank { "Szczegóły ostrzeżenia niedostępne." },
                comment = dto.comment?.trim()?.takeIf { it.isNotEmpty() && !it.equals("Brak.", ignoreCase = true) },
                office = dto.office?.trim()?.takeIf { it.isNotEmpty() },
                terytCodes = dto.teryt,
            )
        }
    }

    private fun matchNearestImgwMeasurement(
        location: GeoLocation,
        activeStations: List<ImgwSynopDto>,
    ): ImgwMeasurement? {
        if (activeStations.isEmpty()) return null

        val byId = activeStations.associateBy { it.stationId.trim() }
        val byNormalizedName = activeStations.associateBy {
            ImgwStationCatalog.normalizeText(it.stationName)
        }

        val nearest = ImgwStationCatalog.findNearestActiveStation(
            targetLat = location.latitude,
            targetLon = location.longitude,
            activeIds = byId.keys,
            activeNormalizedNames = byNormalizedName.keys,
        ) ?: return null

        val dto = byId[nearest.station.id]
            ?: byNormalizedName[ImgwStationCatalog.normalizeText(nearest.station.name)]
            ?: return null

        val windMs = dto.windSpeedMs?.toDoubleOrNull()
        val windKmh = windMs?.times(3.6)

        return ImgwMeasurement(
            stationId = dto.stationId.ifBlank { nearest.station.id },
            stationName = dto.stationName.ifBlank { nearest.station.name },
            distanceKm = nearest.distanceKm,
            measurementDate = dto.measurementDate.orEmpty(),
            measurementHour = dto.measurementHour?.trim()?.padStart(2, '0') ?: "--",
            temperatureC = dto.temperature?.toDoubleOrNull(),
            windSpeedMs = windMs,
            windSpeedKmh = windKmh,
            windDirectionDeg = dto.windDirectionDeg?.toFloatOrNull(),
            relativeHumidityPct = dto.relativeHumidity?.toDoubleOrNull(),
            precipitationMm = dto.precipitationSum?.toDoubleOrNull(),
            pressureHpa = dto.pressureHpa?.toDoubleOrNull(),
        )
    }

    private fun filterWarningsForRegion(
        warnings: List<WeatherWarning>,
        voivodeshipTeryt: String?,
    ): List<WeatherWarning> {
        if (warnings.isEmpty()) return emptyList()
        val matched = if (voivodeshipTeryt != null) {
            warnings.filter { warning ->
                warning.terytCodes.isEmpty() ||
                    warning.terytCodes.any { code -> code.startsWith(voivodeshipTeryt) }
            }
        } else {
            warnings
        }
        return matched
            .distinctBy { "${it.eventName}|${it.level}|${it.validTo}" }
            .sortedByDescending { it.level }
    }

    private fun mapOpenMeteoForecast(dto: OpenMeteoForecastResponseDto): OpenMeteoForecast {
        val current = dto.current
        val hourly = dto.hourly
        val daily = dto.daily

        val nowHourIso = PolishDateTimeUtils.currentWarsawHourIso()
        val hourlyItems = buildHourly24h(hourly, nowHourIso)
        val dailyItems = buildDaily7d(daily)

        val todayDaily = dailyItems.firstOrNull()

        return OpenMeteoForecast(
            currentTempC = current?.temperature2m,
            apparentTempC = current?.apparentTemperature,
            currentWeatherCode = current?.weatherCode,
            isDayNow = (current?.isDay ?: 1) == 1,
            currentHumidityPct = current?.relativeHumidity2m,
            currentPressureHpa = current?.surfacePressure,
            currentWindSpeedKmh = current?.windSpeed10m,
            currentWindDirectionDeg = current?.windDirection10m?.toFloat(),
            currentPrecipitationMm = current?.precipitation,
            todaySunriseIso = todayDaily?.sunriseIso,
            todaySunsetIso = todayDaily?.sunsetIso,
            todaySunriseLabel = PolishDateTimeUtils.extractTimeLabel(todayDaily?.sunriseIso),
            todaySunsetLabel = PolishDateTimeUtils.extractTimeLabel(todayDaily?.sunsetIso),
            todayUvIndexMax = todayDaily?.uvIndexMax,
            hourly24h = hourlyItems,
            daily7d = dailyItems,
        )
    }

    private fun buildHourly24h(
        hourly: com.example.data.remote.dto.OpenMeteoHourlyDto?,
        nowHourIso: String,
    ): List<HourlyForecastItem> {
        if (hourly == null || hourly.time.isEmpty()) return emptyList()

        val startIndex = hourly.time.indexOfFirst { it >= nowHourIso }.coerceAtLeast(0)
        val endIndex = (startIndex + 24).coerceAtMost(hourly.time.size)

        return (startIndex until endIndex).mapIndexed { offset, idx ->
            val iso = hourly.time[idx]
            val hourLabel = if (offset == 0) {
                "Teraz"
            } else {
                PolishDateTimeUtils.extractTimeLabel(iso) ?: iso
            }
            HourlyForecastItem(
                isoTime = iso,
                hourLabel = hourLabel,
                temperatureC = hourly.temperature2m.getOrNull(idx) ?: 0.0,
                weatherCode = hourly.weatherCode.getOrNull(idx) ?: 0,
                precipitationProbabilityPct = (hourly.precipitationProbability.getOrNull(idx) ?: 0)
                    .coerceIn(0, 100),
                isDay = (hourly.isDay.getOrNull(idx) ?: 1) == 1,
                isCurrentHour = offset == 0,
            )
        }
    }

    private fun buildDaily7d(
        daily: com.example.data.remote.dto.OpenMeteoDailyDto?,
    ): List<DailyForecastItem> {
        if (daily == null || daily.time.isEmpty()) return emptyList()

        return daily.time.take(7).mapIndexed { index, isoDate ->
            val (dayLabel, shortDate) = PolishDateTimeUtils.formatDailyLabels(isoDate, index)
            DailyForecastItem(
                isoDate = isoDate,
                dayLabel = dayLabel,
                dateShortLabel = shortDate,
                weatherCode = daily.weatherCode.getOrNull(index) ?: 0,
                tempMinC = daily.temperature2mMin.getOrNull(index) ?: 0.0,
                tempMaxC = daily.temperature2mMax.getOrNull(index) ?: 0.0,
                precipitationProbabilityMaxPct = (daily.precipitationProbabilityMax.getOrNull(index)
                    ?: 0).coerceIn(0, 100),
                uvIndexMax = daily.uvIndexMax.getOrNull(index)?.let { (it * 10.0).roundToInt() / 10.0 },
                sunriseIso = daily.sunrise.getOrNull(index),
                sunsetIso = daily.sunset.getOrNull(index),
            )
        }
    }

    private suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
