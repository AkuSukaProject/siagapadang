package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DirectOrientationCalculatorTest {
    @Test
    fun `menghasilkan arah dan jarak lurus ke tujuan`() {
        val orientation = DirectOrientationCalculator.calculate(
            currentLocation = GeoCoordinate(latitude = -0.9500, longitude = 100.4000),
            destinationName = "TES Contoh",
            destinationCoordinate = GeoCoordinate(latitude = -0.9400, longitude = 100.4000),
            routeCoordinates = emptyList(),
        )

        assertNotNull(orientation)
        assertEquals("TES Contoh", orientation?.destinationName)
        assertEquals(0.0, orientation?.bearingDegrees ?: -1.0, 0.01)
        assertEquals(1_112, orientation?.distanceMeters)
    }

    @Test
    fun `ujung geometri rute digunakan ketika koordinat tujuan tidak tersedia`() {
        val routeEnd = GeoCoordinate(latitude = -0.9500, longitude = 100.4010)

        val orientation = DirectOrientationCalculator.calculate(
            currentLocation = GeoCoordinate(latitude = -0.9500, longitude = 100.4000),
            destinationName = "TES Contoh",
            destinationCoordinate = null,
            routeCoordinates = listOf(
                GeoCoordinate(latitude = -0.9500, longitude = 100.4005),
                routeEnd,
            ),
        )

        assertNotNull(orientation)
        assertEquals(90.0, orientation?.bearingDegrees ?: -1.0, 0.02)
    }
}
