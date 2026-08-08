package com.akusukaproject.siagapadang.ui.evacuation

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.domain.RouteGuidanceSnapshot

data class EvacuationUiState(
    val hasLocationPermission: Boolean = false,
    val isLoadingRoute: Boolean = false,
    val currentLocation: GeoCoordinate? = null,
    val locationAccuracyMeters: Float? = null,
    val route: EvacuationRoute? = null,
    val guidance: RouteGuidanceSnapshot? = null,
    val deviceHeadingDegrees: Float? = null,
    val remainingEvacuationSeconds: Int = EVACUATION_WINDOW_SECONDS,
    val errorMessage: String? = null,
    val compassMessage: String? = null,
) {
    val canSelectAlternative: Boolean
        get() = route != null && route.rank < 3 && !isLoadingRoute

    companion object {
        const val EVACUATION_WINDOW_SECONDS = 20 * 60
    }
}
