package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ManeuverType {
    STRAIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    LEFT,
    RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    U_TURN,
    ARRIVE,
}

data class ManeuverGuidance(
    val type: ManeuverType,
    val distanceMeters: Int,
)

data class RouteGuidanceSnapshot(
    val instructions: List<ManeuverGuidance>,
    val remainingDistanceMeters: Int,
) {
    val currentInstruction: ManeuverGuidance
        get() = instructions.first()
}

object RouteGuidanceCalculator {
    fun calculate(
        currentLocation: GeoCoordinate,
        routeCoordinates: List<GeoCoordinate>,
        maxInstructions: Int = DEFAULT_MAX_INSTRUCTIONS,
    ): RouteGuidanceSnapshot? {
        if (routeCoordinates.isEmpty() || maxInstructions <= 0) return null

        val nearestIndex = routeCoordinates.indices.minBy { index ->
            NearestNodeFinder.distanceMeters(currentLocation, routeCoordinates[index])
        }
        val samples = sampleRemainingRoute(
            currentLocation = currentLocation,
            routeCoordinates = routeCoordinates,
            nearestIndex = nearestIndex,
        )
        val remainingDistance = samples.last().distanceFromUser.roundToInt().coerceAtLeast(0)
        if (remainingDistance <= ARRIVAL_DISTANCE_METERS) {
            return RouteGuidanceSnapshot(
                instructions = listOf(ManeuverGuidance(ManeuverType.ARRIVE, remainingDistance)),
                remainingDistanceMeters = remainingDistance,
            )
        }

        val maneuvers = mutableListOf<ManeuverGuidance>()
        var lastManeuverDistance = Double.NEGATIVE_INFINITY
        for (index in 1 until samples.lastIndex) {
            val incomingBearing = BearingCalculator.bearingDegrees(
                samples[index - 1].coordinate,
                samples[index].coordinate,
            )
            val outgoingBearing = BearingCalculator.bearingDegrees(
                samples[index].coordinate,
                samples[index + 1].coordinate,
            )
            val signedTurn = signedAngleDelta(incomingBearing, outgoingBearing)
            val type = classifyTurn(signedTurn) ?: continue
            val maneuverDistance = samples[index].distanceFromUser
            if (maneuverDistance - lastManeuverDistance < MIN_MANEUVER_SPACING_METERS) continue

            maneuvers += ManeuverGuidance(
                type = type,
                distanceMeters = maneuverDistance.roundToInt().coerceAtLeast(0),
            )
            lastManeuverDistance = maneuverDistance
            if (maneuvers.size >= maxInstructions) break
        }

        if (maneuvers.isEmpty()) {
            maneuvers += ManeuverGuidance(ManeuverType.STRAIGHT, remainingDistance)
        }
        if (maneuvers.size < maxInstructions) {
            maneuvers += ManeuverGuidance(ManeuverType.ARRIVE, remainingDistance)
        }

        return RouteGuidanceSnapshot(
            instructions = maneuvers.take(maxInstructions),
            remainingDistanceMeters = remainingDistance,
        )
    }

    private fun sampleRemainingRoute(
        currentLocation: GeoCoordinate,
        routeCoordinates: List<GeoCoordinate>,
        nearestIndex: Int,
    ): List<RouteSample> {
        val firstCoordinate = routeCoordinates[nearestIndex]
        var totalDistance = NearestNodeFinder.distanceMeters(currentLocation, firstCoordinate)
        var distanceSinceSample = 0.0
        var previous = firstCoordinate
        val samples = mutableListOf(RouteSample(firstCoordinate, totalDistance))

        for (index in nearestIndex + 1..routeCoordinates.lastIndex) {
            val coordinate = routeCoordinates[index]
            val segmentDistance = NearestNodeFinder.distanceMeters(previous, coordinate)
            totalDistance += segmentDistance
            distanceSinceSample += segmentDistance
            val isLastCoordinate = index == routeCoordinates.lastIndex
            if (distanceSinceSample >= SAMPLE_SPACING_METERS || isLastCoordinate) {
                if (coordinate != samples.last().coordinate) {
                    samples += RouteSample(coordinate, totalDistance)
                }
                distanceSinceSample = 0.0
            }
            previous = coordinate
        }

        return samples
    }

    private fun signedAngleDelta(fromDegrees: Double, toDegrees: Double): Double =
        (toDegrees - fromDegrees + 540.0) % 360.0 - 180.0

    private fun classifyTurn(signedTurnDegrees: Double): ManeuverType? {
        val magnitude = abs(signedTurnDegrees)
        if (magnitude < MIN_TURN_ANGLE_DEGREES) return null
        if (magnitude >= U_TURN_ANGLE_DEGREES) return ManeuverType.U_TURN

        val isRight = signedTurnDegrees > 0.0
        return when {
            magnitude < REGULAR_TURN_ANGLE_DEGREES ->
                if (isRight) ManeuverType.SLIGHT_RIGHT else ManeuverType.SLIGHT_LEFT
            magnitude < SHARP_TURN_ANGLE_DEGREES ->
                if (isRight) ManeuverType.RIGHT else ManeuverType.LEFT
            else -> if (isRight) ManeuverType.SHARP_RIGHT else ManeuverType.SHARP_LEFT
        }
    }

    private data class RouteSample(
        val coordinate: GeoCoordinate,
        val distanceFromUser: Double,
    )

    private const val SAMPLE_SPACING_METERS = 12.0
    private const val MIN_MANEUVER_SPACING_METERS = 25.0
    private const val ARRIVAL_DISTANCE_METERS = 20
    private const val MIN_TURN_ANGLE_DEGREES = 28.0
    private const val REGULAR_TURN_ANGLE_DEGREES = 60.0
    private const val SHARP_TURN_ANGLE_DEGREES = 125.0
    private const val U_TURN_ANGLE_DEGREES = 165.0
    private const val DEFAULT_MAX_INSTRUCTIONS = 4
}
