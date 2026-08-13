package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class PolylineSimplifierTest {
    @Test
    fun `removes tiny render noise but keeps endpoints`() {
        val first = coordinate(0.0, 0.0)
        val last = coordinate(0.0, 0.002)

        val simplified = PolylineSimplifier.simplify(
            coordinates = listOf(first, coordinate(0.000001, 0.001), last),
            toleranceMeters = 2.0,
        )

        assertEquals(listOf(first, last), simplified)
    }

    @Test
    fun `keeps a meaningful road turn`() {
        val coordinates = listOf(
            coordinate(0.0, 0.0),
            coordinate(0.001, 0.001),
            coordinate(0.0, 0.002),
        )

        assertEquals(
            coordinates,
            PolylineSimplifier.simplify(coordinates, toleranceMeters = 2.0),
        )
    }

    private fun coordinate(latitude: Double, longitude: Double) =
        GeoCoordinate(latitude = latitude, longitude = longitude)
}
