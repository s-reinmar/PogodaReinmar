package com.example.domain.catalog

import com.example.domain.model.ImgwStationReference
import com.example.domain.model.StationMatch
import java.text.Normalizer
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Catalog of 62 official IMGW-PIB synoptic weather stations in Poland with GPS coordinates
 * and 2-digit TERYT voivodeship codes, plus Haversine nearest-station matching.
 */
object ImgwStationCatalog {

    private const val EARTH_RADIUS_KM = 6371.0

    val stations: List<ImgwStationReference> = listOf(
        // Mazowieckie (14)
        ImgwStationReference("12375", "Warszawa", 52.1657, 20.9671, "14"),
        ImgwStationReference("12270", "Mława", 53.1072, 20.3606, "14"),
        ImgwStationReference("12360", "Płock", 52.5878, 19.7258, "14"),
        ImgwStationReference("12385", "Siedlce", 52.1819, 22.2447, "14"),
        ImgwStationReference("12488", "Kozienice", 51.5653, 21.5453, "14"),
        // Małopolskie (12)
        ImgwStationReference("12566", "Kraków", 50.0777, 19.7848, "12"),
        ImgwStationReference("12625", "Zakopane", 49.2939, 19.9600, "12"),
        ImgwStationReference("12650", "Kasprowy Wierch", 49.2325, 19.9817, "12"),
        ImgwStationReference("12575", "Tarnów", 50.0303, 20.9850, "12"),
        ImgwStationReference("12660", "Nowy Sącz", 49.6272, 20.6889, "12"),
        // Pomorskie (22)
        ImgwStationReference("12155", "Gdańsk", 54.3775, 18.4662, "22"),
        ImgwStationReference("12135", "Hel", 54.6033, 18.8117, "22"),
        ImgwStationReference("12120", "Łeba", 54.7536, 17.5344, "22"),
        ImgwStationReference("12115", "Ustka", 54.5883, 16.8542, "22"),
        ImgwStationReference("12125", "Lębork", 54.5528, 17.7561, "22"),
        ImgwStationReference("12235", "Chojnice", 53.7153, 17.5333, "22"),
        // Dolnośląskie (02)
        ImgwStationReference("12424", "Wrocław", 51.1027, 16.8858, "02"),
        ImgwStationReference("12500", "Jelenia Góra", 50.9003, 15.7886, "02"),
        ImgwStationReference("12510", "Śnieżka", 50.7361, 15.7397, "02"),
        ImgwStationReference("12520", "Kłodzko", 50.4369, 16.6139, "02"),
        ImgwStationReference("12415", "Legnica", 51.1925, 16.2089, "02"),
        // Wielkopolskie (30)
        ImgwStationReference("12330", "Poznań", 52.4210, 16.8263, "30"),
        ImgwStationReference("12230", "Piła", 53.1306, 16.7483, "30"),
        ImgwStationReference("12418", "Leszno", 51.8358, 16.5356, "30"),
        ImgwStationReference("12435", "Kalisz", 51.7811, 18.0811, "30"),
        ImgwStationReference("12345", "Koło", 52.2014, 18.6606, "30"),
        // Zachodniopomorskie (32)
        ImgwStationReference("12205", "Szczecin", 53.3950, 14.6225, "32"),
        ImgwStationReference("12200", "Świnoujście", 53.9233, 14.2422, "32"),
        ImgwStationReference("12100", "Kołobrzeg", 54.1839, 15.5819, "32"),
        ImgwStationReference("12105", "Koszalin", 54.2044, 16.1553, "32"),
        ImgwStationReference("12215", "Szczecinek", 53.7136, 16.7064, "32"),
        ImgwStationReference("12210", "Resko", 53.7678, 15.4125, "32"),
        // Łódzkie (10)
        ImgwStationReference("12465", "Łódź", 51.7219, 19.3981, "10"),
        ImgwStationReference("12455", "Wieluń", 51.2106, 18.5567, "10"),
        ImgwStationReference("12469", "Sulejów", 51.3536, 19.8658, "10"),
        // Śląskie (24)
        ImgwStationReference("12560", "Katowice", 50.2406, 19.0328, "24"),
        ImgwStationReference("12550", "Częstochowa", 50.8118, 19.0919, "24"),
        ImgwStationReference("12600", "Bielsko-Biała", 49.8069, 19.0019, "24"),
        ImgwStationReference("12540", "Racibórz", 50.0608, 18.1908, "24"),
        // Podlaskie (20)
        ImgwStationReference("12295", "Białystok", 53.1072, 23.1622, "20"),
        ImgwStationReference("12195", "Suwałki", 54.1306, 22.9489, "20"),
        // Lubelskie (06)
        ImgwStationReference("12495", "Lublin", 51.2167, 22.3931, "06"),
        ImgwStationReference("12399", "Terespol", 52.0786, 23.6219, "06"),
        ImgwStationReference("12497", "Włodawa", 51.5536, 23.5297, "06"),
        ImgwStationReference("12595", "Zamość", 50.7031, 23.2089, "06"),
        // Podkarpackie (18)
        ImgwStationReference("12580", "Rzeszów", 50.1100, 22.0190, "18"),
        ImgwStationReference("12670", "Krosno", 49.6836, 21.7689, "18"),
        ImgwStationReference("12690", "Lesko", 49.4664, 22.3417, "18"),
        ImgwStationReference("12695", "Przemyśl", 49.8011, 22.7728, "18"),
        // Warmińsko-Mazurskie (28)
        ImgwStationReference("12272", "Olsztyn", 53.7672, 20.4222, "28"),
        ImgwStationReference("12160", "Elbląg", 54.1706, 19.4317, "28"),
        ImgwStationReference("12185", "Kętrzyn", 54.0672, 21.3700, "28"),
        ImgwStationReference("12280", "Mikołajki", 53.7889, 21.5897, "28"),
        // Świętokrzyskie (26)
        ImgwStationReference("12570", "Kielce", 50.8103, 20.6922, "26"),
        ImgwStationReference("12585", "Sandomierz", 50.6967, 21.7161, "26"),
        // Opolskie (16)
        ImgwStationReference("12530", "Opole", 50.6269, 17.9686, "16"),
        // Lubuskie (08)
        ImgwStationReference("12400", "Zielona Góra", 51.9300, 15.5244, "08"),
        ImgwStationReference("12300", "Gorzów Wielkopolski", 52.7422, 15.2764, "08"),
        ImgwStationReference("12310", "Słubice", 52.3489, 14.6186, "08"),
        // Kujawsko-Pomorskie (04)
        ImgwStationReference("12250", "Toruń", 53.0422, 18.5956, "04"),
        ImgwStationReference("12240", "Bydgoszcz", 53.0967, 17.9778, "04"),
        ImgwStationReference("12220", "Włocławek", 52.6594, 19.0686, "04"),
    )

    private val voivodeshipNameToTeryt: Map<String, String> = mapOf(
        "dolnoslaskie" to "02",
        "lower silesia" to "02",
        "kujawsko-pomorskie" to "04",
        "kuyavia-pomerania" to "04",
        "lubelskie" to "06",
        "lublin" to "06",
        "lubuskie" to "08",
        "lubusz" to "08",
        "lodzkie" to "10",
        "lodz" to "10",
        "malopolskie" to "12",
        "lesser poland" to "12",
        "mazowieckie" to "14",
        "masovia" to "14",
        "opolskie" to "16",
        "opole" to "16",
        "podkarpackie" to "18",
        "subcarpathia" to "18",
        "podlaskie" to "20",
        "podlasie" to "20",
        "pomorskie" to "22",
        "pomerania" to "22",
        "slaskie" to "24",
        "silesia" to "24",
        "swietokrzyskie" to "26",
        "holy cross" to "26",
        "warminsko-mazurskie" to "28",
        "warmia-masuria" to "28",
        "wielkopolskie" to "30",
        "greater poland" to "30",
        "zachodniopomorskie" to "32",
        "west pomerania" to "32",
    )

    /**
     * Calculates great-circle distance in kilometers between two WGS-84 coordinates using Haversine.
     */
    fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
            cos(radLat1) * cos(radLat2) * sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Finds the closest station from [stations] whose ID or normalized name is present in the
     * active IMGW synop feed. Returns null if none of the catalog stations exist in the active feed.
     */
    fun findNearestActiveStation(
        targetLat: Double,
        targetLon: Double,
        activeIds: Set<String>,
        activeNormalizedNames: Set<String>,
    ): StationMatch? {
        return stations
            .asSequence()
            .filter { station ->
                station.id in activeIds || normalizeText(station.name) in activeNormalizedNames
            }
            .map { station ->
                StationMatch(
                    station = station,
                    distanceKm = haversineDistanceKm(
                        targetLat,
                        targetLon,
                        station.latitude,
                        station.longitude,
                    ),
                )
            }
            .minByOrNull { it.distanceKm }
    }

    /**
     * Finds the geographically closest catalog station regardless of live feed availability
     * (useful for resolving the local voivodeship TERYT prefix for weather warnings).
     */
    fun findNearestCatalogStation(targetLat: Double, targetLon: Double): StationMatch {
        return stations
            .map { station ->
                StationMatch(
                    station = station,
                    distanceKm = haversineDistanceKm(
                        targetLat,
                        targetLon,
                        station.latitude,
                        station.longitude,
                    ),
                )
            }
            .minBy { it.distanceKm }
    }

    /**
     * Resolves a 2-digit Polish TERYT voivodeship code from an admin1 name (from Open-Meteo or Geocoder),
     * falling back to the nearest IMGW station's voivodeship code.
     */
    fun resolveVoivodeshipTeryt(
        explicitTeryt: String?,
        admin1: String?,
        fallbackStation: ImgwStationReference?,
    ): String? {
        if (!explicitTeryt.isNullOrBlank()) return explicitTeryt
        if (!admin1.isNullOrBlank()) {
            val normalized = normalizeText(admin1)
                .replace("wojewodztwo ", "")
                .replace("woj. ", "")
                .trim()
            voivodeshipNameToTeryt.entries.firstOrNull { (key, _) ->
                normalized == key || normalized.contains(key)
            }?.let { return it.value }
        }
        return fallbackStation?.voivodeshipTeryt
    }

    /**
     * Heuristic check for whether WGS-84 coordinates fall within Poland's synoptic network coverage
     * when no explicit ISO country code is available (e.g., GPS fix without Geocoder).
     */
    fun isLikelyInPoland(latitude: Double, longitude: Double): Boolean {
        if (latitude !in 49.0..54.9 || longitude !in 14.1..24.2) return false
        return findNearestCatalogStation(latitude, longitude).distanceKm <= 85.0
    }

    /**
     * Authoritatively determines whether [location] should query Poland's IMGW-PIB endpoints.
     * Uses ISO-3166-1 alpha-2 [com.example.domain.model.GeoLocation.countryCode] whenever available,
     * falling back to TERYT code or geographic proximity to the IMGW station network.
     */
    fun isPolishLocation(location: com.example.domain.model.GeoLocation): Boolean {
        val code = location.countryCode?.trim()
        if (!code.isNullOrEmpty()) {
            return code.equals("PL", ignoreCase = true)
        }
        if (!location.terytVoivodeshipCode.isNullOrBlank()) {
            return true
        }
        return isLikelyInPoland(location.latitude, location.longitude)
    }

    fun normalizeText(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("ł", "l")
            .replace("Ł", "l")
            .lowercase()
            .trim()
    }
}
