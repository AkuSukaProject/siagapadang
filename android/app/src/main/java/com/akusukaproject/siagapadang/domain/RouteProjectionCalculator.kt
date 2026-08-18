package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import kotlin.math.cos

data class RouteProjection(
    val coordinate: GeoCoordinate,
    val segmentStartIndex: Int,
    val segmentFraction: Double,
    val distanceMeters: Double,
)

/** Finds the closest point on the existing precomputed route polyline. */
object RouteProjectionCalculator {
    fun findNearest(
        location: GeoCoordinate,
        routeCoordinates: List<GeoCoordinate>,
        minimumRouteIndex: Int = 0,
        minimumSegmentFraction: Double = 0.0,
    ): RouteProjection? {
        if (routeCoordinates.isEmpty()) return null
        if (routeCoordinates.size == 1) {
            return RouteProjection(
                coordinate = routeCoordinates.first(),
                segmentStartIndex = 0,
                segmentFraction = 0.0,
                distanceMeters = NearestNodeFinder.distanceMeters(location, routeCoordinates.first()),
            )
        }
        if (minimumRouteIndex >= routeCoordinates.lastIndex) {
            val lastCoordinate = routeCoordinates.last()
            return RouteProjection(
                coordinate = lastCoordinate,
                segmentStartIndex = routeCoordinates.lastIndex,
                segmentFraction = 1.0,
                distanceMeters = NearestNodeFinder.distanceMeters(location, lastCoordinate),
            )
        }

        val firstSegmentIndex = minimumRouteIndex
            .coerceIn(0, routeCoordinates.lastIndex - 1)
        var bestProjection: RouteProjection? = null
        for (index in firstSegmentIndex until routeCoordinates.lastIndex) {
            val projected = projectToSegment(
                location = location,
                start = routeCoordinates[index],
                end = routeCoordinates[index + 1],
                minimumFraction = if (index == firstSegmentIndex) {
                    minimumSegmentFraction.coerceIn(0.0, 1.0)
                } else {
                    0.0
                },
            )
            val candidate = RouteProjection(
                coordinate = projected.coordinate,
                segmentStartIndex = index,
                segmentFraction = projected.fraction,
                distanceMeters = NearestNodeFinder.distanceMeters(location, projected.coordinate),
            )
            if (bestProjection == null || candidate.distanceMeters < bestProjection.distanceMeters) {
                bestProjection = candidate
            }
        }
        return bestProjection
    }

    private fun projectToSegment(
        location: GeoCoordinate,
        start: GeoCoordinate,
        end: GeoCoordinate,
        minimumFraction: Double,
    ): SegmentProjection {
        val latitudeReferenceRadians = Math.toRadians(
            (location.latitude + start.latitude + end.latitude) / 3.0,
        )
        val metersPerLongitudeDegree = METERS_PER_LATITUDE_DEGREE * cos(latitudeReferenceRadians)
        val segmentX = (end.longitude - start.longitude) * metersPerLongitudeDegree
        val segmentY = (end.latitude - start.latitude) * METERS_PER_LATITUDE_DEGREE
        val pointX = (location.longitude - start.longitude) * metersPerLongitudeDegree
        val pointY = (location.latitude - start.latitude) * METERS_PER_LATITUDE_DEGREE
        val segmentLengthSquared = segmentX * segmentX + segmentY * segmentY
        val fraction = if (segmentLengthSquared <= MIN_SEGMENT_LENGTH_SQUARED) {
            0.0
        } else {
            ((pointX * segmentX + pointY * segmentY) / segmentLengthSquared).coerceIn(0.0, 1.0)
        }.coerceAtLeast(minimumFraction)
        return SegmentProjection(
            coordinate = GeoCoordinate(
                latitude = start.latitude + (end.latitude - start.latitude) * fraction,
                longitude = start.longitude + (end.longitude - start.longitude) * fraction,
            ),
            fraction = fraction,
        )
    }

    private data class SegmentProjection(
        val coordinate: GeoCoordinate,
        val fraction: Double,
    )

    private const val METERS_PER_LATITUDE_DEGREE = 111_320.0
    private const val MIN_SEGMENT_LENGTH_SQUARED = 0.0001
}
