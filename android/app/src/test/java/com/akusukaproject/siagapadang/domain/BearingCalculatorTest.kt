package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class BearingCalculatorTest {
    @Test
    fun `bearing ke utara mendekati nol derajat`() {
        val bearing = BearingCalculator.bearingDegrees(
            from = GeoCoordinate(latitude = -0.95, longitude = 100.4),
            to = GeoCoordinate(latitude = -0.94, longitude = 100.4),
        )

        assertEquals(0.0, bearing, 0.01)
    }

    @Test
    fun `rotasi relatif memilih arah terpendek`() {
        assertEquals(20f, BearingCalculator.relativeRotationDegrees(10.0, 350.0), 0.01f)
        assertEquals(-20f, BearingCalculator.relativeRotationDegrees(350.0, 10.0), 0.01f)
    }

    @Test
    fun `target berikutnya dipilih setelah titik rute terdekat`() {
        val route = listOf(
            GeoCoordinate(latitude = -0.9500, longitude = 100.4000),
            GeoCoordinate(latitude = -0.9500, longitude = 100.4010),
            GeoCoordinate(latitude = -0.9500, longitude = 100.4020),
        )
        val current = GeoCoordinate(latitude = -0.9500, longitude = 100.40095)

        val target = BearingCalculator.nextTarget(current, route)

        assertEquals(100.4020, target?.longitude ?: 0.0, 0.0)
    }
}
