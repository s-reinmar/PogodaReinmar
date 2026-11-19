package com.example.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response envelope from https://geocoding-api.open-meteo.com/v1/search.
 */
@Serializable
data class OpenMeteoGeocodingResponseDto(
    @SerialName("results") val results: List<OpenMeteoGeocodingResultDto>? = null,
)

@Serializable
data class OpenMeteoGeocodingResultDto(
    @SerialName("id") val id: Long = 0L,
    @SerialName("name") val name: String = "",
    @SerialName("latitude") val latitude: Double = 0.0,
    @SerialName("longitude") val longitude: Double = 0.0,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("admin1") val admin1: String? = null,
    @SerialName("admin2") val admin2: String? = null,
)

/**
 * Response envelope from https://api.open-meteo.com/v1/forecast.
 */
@Serializable
data class OpenMeteoForecastResponseDto(
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("current") val current: OpenMeteoCurrentDto? = null,
    @SerialName("hourly") val hourly: OpenMeteoHourlyDto? = null,
    @SerialName("daily") val daily: OpenMeteoDailyDto? = null,
)

@Serializable
data class OpenMeteoCurrentDto(
    @SerialName("time") val time: String? = null,
    @SerialName("temperature_2m") val temperature2m: Double? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
    @SerialName("precipitation") val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("surface_pressure") val surfacePressure: Double? = null,
    @SerialName("wind_speed_10m") val windSpeed10m: Double? = null,
    @SerialName("wind_direction_10m") val windDirection10m: Double? = null,
)

@Serializable
data class OpenMeteoHourlyDto(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList(),
)

@Serializable
data class OpenMeteoDailyDto(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double?> = emptyList(),
    @SerialName("sunrise") val sunrise: List<String?> = emptyList(),
    @SerialName("sunset") val sunset: List<String?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
)
