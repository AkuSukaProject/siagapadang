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
    val routeCoordinateIndex: Int = 0,
)

data class RouteGuidanceSnapshot(
    val instructions: List<ManeuverGuidance>,
    val remainingDistanceMeters: Int,
    val nearestRouteIndex: Int = 0,
    val distanceFromRouteMeters: Int = 0,
    val nearestRouteCoordinate: GeoCoordinate? = null,
    val isApproachingRoute: Boolean = false,
) {
    val currentInstruction: ManeuverGuidance
        get() = instructions.first()
}

object RouteGuidanceCalculator {
    fun calculate(
        currentLocation: GeoCoordinate,
        routeCoordinates: List<GeoCoordinate>,
        maxInstructions: Int = DEFAULT_MAX_INSTRUCTIONS,
        minimumRouteIndex: Int = 0,
        deviceHeadingDegrees: Float? = null,
    ): RouteGuidanceSnapshot? {
        if (routeCoordinates.isEmpty() || maxInstructions <= 0) return null

        val projection = RouteProjectionCalculator.findNearest(
            location = currentLocation,
            routeCoordinates = routeCoordinates,
            minimumRouteIndex = minimumRouteIndex,
        ) ?: return null
        val nearestIndex = projection.segmentStartIndex
        val distanceFromRoute = projection.distanceMeters
            .roundToInt()
            .coerceAtLeast(0)
        val samples = sampleRemainingRoute(
            routeCoordinates = routeCoordinates,
            projection = projection,
        )
        val routeDistance = samples.last().distanceFromRouteStart.roundToInt().coerceAtLeast(0)
        val isApproachingRoute = distanceFromRoute > ON_ROUTE_DISTANCE_METERS
        val remainingDistance = (
            routeDistance + if (isApproachingRoute) distanceFromRoute else 0
            ).coerceAtLeast(0)
        if (!isApproachingRoute && routeDistance <= ARRIVAL_DISTANCE_METERS) {
            return RouteGuidanceSnapshot(
                instructions = listOf(ManeuverGuidance(ManeuverType.ARRIVE, remainingDistance)),
                remainingDistanceMeters = remainingDistance,
                nearestRouteIndex = nearestIndex,
                distanceFromRouteMeters = distanceFromRoute,
                nearestRouteCoordinate = projection.coordinate,
            )
        }

        val routeManeuvers = mutableListOf<ManeuverGuidance>()
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
            val maneuverDistance = samples[index].distanceFromRouteStart
            if (maneuverDistance - lastManeuverDistance < MIN_MANEUVER_SPACING_METERS) continue

            routeManeuvers += ManeuverGuidance(
                type = type,
                distanceMeters = maneuverDistance.roundToInt().coerceAtLeast(0),
                routeCoordinateIndex = samples[index].routeCoordinateIndex,
            )
            lastManeuverDistance = maneuverDistance
            if (routeManeuvers.size >= maxInstructions) break
        }

        val stagedRouteInstructions = mutableListOf<ManeuverGuidance>()
        val firstTurn = routeManeuvers.firstOrNull()
        if (firstTurn == null) {
            stagedRouteInstructions += ManeuverGuidance(ManeuverType.STRAIGHT, routeDistance)
        } else {
            if (firstTurn.distanceMeters > TURN_INSTRUCTION_DISTANCE_METERS) {
                stagedRouteInstructions += ManeuverGuidance(
                    type = ManeuverType.STRAIGHT,
                    distanceMeters = firstTurn.distanceMeters,
                    routeCoordinateIndex = nearestIndex,
                )
            }
            stagedRouteInstructions += routeManeuvers
        }
        if (stagedRouteInstructions.size < maxInstructions) {
            stagedRouteInstructions += ManeuverGuidance(ManeuverType.ARRIVE, routeDistance)
        }

        val instructions = if (isApproachingRoute) {
            val targetBearing = BearingCalculator.bearingDegrees(
                from = currentLocation,
                to = projection.coordinate,
            )
            val approachType = deviceHeadingDegrees?.let { heading ->
                classifyApproachDirection(
                    BearingCalculator.relativeRotationDegrees(targetBearing, heading.toDouble()).toDouble(),
                )
            } ?: ManeuverType.STRAIGHT
            listOf(
                ManeuverGuidance(
                    type = approachType,
                    distanceMeters = distanceFromRoute,
                    routeCoordinateIndex = nearestIndex,
                ),
            ) + stagedRouteInstructions
        } else {
            stagedRouteInstructions
        }

        return RouteGuidanceSnapshot(
            instructions = instructions.take(maxInstructions),
            remainingDistanceMeters = remainingDistance,
            nearestRouteIndex = nearestIndex,
            distanceFromRouteMeters = distanceFromRoute,
            nearestRouteCoordinate = projection.coordinate,
            isApproachingRoute = isApproachingRoute,
        )
    }

    private fun sampleRemainingRoute(
        routeCoordinates: List<GeoCoordinate>,
        projection: RouteProjection,
    ): List<RouteSample> {
        val firstCoordinate = projection.coordinate
        var totalDistance = 0.0
        var distanceSinceSample = 0.0
        var previous = firstCoordinate
        val samples = mutableListOf(
            RouteSample(firstCoordinate, totalDistance, projection.segmentStartIndex),
        )

        for (index in projection.segmentStartIndex + 1..routeCoordinates.lastIndex) {
            val coordinate = routeCoordinates[index]
            val segmentDistance = NearestNodeFinder.distanceMeters(previous, coordinate)
            totalDistance += segmentDistance
            distanceSinceSample += segmentDistance
            val isLastCoordinate = index == routeCoordinates.lastIndex
            if (distanceSinceSample >= SAMPLE_SPACING_METERS || isLastCoordinate) {
                if (coordinate != samples.last().coordinate) {
                    samples += RouteSample(coordinate, totalDistance, index)
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

    private fun classifyApproachDirection(relativeBearingDegrees: Double): ManeuverType {
        val magnitude = abs(relativeBearingDegrees)
        if (magnitude <= APPROACH_STRAIGHT_ANGLE_DEGREES) return ManeuverType.STRAIGHT
        if (magnitude >= APPROACH_U_TURN_ANGLE_DEGREES) return ManeuverType.U_TURN
        val isRight = relativeBearingDegrees > 0.0
        return if (magnitude <= APPROACH_SLIGHT_ANGLE_DEGREES) {
            if (isRight) ManeuverType.SLIGHT_RIGHT else ManeuverType.SLIGHT_LEFT
        } else {
            if (isRight) ManeuverType.RIGHT else ManeuverType.LEFT
        }
    }

    private data class RouteSample(
        val coordinate: GeoCoordinate,
        val distanceFromRouteStart: Double,
        val routeCoordinateIndex: Int,
    )

    private const val SAMPLE_SPACING_METERS = 12.0
    private const val MIN_MANEUVER_SPACING_METERS = 25.0
    private const val ARRIVAL_DISTANCE_METERS = 0
    private const val ON_ROUTE_DISTANCE_METERS = 18
    private const val TURN_INSTRUCTION_DISTANCE_METERS = 50
    private const val MIN_TURN_ANGLE_DEGREES = 28.0
    private const val REGULAR_TURN_ANGLE_DEGREES = 60.0
    private const val SHARP_TURN_ANGLE_DEGREES = 125.0
    private const val U_TURN_ANGLE_DEGREES = 165.0
    private const val APPROACH_STRAIGHT_ANGLE_DEGREES = 22.5
    private const val APPROACH_SLIGHT_ANGLE_DEGREES = 60.0
    private const val APPROACH_U_TURN_ANGLE_DEGREES = 150.0
    private const val DEFAULT_MAX_INSTRUCTIONS = 4
}
