package com.akusukaproject.siagapadang.data.model

data class EvacuationRoute(
    val originNodeId: Long,
    val rank: Int,
    val destinationName: String,
    val estimatedSeconds: Int,
    val coordinates: List<GeoCoordinate>,
    val destinationCoordinate: GeoCoordinate?,
    val destinationCapacityPeople: Int? = null,
    val destinationZoneCode: String? = null,
)
