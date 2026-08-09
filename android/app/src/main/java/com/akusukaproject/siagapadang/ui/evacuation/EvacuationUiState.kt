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
    val previousRoutes: List<EvacuationRoute> = emptyList(),
    val guidance: RouteGuidanceSnapshot? = null,
    val deviceHeadingDegrees: Float? = null,
    val remainingEvacuationSeconds: Int = EVACUATION_WINDOW_SECONDS,
    val remainingAlternativeCount: Int = MAX_ALTERNATIVE_COUNT,
    val alternativeRouteVersion: Int = 0,
    val alternativeRouteMessage: String? = null,
    val hasArrived: Boolean = false,
    val arrivalDistanceMeters: Int? = null,
    val errorMessage: String? = null,
    val compassMessage: String? = null,
) {
    val canSelectAlternative: Boolean
        get() = route != null && remainingAlternativeCount > 0 && !isLoadingRoute && !hasArrived

    companion object {
        const val EVACUATION_WINDOW_SECONDS = 20 * 60
        const val MAX_ALTERNATIVE_COUNT = 2
    }
}
