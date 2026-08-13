package com.akusukaproject.siagapadang.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

@Dao
interface ZoneDao {
    @SkipQueryVerification
    @Query(
        """
        SELECT node_id, lat, lon, is_safe
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
        SELECT zone_id, nama_zona, tingkat_bahaya, geometry_wkt
        FROM tb_inundation_zones
        """,
    )
    suspend fun findAllInundationZones(): List<InundationZoneRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT nama_zona AS name, tingkat_bahaya AS level, geometry_wkt
        FROM tb_inundation_zones
        """,
    )
    suspend fun findInundationZoneGeometries(): List<ZoneGeometryRow>

    @SkipQueryVerification
    @Query(
        """
        SELECT nama_zona AS name, 'Aman' AS level, geometry_wkt
        FROM tb_safe_zones
        """,
    )
    suspend fun findSafeZoneGeometries(): List<ZoneGeometryRow>
}
