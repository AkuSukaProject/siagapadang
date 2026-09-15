package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveEdgeFinderTest {
    @Test
    fun `mengembalikan null jika route tidak memiliki edgeIds`() {
        val route = EvacuationRoute(
            originNodeId = 1L,
            rank = 1,
            destinationName = "TES A",
            estimatedSeconds = 600,
            coordinates = emptyList(),
            destinationCoordinate = null,
            edgeIds = emptyList(),
        )

        val result = ActiveEdgeFinder.findActiveEdgeId(
            location = GeoCoordinate(latitude = 0.0, longitude = 100.0),
            route = route,
        )

        assertNull(result)
    }

    @Test
    fun `mengembalikan satu-satunya edgeId jika rute hanya memiliki satu ruas`() {
        val route = EvacuationRoute(
            originNodeId = 1L,
            rank = 1,
            destinationName = "TES A",
            estimatedSeconds = 600,
            coordinates = listOf(
                GeoCoordinate(latitude = 0.0, longitude = 100.0),
                GeoCoordinate(latitude = 0.1, longitude = 100.1),
            ),
            destinationCoordinate = null,
            edgeIds = listOf(42L),
        )

        val result = ActiveEdgeFinder.findActiveEdgeId(
            location = GeoCoordinate(latitude = 0.05, longitude = 100.05),
            route = route,
        )

        assertEquals(42L, result)
    }

    @Test
    fun `mengidentifikasi ruas aktif berdasarkan indeks koordinat rute terdekat`() {
        val route = EvacuationRoute(
            originNodeId = 1L,
            rank = 1,
            destinationName = "TES B",
            estimatedSeconds = 1200,
            coordinates = listOf(
                GeoCoordinate(latitude = 0.0, longitude = 100.0), // index 0
                GeoCoordinate(latitude = 0.1, longitude = 100.1), // index 1
                GeoCoordinate(latitude = 0.2, longitude = 100.2), // index 2
                GeoCoordinate(latitude = 0.3, longitude = 100.3), // index 3
            ),
            destinationCoordinate = null,
            edgeIds = listOf(10L, 20L),
            edgeCoordinateRanges = listOf(0..2, 2..3),
        )

        // Indeks 1 berada di range 0..2 (ruas 10L)
        val edgeOnFirstSegment = ActiveEdgeFinder.findActiveEdgeId(
            location = GeoCoordinate(latitude = 0.1, longitude = 100.1),
            route = route,
            nearestRouteCoordinateIndex = 1,
        )
        assertEquals(10L, edgeOnFirstSegment)

        // Indeks 3 berada di range 2..3 (ruas 20L)
        val edgeOnSecondSegment = ActiveEdgeFinder.findActiveEdgeId(
            location = GeoCoordinate(latitude = 0.3, longitude = 100.3),
            route = route,
            nearestRouteCoordinateIndex = 3,
        )
        assertEquals(20L, edgeOnSecondSegment)
    }
}
