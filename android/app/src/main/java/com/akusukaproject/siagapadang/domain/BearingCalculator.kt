package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object BearingCalculator {
    fun bearingDegrees(from: GeoCoordinate, to: GeoCoordinate): Double {
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        val y = sin(longitudeDelta) * cos(toLatitude)
        val x = cos(fromLatitude) * sin(toLatitude) -
            sin(fromLatitude) * cos(toLatitude) * cos(longitudeDelta)
        return normalizeDegrees(Math.toDegrees(atan2(y, x)))
    }

    fun relativeRotationDegrees(targetBearing: Double, deviceHeading: Double): Float {
        val normalized = (targetBearing - deviceHeading + 540.0) % 360.0 - 180.0
        return normalized.toFloat()
    }

    fun nextTarget(
        currentLocation: GeoCoordinate,
        routeCoordinates: List<GeoCoordinate>,
        minimumDistanceMeters: Double = 8.0,
    ): GeoCoordinate? {
        if (routeCoordinates.isEmpty()) return null
        val nearestIndex = routeCoordinates.indices.minBy { index ->
            NearestNodeFinder.distanceMeters(currentLocation, routeCoordinates[index])
        }
        return routeCoordinates.drop(nearestIndex).firstOrNull { coordinate ->
            NearestNodeFinder.distanceMeters(currentLocation, coordinate) >= minimumDistanceMeters
        } ?: routeCoordinates.last()
    }

    private fun normalizeDegrees(value: Double): Double = (value % 360.0 + 360.0) % 360.0
}
