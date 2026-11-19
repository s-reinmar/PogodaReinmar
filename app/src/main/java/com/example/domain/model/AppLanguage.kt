package com.example.domain.model

import java.util.Locale

/**
 * Supported in-app display languages for UI labels, weather conditions, dates, and city geocoding.
 */
enum class AppLanguage(
    val code: String,
    val shortTag: String,
    val nativeName: String,
    val warsawCityName: String,
    val warsawRegionName: String,
) {
    POLISH(
        code = "pl",
        shortTag = "PL",
        nativeName = "Polski",
        warsawCityName = "Warszawa",
        warsawRegionName = "Województwo mazowieckie",
    ),
    ENGLISH(
        code = "en",
        shortTag = "EN",
        nativeName = "English",
        warsawCityName = "Warsaw",
        warsawRegionName = "Masovian Voivodeship",
    ),
    GERMAN(
        code = "de",
        shortTag = "DE",
        nativeName = "Deutsch",
        warsawCityName = "Warschau",
        warsawRegionName = "Woiwodschaft Masowien",
    );

    fun toLocale(): Locale = when (this) {
        POLISH -> Locale("pl", "PL")
        ENGLISH -> Locale("en", "US")
        GERMAN -> Locale("de", "DE")
    }

    fun localizedWarsaw(): GeoLocation = GeoLocation.WARSAW.copy(
        name = warsawCityName,
        admin1 = warsawRegionName,
    )

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: POLISH

        fun defaultForSystem(): AppLanguage {
            val sysLang = Locale.getDefault().language
            return entries.firstOrNull { it.code.equals(sysLang, ignoreCase = true) } ?: POLISH
        }
    }
}
