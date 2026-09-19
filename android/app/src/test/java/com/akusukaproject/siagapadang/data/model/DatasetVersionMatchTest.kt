package com.akusukaproject.siagapadang.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatasetVersionMatchTest {
    private val local = LocalDatasetManifest(
        version = "2026.09.13",
        checksum = "c1e2d6",
        sizeBytes = 70_176_768,
    )

    private fun remote(id: Int?, name: String, checksum: String) = RemoteDatasetVersion(
        id = id,
        datasetName = name,
        version = "android-$checksum",
        schemaVersion = "android-v1",
        checksum = checksum,
        downloadUrl = "",
        sizeBytes = null,
        minimumAppVersion = null,
        publishedAt = "2026-09-16T00:00:00Z",
    )

    private fun statusOf(vararg versions: RemoteDatasetVersion) = DatasetUpdateStatus(
        local = local,
        latestVersions = versions.toList(),
        checkedAtMillis = 1L,
    )

    @Test
    fun `server network version with same checksum supplies its id`() {
        val status = statusOf(remote(19, "shelters", "C1E2D6"), remote(20, "network", "C1E2D6"))

        assertEquals(20, status.serverNetworkVersionIdForLocal)
    }

    @Test
    fun `different server dataset gives no id so reports are not sent`() {
        val status = statusOf(remote(20, "network", "6c12d8"))

        assertNull(status.serverNetworkVersionIdForLocal)
    }

    @Test
    fun `non network dataset with same checksum is ignored`() {
        val status = statusOf(remote(19, "shelters", "c1e2d6"))

        assertNull(status.serverNetworkVersionIdForLocal)
    }
}
