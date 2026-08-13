package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import kotlin.math.abs

object ZoneChecker {
    fun contains(
        point: GeoCoordinate,
        rings: List<List<GeoCoordinate>>,
    ): Boolean {
        if (rings.isEmpty() || !isInsideRing(point, rings.first())) return false
        return rings.drop(1).none { hole -> isInsideRing(point, hole) }
    }

    private fun isInsideRing(point: GeoCoordinate, ring: List<GeoCoordinate>): Boolean {
        if (ring.size < 3) return false
        var inside = false
        var previous = ring.last()

        ring.forEach { current ->
            if (isOnSegment(point, previous, current)) return true

            val crossesLatitude = (current.latitude > point.latitude) !=
                (previous.latitude > point.latitude)
            if (crossesLatitude) {
                val crossingLongitude = (previous.longitude - current.longitude) *
                    (point.latitude - current.latitude) /
                    (previous.latitude - current.latitude) + current.longitude
                if (point.longitude < crossingLongitude) inside = !inside
            }
            previous = current
        }
        return inside
    }

    private fun isOnSegment(
        point: GeoCoordinate,
        start: GeoCoordinate,
        end: GeoCoordinate,
    ): Boolean {
        val crossProduct = (point.latitude - start.latitude) *
            (end.longitude - start.longitude) -
            (point.longitude - start.longitude) * (end.latitude - start.latitude)
        if (abs(crossProduct) > BOUNDARY_EPSILON) return false

        return point.latitude in minOf(start.latitude, end.latitude)..maxOf(start.latitude, end.latitude) &&
            point.longitude in minOf(start.longitude, end.longitude)..maxOf(start.longitude, end.longitude)
    }

    private const val BOUNDARY_EPSILON = 1e-10
}
