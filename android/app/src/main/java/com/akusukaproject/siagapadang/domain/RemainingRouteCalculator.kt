package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate

/** Returns only the active part of a route, starting at the user's projected progress. */
object RemainingRouteCalculator {
    fun calculate(
        routeCoordinates: List<GeoCoordinate>,
        nearestRouteIndex: Int?,
        nearestRouteCoordinate: GeoCoordinate?,
    ): List<GeoCoordinate> {
        if (
            routeCoordinates.size < 2 ||
            nearestRouteIndex == null ||
            nearestRouteCoordinate == null
        ) {
            return routeCoordinates
        }

        val segmentStartIndex = nearestRouteIndex.coerceIn(0, routeCoordinates.lastIndex)
        if (segmentStartIndex == routeCoordinates.lastIndex) {
            return listOf(routeCoordinates.last())
        }

        return buildList {
            add(nearestRouteCoordinate)
            addAll(routeCoordinates.drop(segmentStartIndex + 1))
        }.removeAdjacentDuplicates()
    }

    private fun List<GeoCoordinate>.removeAdjacentDuplicates(): List<GeoCoordinate> =
        fold(mutableListOf<GeoCoordinate>()) { result, coordinate ->
            if (result.lastOrNull() != coordinate) result += coordinate
            result
        }
}
