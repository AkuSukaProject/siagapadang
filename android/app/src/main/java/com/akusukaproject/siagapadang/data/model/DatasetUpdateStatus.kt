package com.akusukaproject.siagapadang.data.model

data class LocalDatasetManifest(
    val version: String,
    val checksum: String,
    val sizeBytes: Long,
)

data class RemoteDatasetVersion(
    val datasetName: String,
    val version: String,
    val checksum: String,
    val sizeBytes: Long?,
    val publishedAt: String,
)

data class DatasetUpdateStatus(
    val local: LocalDatasetManifest,
    val latestVersions: List<RemoteDatasetVersion>,
    val checkedAtMillis: Long,
) {
    val updateAvailable: Boolean
        get() = latestVersions.any { remote ->
            !remote.checksum.equals(local.checksum, ignoreCase = true)
        }

    val latestVersionLabel: String
        get() = latestVersions
            .map(RemoteDatasetVersion::version)
            .distinct()
            .joinToString(", ")
            .ifBlank { local.version }
}
