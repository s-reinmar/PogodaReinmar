package com.example.domain.util

import com.example.domain.model.WeatherCondition
import kotlin.math.roundToInt

/**
 * Maps Open-Meteo WMO weather interpretation codes combined with IMGW ground precipitation
 * measurements into unified domain conditions and Polish meteorological telemetry labels.
 */
object WeatherCodeMapper {

    fun resolveCondition(
        wmoCode: Int?,
        imgwPrecipitationMm: Double? = null,
        temperatureC: Double? = null,
    ): WeatherCondition {
        val base = fromWmoCode(wmoCode)

        // When IMGW reports active ground precipitation while Open-Meteo predicts dry skies,
        // upgrade the icon/condition gently so ground telemetry and hero visuals stay consistent.
        if (imgwPrecipitationMm != null && imgwPrecipitationMm >= 0.4) {
            val isDryCode = base == WeatherCondition.CLEAR ||
                base == WeatherCondition.MAINLY_CLEAR ||
                base == WeatherCondition.PARTLY_CLOUDY ||
                base == WeatherCondition.OVERCAST
            if (isDryCode) {
                return when {
                    temperatureC != null && temperatureC <= 0.5 -> WeatherCondition.SNOW
                    imgwPrecipitationMm >= 5.0 -> WeatherCondition.HEAVY_RAIN
                    else -> WeatherCondition.RAIN
                }
            }
        }
        return base
    }

    fun fromWmoCode(code: Int?): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1 -> WeatherCondition.MAINLY_CLEAR
        2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.OVERCAST
        45, 48 -> WeatherCondition.FOG
        51, 53, 55 -> WeatherCondition.DRIZZLE
        56, 57, 66, 67 -> WeatherCondition.FREEZING_RAIN
        61, 63, 80, 81 -> WeatherCondition.RAIN
        65, 82 -> WeatherCondition.HEAVY_RAIN
        71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.PARTLY_CLOUDY
    }

    fun windDirectionLabel(degrees: Float?): String {
        if (degrees == null) return "—"
        val normalized = ((degrees % 360f) + 360f) % 360f
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((normalized + 22.5f) / 45f).toInt() % 8
        return directions[index]
    }

    fun windDirectionPolishFull(degrees: Float?): String {
        if (degrees == null) return "Brak kierunku"
        return when (windDirectionLabel(degrees)) {
            "N" -> "Północny"
            "NE" -> "Płn.-wsch."
            "E" -> "Wschodni"
            "SE" -> "Płd.-wsch."
            "S" -> "Południowy"
            "SW" -> "Płd.-zach."
            "W" -> "Zachodni"
            "NW" -> "Płn.-zach."
            else -> "Zmienny"
        }
    }

    fun pressureDescription(hpa: Double?): String = when {
        hpa == null -> "Brak pomiaru"
        hpa < 1000.0 -> "Niskie"
        hpa <= 1020.0 -> "W normie"
        else -> "Wysokie"
    }

    fun humidityDescription(humidity: Double?): String = when {
        humidity == null -> "Brak pomiaru"
        humidity < 35.0 -> "Suche powietrze"
        humidity <= 65.0 -> "Komfortowa"
        humidity <= 82.0 -> "Wilgotno"
        else -> "Bardzo wilgotno"
    }

    fun uvRiskDescription(uv: Double?): String = when {
        uv == null -> "Brak danych"
        uv < 3.0 -> "Niski"
        uv < 6.0 -> "Umiarkowany"
        uv < 8.0 -> "Wysoki"
        uv < 11.0 -> "Bardzo wysoki"
        else -> "Ekstremalny"
    }

    fun formatTemperature(tempC: Double?): String {
        return tempC?.roundToInt()?.toString() ?: "—"
    }
}
