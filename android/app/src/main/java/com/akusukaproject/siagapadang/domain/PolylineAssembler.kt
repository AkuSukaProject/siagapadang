package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.local.EdgeRow
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import kotlin.math.abs

object PolylineAssembler {
    fun assemble(
        pathNodeIds: List<Long>,
        edges: List<EdgeRow>,
        nodeCoordinates: Map<Long, GeoCoordinate> = emptyMap(),
    ): List<GeoCoordinate> {
        require(pathNodeIds.size >= 2) { "Rute harus memiliki sedikitnya dua node" }

        val shortestEdgeByPair = edges
            .groupBy { edge -> unorderedPair(edge.u, edge.v) }
            .mapValues { (_, candidates) -> candidates.minBy { it.length } }

        return buildList {
            pathNodeIds.zipWithNext().forEach { (fromNodeId, toNodeId) ->
                val edge = shortestEdgeByPair[unorderedPair(fromNodeId, toNodeId)]
                    ?: throw IllegalStateException(
                        "Ruas rute tidak ditemukan: $fromNodeId → $toNodeId",
                    )
                val ordered = if (edge.geometry.isBlank()) {
                    listOf(
                        nodeCoordinates[fromNodeId]
                            ?: throw IllegalStateException("Koordinat node $fromNodeId tidak ditemukan"),
                        nodeCoordinates[toNodeId]
                            ?: throw IllegalStateException("Koordinat node $toNodeId tidak ditemukan"),
                    )
                } else {
                    val parsed = WktLineStringParser.parse(edge.geometry)
                    orderGeometry(
                        parsed = parsed,
                        edge = edge,
                        fromNodeId = fromNodeId,
                        toNodeId = toNodeId,
                        nodeCoordinates = nodeCoordinates,
                    )
                }

                if (isNotEmpty() && sameCoordinate(last(), ordered.first())) {
                    addAll(ordered.drop(1))
                } else {
                    addAll(ordered)
                }
            }
        }
    }

    private fun unorderedPair(first: Long, second: Long): Pair<Long, Long> =
        if (first <= second) first to second else second to first

    private fun orderGeometry(
        parsed: List<GeoCoordinate>,
        edge: EdgeRow,
        fromNodeId: Long,
        toNodeId: Long,
        nodeCoordinates: Map<Long, GeoCoordinate>,
    ): List<GeoCoordinate> {
        val fromCoordinate = nodeCoordinates[fromNodeId]
        val toCoordinate = nodeCoordinates[toNodeId]
        if (fromCoordinate != null && toCoordinate != null) {
            val forwardEndpointDistance =
                NearestNodeFinder.distanceMeters(fromCoordinate, parsed.first()) +
                    NearestNodeFinder.distanceMeters(toCoordinate, parsed.last())
            val reverseEndpointDistance =
                NearestNodeFinder.distanceMeters(fromCoordinate, parsed.last()) +
                    NearestNodeFinder.distanceMeters(toCoordinate, parsed.first())
            return if (forwardEndpointDistance <= reverseEndpointDistance) parsed else parsed.reversed()
        }

        return when {
            edge.u == fromNodeId && edge.v == toNodeId -> parsed
            edge.v == fromNodeId && edge.u == toNodeId -> parsed.reversed()
            else -> throw IllegalStateException("Arah ruas tidak sesuai pasangan node")
        }
    }

    private fun sameCoordinate(first: GeoCoordinate, second: GeoCoordinate): Boolean =
        abs(first.latitude - second.latitude) < COORDINATE_EPSILON &&
            abs(first.longitude - second.longitude) < COORDINATE_EPSILON

    private const val COORDINATE_EPSILON = 1e-7
}
