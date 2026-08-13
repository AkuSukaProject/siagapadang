package com.akusukaproject.siagapadang.data.model

data class OfflineRoadOverlay(
    val viewportId: String,
    val geoJson: String,
    val segmentCount: Int,
)
