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
import com.akusukaproject.siagapadang.data.remote.model.ObstructionReportRequestDto
import com.akusukaproject.siagapadang.data.remote.model.OccupancyReportRequestDto
import com.akusukaproject.siagapadang.data.remote.model.ShelterCheckinRequestDto
import kotlinx.coroutines.Dispatchers
import com.akusukaproject.siagapadang.domain.ActiveEdgeFinder
import com.akusukaproject.siagapadang.domain.ArrivalConfirmationTracker
import com.akusukaproject.siagapadang.domain.ManeuverGuidance
import com.akusukaproject.siagapadang.domain.ManeuverType
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import com.akusukaproject.siagapadang.domain.RouteGuidanceCalculator
import com.akusukaproject.siagapadang.domain.RouteGuidanceSnapshot
import com.akusukaproject.siagapadang.domain.ZoneExitConfirmationTracker
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
    private var lastZoneCheckElapsedMillis = 0L
    private var lastOfflineRoadCenter: GeoCoordinate? = null
    private var confirmedZoneKey: String? = null
    private var pendingZoneKey: String? = null
    private var pendingZoneConfirmationCount = 0
    private val countdownStartedAtElapsedMillis = SystemClock.elapsedRealtime()
    private val rejectedDestinationNames = mutableSetOf<String>()
    private val arrivalTracker = ArrivalConfirmationTracker()
    private val zoneExitTracker = ZoneExitConfirmationTracker()

    init {
        loadTsunamiZoneOverlay()
        loadLocalDatasetManifest()
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
                    val shouldLoadBmkg = isAvailable &&
                        mutableUiState.value.bmkgStatus == null &&
                        !mutableUiState.value.isLoadingBmkgStatus
                    mutableUiState.update { state ->
                        state.copy(isNetworkAvailable = isAvailable)
                    }
                    if (shouldLoadBmkg) refreshBmkgStatus()
                    if (isAvailable) {
                        flushPendingObstructionReports()
                        refreshConfirmedObstructions()
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

    fun refreshBmkgStatus() {
        if (mutableUiState.value.isLoadingBmkgStatus) return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(isLoadingBmkgStatus = true, bmkgErrorMessage = null)
            }
            runCatching { app.bmkgApiClient.getLatestStatus() }
                .onSuccess { status ->
                    mutableUiState.update {
                        it.copy(
                            bmkgStatus = status,
                            isLoadingBmkgStatus = false,
                            bmkgErrorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Status BMKG tidak dapat dimuat", error)
                    mutableUiState.update {
                        it.copy(
                            isLoadingBmkgStatus = false,
                            bmkgErrorMessage = "Informasi BMKG belum dapat diambil.",
                        )
                    }
                }
        }
    }

    private fun loadLocalDatasetManifest() {
        viewModelScope.launch {
            runCatching { app.dataUpdateApiClient.loadLocalManifest() }
                .onSuccess { manifest ->
                    mutableUiState.update { state ->
                        state.copy(localDatasetManifest = manifest)
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Manifest dataset lokal tidak dapat dibaca", error)
                }
        }
    }

    fun checkDataUpdates() {
        if (mutableUiState.value.isCheckingDataUpdate || mutableUiState.value.isInstallingDataUpdate) return
        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isCheckingDataUpdate = true,
                    dataUpdateErrorMessage = null,
                    dataUpdateInstallMessage = null,
                )
            }
            runCatching { app.dataUpdateApiClient.checkForUpdates() }
                .onSuccess { status ->
                    mutableUiState.update { state ->
                        state.copy(
                            localDatasetManifest = status.local,
                            datasetUpdateStatus = status,
                            isCheckingDataUpdate = false,
                            dataUpdateErrorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Pembaruan dataset tidak dapat diperiksa", error)
                    mutableUiState.update { state ->
                        state.copy(
                            isCheckingDataUpdate = false,
                            dataUpdateErrorMessage =
                                "Versi terbaru belum dapat diperiksa. Data lokal tetap dapat digunakan.",
                        )
                    }
                }
        }
    }

    fun installDataUpdate() {
        val remote = mutableUiState.value.datasetUpdateStatus?.downloadableVersion ?: return
        if (mutableUiState.value.isInstallingDataUpdate) return
        viewModelScope.launch {
            mutableUiState.update { state ->
                state.copy(
                    isInstallingDataUpdate = true,
                    dataUpdateErrorMessage = null,
                    dataUpdateInstallMessage = "Mengunduh dan memeriksa paket data…",
                )
            }
            runCatching { app.datasetPackageInstaller.downloadAndStage(remote) }
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            isInstallingDataUpdate = false,
                            dataUpdateInstallMessage =
                                "Pembaruan telah diverifikasi. Tutup dan buka kembali aplikasi untuk mengaktifkannya.",
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(LOG_TAG, "Paket dataset gagal dipasang", error)
                    mutableUiState.update { state ->
                        state.copy(
                            isInstallingDataUpdate = false,
                            dataUpdateErrorMessage =
                                error.message ?: "Paket pembaruan gagal dipasang. Data lama tetap digunakan.",
                            dataUpdateInstallMessage = null,
                        )
                    }
                }
        }
    }

    fun selectAlternativeDestination() {
        val currentState = mutableUiState.value
        val currentRoute = currentState.route ?: return
        val currentLocation = currentState.currentLocation ?: return
        if (!currentState.canSelectAlternative || routeJob?.isActive == true) return

        val blockedEdgeId = currentState.activeEdgeId
        val updatedBlockedEdgeIds = if (blockedEdgeId != null) {
            currentState.blockedEdgeIds + blockedEdgeId
        } else {
            currentState.blockedEdgeIds
        }

        routeJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoadingRoute = true, errorMessage = null) }
            runCatching {
                repository.findAlternativeRoute(
                    location = currentLocation,
                    currentRoute = currentRoute,
                    excludedDestinationNames = rejectedDestinationNames,
                    excludedEdgeIds = updatedBlockedEdgeIds + currentState.confirmedBlockedEdgeIds,
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
                            arrivalReason = null,
                            arrivalDistanceMeters = null,
                            checkinStatus = CheckinStatus.IDLE,
                            checkinMessage = null,
                            checkedInAt = null,
                            blockedEdgeIds = updatedBlockedEdgeIds,
                        ),
                    )
                }

                if (blockedEdgeId != null) {
                    val report = ObstructionReportRequestDto(
                        latitude = currentLocation.latitude,
                        longitude = currentLocation.longitude,
                        datasetVersionId = 1,
                        edgeExternalId = blockedEdgeId.toString(),
                        description = "Jalur terhalang dilaporkan warga via aplikasi",
                    )
                    app.obstructionReportQueue.enqueue(report)
                    mutableUiState.update { state ->
                        state.copy(
                            pendingObstructionCount = app.obstructionReportQueue.getPendingReports().size,
                            obstructionReportMessage = "Rute dialihkan. Laporan jalan terhalang sedang diproses...",
                        )
                    }
                    flushPendingObstructionReports()
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

    fun flushPendingObstructionReports() {
        viewModelScope.launch(Dispatchers.IO) {
            val pendingReports = app.obstructionReportQueue.getPendingReports()
            if (pendingReports.isEmpty()) {
                mutableUiState.update { it.copy(pendingObstructionCount = 0) }
                return@launch
            }

            var anySent = false
            var confirmedCount = 0
            for (report in pendingReports) {
                val result = app.emergencyApiClient.reportObstruction(report)
                result.onSuccess { response ->
                    app.obstructionReportQueue.remove(report.edgeExternalId)
                    anySent = true
                    if (response.isConfirmedBlocked) {
                        confirmedCount++
                    }
                }.onFailure {
                    // Tetap tersimpan di antrean luring
                }
            }

            val remainingCount = app.obstructionReportQueue.getPendingReports().size
            mutableUiState.update { state ->
                state.copy(
                    pendingObstructionCount = remainingCount,
                    obstructionReportMessage = when {
                        confirmedCount > 0 -> "Ruas jalan kini ditandai putus untuk semua pengguna."
                        anySent && remainingCount == 0 -> "Laporan jalan terhalang diterima posko bencana."
                        remainingCount > 0 -> "Laporan tersimpan luring ($remainingCount). Akan dikirim saat sinyal tersedia."
                        else -> state.obstructionReportMessage
                    },
                )
            }
        }
    }

    fun refreshConfirmedObstructions() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = app.emergencyApiClient.getConfirmedObstructions()
            result.onSuccess { externalIds ->
                val edgeIds = externalIds.mapNotNull { it.toLongOrNull() }.toSet()
                if (edgeIds.isNotEmpty()) {
                    mutableUiState.update { state ->
                        state.copy(confirmedBlockedEdgeIds = state.confirmedBlockedEdgeIds + edgeIds)
                    }
                }
            }
        }
    }

    fun dismissObstructionMessage() {
        mutableUiState.update { it.copy(obstructionReportMessage = null) }
    }

    fun performShelterCheckin() {
        val currentState = mutableUiState.value
        if (
            !currentState.hasArrived ||
            currentState.arrivalReason != EvacuationArrivalReason.EVACUATION_POINT ||
            currentState.isCheckingIn
        ) return

        val tesId = currentState.destinationExternalId
        if (tesId.isNullOrBlank()) {
            mutableUiState.update {
                it.copy(
                    checkinStatus = CheckinStatus.FAILED,
                    checkinMessage = "ID tempat evakuasi tidak tersedia pada rute.",
                )
            }
            return
        }

        val location = currentState.currentLocation
        if (location == null) {
            mutableUiState.update {
                it.copy(
                    checkinStatus = CheckinStatus.FAILED,
                    checkinMessage = "Lokasi GPS belum tersedia untuk lapor selamat.",
                )
            }
            return
        }

        val accuracy = currentState.locationAccuracyMeters ?: 50f
        if (accuracy > MAX_CHECKIN_ACCURACY_METERS) {
            mutableUiState.update {
                it.copy(
                    checkinStatus = CheckinStatus.FAILED,
                    checkinMessage = "Akurasi GPS (${accuracy.roundToInt()}m) melebihi batas maksimal 35m. Tunggu sinyal GPS membaik.",
                )
            }
            return
        }

        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    checkinStatus = CheckinStatus.CHECKING_IN,
                    checkinMessage = "Menghubungi posko bencana...",
                )
            }

            val request = ShelterCheckinRequestDto(
                evacuationPointExternalId = tesId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = accuracy,
                status = "Selamat",
            )

            val result = app.emergencyApiClient.checkIn(request)
            result.onSuccess { response ->
                mutableUiState.update {
                    it.copy(
                        checkinStatus = CheckinStatus.SUCCESS,
                        checkinMessage = response.message.ifBlank { "Lapor selamat berhasil dicatat posko." },
                        checkedInAt = response.checkedInAt,
                    )
                }
                fetchShelterOccupancy(tesId)
            }.onFailure { error ->
                mutableUiState.update {
                    it.copy(
                        checkinStatus = CheckinStatus.FAILED,
                        checkinMessage = error.message ?: "Gagal terhubung ke posko bencana.",
                    )
                }
            }
        }
    }

    fun resetCheckinStatus() {
        mutableUiState.update {
            it.copy(
                checkinStatus = CheckinStatus.IDLE,
                checkinMessage = null,
                checkedInAt = null,
                occupancyStatus = null,
                isReportingOccupancy = false,
                occupancyReportMessage = null,
            )
        }
    }

    fun reportShelterOccupancy(level: String) {
        val currentState = mutableUiState.value
        if (currentState.checkinStatus != CheckinStatus.SUCCESS || currentState.isReportingOccupancy) return
        val tesId = currentState.destinationExternalId ?: return

        viewModelScope.launch(Dispatchers.IO) {
            mutableUiState.update {
                it.copy(
                    isReportingOccupancy = true,
                    occupancyReportMessage = "Mengirim laporan kondisi shelter...",
                )
            }
            val result = app.emergencyApiClient.reportOccupancy(
                OccupancyReportRequestDto(
                    evacuationPointExternalId = tesId,
                    level = level,
                ),
            )
            result.onSuccess { statusResponse ->
                mutableUiState.update {
                    it.copy(
                        isReportingOccupancy = false,
                        occupancyStatus = statusResponse,
                        occupancyReportMessage = "Terima kasih, laporan kondisi shelter berhasil diperbarui!",
                    )
                }
            }.onFailure { error ->
                mutableUiState.update {
                    it.copy(
                        isReportingOccupancy = false,
                        occupancyReportMessage = error.message ?: "Gagal mengirim laporan kondisi shelter.",
                    )
                }
            }
        }
    }

    fun fetchShelterOccupancy(evacuationPointExternalId: String? = null) {
        val tesId = evacuationPointExternalId ?: mutableUiState.value.destinationExternalId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = app.emergencyApiClient.getOccupancyStatus(tesId)
            result.onSuccess { statusResponse ->
                mutableUiState.update {
                    it.copy(occupancyStatus = statusResponse)
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
                        evaluateCurrentZone(
                            location = deviceLocation.coordinate,
                            accuracyMeters = deviceLocation.accuracyMeters,
                        )
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
                            arrivalReason = null,
                            arrivalDistanceMeters = null,
                            errorMessage = null,
                            checkinStatus = CheckinStatus.IDLE,
                            checkinMessage = null,
                            checkedInAt = null,
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

    private fun evaluateCurrentZone(location: GeoCoordinate, accuracyMeters: Float?) {
        val movedMeters = lastZoneCheckLocation?.let { previous ->
            NearestNodeFinder.distanceMeters(previous, location)
        } ?: Double.POSITIVE_INFINITY
        val now = SystemClock.elapsedRealtime()
        val elapsedMillis = now - lastZoneCheckElapsedMillis
        if (
            (movedMeters < MIN_ZONE_CHECK_MOVEMENT_METERS &&
                elapsedMillis < MAX_ZONE_CHECK_INTERVAL_MILLIS) ||
            zoneStatusJob?.isActive == true
        ) return
        lastZoneCheckLocation = location
        lastZoneCheckElapsedMillis = now
        zoneStatusJob = viewModelScope.launch {
            runCatching { zoneRepository.findStatus(location) }
                .onSuccess { status -> applyZoneStatus(status, accuracyMeters) }
                .onFailure { error -> Log.w(LOG_TAG, "Status zona tidak dapat diperbarui", error) }
        }
    }

    private fun applyZoneStatus(status: InundationZoneStatus, accuracyMeters: Float?) {
        if (status == InundationZoneStatus.DataUnavailable) {
            zoneExitTracker.update(status, accuracyMeters)
            pendingZoneKey = null
            pendingZoneConfirmationCount = 0
            return
        }
        val exitConfirmed = zoneExitTracker.update(status, accuracyMeters)
        if (accuracyMeters == null || accuracyMeters > MAX_ZONE_ACCURACY_METERS) {
            pendingZoneKey = null
            pendingZoneConfirmationCount = 0
            return
        }
        val candidateKey = status.zoneCategoryKey()
        if (confirmedZoneKey == null) {
            confirmZoneStatus(status, candidateKey, isInitial = true)
            if (exitConfirmed) confirmOutsideZoneArrival()
            return
        }
        if (candidateKey == confirmedZoneKey) {
            pendingZoneKey = null
            pendingZoneConfirmationCount = 0
            if (exitConfirmed) confirmOutsideZoneArrival()
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
        if (exitConfirmed) confirmOutsideZoneArrival()
    }

    private fun confirmOutsideZoneArrival() {
        var confirmedNow = false
        mutableUiState.update { state ->
            if (state.hasArrived || state.route == null) return@update state
            confirmedNow = true
            state.copy(
                hasArrived = true,
                arrivalReason = EvacuationArrivalReason.OUTSIDE_INUNDATION_ZONE,
                arrivalDistanceMeters = null,
                guidance = outsideZoneArrivalGuidance(),
            )
        }
        if (confirmedNow) onArrivalConfirmed()
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
        val activeEdgeId = state.route.let { currentRoute ->
            ActiveEdgeFinder.findActiveEdgeId(
                location = location,
                route = currentRoute,
                nearestRouteCoordinateIndex = guidance?.nearestRouteIndex,
            )
        }
        return state.copy(
            guidance = guidance,
            activeEdgeId = activeEdgeId,
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
                arrivalReason = EvacuationArrivalReason.EVACUATION_POINT,
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

    private fun outsideZoneArrivalGuidance() = RouteGuidanceSnapshot(
        instructions = listOf(
            ManeuverGuidance(
                type = ManeuverType.ARRIVE,
                distanceMeters = 0,
            ),
        ),
        remainingDistanceMeters = 0,
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

    companion object {
        private const val LOG_TAG = "EvacuationTiming"
        private const val MANEUVER_ALERT_DISTANCE_METERS = 30
        private const val MIN_ZONE_CHECK_MOVEMENT_METERS = 12.0
        private const val MAX_ZONE_CHECK_INTERVAL_MILLIS = 1_500L
        private const val MAX_ZONE_ACCURACY_METERS = 35f
        private const val REQUIRED_ZONE_TRANSITION_CONFIRMATIONS = 2
        private const val ROAD_VIEWPORT_RELOAD_METERS = 500.0
        private const val ROAD_VIEWPORT_DEBOUNCE_MILLIS = 120L
        const val MAX_CHECKIN_ACCURACY_METERS = 35f
    }
}
