package com.akusukaproject.siagapadang.data.local

import androidx.room.ColumnInfo

data class NodeRow(
    @ColumnInfo(name = "node_id") val nodeId: Long,
    val lat: Double,
    val lon: Double,
)

data class RouteRow(
    @ColumnInfo(name = "origin_node_id") val originNodeId: Long,
    @ColumnInfo(name = "rank_1_tes") val rank1Tes: String,
    @ColumnInfo(name = "rank_1_path") val rank1Path: String,
    @ColumnInfo(name = "rank_1_eta") val rank1Eta: Double,
    @ColumnInfo(name = "rank_2_tes") val rank2Tes: String,
    @ColumnInfo(name = "rank_2_path") val rank2Path: String,
    @ColumnInfo(name = "rank_2_eta") val rank2Eta: Double,
    @ColumnInfo(name = "rank_3_tes") val rank3Tes: String,
    @ColumnInfo(name = "rank_3_path") val rank3Path: String,
    @ColumnInfo(name = "rank_3_eta") val rank3Eta: Double,
)

data class EdgeRow(
    @ColumnInfo(name = "edge_id") val edgeId: Long,
    val u: Long,
    val v: Long,
    val length: Double,
    val geometry: String,
)

data class TesRow(
    @ColumnInfo(name = "tes_id") val tesId: String,
    @ColumnInfo(name = "nama_tes") val name: String,
    val lat: Double,
    val lon: Double,
)

