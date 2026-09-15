package com.akusukaproject.siagapadang.ui.evacuation

import com.akusukaproject.siagapadang.data.model.EvacuationRoute
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelterCheckinTest {

    @Test
    fun `default checkin status is idle and not checking in`() {
        val state = EvacuationUiState()
        assertEquals(CheckinStatus.IDLE, state.checkinStatus)
        assertFalse(state.isCheckingIn)
        assertNull(state.checkinMessage)
        assertNull(state.checkedInAt)
    }

    @Test
    fun `isCheckingIn returns true only when status is CHECKING_IN`() {
        val idleState = EvacuationUiState(checkinStatus = CheckinStatus.IDLE)
        assertFalse(idleState.isCheckingIn)

        val checkingInState = EvacuationUiState(checkinStatus = CheckinStatus.CHECKING_IN)
        assertTrue(checkingInState.isCheckingIn)

        val successState = EvacuationUiState(checkinStatus = CheckinStatus.SUCCESS)
        assertFalse(successState.isCheckingIn)

        val failedState = EvacuationUiState(checkinStatus = CheckinStatus.FAILED)
        assertFalse(failedState.isCheckingIn)
    }

    @Test
    fun `destinationExternalId is extracted correctly from route`() {
        val routeWithoutTesId = EvacuationRoute(
            originNodeId = 101L,
            rank = 1,
            destinationName = "TES Pasar Raya",
            estimatedSeconds = 300,
            coordinates = emptyList(),
            destinationCoordinate = GeoCoordinate(-0.95, 100.36),
            destinationExternalId = null,
        )
        val stateWithoutTesId = EvacuationUiState(route = routeWithoutTesId)
        assertNull(stateWithoutTesId.destinationExternalId)

        val routeWithTesId = EvacuationRoute(
            originNodeId = 101L,
            rank = 1,
            destinationName = "TES Pasar Raya",
            estimatedSeconds = 300,
            coordinates = emptyList(),
            destinationCoordinate = GeoCoordinate(-0.95, 100.36),
            destinationExternalId = "TES_PASAR_RAYA_01",
        )
        val stateWithTesId = EvacuationUiState(route = routeWithTesId)
        assertEquals("TES_PASAR_RAYA_01", stateWithTesId.destinationExternalId)
    }

    @Test
    fun `max checkin accuracy threshold is 35 meters`() {
        assertEquals(35f, EvacuationViewModel.MAX_CHECKIN_ACCURACY_METERS)
    }
}
