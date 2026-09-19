package com.akusukaproject.siagapadang.data.model

data class EvacuationPoint(
    val externalId: String,
    val name: String,
    val zoneCode: String,
    val capacityPeople: Int,
    val coordinate: GeoCoordinate,
)
