package com.akusukaproject.siagapadang.data.model

data class TsunamiZoneOverlay(
    val safeGeoJson: String,
    val lowRiskGeoJson: String,
    val mediumRiskGeoJson: String,
    val highRiskGeoJson: String,
)
