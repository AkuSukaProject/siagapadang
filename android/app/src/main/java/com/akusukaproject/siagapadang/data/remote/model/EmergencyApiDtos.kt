package com.akusukaproject.siagapadang.data.remote.model

import org.json.JSONObject

data class ActiveEmergencyEventDto(
    val active: Boolean,
    val eventExternalId: String? = null,
    val source: String? = null,
    val startedAt: String? = null,
) {
    companion object {
        fun fromJson(json: JSONObject): ActiveEmergencyEventDto = ActiveEmergencyEventDto(
            active = json.optBoolean("active", false),
            eventExternalId = json.optString("event_external_id").takeIf { it.isNotBlank() },
            source = json.optString("source").takeIf { it.isNotBlank() },
            startedAt = json.optString("started_at").takeIf { it.isNotBlank() },
        )
    }
}

data class ShelterCheckinRequestDto(
    val evacuationPointExternalId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val eventExternalId: String? = null,
    val status: String = "Selamat",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("evacuation_point_external_id", evacuationPointExternalId)
        put("latitude", latitude)
        put("longitude", longitude)
        put("accuracy_m", accuracyMeters)
        if (!eventExternalId.isNullOrBlank()) {
            put("event_external_id", eventExternalId)
        }
        put("status", status)
    }
}

data class ShelterCheckinResponseDto(
    val status: String,
    val message: String,
    val eventExternalId: String,
    val evacuationPointExternalId: String,
    val checkedInAt: String,
) {
    companion object {
        fun fromJson(json: JSONObject): ShelterCheckinResponseDto = ShelterCheckinResponseDto(
            status = json.optString("status"),
            message = json.optString("message"),
            eventExternalId = json.optString("event_external_id"),
            evacuationPointExternalId = json.optString("evacuation_point_external_id"),
            checkedInAt = json.optString("checked_in_at"),
        )
    }
}

data class ObstructionReportRequestDto(
    val latitude: Double,
    val longitude: Double,
    val datasetVersionId: Int,
    val edgeExternalId: String,
    val description: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("latitude", latitude)
        put("longitude", longitude)
        put("dataset_version_id", datasetVersionId)
        put("edge_external_id", edgeExternalId)
        if (!description.isNullOrBlank()) {
            put("description", description)
        }
    }
}

data class ObstructionReportResponseDto(
    val status: String,
    val message: String,
    val obstructionId: Int,
    val isConfirmedBlocked: Boolean,
) {
    companion object {
        fun fromJson(json: JSONObject): ObstructionReportResponseDto = ObstructionReportResponseDto(
            status = json.optString("status"),
            message = json.optString("message"),
            obstructionId = json.optInt("obstruction_id"),
            isConfirmedBlocked = json.optBoolean("is_confirmed_blocked", false),
        )
    }
}

data class OccupancyReportRequestDto(
    val evacuationPointExternalId: String,
    val level: String, // "LOW", "MODERATE", "FULL"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("evacuation_point_external_id", evacuationPointExternalId)
        put("level", level)
    }
}

data class OccupancyStatusResponseDto(
    val evacuationPointExternalId: String,
    val level: String,
    val reportCount: Int,
    val updatedAt: String? = null,
    val source: String = "Laporan pengguna yang sudah check-in",
) {
    companion object {
        fun fromJson(json: JSONObject): OccupancyStatusResponseDto = OccupancyStatusResponseDto(
            evacuationPointExternalId = json.optString("evacuation_point_external_id"),
            level = json.optString("level"),
            reportCount = json.optInt("report_count"),
            updatedAt = json.optString("updated_at").takeIf { it.isNotBlank() },
            source = json.optString("source", "Laporan pengguna yang sudah check-in"),
        )
    }
}
