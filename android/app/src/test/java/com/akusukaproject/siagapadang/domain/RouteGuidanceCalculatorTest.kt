package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGuidanceCalculatorTest {
    @Test
    fun `classifies a right turn from route geometry`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.001, 0.001),
            ),
        )

        assertEquals(ManeuverType.RIGHT, guidance?.currentInstruction?.type)
        assertTrue(guidance!!.currentInstruction.distanceMeters in 105..118)
    }

    @Test
    fun `classifies a left turn from route geometry`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.001, -0.001),
            ),
        )

        assertEquals(ManeuverType.LEFT, guidance?.currentInstruction?.type)
    }

    @Test
    fun `uses straight instruction when route has no turn`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.002, 0.0),
            ),
        )

        assertEquals(ManeuverType.STRAIGHT, guidance?.currentInstruction?.type)
        assertTrue(guidance!!.remainingDistanceMeters in 215..228)
    }

    @Test
    fun `classifies a u turn from route geometry`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.0, 0.0),
            ),
        )

        assertEquals(ManeuverType.U_TURN, guidance?.currentInstruction?.type)
    }

    @Test
    fun `reports arrival near final coordinate`() {
        val destination = coordinate(0.001, 0.0)
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = destination,
            routeCoordinates = listOf(coordinate(0.0, 0.0), destination),
        )

        assertEquals(ManeuverType.ARRIVE, guidance?.currentInstruction?.type)
        assertEquals(0, guidance?.remainingDistanceMeters)
    }

    private fun coordinate(latitude: Double, longitude: Double) =
        GeoCoordinate(latitude = latitude, longitude = longitude)
}
