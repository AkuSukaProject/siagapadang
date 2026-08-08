package com.akusukaproject.siagapadang.data.repository

import com.akusukaproject.siagapadang.data.local.EvacuationDao
import com.akusukaproject.siagapadang.data.local.RouteRow
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import com.akusukaproject.siagapadang.domain.PolylineAssembler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class EvacuationRepository(
    private val dao: EvacuationDao,
) {
    suspend fun findRouteFromLocation(
        location: GeoCoordinate,
        rank: Int = 1,
    ): EvacuationRoute {
        val nearestNode = findNearestNode(location)
        return loadRoute(originNodeId = nearestNode.nodeId, rank = rank)
    }

    suspend fun loadRoute(originNodeId: Long, rank: Int): EvacuationRoute {
        require(rank in 1..3) { "Peringkat rute harus 1, 2, atau 3" }
        val routeRow = dao.findRoute(originNodeId)
            ?: throw IllegalStateException("Rute evakuasi tidak tersedia untuk lokasi ini")
        val selection = routeRow.select(rank)
        val pathNodeIds = selection.path
            .split(',')
            .map { value ->
                value.trim().toLongOrNull()
                    ?: throw IllegalStateException("Data node pada rute tidak valid")
            }
        require(pathNodeIds.size >= 2) { "Data rute terlalu pendek" }

        val distinctNodeIds = pathNodeIds.distinct()
        val edges = dao.findEdgesForNodes(distinctNodeIds)
        val nodeCoordinates = dao.findNodesByIds(distinctNodeIds).associate { node ->
            node.nodeId to GeoCoordinate(latitude = node.lat, longitude = node.lon)
        }
        val coordinates = withContext(Dispatchers.Default) {
            PolylineAssembler.assemble(pathNodeIds, edges, nodeCoordinates)
        }
        val destination = dao.findTesByName(selection.destinationName)

        return EvacuationRoute(
            originNodeId = originNodeId,
            rank = rank,
            destinationName = selection.destinationName,
            estimatedSeconds = (selection.etaMinutes * 60.0).roundToInt(),
            coordinates = coordinates,
            destinationCoordinate = destination?.let { tes ->
                GeoCoordinate(latitude = tes.lat, longitude = tes.lon)
            },
        )
    }

    private suspend fun findNearestNode(location: GeoCoordinate) =
        SEARCH_WINDOWS.firstNotNullOfOrNull { window ->
            val candidates = dao.findNodesInBounds(
                minLat = location.latitude - window,
                maxLat = location.latitude + window,
                minLon = location.longitude - window,
                maxLon = location.longitude + window,
            )
            NearestNodeFinder.findNearest(location, candidates)
        } ?: throw IllegalStateException("Posisi berada di luar cakupan jaringan evakuasi")

    private fun RouteRow.select(rank: Int): RouteSelection = when (rank) {
        1 -> RouteSelection(rank1Tes, rank1Path, rank1Eta)
        2 -> RouteSelection(rank2Tes, rank2Path, rank2Eta)
        3 -> RouteSelection(rank3Tes, rank3Path, rank3Eta)
        else -> error("Peringkat rute tidak didukung")
    }

    private data class RouteSelection(
        val destinationName: String,
        val path: String,
        val etaMinutes: Double,
    )

    private companion object {
        val SEARCH_WINDOWS = listOf(0.005, 0.02)
    }
}
