package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate

object AlternativeRouteSelector {
    fun select(
        currentLocation: GeoCoordinate,
        currentRoute: EvacuationRoute,
        candidates: List<EvacuationRoute>,
        excludedDestinationNames: Set<String> = emptySet(),
    ): EvacuationRoute? = candidates
        .asSequence()
        .filter { candidate -> candidate.destinationName != currentRoute.destinationName }
        .filterNot { candidate -> candidate.destinationName in excludedDestinationNames }
        .minWithOrNull(
            compareBy<EvacuationRoute> { candidate ->
                sharedLeadingDistanceMeters(
                    currentLocation = currentLocation,
                    referenceCoordinates = currentRoute.coordinates,
                    candidateCoordinates = candidate.coordinates,
                )
            }.thenBy { candidate -> candidate.estimatedSeconds }
                .thenBy { candidate -> candidate.rank },
        )

    internal fun sharedLeadingDistanceMeters(
        currentLocation: GeoCoordinate,
        referenceCoordinates: List<GeoCoordinate>,
        candidateCoordinates: List<GeoCoordinate>,
    ): Double {
        if (referenceCoordinates.isEmpty() || candidateCoordinates.isEmpty()) return 0.0

        val referenceRemaining = referenceCoordinates
            .drop(nearestCoordinateIndex(currentLocation, referenceCoordinates))
            .take(MAX_COMPARISON_POINTS)
        val candidateRemaining = candidateCoordinates
            .drop(nearestCoordinateIndex(currentLocation, candidateCoordinates))
            .take(MAX_COMPARISON_POINTS)
        if (referenceRemaining.isEmpty() || candidateRemaining.size < 2) return 0.0

        var sharedDistanceMeters = 0.0
        var previousCoordinate = candidateRemaining.first()
        for (coordinate in candidateRemaining.drop(1)) {
            val distanceFromReference = referenceRemaining.minOf { referenceCoordinate ->
                NearestNodeFinder.distanceMeters(coordinate, referenceCoordinate)
            }
            if (distanceFromReference > SAME_ROUTE_CORRIDOR_METERS) break

            sharedDistanceMeters += NearestNodeFinder.distanceMeters(previousCoordinate, coordinate)
            if (sharedDistanceMeters >= MAX_COMPARISON_DISTANCE_METERS) break
            previousCoordinate = coordinate
        }
        return sharedDistanceMeters
    }

    private fun nearestCoordinateIndex(
        location: GeoCoordinate,
        coordinates: List<GeoCoordinate>,
    ): Int = coordinates.indices.minBy { index ->
        NearestNodeFinder.distanceMeters(location, coordinates[index])
    }

    private const val SAME_ROUTE_CORRIDOR_METERS = 20.0
    private const val MAX_COMPARISON_DISTANCE_METERS = 2_000.0
    private const val MAX_COMPARISON_POINTS = 400
}
