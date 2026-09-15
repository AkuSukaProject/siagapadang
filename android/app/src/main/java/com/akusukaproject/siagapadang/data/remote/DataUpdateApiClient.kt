package com.akusukaproject.siagapadang.data.remote

import android.content.Context
import com.akusukaproject.siagapadang.data.model.DatasetUpdateStatus
import com.akusukaproject.siagapadang.data.model.LocalDatasetManifest
import com.akusukaproject.siagapadang.data.model.RemoteDatasetVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DataUpdateApiClient(
    context: Context,
    private val baseUrl: String,
) {
    private val appContext = context.applicationContext

    suspend fun loadLocalManifest(): LocalDatasetManifest = withContext(Dispatchers.IO) {
        readLocalManifest()
    }

    suspend fun checkForUpdates(): DatasetUpdateStatus = withContext(Dispatchers.IO) {
        val local = readLocalManifest()
        check(baseUrl.isNotBlank()) { "Alamat backend belum dikonfigurasi." }
        val connection = URL("${baseUrl.trimEnd('/')}/api/v1/sync/check")
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
                                checksum = item.optString("checksum"),
                                sizeBytes = item.optLong("size_bytes").takeIf { !item.isNull("size_bytes") },
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
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun readLocalManifest(): LocalDatasetManifest {
        val json = appContext.assets.open(MANIFEST_ASSET).bufferedReader().use {
            JSONObject(it.readText())
        }
        return LocalDatasetManifest(
            version = json.getString("version"),
            checksum = json.getString("checksum"),
            sizeBytes = json.getLong("size_bytes"),
        )
    }

    private companion object {
        const val MANIFEST_ASSET = "dataset_manifest.json"
        const val CONNECT_TIMEOUT_MILLIS = 1_000
        const val READ_TIMEOUT_MILLIS = 2_000
    }
}
