package com.akusukaproject.siagapadang.data.remote

import com.akusukaproject.siagapadang.data.remote.model.ActiveEmergencyEventDto
import com.akusukaproject.siagapadang.data.remote.model.ObstructionReportRequestDto
import com.akusukaproject.siagapadang.data.remote.model.ObstructionReportResponseDto
import com.akusukaproject.siagapadang.data.remote.model.OccupancyReportRequestDto
import com.akusukaproject.siagapadang.data.remote.model.OccupancyStatusResponseDto
import com.akusukaproject.siagapadang.data.remote.model.ShelterCheckinRequestDto
import com.akusukaproject.siagapadang.data.remote.model.ShelterCheckinResponseDto
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyApiDtosTest {

    @Test
    fun `ActiveEmergencyEventDto parsing json aktif dan tidak aktif`() {
        val activeJson = JSONObject(
            """
            {
                "active": true,
                "event_external_id": "EVENT-2026-09-15",
                "source": "BPBD",
                "started_at": "2026-09-15T08:00:00Z"
            }
            """.trimIndent(),
        )
        val activeDto = ActiveEmergencyEventDto.fromJson(activeJson)
        assertTrue(activeDto.active)
        assertEquals("EVENT-2026-09-15", activeDto.eventExternalId)
        assertEquals("BPBD", activeDto.source)

        val inactiveJson = JSONObject("""{"active": false}""")
        val inactiveDto = ActiveEmergencyEventDto.fromJson(inactiveJson)
        assertFalse(inactiveDto.active)
    }

    @Test
    fun `ShelterCheckinRequestDto serialisasi json sesuai skema backend`() {
        val request = ShelterCheckinRequestDto(
            evacuationPointExternalId = "TES_01",
            latitude = -0.95,
            longitude = 100.35,
            accuracyMeters = 12.5f,
            eventExternalId = "EVENT-123",
            status = "Selamat",
        )
        val json = request.toJson()

        assertEquals("TES_01", json.getString("evacuation_point_external_id"))
        assertEquals(-0.95, json.getDouble("latitude"), 0.001)
        assertEquals(100.35, json.getDouble("longitude"), 0.001)
        assertEquals(12.5, json.getDouble("accuracy_m"), 0.001)
        assertEquals("EVENT-123", json.getString("event_external_id"))
        assertEquals("Selamat", json.getString("status"))
    }

    @Test
    fun `ShelterCheckinResponseDto deserialisasi respon backend`() {
        val json = JSONObject(
            """
            {
                "status": "success",
                "message": "Berhasil lapor selamat!",
                "event_external_id": "EVENT-123",
                "evacuation_point_external_id": "TES_01",
                "checked_in_at": "2026-09-15T08:30:00Z"
            }
            """.trimIndent(),
        )
        val dto = ShelterCheckinResponseDto.fromJson(json)

        assertEquals("success", dto.status)
        assertEquals("Berhasil lapor selamat!", dto.message)
        assertEquals("TES_01", dto.evacuationPointExternalId)
    }

    @Test
    fun `ObstructionReportRequestDto dan ResponseDto serialisasi dan deserialisasi`() {
        val request = ObstructionReportRequestDto(
            latitude = -0.945,
            longitude = 100.355,
            datasetVersionId = 1,
            edgeExternalId = "45678",
            description = "Pohon tumbang menutup dua lajur",
        )
        val json = request.toJson()

        assertEquals(-0.945, json.getDouble("latitude"), 0.001)
        assertEquals(1, json.getInt("dataset_version_id"))
        assertEquals("45678", json.getString("edge_external_id"))
        assertEquals("Pohon tumbang menutup dua lajur", json.getString("description"))

        val responseJson = JSONObject(
            """
            {
                "status": "success",
                "message": "Laporan diterima.",
                "obstruction_id": 99,
                "is_confirmed_blocked": true
            }
            """.trimIndent(),
        )
        val responseDto = ObstructionReportResponseDto.fromJson(responseJson)

        assertEquals("success", responseDto.status)
        assertEquals(99, responseDto.obstructionId)
        assertTrue(responseDto.isConfirmedBlocked)
    }

    @Test
    fun `OccupancyReportRequestDto dan StatusResponseDto`() {
        val request = OccupancyReportRequestDto(
            evacuationPointExternalId = "TES_01",
            level = "MODERATE",
        )
        val json = request.toJson()

        assertEquals("TES_01", json.getString("evacuation_point_external_id"))
        assertEquals("MODERATE", json.getString("level"))

        val statusJson = JSONObject(
            """
            {
                "evacuation_point_external_id": "TES_01",
                "level": "MODERATE",
                "report_count": 5,
                "updated_at": "2026-09-15T08:45:00Z",
                "source": "Laporan pengguna yang sudah check-in"
            }
            """.trimIndent(),
        )
        val statusDto = OccupancyStatusResponseDto.fromJson(statusJson)

        assertEquals("TES_01", statusDto.evacuationPointExternalId)
        assertEquals("MODERATE", statusDto.level)
        assertEquals(5, statusDto.reportCount)
    }
}
