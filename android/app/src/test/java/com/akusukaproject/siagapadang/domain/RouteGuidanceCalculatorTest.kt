package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGuidanceCalculatorTest {
    @Test
    fun `keeps straight instruction while right turn is still far away`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.001, 0.001),
            ),
        )

        assertEquals(ManeuverType.STRAIGHT, guidance?.currentInstruction?.type)
        assertEquals(ManeuverType.RIGHT, guidance?.instructions?.get(1)?.type)
        assertTrue(guidance!!.currentInstruction.distanceMeters in 105..118)
    }

    @Test
    fun `shows right turn after user is close to intersection`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0007, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.001, 0.001),
            ),
        )

        assertEquals(ManeuverType.RIGHT, guidance?.currentInstruction?.type)
        assertTrue(guidance!!.currentInstruction.distanceMeters in 28..39)
    }

    @Test
    fun `classifies a nearby left turn from route geometry`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0007, 0.0),
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
            currentLocation = coordinate(0.0007, 0.0),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.0002, 0.0001),
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

    @Test
    fun `does not move route progress backwards after gps jitter`() {
        val route = listOf(
            coordinate(0.0, 0.0),
            coordinate(0.001, 0.0),
            coordinate(0.002, 0.0),
            coordinate(0.003, 0.0),
            coordinate(0.004, 0.0),
        )

        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0011, 0.0),
            routeCoordinates = route,
            minimumRouteIndex = 3,
        )

        assertEquals(3, guidance?.nearestRouteIndex)
    }

    @Test
    fun `reports distance from route for reliable deviation warning`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0, 0.001),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
            ),
        )

        assertTrue(guidance!!.distanceFromRouteMeters in 105..118)
    }

    @Test
    fun `targets nearest road point while user is outside route`() {
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = coordinate(0.0005, 0.001),
            routeCoordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
            ),
            deviceHeadingDegrees = 0f,
        )

        assertEquals(true, guidance?.isApproachingRoute)
        assertEquals(ManeuverType.LEFT, guidance?.currentInstruction?.type)
        assertEquals(0.0005, guidance?.nearestRouteCoordinate?.latitude ?: 0.0, 0.00001)
        assertEquals(0.0, guidance?.nearestRouteCoordinate?.longitude ?: 1.0, 0.00001)
    }

    @Test
    fun `recalculates road approach direction from device heading`() {
        val route = listOf(
            coordinate(0.0, 0.0),
            coordinate(0.001, 0.0),
        )
        val location = coordinate(0.0005, 0.001)

        val facingNorth = RouteGuidanceCalculator.calculate(
            currentLocation = location,
            routeCoordinates = route,
            deviceHeadingDegrees = 0f,
        )
        val facingSouth = RouteGuidanceCalculator.calculate(
            currentLocation = location,
            routeCoordinates = route,
            deviceHeadingDegrees = 180f,
        )

        assertEquals(ManeuverType.LEFT, facingNorth?.currentInstruction?.type)
        assertEquals(ManeuverType.RIGHT, facingSouth?.currentInstruction?.type)
    }

    private fun coordinate(latitude: Double, longitude: Double) =
        GeoCoordinate(latitude = latitude, longitude = longitude)
}
