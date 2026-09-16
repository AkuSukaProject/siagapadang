package com.akusukaproject.siagapadang.data.remote

import com.akusukaproject.siagapadang.data.local.DatasetStorage
import com.akusukaproject.siagapadang.data.model.DatasetUpdateStatus
import com.akusukaproject.siagapadang.data.model.LocalDatasetManifest
import com.akusukaproject.siagapadang.data.model.RemoteDatasetVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DataUpdateApiClient(
    private val baseUrl: String,
    private val storage: DatasetStorage,
) {
    suspend fun loadLocalManifest(): LocalDatasetManifest = withContext(Dispatchers.IO) {
        storage.localManifest()
    }

    suspend fun checkForUpdates(): DatasetUpdateStatus = withContext(Dispatchers.IO) {
        val local = storage.localManifest()
        check(baseUrl.isNotBlank()) { "Alamat backend belum dikonfigurasi." }
        val query = "dataset_name=network" +
            "&current_version=${encode(local.version)}" +
            "&current_checksum=${encode(local.checksum)}"
        val connection = URL("${baseUrl.trimEnd('/')}/api/v1/sync/check?$query")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS

            val responseCode = connection.responseCode
            val responseBody = (
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
                )?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                val detail = runCatching { JSONObject(responseBody).optString("detail") }.getOrNull()
                error(detail?.takeIf { it.isNotBlank() } ?: "Backend tidak tersedia ($responseCode).")
            }

            val json = JSONObject(responseBody)
            val versionsJson = json.optJSONArray("latest_versions")
            val versions = buildList {
                if (versionsJson != null) {
                    repeat(versionsJson.length()) { index ->
                        val item = versionsJson.getJSONObject(index)
                        add(
                            RemoteDatasetVersion(
                                datasetName = item.optString("dataset_name"),
                                version = item.optString("version"),
                                schemaVersion = item.optString("schema_version"),
                                checksum = item.optString("checksum"),
                                downloadUrl = item.optString("download_url"),
                                sizeBytes = item.optLong("size_bytes").takeIf { !item.isNull("size_bytes") },
                                minimumAppVersion = item.optString("minimum_app_version")
                                    .takeIf { !item.isNull("minimum_app_version") && it.isNotBlank() },
                                publishedAt = item.optString("published_at"),
                            ),
                        )
                    }
                }
            }
            DatasetUpdateStatus(
                local = local,
                latestVersions = versions,
                checkedAtMillis = System.currentTimeMillis(),
                serverReportsUpdate = json.optBoolean("has_update", false),
            )
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 1_000
        const val READ_TIMEOUT_MILLIS = 2_000

        fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}
