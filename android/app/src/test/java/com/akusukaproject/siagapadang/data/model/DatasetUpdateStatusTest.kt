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
                    checksum = "ABC123",
                    sizeBytes = local.sizeBytes,
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
                    checksum = "different",
                    sizeBytes = local.sizeBytes,
                    publishedAt = "2026-09-15T00:00:00Z",
                ),
            ),
            checkedAtMillis = 1L,
        )

        assertTrue(status.updateAvailable)
    }
}
