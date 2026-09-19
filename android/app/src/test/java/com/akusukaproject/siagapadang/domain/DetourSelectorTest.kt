package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetourSelectorTest {
    private fun route(dest: String, seconds: Int, nodes: List<Long>, edges: List<Long>) = EvacuationRoute(
        originNodeId = nodes.first(),
        rank = 1,
        destinationName = dest,
        estimatedSeconds = seconds,
        coordinates = emptyList(),
        destinationCoordinate = null,
        nodeIds = nodes,
        edgeIds = edges,
    )

    @Test
    fun `picks fastest detour to the same destination`() {
        val slow = route("TES A", 900, listOf(1, 2, 5), listOf(12, 25))
        val fast = route("TES A", 600, listOf(1, 3, 5), listOf(13, 35))

        assertEquals(fast, DetourSelector.select(1, "TES A", listOf(slow, fast), blockedEdgeIds = setOf(14)))
    }

    @Test
    fun `rejects neighbor route that goes back over the blocked edge`() {
        val throughBlocked = route("TES A", 300, listOf(1, 3, 1, 4, 5), listOf(13, 13, 14, 45))
        val clean = route("TES A", 700, listOf(1, 2, 5), listOf(12, 25))

        assertEquals(clean, DetourSelector.select(1, "TES A", listOf(throughBlocked, clean), setOf(14)))
    }

    @Test
    fun `rejects route that returns to the start node`() {
        val loop = route("TES A", 200, listOf(1, 2, 1, 6), listOf(12, 12, 16))

        assertNull(DetourSelector.select(1, "TES A", listOf(loop), setOf(14)))
    }

    @Test
    fun `ignores other destinations`() {
        val other = route("TES B", 100, listOf(1, 2, 9), listOf(12, 29))

        assertNull(DetourSelector.select(1, "TES A", listOf(other), setOf(14)))
    }
}
