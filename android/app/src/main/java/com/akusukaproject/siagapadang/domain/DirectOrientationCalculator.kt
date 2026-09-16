package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import kotlin.math.roundToInt

data class DirectOrientation(
    val destinationName: String,
    val distanceMeters: Int,
    val bearingDegrees: Double,
)

object DirectOrientationCalculator {
    fun calculate(
        currentLocation: GeoCoordinate,
        destinationName: String,
        destinationCoordinate: GeoCoordinate?,
        routeCoordinates: List<GeoCoordinate>,
    ): DirectOrientation? {
        val target = destinationCoordinate ?: routeCoordinates.lastOrNull() ?: return null
        return DirectOrientation(
            destinationName = destinationName,
            distanceMeters = NearestNodeFinder.distanceMeters(currentLocation, target)
                .roundToInt()
                .coerceAtLeast(0),
            bearingDegrees = BearingCalculator.bearingDegrees(currentLocation, target),
        )
    }
}
