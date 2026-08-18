package com.akusukaproject.siagapadang.ui.evacuation

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akusukaproject.siagapadang.SiagaPadangApplication
import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.InundationZoneStatus
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
import kotlin.math.ceil
import kotlin.math.abs
import kotlin.math.roundToInt

class EvacuationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SiagaPadangApplication
    private val repository = app.evacuationRepository
    private val zoneRepository = app.zoneRepository
    private val mutableUiState = MutableStateFlow(EvacuationUiState())
    val uiState: StateFlow<EvacuationUiState> = mutableUiState.asStateFlow()

    private var locationJob: Job? = null
    private var compassJob: Job? = null
    private var routeJob: Job? = null
    private var countdownJob: Job? = null
    private var zoneStatusJob: Job? = null
    private var offlineRoadOverlayJob: Job? = null
    private var initialRouteRequested = false
    private var minimumRouteIndex = 0
    private var minimumRouteSegmentFraction = 0.0
    private var announcedManeuverIndex: Int? = null
    private var lastZoneCheckLocation: GeoCoordinate? = null
    private var lastOfflineRoadCenter: GeoCoordinate? = null
    private var confirmedZoneKey: String? = null
    private var pendingZoneKey: String? = null
    private var pendingZoneConfirmationCount = 0
    private val countdownStartedAtElapsedMillis = SystemClock.elapsedRealtime()
    private val rejectedDestinationNames = mutableSetOf<String>()
    private val arrivalTracker = ArrivalConfirmationTracker()

    init {
        loadTsunamiZoneOverlay()
        monitorNetworkStatus()
        startCountdown()
    }

    fun onMapViewportChanged(center: GeoCoordinate) {
        val previousCenter = lastOfflineRoadCenter
        if (
            previousCenter != null &&
            NearestNodeFinder.distanceMeters(previousCenter, center) < ROAD_VIEWPORT_RELOAD_METERS
        ) {
            return
        }
        lastOfflineRoadCenter = center
        offlineRoadOverlayJob?.cancel()
        offlineRoadOverlayJob = viewModelScope.launch {
            delay(ROAD_VIEWPORT_DEBOUNCE_MILLIS)
            runCatching { repository.loadOfflineRoadOverlay(center) }
                .onSuccess { overlay ->
                    mutableUiState.update { state ->
                        state.copy(offlineRoadOverlay = overlay)
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Jaringan jalan lokal tidak dapat dimuat", error)
                }
        }
    }

    private fun monitorNetworkStatus() {
        viewModelScope.launch {
            app.networkStatusProvider.availability()
                .catch {
                    mutableUiState.update { state -> state.copy(isNetworkAvailable = false) }
                }
                .collect { isAvailable ->
                    mutableUiState.update { state ->
                        state.copy(isNetworkAvailable = isAvailable)
                    }
                }
        }
    }

    private fun loadTsunamiZoneOverlay() {
        viewModelScope.launch {
            runCatching { zoneRepository.loadMapOverlay() }
                .onSuccess { overlay ->
                    mutableUiState.update { state ->
                        state.copy(tsunamiZoneOverlay = overlay)
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Layer zona tsunami tidak dapat dimuat", error)
                }
        }
    }

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
                resetRouteProgress()
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
                            alternativeRouteMessage = buildAlternativeRouteMessage(
                                previousRoute = currentRoute,
                                newRoute = route,
                            ),
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
                        if (!arrivalConfirmedNow) maybeVibrateUpcomingManeuver()
                        requestInitialRoute(deviceLocation.coordinate)
                        evaluateCurrentZone(deviceLocation.coordinate)
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
                            withGuidance(
                                state.copy(
                                    deviceHeadingDegrees = heading,
                                    compassMessage = null,
                                ),
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
                resetRouteProgress()
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

    private fun evaluateCurrentZone(location: GeoCoordinate) {
        val movedMeters = lastZoneCheckLocation?.let { previous ->
            NearestNodeFinder.distanceMeters(previous, location)
        } ?: Double.POSITIVE_INFINITY
        if (movedMeters < MIN_ZONE_CHECK_MOVEMENT_METERS || zoneStatusJob?.isActive == true) return
        lastZoneCheckLocation = location
        zoneStatusJob = viewModelScope.launch {
            runCatching { zoneRepository.findStatus(location) }
                .onSuccess(::applyZoneStatus)
                .onFailure { error -> Log.w(LOG_TAG, "Status zona tidak dapat diperbarui", error) }
        }
    }

    private fun applyZoneStatus(status: InundationZoneStatus) {
        if (status == InundationZoneStatus.DataUnavailable) return
        val candidateKey = status.zoneCategoryKey()
        if (confirmedZoneKey == null) {
            confirmZoneStatus(status, candidateKey, isInitial = true)
            return
        }
        if (candidateKey == confirmedZoneKey) {
            pendingZoneKey = null
            pendingZoneConfirmationCount = 0
            return
        }
        if (pendingZoneKey == candidateKey) {
            pendingZoneConfirmationCount += 1
        } else {
            pendingZoneKey = candidateKey
            pendingZoneConfirmationCount = 1
        }
        if (pendingZoneConfirmationCount >= REQUIRED_ZONE_TRANSITION_CONFIRMATIONS) {
            confirmZoneStatus(status, candidateKey, isInitial = false)
        }
    }

    private fun confirmZoneStatus(
        status: InundationZoneStatus,
        categoryKey: String,
        isInitial: Boolean,
    ) {
        confirmedZoneKey = categoryKey
        pendingZoneKey = null
        pendingZoneConfirmationCount = 0
        mutableUiState.update { state ->
            state.copy(
                currentZoneStatus = status,
                zoneTransitionVersion = state.zoneTransitionVersion + 1,
                zoneTransitionMessage = status.zoneMessage(isInitial),
            )
        }
    }

    private fun InundationZoneStatus.zoneCategoryKey(): String = when (this) {
        InundationZoneStatus.DataUnavailable -> "unknown"
        InundationZoneStatus.OutsideRecordedZone -> "safe"
        is InundationZoneStatus.InsideRecordedZone ->
            "risk-${dangerLevel.trim().lowercase()}"
    }

    private fun InundationZoneStatus.zoneMessage(isInitial: Boolean): String = when (this) {
        InundationZoneStatus.DataUnavailable -> "Status zona belum tersedia"
        InundationZoneStatus.OutsideRecordedZone ->
            if (isInitial) {
                "Lokasi Anda di luar zona rendaman"
            } else {
                "Lokasi Anda keluar dari zona rendaman"
            }
        is InundationZoneStatus.InsideRecordedZone ->
            if (isInitial) {
                "Lokasi Anda di zona rendaman"
            } else {
                "Lokasi Anda memasuki zona rendaman"
            }
    }

    private fun startCountdown() {
        if (countdownJob != null) return
        countdownJob = viewModelScope.launch {
            while (isActive && !mutableUiState.value.hasArrived) {
                val elapsedSeconds = (
                    (SystemClock.elapsedRealtime() - countdownStartedAtElapsedMillis) / 1_000L
                    ).toInt()
                val remainingSeconds = (
                    EvacuationUiState.EVACUATION_WINDOW_SECONDS - elapsedSeconds
                    ).coerceAtLeast(0)
                mutableUiState.update { state ->
                    state.copy(remainingEvacuationSeconds = remainingSeconds)
                }
                if (remainingSeconds == 0) break
                delay(1_000)
            }
        }
    }

    private fun withGuidance(state: EvacuationUiState): EvacuationUiState {
        val location = state.currentLocation ?: return state
        val routeCoordinates = state.route?.coordinates ?: return state
        val guidance = RouteGuidanceCalculator.calculate(
            currentLocation = location,
            routeCoordinates = routeCoordinates,
            minimumRouteIndex = minimumRouteIndex,
            minimumSegmentFraction = minimumRouteSegmentFraction,
            deviceHeadingDegrees = state.deviceHeadingDegrees,
        )
        guidance?.let { snapshot ->
            when {
                snapshot.nearestRouteIndex > minimumRouteIndex -> {
                    minimumRouteIndex = snapshot.nearestRouteIndex
                    minimumRouteSegmentFraction = snapshot.routeSegmentFraction
                }
                snapshot.nearestRouteIndex == minimumRouteIndex -> {
                    minimumRouteSegmentFraction = maxOf(
                        minimumRouteSegmentFraction,
                        snapshot.routeSegmentFraction,
                    )
                }
            }
        }
        return state.copy(
            guidance = guidance,
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

    private fun resetRouteProgress() {
        minimumRouteIndex = 0
        minimumRouteSegmentFraction = 0.0
        announcedManeuverIndex = null
    }

    private fun maybeVibrateUpcomingManeuver() {
        val instruction = mutableUiState.value.guidance?.currentInstruction ?: return
        if (
            instruction.type == ManeuverType.STRAIGHT ||
            instruction.type == ManeuverType.ARRIVE ||
            instruction.distanceMeters > MANEUVER_ALERT_DISTANCE_METERS ||
            instruction.routeCoordinateIndex == announcedManeuverIndex
        ) {
            return
        }
        announcedManeuverIndex = instruction.routeCoordinateIndex
        vibratePattern(longArrayOf(0L, 120L))
    }

    private fun buildAlternativeRouteMessage(
        previousRoute: EvacuationRoute,
        newRoute: EvacuationRoute,
    ): String {
        val differenceSeconds = newRoute.estimatedSeconds - previousRoute.estimatedSeconds
        val comparison = when {
            abs(differenceSeconds) < 30 -> "waktu hampir sama"
            differenceSeconds > 0 ->
                "+${ceil(differenceSeconds / 60.0).toInt()} menit"
            else ->
                "${ceil(abs(differenceSeconds) / 60.0).toInt()} menit lebih cepat"
        }
        return "Rute baru ke ${newRoute.destinationName} · $comparison"
    }

    @Suppress("DEPRECATION")
    private fun vibrateArrivalPattern() {
        vibratePattern(longArrayOf(0L, 180L, 100L, 260L))
    }

    @Suppress("DEPRECATION")
    private fun vibratePattern(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            app.getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }

    @SuppressLint("LogNotTimber")
    private fun logRouteTiming(elapsedMillis: Long) {
        // Pencatatan ini menjadi bukti pengukuran NF-02 pada perangkat uji.
        Log.i(LOG_TAG, "Arahan siap dalam $elapsedMillis ms")
    }

    private companion object {
        const val LOG_TAG = "EvacuationTiming"
        const val MANEUVER_ALERT_DISTANCE_METERS = 30
        const val MIN_ZONE_CHECK_MOVEMENT_METERS = 12.0
        const val REQUIRED_ZONE_TRANSITION_CONFIRMATIONS = 2
        const val ROAD_VIEWPORT_RELOAD_METERS = 500.0
        const val ROAD_VIEWPORT_DEBOUNCE_MILLIS = 120L
    }
}
