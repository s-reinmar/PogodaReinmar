package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.domain.model.WeatherCondition

// Core Atmospheric Brand Colors
val SkyPrimary = Color(0xFF38BDF8)
val SkyOnPrimary = Color(0xFF002438)
val SkyPrimaryContainer = Color(0xFF0C4A6E)
val SkyOnPrimaryContainer = Color(0xFFE0F2FE)

val SunAmber = Color(0xFFFBBF24)
val SunGold = Color(0xFFF59E0B)
val RainCyan = Color(0xFF67E8F9)
val IceBlue = Color(0xFFBAE6FD)

// Warning Severity Colors (IMGW-PIB Stopień 1, 2, 3)
val WarningLevel1Bg = Color(0xFF422006)
val WarningLevel1Border = Color(0xFFFACC15)
val WarningLevel1Accent = Color(0xFFFDE047)

val WarningLevel2Bg = Color(0xFF431407)
val WarningLevel2Border = Color(0xFFFB923C)
val WarningLevel2Accent = Color(0xFFFDBA74)

val WarningLevel3Bg = Color(0xFF450A0A)
val WarningLevel3Border = Color(0xFFF87171)
val WarningLevel3Accent = Color(0xFFFCA5A5)

// Thermal Scale Colors for 7-day Temperature Range Bar
val TempFreezing = Color(0xFF60A5FA)
val TempCool = Color(0xFF38BDF8)
val TempMild = Color(0xFF34D399)
val TempWarm = Color(0xFFFBBF24)
val TempHot = Color(0xFFF97316)

/**
 * Dynamic sky atmosphere gradient palette reacting to day/night and live weather conditions.
 */
data class SkyPalette(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val cardContainer: Color,
    val cardHighlight: Color,
    val cardBorder: Color,
    val accent: Color,
    val showStars: Boolean,
)

object SkyPalettes {

    private val ClearDay = SkyPalette(
        top = Color(0xFF0C4A8A),
        middle = Color(0xFF1D6FB8),
        bottom = Color(0xFF3B93D4),
        glowPrimary = Color(0x55FBBF24),
        glowSecondary = Color(0x337DD3FC),
        cardContainer = Color(0x2E071B34),
        cardHighlight = Color(0x420B2A4D),
        cardBorder = Color(0x3DFFFFFF),
        accent = Color(0xFFFDE047),
        showStars = false,
    )

    private val PartlyCloudyDay = SkyPalette(
        top = Color(0xFF164373),
        middle = Color(0xFF285D91),
        bottom = Color(0xFF467CA8),
        glowPrimary = Color(0x3DFCD34D),
        glowSecondary = Color(0x2EBAE6FD),
        cardContainer = Color(0x330A1E35),
        cardHighlight = Color(0x4A123052),
        cardBorder = Color(0x38FFFFFF),
        accent = Color(0xFF7DD3FC),
        showStars = false,
    )

    private val OvercastDay = SkyPalette(
        top = Color(0xFF283A52),
        middle = Color(0xFF3B4F69),
        bottom = Color(0xFF536982),
        glowPrimary = Color(0x28CBD5E1),
        glowSecondary = Color(0x2294A3B8),
        cardContainer = Color(0x3D111C2B),
        cardHighlight = Color(0x521C2B40),
        cardBorder = Color(0x33E2E8F0),
        accent = Color(0xFF93C5FD),
        showStars = false,
    )

    private val RainStorm = SkyPalette(
        top = Color(0xFF0F1D33),
        middle = Color(0xFF1C2E4A),
        bottom = Color(0xFF2A4163),
        glowPrimary = Color(0x3338BDF8),
        glowSecondary = Color(0x246366F1),
        cardContainer = Color(0x47091324),
        cardHighlight = Color(0x5C122340),
        cardBorder = Color(0x3393C5FD),
        accent = Color(0xFF67E8F9),
        showStars = false,
    )

    private val SnowAtmosphere = SkyPalette(
        top = Color(0xFF1E3A5F),
        middle = Color(0xFF355C85),
        bottom = Color(0xFF5A82AD),
        glowPrimary = Color(0x3DE0F2FE),
        glowSecondary = Color(0x29BAE6FD),
        cardContainer = Color(0x380C1F38),
        cardHighlight = Color(0x4F163459),
        cardBorder = Color(0x47E0F2FE),
        accent = Color(0xFFE0F2FE),
        showStars = false,
    )

    private val ClearNight = SkyPalette(
        top = Color(0xFF060E21),
        middle = Color(0xFF0E1D3D),
        bottom = Color(0xFF1A3159),
        glowPrimary = Color(0x2E38BDF8),
        glowSecondary = Color(0x24818CF8),
        cardContainer = Color(0x47071022),
        cardHighlight = Color(0x610F1F3E),
        cardBorder = Color(0x30A5B4FC),
        accent = Color(0xFF7DD3FC),
        showStars = true,
    )

    private val CloudyNight = SkyPalette(
        top = Color(0xFF0B1324),
        middle = Color(0xFF17233B),
        bottom = Color(0xFF243452),
        glowPrimary = Color(0x2464748B),
        glowSecondary = Color(0x1E38BDF8),
        cardContainer = Color(0x4A080F1E),
        cardHighlight = Color(0x61132138),
        cardBorder = Color(0x2E94A3B8),
        accent = Color(0xFF93C5FD),
        showStars = false,
    )

    fun resolve(condition: WeatherCondition, isDaytime: Boolean): SkyPalette {
        if (!isDaytime) {
            return when (condition) {
                WeatherCondition.CLEAR,
                WeatherCondition.MAINLY_CLEAR -> ClearNight
                WeatherCondition.RAIN,
                WeatherCondition.HEAVY_RAIN,
                WeatherCondition.THUNDERSTORM,
                WeatherCondition.FREEZING_RAIN -> RainStorm
                else -> CloudyNight
            }
        }
        return when (condition) {
            WeatherCondition.CLEAR,
            WeatherCondition.MAINLY_CLEAR -> ClearDay
            WeatherCondition.PARTLY_CLOUDY -> PartlyCloudyDay
            WeatherCondition.OVERCAST,
            WeatherCondition.FOG -> OvercastDay
            WeatherCondition.DRIZZLE,
            WeatherCondition.RAIN,
            WeatherCondition.HEAVY_RAIN,
            WeatherCondition.FREEZING_RAIN,
            WeatherCondition.THUNDERSTORM -> RainStorm
            WeatherCondition.SNOW -> SnowAtmosphere
        }
    }
}
