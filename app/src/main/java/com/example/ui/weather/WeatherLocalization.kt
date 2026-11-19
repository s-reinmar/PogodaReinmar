package com.example.ui.weather

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.WeatherCondition
import com.example.domain.util.WeatherCodeMapper

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.POLISH }
private val LocalLocalizedResources = compositionLocalOf<Resources?> { null }

/**
 * Provides localized [Resources] for the selected in-app [AppLanguage] WITHOUT replacing
 * [LocalContext] or [LocalConfiguration], preserving the host Activity window context for
 * popups, dialogs, and system insets.
 */
@Composable
fun ProvideLocalizedAppContext(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    val localizedResources = remember(language, baseConfiguration) {
        val config = Configuration(baseConfiguration).apply {
            setLocale(language.toLocale())
        }
        baseContext.createConfigurationContext(config).resources
    }

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalLocalizedResources provides localizedResources,
        content = content,
    )
}

/**
 * Resolves a string resource using the active in-app [AppLanguage] resources.
 */
@Composable
fun appString(@StringRes id: Int, vararg formatArgs: Any): String {
    val resources = LocalLocalizedResources.current ?: LocalContext.current.resources
    return if (formatArgs.isEmpty()) {
        resources.getString(id)
    } else {
        resources.getString(id, *formatArgs)
    }
}

@Composable
fun localizedConditionLabel(condition: WeatherCondition): String = when (condition) {
    WeatherCondition.CLEAR -> appString(R.string.condition_clear)
    WeatherCondition.MAINLY_CLEAR -> appString(R.string.condition_mainly_clear)
    WeatherCondition.PARTLY_CLOUDY -> appString(R.string.condition_partly_cloudy)
    WeatherCondition.OVERCAST -> appString(R.string.condition_overcast)
    WeatherCondition.FOG -> appString(R.string.condition_fog)
    WeatherCondition.DRIZZLE -> appString(R.string.condition_drizzle)
    WeatherCondition.RAIN -> appString(R.string.condition_rain)
    WeatherCondition.HEAVY_RAIN -> appString(R.string.condition_heavy_rain)
    WeatherCondition.FREEZING_RAIN -> appString(R.string.condition_freezing_rain)
    WeatherCondition.HAIL -> appString(R.string.condition_hail)
    WeatherCondition.SNOW -> appString(R.string.condition_snow)
    WeatherCondition.THUNDERSTORM -> appString(R.string.condition_thunderstorm)
}

@Composable
fun localizedWindDirectionName(degrees: Float?): String {
    if (degrees == null) return appString(R.string.wind_dir_unknown)
    return when (WeatherCodeMapper.windDirectionLabel(degrees)) {
        "N" -> appString(R.string.wind_dir_n)
        "NE" -> appString(R.string.wind_dir_ne)
        "E" -> appString(R.string.wind_dir_e)
        "SE" -> appString(R.string.wind_dir_se)
        "S" -> appString(R.string.wind_dir_s)
        "SW" -> appString(R.string.wind_dir_sw)
        "W" -> appString(R.string.wind_dir_w)
        "NW" -> appString(R.string.wind_dir_nw)
        else -> appString(R.string.wind_dir_unknown)
    }
}

@Composable
fun localizedPressureDescription(hpa: Double?): String = when {
    hpa == null -> appString(R.string.metric_no_data)
    hpa < 1000.0 -> appString(R.string.pressure_low)
    hpa <= 1020.0 -> appString(R.string.pressure_normal)
    else -> appString(R.string.pressure_high)
}

@Composable
fun localizedHumidityDescription(humidity: Double?): String = when {
    humidity == null -> appString(R.string.metric_no_data)
    humidity < 35.0 -> appString(R.string.humidity_dry)
    humidity <= 65.0 -> appString(R.string.humidity_comfortable)
    humidity <= 82.0 -> appString(R.string.humidity_humid)
    else -> appString(R.string.humidity_very_humid)
}

@Composable
fun localizedPrecipitationDescription(precipMm: Double?): String = when {
    precipMm == null -> appString(R.string.metric_no_data)
    precipMm == 0.0 -> appString(R.string.precip_none)
    precipMm < 2.5 -> appString(R.string.precip_light)
    precipMm < 10.0 -> appString(R.string.precip_moderate)
    else -> appString(R.string.precip_heavy)
}

@Composable
fun localizedUvRiskDescription(uv: Double?): String = when {
    uv == null -> appString(R.string.metric_no_data)
    uv < 3.0 -> appString(R.string.uv_low)
    uv < 6.0 -> appString(R.string.uv_moderate)
    uv < 8.0 -> appString(R.string.uv_high)
    uv < 11.0 -> appString(R.string.uv_very_high)
    else -> appString(R.string.uv_extreme)
}
