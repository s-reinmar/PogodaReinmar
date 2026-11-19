package com.example.domain.model

/**
 * Geographic location selected by GPS or Open-Meteo Geocoding search (Poland or worldwide).
 */
data class GeoLocation(
    val name: String,
    val admin1: String? = null,
    val countryName: String? = null,
    val countryCode: String? = null,
    val timezoneId: String? = null,
    val latitude: Double,
    val longitude: Double,
    val isCurrentGps: Boolean = false,
    val terytVoivodeshipCode: String? = null,
) {
    /**
     * Checks whether two locations refer to the same city within ~2 km coordinate tolerance,
     * keeping favorites stable across language switches and GPS vs. geocoded coordinates.
     */
    fun matchesLocation(other: GeoLocation): Boolean =
        kotlin.math.abs(latitude - other.latitude) < 0.02 &&
            kotlin.math.abs(longitude - other.longitude) < 0.02

    /**
     * Deterministic key suitable for Compose LazyRow/LazyColumn item keys.
     */
    fun stableId(): String =
        "${countryCode.orEmpty().uppercase()}_${(latitude * 100).toInt()}_${(longitude * 100).toInt()}_${name.lowercase()}"

    /**
     * Builds a clean region + country subtitle avoiding duplicate segments (e.g. "Berlin, Niemcy").
     */
    fun formattedSubtitle(): String? {
        val parts = buildList {
            val cleanAdmin = admin1?.trim()?.takeIf {
                it.isNotEmpty() && !it.equals(name.trim(), ignoreCase = true)
            }
            val cleanCountry = countryName?.trim()?.takeIf {
                it.isNotEmpty() && !it.equals(name.trim(), ignoreCase = true)
            }
            if (cleanAdmin != null) add(cleanAdmin)
            if (cleanCountry != null && !cleanCountry.equals(cleanAdmin, ignoreCase = true)) {
                add(cleanCountry)
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    companion object {
        val WARSAW = GeoLocation(
            name = "Warszawa",
            admin1 = "Województwo mazowieckie",
            countryName = "Polska",
            countryCode = "PL",
            timezoneId = "Europe/Warsaw",
            latitude = 52.2297,
            longitude = 21.0122,
            isCurrentGps = false,
            terytVoivodeshipCode = "14",
        )
    }
}

/**
 * Static reference coordinates for an official IMGW-PIB synoptic station.
 */
data class ImgwStationReference(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val voivodeshipTeryt: String,
)

/**
 * Result of matching user coordinates to the nearest active IMGW station via Haversine formula.
 */
data class StationMatch(
    val station: ImgwStationReference,
    val distanceKm: Double,
)

/**
 * Ground observation from IMGW-PIB `/api/data/synop`.
 */
data class ImgwMeasurement(
    val stationId: String,
    val stationName: String,
    val distanceKm: Double,
    val measurementDate: String,
    val measurementHour: String,
    val temperatureC: Double?,
    val windSpeedMs: Double?,
    val windSpeedKmh: Double?,
    val windDirectionDeg: Float?,
    val relativeHumidityPct: Double?,
    val precipitationMm: Double?,
    val pressureHpa: Double?,
)

/**
 * Active meteorological warning from IMGW-PIB `/api/data/warningsmeteo`.
 */
data class WeatherWarning(
    val id: String,
    val eventName: String,
    val level: Int,
    val probabilityPct: Int?,
    val validFrom: String,
    val validTo: String,
    val description: String,
    val comment: String?,
    val office: String?,
    val terytCodes: List<String>,
)

/**
 * Single hour forecast item from Open-Meteo.
 */
data class HourlyForecastItem(
    val isoTime: String,
    val hourLabel: String,
    val temperatureC: Double,
    val weatherCode: Int,
    val precipitationProbabilityPct: Int,
    val isDay: Boolean,
    val isCurrentHour: Boolean,
)

/**
 * Single day forecast item from Open-Meteo (7-day outlook).
 */
data class DailyForecastItem(
    val isoDate: String,
    val dayLabel: String,
    val dateShortLabel: String,
    val weatherCode: Int,
    val tempMinC: Double,
    val tempMaxC: Double,
    val precipitationProbabilityMaxPct: Int,
    val uvIndexMax: Double?,
    val sunriseIso: String?,
    val sunsetIso: String?,
)

/**
 * Aggregated forecast payload from Open-Meteo.
 */
data class OpenMeteoForecast(
    val timezoneId: String? = null,
    val currentLocalTimeLabel: String? = null,
    val currentTempC: Double?,
    val apparentTempC: Double?,
    val currentWeatherCode: Int?,
    val isDayNow: Boolean,
    val currentHumidityPct: Double?,
    val currentPressureHpa: Double?,
    val currentWindSpeedKmh: Double?,
    val currentWindDirectionDeg: Float?,
    val currentPrecipitationMm: Double?,
    val todaySunriseLabel: String?,
    val todaySunsetLabel: String?,
    val todaySunriseIso: String?,
    val todaySunsetIso: String?,
    val todayUvIndexMax: Double?,
    val hourly24h: List<HourlyForecastItem>,
    val daily7d: List<DailyForecastItem>,
)

/**
 * Unified meteorological condition resolved from Open-Meteo WMO code and IMGW ground precipitation.
 */
enum class WeatherCondition(val polishLabel: String) {
    CLEAR("Bezchmurnie"),
    MAINLY_CLEAR("Przeważnie pogodnie"),
    PARTLY_CLOUDY("Częściowe zachmurzenie"),
    OVERCAST("Pochmurno"),
    FOG("Mgła"),
    DRIZZLE("Mżawka"),
    RAIN("Deszcz"),
    HEAVY_RAIN("Silny deszcz"),
    FREEZING_RAIN("Marznący deszcz"),
    HAIL("Grad"),
    SNOW("Opady śniegu"),
    THUNDERSTORM("Burza"),
}

/**
 * Availability state for each independent data provider in the hybrid architecture.
 */
sealed interface DataSourceStatus {
    data object Available : DataSourceStatus
    data object NotApplicable : DataSourceStatus
    data class Unavailable(val reason: String) : DataSourceStatus
}

/**
 * Hybrid weather payload merging IMGW-PIB ground measurements & alerts with Open-Meteo forecast.
 */
data class HybridWeatherBundle(
    val location: GeoLocation,
    val imgwMeasurement: ImgwMeasurement?,
    val imgwStatus: DataSourceStatus,
    val warnings: List<WeatherWarning>,
    val openMeteo: OpenMeteoForecast?,
    val openMeteoStatus: DataSourceStatus,
    val isDaytime: Boolean,
    val resolvedCondition: WeatherCondition,
    val effectiveTemperatureC: Double?,
    val effectiveWindSpeedKmh: Double?,
    val effectiveWindSpeedMs: Double?,
    val effectiveWindDirectionDeg: Float?,
    val effectivePressureHpa: Double?,
    val effectiveHumidityPct: Double?,
    val effectivePrecipitationMm: Double?,
    val sunriseProgress: Float?,
) {
    val hasAnyData: Boolean
        get() = imgwMeasurement != null || openMeteo != null
}
