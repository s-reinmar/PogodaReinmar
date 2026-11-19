package com.example

import com.example.domain.catalog.ImgwStationCatalog
import com.example.domain.model.WeatherCondition
import com.example.domain.util.WeatherCodeMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImgwStationCatalogTest {

    @Test
    fun `catalog contains at least 60 official Polish IMGW stations`() {
        assertTrue(ImgwStationCatalog.stations.size >= 60)
    }

    @Test
    fun `finds nearest active IMGW station for Krakow coordinates`() {
        val activeIds = setOf("12375", "12566", "12155", "12625")
        val match = ImgwStationCatalog.findNearestActiveStation(
            targetLat = 50.0647,
            targetLon = 19.9450,
            activeIds = activeIds,
            activeNormalizedNames = emptySet(),
        )
        assertNotNull(match)
        assertEquals("12566", match?.station?.id)
        assertEquals("Kraków", match?.station?.name)
        assertTrue((match?.distanceKm ?: 999.0) < 20.0)
    }

    @Test
    fun `skips inactive closest station and selects next nearest active station`() {
        // Coordinates in Zakopane, but Zakopane (12625) is missing from activeIds; Kasprowy Wierch (12650) is active
        val activeIds = setOf("12375", "12650", "12566")
        val match = ImgwStationCatalog.findNearestActiveStation(
            targetLat = 49.2992,
            targetLon = 19.9496,
            activeIds = activeIds,
            activeNormalizedNames = emptySet(),
        )
        assertNotNull(match)
        assertEquals("12650", match?.station?.id)
    }

    @Test
    fun `resolves Polish voivodeship TERYT codes accurately`() {
        assertEquals(
            "12",
            ImgwStationCatalog.resolveVoivodeshipTeryt(null, "Województwo małopolskie", null),
        )
        assertEquals(
            "22",
            ImgwStationCatalog.resolveVoivodeshipTeryt(null, "Pomorskie", null),
        )
    }

    @Test
    fun `upgrades dry WMO code when IMGW station reports active rainfall`() {
        val resolved = WeatherCodeMapper.resolveCondition(
            wmoCode = 2,
            imgwPrecipitationMm = 1.8,
            temperatureC = 9.0,
        )
        assertEquals(WeatherCondition.RAIN, resolved)
    }
}
