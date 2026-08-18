package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteProjectionCalculatorTest {
    @Test
    fun `projects location to middle of nearest road segment`() {
        val projection = RouteProjectionCalculator.findNearest(
            location = coordinate(0.001, 0.001),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.0, 0.002),
            ),
        )

        assertEquals(0, projection?.segmentStartIndex)
        assertEquals(0.0, projection?.coordinate?.latitude ?: 1.0, 0.00001)
        assertEquals(0.001, projection?.coordinate?.longitude ?: 0.0, 0.00001)
        assertTrue(projection!!.distanceMeters in 105.0..118.0)
    }

    @Test
    fun `does not search segments before minimum progress`() {
        val projection = RouteProjectionCalculator.findNearest(
            location = coordinate(0.0001, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.002, 0.0),
                coordinate(0.003, 0.0),
            ),
            minimumRouteIndex = 2,
        )

        assertEquals(2, projection?.segmentStartIndex)
    }

    @Test
    fun `does not move backwards within current segment after gps jitter`() {
        val projection = RouteProjectionCalculator.findNearest(
            location = coordinate(0.0002, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.002, 0.0),
            ),
            minimumRouteIndex = 0,
            minimumSegmentFraction = 0.7,
        )

        assertEquals(0, projection?.segmentStartIndex)
        assertEquals(0.7, projection?.segmentFraction ?: 0.0, 0.00001)
        assertEquals(0.0007, projection?.coordinate?.latitude ?: 0.0, 0.00001)
    }

    private fun coordinate(latitude: Double, longitude: Double) =
        GeoCoordinate(latitude = latitude, longitude = longitude)
}
