package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.local.EdgeRow
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class PolylineAssemblerTest {
    @Test
    fun `assembler membalik geometri saat arah ruas berlawanan`() {
        val edge = EdgeRow(
            edgeId = 1,
            u = 2,
            v = 1,
            length = 10.0,
            geometry = "LINESTRING (100.2 -0.2, 100.1 -0.1)",
        )

        val coordinates = PolylineAssembler.assemble(
            pathNodeIds = listOf(1, 2),
            edges = listOf(edge),
        )

        assertEquals(100.1, coordinates.first().longitude, 0.0)
        assertEquals(-0.1, coordinates.first().latitude, 0.0)
        assertEquals(100.2, coordinates.last().longitude, 0.0)
    }

    @Test
    fun `assembler memilih ruas paralel terpendek`() {
        val longEdge = EdgeRow(
            edgeId = 1,
            u = 1,
            v = 2,
            length = 50.0,
            geometry = "LINESTRING (101.0 -1.0, 101.1 -1.1)",
        )
        val shortEdge = EdgeRow(
            edgeId = 2,
            u = 1,
            v = 2,
            length = 10.0,
            geometry = "LINESTRING (100.0 -0.0, 100.1 -0.1)",
        )

        val coordinates = PolylineAssembler.assemble(
            pathNodeIds = listOf(1, 2),
            edges = listOf(longEdge, shortEdge),
        )

        assertEquals(100.0, coordinates.first().longitude, 0.0)
        assertEquals(100.1, coordinates.last().longitude, 0.0)
    }

    @Test
    fun `assembler memakai koordinat node saat geometri ruas kosong`() {
        val edge = EdgeRow(
            edgeId = 3,
            u = 1,
            v = 2,
            length = 12.0,
            geometry = "",
        )

        val coordinates = PolylineAssembler.assemble(
            pathNodeIds = listOf(1, 2),
            edges = listOf(edge),
            nodeCoordinates = mapOf(
                1L to GeoCoordinate(latitude = -0.1, longitude = 100.1),
                2L to GeoCoordinate(latitude = -0.2, longitude = 100.2),
            ),
        )

        assertEquals(100.1, coordinates.first().longitude, 0.0)
        assertEquals(100.2, coordinates.last().longitude, 0.0)
    }
}
