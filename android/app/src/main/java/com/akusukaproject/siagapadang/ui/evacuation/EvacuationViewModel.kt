package com.akusukaproject.siagapadang.ui.evacuation

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akusukaproject.siagapadang.SiagaPadangApplication
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.domain.RouteGuidanceCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EvacuationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SiagaPadangApplication
    private val repository = app.evacuationRepository
    private val mutableUiState = MutableStateFlow(EvacuationUiState())
    val uiState: StateFlow<EvacuationUiState> = mutableUiState.asStateFlow()

    private var locationJob: Job? = null
    private var compassJob: Job? = null
    private var routeJob: Job? = null
    private var countdownJob: Job? = null
    private var initialRouteRequested = false

    fun onLocationPermissionChanged(granted: Boolean) {
        mutableUiState.update { state ->
            state.copy(
                hasLocationPermission = granted,
                errorMessage = if (granted) null else "Izin lokasi diperlukan untuk mencari rute evakuasi.",
            )
        }
        if (granted) {
            startLocationAndCompass()
        } else {
            locationJob?.cancel()
            compassJob?.cancel()
            locationJob = null
            compassJob = null
        }
    }

    fun retryRoute() {
        initialRouteRequested = false
        mutableUiState.value.currentLocation?.let(::requestInitialRoute)
    }

    fun selectAlternativeDestination() {
        val currentRoute = mutableUiState.value.route ?: return
        val nextRank = currentRoute.rank + 1
        if (nextRank > 3 || routeJob?.isActive == true) return

        routeJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoadingRoute = true, errorMessage = null) }
            runCatching {
                repository.loadRoute(currentRoute.originNodeId, nextRank)
            }.onSuccess { route ->
                mutableUiState.update { state ->
                    withGuidance(state.copy(route = route, isLoadingRoute = false))
                }
            }.onFailure { error ->
                mutableUiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        errorMessage = error.message ?: "Alternatif tujuan tidak dapat dimuat.",
                    )
                }
            }
        }
    }

    private fun startLocationAndCompass() {
        if (locationJob == null) {
            locationJob = viewModelScope.launch {
                app.locationProvider.locations()
                    .catch { error ->
                        mutableUiState.update {
                            it.copy(errorMessage = error.message ?: "Lokasi perangkat tidak tersedia.")
                        }
                    }
                    .collect { deviceLocation ->
                        mutableUiState.update { state ->
                            withGuidance(
                                state.copy(
                                    currentLocation = deviceLocation.coordinate,
                                    locationAccuracyMeters = deviceLocation.accuracyMeters,
                                ),
                            )
                        }
                        requestInitialRoute(deviceLocation.coordinate)
                    }
            }
        }

        if (compassJob == null) {
            compassJob = viewModelScope.launch {
                app.compassProvider.headings()
                    .catch { error ->
                        mutableUiState.update {
                            it.copy(compassMessage = error.message ?: "Kompas tidak tersedia.")
                        }
                    }
                    .collect { heading ->
                        mutableUiState.update { state ->
                            state.copy(
                                deviceHeadingDegrees = heading,
                                compassMessage = null,
                            )
                        }
                    }
            }
        }
    }

    private fun requestInitialRoute(location: GeoCoordinate) {
        if (initialRouteRequested) return
        initialRouteRequested = true
        routeJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            mutableUiState.update { it.copy(isLoadingRoute = true, errorMessage = null) }
            runCatching {
                repository.findRouteFromLocation(location)
            }.onSuccess { route ->
                val elapsedMillis = System.currentTimeMillis() - startedAt
                logRouteTiming(elapsedMillis)
                mutableUiState.update { state ->
                    withGuidance(
                        state.copy(
                            route = route,
                            isLoadingRoute = false,
                            errorMessage = null,
                        ),
                    )
                }
                startCountdown()
            }.onFailure { error ->
                mutableUiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        errorMessage = error.message ?: "Rute evakuasi tidak dapat disiapkan.",
                    )
                }
            }
        }
    }

    private fun startCountdown() {
        if (countdownJob != null) return
        countdownJob = viewModelScope.launch {
            while (isActive && mutableUiState.value.remainingEvacuationSeconds > 0) {
                delay(1_000)
                mutableUiState.update { state ->
                    state.copy(
                        remainingEvacuationSeconds =
                            (state.remainingEvacuationSeconds - 1).coerceAtLeast(0),
                    )
                }
            }
        }
    }

    private fun withGuidance(state: EvacuationUiState): EvacuationUiState {
        val location = state.currentLocation ?: return state
        val routeCoordinates = state.route?.coordinates ?: return state
        return state.copy(
            guidance = RouteGuidanceCalculator.calculate(location, routeCoordinates),
        )
    }

    @SuppressLint("LogNotTimber")
    private fun logRouteTiming(elapsedMillis: Long) {
        // Pencatatan ini menjadi bukti pengukuran NF-02 pada perangkat uji.
        Log.i(LOG_TAG, "Arahan siap dalam $elapsedMillis ms")
    }

    private companion object {
        const val LOG_TAG = "EvacuationTiming"
    }
}
