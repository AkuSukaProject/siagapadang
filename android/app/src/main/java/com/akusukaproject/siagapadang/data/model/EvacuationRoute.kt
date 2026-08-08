package com.akusukaproject.siagapadang.data.model

data class EvacuationRoute(
    val originNodeId: Long,
    val rank: Int,
    val destinationName: String,
    val estimatedSeconds: Int,
    val coordinates: List<GeoCoordinate>,
    val destinationCoordinate: GeoCoordinate?,
)

