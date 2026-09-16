package com.akusukaproject.siagapadang.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatasetUpdateStatusTest {
    private val local = LocalDatasetManifest(
        version = "2026.09.13",
        checksum = "abc123",
        sizeBytes = 70_176_768,
    )

    @Test
    fun `same checksum means local data is current`() {
        val status = DatasetUpdateStatus(
            local = local,
            latestVersions = listOf(
                RemoteDatasetVersion(
                    datasetName = "network",
                    version = "2026.09.13",
                    schemaVersion = "android-v1",
                    checksum = "ABC123",
                    downloadUrl = "/api/v1/sync/network",
                    sizeBytes = local.sizeBytes,
                    minimumAppVersion = "0.1.0",
                    publishedAt = "2026-09-13T00:00:00Z",
                ),
            ),
            checkedAtMillis = 1L,
        )

        assertFalse(status.updateAvailable)
    }

    @Test
    fun `different checksum marks an update as available`() {
        val status = DatasetUpdateStatus(
            local = local,
            latestVersions = listOf(
                RemoteDatasetVersion(
                    datasetName = "network",
                    version = "2026.09.15",
                    schemaVersion = "android-v1",
                    checksum = "different",
                    downloadUrl = "/api/v1/sync/network",
                    sizeBytes = local.sizeBytes,
                    minimumAppVersion = "0.1.0",
                    publishedAt = "2026-09-15T00:00:00Z",
                ),
            ),
            checkedAtMillis = 1L,
        )

        assertTrue(status.updateAvailable)
        assertTrue(status.downloadableVersion != null)
    }

    @Test
    fun `server can prevent a different checksum from being offered as downgrade`() {
        val status = DatasetUpdateStatus(
            local = local,
            latestVersions = listOf(
                RemoteDatasetVersion(
                    datasetName = "network",
                    version = "2026.09.01",
                    schemaVersion = "android-v1",
                    checksum = "older-checksum",
                    downloadUrl = "/api/v1/sync/network",
                    sizeBytes = local.sizeBytes,
                    minimumAppVersion = "0.1.0",
                    publishedAt = "2026-09-01T00:00:00Z",
                ),
            ),
            checkedAtMillis = 1L,
            serverReportsUpdate = false,
        )

        assertFalse(status.updateAvailable)
        assertTrue(status.downloadableVersion == null)
    }
}
