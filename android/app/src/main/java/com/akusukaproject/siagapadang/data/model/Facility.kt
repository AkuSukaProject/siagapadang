package com.akusukaproject.siagapadang.data.model

/** Tempat evakuasi dari view `v_fasilitas_evakuasi`: TES (gedung, vertikal) atau TEA (kawasan, horizontal). */
data class Facility(
    val id: String,
    val name: String,
    val kind: FacilityKind,
    val zone: String,
    val capacityPeople: Int,
    val coordinate: GeoCoordinate,
)

enum class FacilityKind(val label: String) {
    TES("TES"),
    TEA("TEA"),
}
