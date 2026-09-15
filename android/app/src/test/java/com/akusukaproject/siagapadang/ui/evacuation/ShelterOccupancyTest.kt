package com.akusukaproject.siagapadang.ui.evacuation

import com.akusukaproject.siagapadang.data.remote.model.OccupancyReportRequestDto
import com.akusukaproject.siagapadang.data.remote.model.OccupancyStatusResponseDto
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelterOccupancyTest {

    @Test
    fun `default occupancy state is null and not reporting`() {
        val state = EvacuationUiState()
        assertNull(state.occupancyStatus)
        assertFalse(state.isReportingOccupancy)
        assertNull(state.occupancyReportMessage)
    }

    @Test
    fun `OccupancyReportRequestDto serializes to JSON properly`() {
        val request = OccupancyReportRequestDto(
            evacuationPointExternalId = "TES_PASAR_RAYA",
            level = "MODERATE",
        )
        val json = request.toJson()
        assertEquals("TES_PASAR_RAYA", json.getString("evacuation_point_external_id"))
        assertEquals("MODERATE", json.getString("level"))
    }

    @Test
    fun `OccupancyStatusResponseDto deserializes from JSON with all fields and fallback source`() {
        val json = JSONObject(
            """
            {
                "evacuation_point_external_id": "TES_01",
                "level": "LOW",
                "report_count": 5,
                "updated_at": "2026-09-15T12:00:00Z",
                "source": "Laporan pengguna yang sudah check-in"
            }
            """.trimIndent(),
        )
        val dto = OccupancyStatusResponseDto.fromJson(json)
        assertEquals("TES_01", dto.evacuationPointExternalId)
        assertEquals("LOW", dto.level)
        assertEquals(5, dto.reportCount)
        assertEquals("2026-09-15T12:00:00Z", dto.updatedAt)
        assertEquals("Laporan pengguna yang sudah check-in", dto.source)
    }

    @Test
    fun `OccupancyStatusResponseDto defaults source if not present in JSON`() {
        val json = JSONObject(
            """
            {
                "evacuation_point_external_id": "TES_01",
                "level": "UNKNOWN",
                "report_count": 0
            }
            """.trimIndent(),
        )
        val dto = OccupancyStatusResponseDto.fromJson(json)
        assertEquals("TES_01", dto.evacuationPointExternalId)
        assertEquals("UNKNOWN", dto.level)
        assertEquals(0, dto.reportCount)
        assertNull(dto.updatedAt)
        assertEquals("Laporan pengguna yang sudah check-in", dto.source)
    }

    @Test
    fun `state reflects reporting and success status properly`() {
        val reportingState = EvacuationUiState(isReportingOccupancy = true)
        assertTrue(reportingState.isReportingOccupancy)

        val updatedState = EvacuationUiState(
            occupancyStatus = OccupancyStatusResponseDto(
                evacuationPointExternalId = "TES_01",
                level = "FULL",
                reportCount = 8,
                updatedAt = "2026-09-15T12:15:00Z",
            ),
            occupancyReportMessage = "Kondisi shelter berhasil dikirim.",
        )
        assertEquals("FULL", updatedState.occupancyStatus?.level)
        assertEquals(8, updatedState.occupancyStatus?.reportCount)
        assertEquals("Kondisi shelter berhasil dikirim.", updatedState.occupancyReportMessage)
    }
}
