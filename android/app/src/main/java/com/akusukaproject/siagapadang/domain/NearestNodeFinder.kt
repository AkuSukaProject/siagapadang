package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.local.NodeRow
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object NearestNodeFinder {
    fun findNearest(origin: GeoCoordinate, candidates: List<NodeRow>): NodeRow? =
        candidates.minByOrNull { candidate ->
            distanceMeters(
                origin,
                GeoCoordinate(candidate.lat, candidate.lon),
            )
        }

    fun distanceMeters(from: GeoCoordinate, to: GeoCoordinate): Double {
        val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(fromLatitude) * cos(toLatitude) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        return EARTH_RADIUS_METERS * 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}

