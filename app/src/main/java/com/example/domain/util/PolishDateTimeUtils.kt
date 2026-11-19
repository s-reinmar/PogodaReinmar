package com.example.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Pure API-24-compatible date/time formatting and sunrise/sunset day-progress calculations
 * supporting Europe/Warsaw (default for Poland / IMGW) as well as any IANA timezone returned
 * by Open-Meteo for international locations.
 */
object PolishDateTimeUtils {

    private val warsawTimeZone: TimeZone = TimeZone.getTimeZone("Europe/Warsaw")
    private val polishLocale: Locale = Locale("pl", "PL")

    private fun resolveTimeZone(timezoneId: String?): TimeZone =
        if (timezoneId.isNullOrBlank()) warsawTimeZone else TimeZone.getTimeZone(timezoneId)

    /**
     * Extracts "HH:mm" from ISO-8601 datetime strings like "2026-11-19T07:04".
     */
    fun extractTimeLabel(isoDateTime: String?): String? {
        if (isoDateTime.isNullOrBlank()) return null
        val tIndex = isoDateTime.indexOf('T')
        if (tIndex == -1 || tIndex + 5 > isoDateTime.length) return null
        return isoDateTime.substring(tIndex + 1, tIndex + 6)
    }

    /**
     * Parses "yyyy-MM-dd'T'HH:mm" in the specified IANA timezone (default Europe/Warsaw) to epoch millis.
     */
    fun parseIsoDateTimeMillis(
        isoDateTime: String?,
        timezoneId: String? = null,
    ): Long? {
        if (isoDateTime.isNullOrBlank()) return null
        val tz = resolveTimeZone(timezoneId)
        return runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
                timeZone = tz
                isLenient = false
            }
            fmt.parse(isoDateTime)?.time
        }.getOrNull()
    }

    /**
     * Formats IMGW warning timestamps ("2026-11-19 20:00:00") into compact Polish labels ("19.11, 20:00").
     */
    fun formatImgwTimestamp(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = warsawTimeZone
            }
            val parsed = parser.parse(raw) ?: return raw
            val formatter = SimpleDateFormat("dd.MM, HH:mm", polishLocale).apply {
                timeZone = warsawTimeZone
            }
            formatter.format(parsed)
        }.getOrDefault(raw)
    }

    /**
     * Formats an ISO date ("2026-11-19") into localized weekday abbreviation plus short date ("19.11").
     * Today/Tomorrow overrides are resolved via Android string resources in the UI layer.
     */
    fun formatDailyLabels(
        isoDate: String,
        index: Int,
        locale: Locale = Locale.getDefault(),
        timezoneId: String? = null,
    ): Pair<String, String> {
        val tz = resolveTimeZone(timezoneId)
        val parsedDate = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = tz
            }.parse(isoDate)
        }.getOrNull() ?: return Pair(isoDate, "")

        val shortDate = SimpleDateFormat("dd.MM", locale).apply {
            timeZone = tz
        }.format(parsedDate)

        val dayLabel = if (locale.language.equals("pl", ignoreCase = true)) {
            val calendar = Calendar.getInstance(tz).apply { time = parsedDate }
            when (index) {
                0 -> "Dziś"
                1 -> "Jutro"
                else -> when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Pon."
                    Calendar.TUESDAY -> "Wt."
                    Calendar.WEDNESDAY -> "Śr."
                    Calendar.THURSDAY -> "Czw."
                    Calendar.FRIDAY -> "Pt."
                    Calendar.SATURDAY -> "Sob."
                    Calendar.SUNDAY -> "Niedz."
                    else -> shortDate
                }
            }
        } else {
            val rawShort = SimpleDateFormat("EEE", locale).apply {
                timeZone = tz
            }.format(parsedDate)
            rawShort.replaceFirstChar { it.titlecase(locale) }
        }
        return Pair(dayLabel, shortDate)
    }

    /**
     * Determines whether current time in [timezoneId] falls between sunrise and sunset.
     * Falls back to [fallbackIsDay] if sunrise/sunset strings cannot be parsed.
     */
    fun isCurrentlyDaytime(
        sunriseIso: String?,
        sunsetIso: String?,
        fallbackIsDay: Boolean,
        timezoneId: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val sunrise = parseIsoDateTimeMillis(sunriseIso, timezoneId)
        val sunset = parseIsoDateTimeMillis(sunsetIso, timezoneId)
        if (sunrise == null || sunset == null || sunset <= sunrise) {
            return fallbackIsDay
        }
        return nowMillis in sunrise..sunset
    }

    /**
     * Computes solar arc progress in [0f..1f] between sunrise and sunset (or null if unavailable).
     */
    fun computeSolarProgress(
        sunriseIso: String?,
        sunsetIso: String?,
        timezoneId: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): Float? {
        val sunrise = parseIsoDateTimeMillis(sunriseIso, timezoneId) ?: return null
        val sunset = parseIsoDateTimeMillis(sunsetIso, timezoneId) ?: return null
        if (sunset <= sunrise) return null
        return ((nowMillis - sunrise).toFloat() / (sunset - sunrise).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Returns current hour key in [timezoneId] ("yyyy-MM-dd'T'HH:00") for slicing 24h hourly forecast.
     */
    fun currentHourIso(
        timezoneId: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).apply {
            timeZone = resolveTimeZone(timezoneId)
        }
        return fmt.format(nowMillis)
    }

    /**
     * Returns current hour key in Europe/Warsaw ("yyyy-MM-dd'T'HH:00").
     */
    fun currentWarsawHourIso(nowMillis: Long = System.currentTimeMillis()): String =
        currentHourIso(timezoneId = null, nowMillis = nowMillis)
}
