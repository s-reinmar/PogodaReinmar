package com.example.data.remote

import com.example.data.remote.api.ImgwApiService
import com.example.data.remote.api.OpenMeteoForecastService
import com.example.data.remote.api.OpenMeteoGeocodingService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Centralized Retrofit & OkHttp client factory configured with Kotlinx Serialization
 * (`ignoreUnknownKeys = true`) and conservative mobile network timeouts.
 */
object NetworkClient {

    private const val IMGW_BASE_URL = "https://danepubliczne.imgw.pl/"
    private const val OPEN_METEO_FORECAST_URL = "https://api.open-meteo.com/"
    private const val OPEN_METEO_GEOCODING_URL = "https://geocoding-api.open-meteo.com/"

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val contentType = "application/json; charset=UTF8".toMediaType()

    private val okHttpClient: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logger)
            .build()
    }

    private fun buildRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

    val imgwApiService: ImgwApiService by lazy {
        buildRetrofit(IMGW_BASE_URL).create(ImgwApiService::class.java)
    }

    val openMeteoForecastService: OpenMeteoForecastService by lazy {
        buildRetrofit(OPEN_METEO_FORECAST_URL).create(OpenMeteoForecastService::class.java)
    }

    val openMeteoGeocodingService: OpenMeteoGeocodingService by lazy {
        buildRetrofit(OPEN_METEO_GEOCODING_URL).create(OpenMeteoGeocodingService::class.java)
    }
}
