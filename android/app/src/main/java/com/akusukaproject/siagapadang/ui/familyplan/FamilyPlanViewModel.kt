package com.akusukaproject.siagapadang.ui.familyplan

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akusukaproject.siagapadang.SiagaPadangApplication
import com.akusukaproject.siagapadang.data.model.EvacuationPoint
import com.akusukaproject.siagapadang.data.model.FamilyMember
import com.akusukaproject.siagapadang.data.model.FamilyPlan
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

data class EvacuationPointOption(
    val point: EvacuationPoint,
    val distanceMeters: Int?,
)

data class DestinationSuggestion(
    val requestId: Long,
    val destinationName: String,
)

data class FamilyPlanUiState(
    val plan: FamilyPlan = FamilyPlan(),
    val evacuationPoints: List<EvacuationPointOption> = emptyList(),
    val isLoadingPoints: Boolean = true,
    val pointsErrorMessage: String? = null,
    val isSortedByDistance: Boolean = false,
    val isFindingSuggestion: Boolean = false,
    val suggestion: DestinationSuggestion? = null,
    val suggestionMessage: String? = null,
    val saveErrorMessage: String? = null,
)

class FamilyPlanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SiagaPadangApplication
    private val planRepository = app.familyPlanRepository
    private val evacuationRepository = app.evacuationRepository
    private val mutableUiState = MutableStateFlow(FamilyPlanUiState())
    val uiState: StateFlow<FamilyPlanUiState> = mutableUiState.asStateFlow()
    private var nextSuggestionRequestId = 0L

    init {
        viewModelScope.launch {
            val plan = withContext(Dispatchers.IO) { planRepository.load() }
            mutableUiState.update { it.copy(plan = plan) }
        }
        loadEvacuationPoints()
    }

    fun newMemberId(): String = UUID.randomUUID().toString()

    fun setMeetingPoint(name: String?) = persist { planRepository.setMeetingPoint(name) }

    fun saveMember(member: FamilyMember) = persist { planRepository.saveMember(member) }

    fun removeMember(memberId: String) = persist { planRepository.removeMember(memberId) }

    /**
     * Mengambil TES rank 1 hasil prakomputasi untuk posisi perangkat saat ini. Berguna ketika
     * rencana disusun sambil berada di lokasi rutin anggota (sekolah, kantor, pasar).
     */
    fun suggestDestinationFromCurrentLocation() {
        if (mutableUiState.value.isFindingSuggestion) return
        if (!hasLocationPermission()) {
            mutableUiState.update {
                it.copy(suggestionMessage = "Izin lokasi belum diberikan. Pilih TES dari daftar.")
            }
            return
        }
        mutableUiState.update { it.copy(isFindingSuggestion = true, suggestionMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                val lastKnown = runCatching { app.locationProvider.lastKnownLocation() }.getOrNull()
                val location = lastKnown?.takeIf { it.accuracyMeters <= MAX_SUGGESTION_ACCURACY_METERS }
                    ?: app.locationProvider.currentOrLastKnownLocation()
                    ?: error("Posisi GPS belum tersedia. Coba di tempat terbuka atau pilih dari daftar.")
                evacuationRepository.findSummaryFromLocation(location.coordinate).destinationName
            }
            mutableUiState.update { state ->
                result.fold(
                    onSuccess = { name ->
                        state.copy(
                            isFindingSuggestion = false,
                            suggestion = DestinationSuggestion(++nextSuggestionRequestId, name),
                            suggestionMessage = "TES dari rute evakuasi di posisi ini.",
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            isFindingSuggestion = false,
                            suggestionMessage = error.message
                                ?: "TES tidak dapat ditentukan dari posisi ini.",
                        )
                    },
                )
            }
        }
    }

    fun consumeSuggestion() {
        mutableUiState.update { it.copy(suggestion = null) }
    }

    fun clearSuggestionMessage() {
        mutableUiState.update { it.copy(suggestionMessage = null, suggestion = null) }
    }

    fun dismissSaveError() {
        mutableUiState.update { it.copy(saveErrorMessage = null) }
    }

    private fun persist(operation: () -> FamilyPlan) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching(operation) }
            mutableUiState.update { state ->
                result.fold(
                    onSuccess = { plan -> state.copy(plan = plan, saveErrorMessage = null) },
                    onFailure = { error ->
                        state.copy(saveErrorMessage = error.message ?: "Rencana gagal disimpan.")
                    },
                )
            }
        }
    }

    private fun loadEvacuationPoints() {
        viewModelScope.launch {
            val points = runCatching { evacuationRepository.loadEvacuationPoints() }.getOrElse {
                mutableUiState.update { state ->
                    state.copy(
                        isLoadingPoints = false,
                        pointsErrorMessage = "Daftar TES tidak dapat dibaca dari data lokal.",
                    )
                }
                return@launch
            }
            val location = if (hasLocationPermission()) {
                runCatching { app.locationProvider.lastKnownLocation() }.getOrNull()?.coordinate
            } else {
                null
            }
            val options = withContext(Dispatchers.Default) { sortPoints(points, location) }
            mutableUiState.update { state ->
                state.copy(
                    evacuationPoints = options,
                    isLoadingPoints = false,
                    isSortedByDistance = location != null,
                )
            }
        }
    }

    private fun sortPoints(
        points: List<EvacuationPoint>,
        location: GeoCoordinate?,
    ): List<EvacuationPointOption> {
        if (location == null) return points.map { EvacuationPointOption(it, distanceMeters = null) }
        return points
            .map { point ->
                EvacuationPointOption(
                    point = point,
                    distanceMeters = NearestNodeFinder.distanceMeters(location, point.coordinate)
                        .roundToInt(),
                )
            }
            .sortedBy { it.distanceMeters }
    }

    private companion object {
        const val MAX_SUGGESTION_ACCURACY_METERS = 150f
    }

    private fun hasLocationPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
