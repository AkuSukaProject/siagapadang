package com.akusukaproject.siagapadang.data.remote

import com.akusukaproject.siagapadang.data.remote.model.ActiveEmergencyEventDto
import com.akusukaproject.siagapadang.data.remote.model.ObstructionReportRequestDto
import com.akusukaproject.siagapadang.data.remote.model.ObstructionReportResponseDto
import com.akusukaproject.siagapadang.data.remote.model.OccupancyReportRequestDto
import com.akusukaproject.siagapadang.data.remote.model.OccupancyStatusResponseDto
import com.akusukaproject.siagapadang.data.remote.model.ShelterCheckinRequestDto
import com.akusukaproject.siagapadang.data.remote.model.ShelterCheckinResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Klien HTTP bersama untuk endpoint darurat backend SIAGA PADANG:
 * status kejadian darurat, check-in keselamatan, pelaporan jalur terhalang, dan okupansi shelter.
 */
class EmergencyApiClient(
    private val baseUrl: String,
    private val deviceIdProvider: AnonymousDeviceIdProvider,
) {
    /**
     * Memeriksa apakah ada status kejadian darurat (Emergency Event) yang sedang aktif di backend.
     */
    suspend fun getActiveEvent(): Result<ActiveEmergencyEventDto> = withContext(Dispatchers.IO) {
        runCatching {
            val responseBody = executeGet(
                endpoint = "${baseUrl.trimEnd('/')}/api/v1/status/emergency",
                requiresDeviceId = false,
            )
            ActiveEmergencyEventDto.fromJson(JSONObject(responseBody))
        }
    }

    /**
     * Mengirimkan check-in keselamatan warga saat tiba di Tempat Evakuasi Sementara (TES/TEA).
     */
    suspend fun checkIn(request: ShelterCheckinRequestDto): Result<ShelterCheckinResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val responseBody = executePost(
                    endpoint = "${baseUrl.trimEnd('/')}/api/v1/shelter/checkin",
                    jsonBody = request.toJson().toString(),
                )
                ShelterCheckinResponseDto.fromJson(JSONObject(responseBody))
            }
        }

    /**
     * Mengirimkan laporan ruas jalan yang terhalang/runtuh.
     */
    suspend fun reportObstruction(request: ObstructionReportRequestDto): Result<ObstructionReportResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val responseBody = executePost(
                    endpoint = "${baseUrl.trimEnd('/')}/api/v1/reports/obstruction",
                    jsonBody = request.toJson().toString(),
                )
                ObstructionReportResponseDto.fromJson(JSONObject(responseBody))
            }
        }

    /**
     * Mengambil daftar external ID ruas jalan yang terkonfirmasi terhalang untuk event darurat aktif.
     */
    suspend fun getConfirmedObstructions(datasetVersionId: Int? = null): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val queryParam = if (datasetVersionId != null) "?dataset_version_id=$datasetVersionId" else ""
                val responseBody = executeGet(
                    endpoint = "${baseUrl.trimEnd('/')}/api/v1/reports/confirmed$queryParam",
                    requiresDeviceId = false,
                )
                val jsonArray = JSONArray(responseBody)
                val result = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    result.add(jsonArray.getString(i))
                }
                result
            }
        }

    /**
     * Mengirimkan laporan tingkat okupansi Tempat Evakuasi Sementara (hanya setelah check-in).
     */
    suspend fun reportOccupancy(request: OccupancyReportRequestDto): Result<OccupancyStatusResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val responseBody = executePost(
                    endpoint = "${baseUrl.trimEnd('/')}/api/v1/shelter/occupancy",
                    jsonBody = request.toJson().toString(),
                )
                OccupancyStatusResponseDto.fromJson(JSONObject(responseBody))
            }
        }

    /**
     * Mengambil status agregat okupansi Tempat Evakuasi Sementara dari laporan warga.
     */
    suspend fun getOccupancyStatus(evacuationPointExternalId: String): Result<OccupancyStatusResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val responseBody = executeGet(
                    endpoint = "${baseUrl.trimEnd('/')}/api/v1/shelter/${evacuationPointExternalId.trim()}/occupancy",
                    requiresDeviceId = false,
                )
                OccupancyStatusResponseDto.fromJson(JSONObject(responseBody))
            }
        }

    private fun executeGet(endpoint: String, requiresDeviceId: Boolean): String {
        check(baseUrl.isNotBlank()) { "Alamat backend belum dikonfigurasi." }
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            if (requiresDeviceId) {
                connection.setRequestProperty("X-Device-ID", deviceIdProvider.getDeviceId())
            }
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS

            val responseCode = connection.responseCode
            val responseBody = (
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
            )?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val detail = runCatching { JSONObject(responseBody).optString("detail") }.getOrNull()
                throw ApiHttpException(
                    statusCode = responseCode,
                    message = detail?.takeIf { it.isNotBlank() } ?: "Backend mengembalikan status $responseCode",
                )
            }
            responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun executePost(endpoint: String, jsonBody: String): String {
        check(baseUrl.isNotBlank()) { "Alamat backend belum dikonfigurasi." }
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Device-ID", deviceIdProvider.getDeviceId())
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = (
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
            )?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (responseCode !in 200..299) {
                val detail = runCatching { JSONObject(responseBody).optString("detail") }.getOrNull()
                throw ApiHttpException(
                    statusCode = responseCode,
                    message = detail?.takeIf { it.isNotBlank() } ?: "Permintaan gagal diproses server ($responseCode)",
                )
            }
            responseBody
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 3_000
        private const val READ_TIMEOUT_MILLIS = 5_000
    }
}
