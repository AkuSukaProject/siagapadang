package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class RemainingRouteCalculatorTest {
    @Test
    fun `removes coordinates behind projected user position`() {
        val route = listOf(
            coordinate(0.0),
            coordinate(0.001),
            coordinate(0.002),
            coordinate(0.003),
        )
        val projectedLocation = coordinate(0.0014)

        val remaining = RemainingRouteCalculator.calculate(
            routeCoordinates = route,
            nearestRouteIndex = 1,
            nearestRouteCoordinate = projectedLocation,
        )

        assertEquals(
            listOf(projectedLocation, coordinate(0.002), coordinate(0.003)),
            remaining,
        )
    }

    @Test
    fun `does not duplicate a route vertex used as projected position`() {
        val route = listOf(coordinate(0.0), coordinate(0.001), coordinate(0.002))

        val remaining = RemainingRouteCalculator.calculate(
            routeCoordinates = route,
            nearestRouteIndex = 0,
            nearestRouteCoordinate = coordinate(0.001),
        )

        assertEquals(listOf(coordinate(0.001), coordinate(0.002)), remaining)
    }

    @Test
    fun `keeps full route until progress is available`() {
        val route = listOf(coordinate(0.0), coordinate(0.001))

        val remaining = RemainingRouteCalculator.calculate(
            routeCoordinates = route,
            nearestRouteIndex = null,
            nearestRouteCoordinate = null,
        )

        assertEquals(route, remaining)
    }

    @Test
    fun `returns destination only when route is completed`() {
        val route = listOf(coordinate(0.0), coordinate(0.001), coordinate(0.002))

        val remaining = RemainingRouteCalculator.calculate(
            routeCoordinates = route,
            nearestRouteIndex = route.lastIndex,
            nearestRouteCoordinate = route.last(),
        )

        assertEquals(listOf(route.last()), remaining)
    }

    private fun coordinate(latitude: Double) =
        GeoCoordinate(latitude = latitude, longitude = 100.0)
}
