package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate

object ActiveEdgeFinder {
    /**
     * Menentukan ID ruas jalan aktif pada rute berdasarkan posisi pengguna
     * atau indeks koordinat terdekat pada garis polyline rute.
     */
    fun findActiveEdgeId(
        location: GeoCoordinate,
        route: EvacuationRoute,
        nearestRouteCoordinateIndex: Int? = null,
    ): Long? {
        if (route.edgeIds.isEmpty()) return null
        if (route.edgeIds.size == 1) return route.edgeIds.first()

        // 1. Jika indeks koordinat terdekat diketahui, cari range ruas yang mencakupnya
        val coordinateIndex = nearestRouteCoordinateIndex ?: run {
            val projection = RouteProjectionCalculator.findNearest(
                location = location,
                routeCoordinates = route.coordinates,
            )
            projection?.segmentStartIndex ?: 0
        }

        if (route.edgeCoordinateRanges.size == route.edgeIds.size) {
            val matchedIndex = route.edgeCoordinateRanges.indexOfFirst { range ->
                coordinateIndex in range
            }
            if (matchedIndex != -1) {
                return route.edgeIds[matchedIndex]
            }
        }

        // 2. Fallback jika ranges tidak tersedia: petakan proporsional terhadap ukuran edgeIds
        val totalCoordinates = route.coordinates.size
        if (totalCoordinates <= 1) return route.edgeIds.first()

        val edgeIndex = ((coordinateIndex.toDouble() / (totalCoordinates - 1)) * route.edgeIds.size)
            .toInt()
            .coerceIn(0, route.edgeIds.lastIndex)

        return route.edgeIds[edgeIndex]
    }
}
