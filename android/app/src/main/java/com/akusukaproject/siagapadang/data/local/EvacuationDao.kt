package com.akusukaproject.siagapadang.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

@Dao
interface EvacuationDao {
    @SkipQueryVerification
    @Query(
        """
        SELECT node_id, lat, lon
        FROM tb_nodes
        WHERE lat BETWEEN :minLat AND :maxLat
          AND lon BETWEEN :minLon AND :maxLon
        """,
    )
    suspend fun findNodesInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<NodeRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT node_id, lat, lon
        FROM tb_nodes
        WHERE node_id IN (:nodeIds)
        """,
    )
    suspend fun findNodesByIds(nodeIds: List<Long>): List<NodeRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT origin_node_id,
               rank_1_tes, rank_1_path, rank_1_eta,
               rank_2_tes, rank_2_path, rank_2_eta,
               rank_3_tes, rank_3_path, rank_3_eta
        FROM tb_routes
        WHERE origin_node_id = :originNodeId
        LIMIT 1
        """,
    )
    suspend fun findRoute(originNodeId: Long): RouteRow?

    @SkipQueryVerification
    @Query(
        """
        SELECT edge_id, u, v, length, geometry
        FROM tb_edges
        WHERE u IN (:nodeIds) OR v IN (:nodeIds)
        ORDER BY length ASC
        """,
    )
    suspend fun findEdgesForNodes(nodeIds: List<Long>): List<EdgeRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT tes_id, nama_tes, lat, lon
        FROM tb_tes
        WHERE nama_tes = :name
        LIMIT 1
        """,
    )
    suspend fun findTesByName(name: String): TesRow?
}
