package com.akusukaproject.siagapadang.ui.evacuation

import org.junit.Assert.assertEquals
import org.junit.Test

class EvacuationUiStateTest {
    @Test
    fun `classifies gps accuracy for emergency feedback`() {
        assertEquals(LocationQuality.SEARCHING, EvacuationUiState().locationQuality)
        assertEquals(
            LocationQuality.GOOD,
            EvacuationUiState(locationAccuracyMeters = 8f).locationQuality,
        )
        assertEquals(
            LocationQuality.FAIR,
            EvacuationUiState(locationAccuracyMeters = 24f).locationQuality,
        )
        assertEquals(
            LocationQuality.WEAK,
            EvacuationUiState(locationAccuracyMeters = 60f).locationQuality,
        )
    }
}
