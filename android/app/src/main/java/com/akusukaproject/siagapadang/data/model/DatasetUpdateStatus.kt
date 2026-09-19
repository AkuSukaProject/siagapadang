package com.akusukaproject.siagapadang.data.model

data class LocalDatasetManifest(
    val version: String,
    val checksum: String,
    val sizeBytes: Long,
)

data class RemoteDatasetVersion(
    val id: Int? = null,
    val datasetName: String,
    val version: String,
    val schemaVersion: String,
    val checksum: String,
    val downloadUrl: String,
    val sizeBytes: Long?,
    val minimumAppVersion: String?,
    val publishedAt: String,
)

data class DatasetUpdateStatus(
    val local: LocalDatasetManifest,
    val latestVersions: List<RemoteDatasetVersion>,
    val checkedAtMillis: Long,
    val serverReportsUpdate: Boolean = true,
) {
    val networkVersion: RemoteDatasetVersion?
        get() = latestVersions.firstOrNull { it.datasetName == "network" }

    val updateAvailable: Boolean
        get() = serverReportsUpdate &&
            networkVersion?.checksum?.equals(local.checksum, ignoreCase = true) == false

    /**
     * ID versi dataset di server yang isinya sama persis dengan dataset di perangkat (checksum
     * sama). Hanya ID ini yang boleh dipakai saat melaporkan ruas jalan, karena ID ruas dari
     * dataset yang berbeda dapat menunjuk jalan lain. Bernilai null bila server tidak memiliki
     * dataset yang sama dengan perangkat.
     */
    val serverNetworkVersionIdForLocal: Int?
        get() = latestVersions.firstOrNull { version ->
            version.datasetName == "network" &&
                version.checksum.equals(local.checksum, ignoreCase = true)
        }?.id

    val downloadableVersion: RemoteDatasetVersion?
        get() = networkVersion?.takeIf {
            updateAvailable && it.downloadUrl.isNotBlank() && it.sizeBytes != null
        }

    val latestVersionLabel: String
        get() = latestVersions
            .map(RemoteDatasetVersion::version)
            .distinct()
            .joinToString(", ")
            .ifBlank { local.version }
}
