package com.example.data.remote.api

import com.example.data.remote.dto.ImgwSynopDto
import com.example.data.remote.dto.OpenMeteoForecastResponseDto
import com.example.data.remote.dto.OpenMeteoGeocodingResponseDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit contract for official IMGW-PIB public meteorological endpoints.
 */
interface ImgwApiService {
    @GET("api/data/synop")
    suspend fun getSynopStations(): List<ImgwSynopDto>

    /**
     * Returns raw [ResponseBody] because IMGW returns a JSON object (`{"status":false,...}`)
     * instead of an empty JSON array when no active warnings exist in Poland.
     */
    @GET("api/data/warningsmeteo")
    suspend fun getWarningsRaw(): ResponseBody
}

/**
 * Retrofit contract for Open-Meteo Geocoding API (Polish cities search).
 */
interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") query: String,
        @Query("count") count: Int = 8,
        @Query("language") language: String = "pl",
        @Query("countryCode") countryCode: String = "PL",
        @Query("format") format: String = "json",
    ): OpenMeteoGeocodingResponseDto
}

/**
 * Retrofit contract for Open-Meteo Forecast API (hourly 24h & daily 7-day forecast).
 */
interface OpenMeteoForecastService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m",
        @Query("hourly") hourly: String =
            "temperature_2m,weather_code,precipitation_probability,is_day",
        @Query("daily") daily: String =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max",
        @Query("timezone") timezone: String = "Europe/Warsaw",
        @Query("forecast_days") forecastDays: Int = 7,
    ): OpenMeteoForecastResponseDto
}
