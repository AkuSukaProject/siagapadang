package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlternativeRouteSelectorTest {
    @Test
    fun `memilih rute yang menyimpang paling awal meski eta lebih lama`() {
        val location = coordinate(0.0, 0.0)
        val current = route(
            rank = 1,
            destination = "TES A",
            etaSeconds = 300,
            coordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.0, 0.001),
                coordinate(0.0, 0.002),
            ),
        )
        val lateDivergence = route(
            rank = 2,
            destination = "TES B",
            etaSeconds = 320,
            coordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.0, 0.001),
                coordinate(0.001, 0.001),
            ),
        )
        val earlyDivergence = route(
            rank = 3,
            destination = "TES C",
            etaSeconds = 500,
            coordinates = listOf(
                coordinate(0.0, 0.0),
                coordinate(0.001, 0.0),
                coordinate(0.002, 0.0),
            ),
        )

        val selected = AlternativeRouteSelector.select(
            currentLocation = location,
            currentRoute = current,
            candidates = listOf(lateDivergence, earlyDivergence),
        )

        assertEquals(3, selected?.rank)
    }

    @Test
    fun `tidak memilih tujuan yang sudah ditolak`() {
        val location = coordinate(0.0, 0.0)
        val current = route(1, "TES A", 300, listOf(location, coordinate(0.0, 0.001)))
        val rejected = route(2, "TES B", 320, listOf(location, coordinate(0.001, 0.0)))

        val selected = AlternativeRouteSelector.select(
            currentLocation = location,
            currentRoute = current,
            candidates = listOf(rejected),
            excludedDestinationNames = setOf("TES B"),
        )

        assertNull(selected)
    }

    @Test
    fun `menghindari rute yang melewati ruas jalan terhalang jika ada alternatif lain`() {
        val location = coordinate(0.0, 0.0)
        val current = route(
            rank = 1,
            destination = "TES A",
            etaSeconds = 300,
            coordinates = listOf(coordinate(0.0, 0.0), coordinate(0.0, 0.001)),
            edgeIds = listOf(10L, 20L),
        )
        val candidateWithBlockedEdge = route(
            rank = 2,
            destination = "TES B",
            etaSeconds = 310,
            coordinates = listOf(coordinate(0.0, 0.0), coordinate(0.001, 0.0)),
            edgeIds = listOf(10L, 30L),
        )
        val candidateWithoutBlockedEdge = route(
            rank = 3,
            destination = "TES C",
            etaSeconds = 350,
            coordinates = listOf(coordinate(0.0, 0.0), coordinate(0.002, 0.0)),
            edgeIds = listOf(40L, 50L),
        )

        val selected = AlternativeRouteSelector.select(
            currentLocation = location,
            currentRoute = current,
            candidates = listOf(candidateWithBlockedEdge, candidateWithoutBlockedEdge),
            excludedEdgeIds = setOf(10L),
        )

        assertEquals(3, selected?.rank)
        assertEquals("TES C", selected?.destinationName)
    }

    private fun route(
        rank: Int,
        destination: String,
        etaSeconds: Int,
        coordinates: List<GeoCoordinate>,
        edgeIds: List<Long> = emptyList(),
    ) = EvacuationRoute(
        originNodeId = 1L,
        rank = rank,
        destinationName = destination,
        estimatedSeconds = etaSeconds,
        coordinates = coordinates,
        destinationCoordinate = coordinates.lastOrNull(),
        edgeIds = edgeIds,
    )

    private fun coordinate(latitude: Double, longitude: Double) =
        GeoCoordinate(latitude = latitude, longitude = longitude)
}
