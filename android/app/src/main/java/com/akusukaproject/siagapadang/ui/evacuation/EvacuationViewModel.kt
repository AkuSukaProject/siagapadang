package com.akusukaproject.siagapadang.ui.evacuation

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akusukaproject.siagapadang.SiagaPadangApplication
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.domain.ArrivalConfirmationTracker
import com.akusukaproject.siagapadang.domain.ManeuverGuidance
import com.akusukaproject.siagapadang.domain.ManeuverType
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import com.akusukaproject.siagapadang.domain.RouteGuidanceCalculator
import com.akusukaproject.siagapadang.domain.RouteGuidanceSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    private val rejectedDestinationNames = mutableSetOf<String>()
    private val arrivalTracker = ArrivalConfirmationTracker()

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
        val currentState = mutableUiState.value
        val currentRoute = currentState.route ?: return
        val currentLocation = currentState.currentLocation ?: return
        if (!currentState.canSelectAlternative || routeJob?.isActive == true) return

        routeJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoadingRoute = true, errorMessage = null) }
            runCatching {
                repository.findAlternativeRoute(
                    location = currentLocation,
                    currentRoute = currentRoute,
                    excludedDestinationNames = rejectedDestinationNames,
                )
            }.onSuccess { route ->
                rejectedDestinationNames += currentRoute.destinationName
                arrivalTracker.reset()
                mutableUiState.update { state ->
                    withGuidance(
                        state.copy(
                            route = route,
                            previousRoutes =
                                (state.previousRoutes + currentRoute)
                                    .distinctBy { previousRoute -> previousRoute.destinationName },
                            isLoadingRoute = false,
                            remainingAlternativeCount =
                                (state.remainingAlternativeCount - 1).coerceAtLeast(0),
                            alternativeRouteVersion = state.alternativeRouteVersion + 1,
                            alternativeRouteMessage = "Rute dialihkan ke ${route.destinationName}",
                            hasArrived = false,
                            arrivalDistanceMeters = null,
                        ),
                    )
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
                        var arrivalConfirmedNow = false
                        mutableUiState.update { state ->
                            val updatedState = withArrivalEvaluation(
                                state.copy(
                                    currentLocation = deviceLocation.coordinate,
                                    locationAccuracyMeters = deviceLocation.accuracyMeters,
                                ),
                            )
                            arrivalConfirmedNow = !state.hasArrived && updatedState.hasArrived
                            updatedState
                        }
                        if (arrivalConfirmedNow) onArrivalConfirmed()
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
                rejectedDestinationNames.clear()
                arrivalTracker.reset()
                mutableUiState.update { state ->
                    withGuidance(
                        state.copy(
                            route = route,
                            previousRoutes = emptyList(),
                            isLoadingRoute = false,
                            remainingAlternativeCount = EvacuationUiState.MAX_ALTERNATIVE_COUNT,
                            alternativeRouteMessage = null,
                            hasArrived = false,
                            arrivalDistanceMeters = null,
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
            while (
                isActive &&
                !mutableUiState.value.hasArrived &&
                mutableUiState.value.remainingEvacuationSeconds > 0
            ) {
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

    private fun withArrivalEvaluation(state: EvacuationUiState): EvacuationUiState {
        val guidedState = withGuidance(state)
        val location = guidedState.currentLocation ?: return guidedState
        val route = guidedState.route ?: return guidedState
        val targets = buildList {
            route.destinationCoordinate?.let(::add)
            route.coordinates.lastOrNull()?.let(::add)
        }
        if (targets.isEmpty()) return guidedState
        val distanceMeters = targets.minOf { target ->
            NearestNodeFinder.distanceMeters(location, target)
        }.roundToInt().coerceAtLeast(0)

        val hasArrived = guidedState.hasArrived || arrivalTracker.update(
            distanceMeters = distanceMeters.toDouble(),
            accuracyMeters = guidedState.locationAccuracyMeters,
        )
        return if (hasArrived) {
            guidedState.copy(
                hasArrived = true,
                arrivalDistanceMeters = distanceMeters,
                guidance = arrivalGuidance(distanceMeters),
            )
        } else {
            guidedState.copy(arrivalDistanceMeters = distanceMeters)
        }
    }

    private fun arrivalGuidance(distanceMeters: Int) = RouteGuidanceSnapshot(
        instructions = listOf(
            ManeuverGuidance(
                type = ManeuverType.ARRIVE,
                distanceMeters = distanceMeters,
            ),
        ),
        remainingDistanceMeters = distanceMeters,
    )

    private fun onArrivalConfirmed() {
        countdownJob?.cancel()
        countdownJob = null
        vibrateArrivalPattern()
    }

    @Suppress("DEPRECATION")
    private fun vibrateArrivalPattern() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            app.getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0L, 180L, 100L, 260L),
                -1,
            ),
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
