package com.akusukaproject.siagapadang.data.local

import com.akusukaproject.siagapadang.data.model.RemoteDatasetVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatasetPackageInstallerTest {
    @Test
    fun `minimum app version comparison supports semantic numeric parts`() {
        assertTrue(DatasetPackageInstaller.isVersionAtLeast("0.2.0", "0.1.9"))
        assertTrue(DatasetPackageInstaller.isVersionAtLeast("0.1.0", "0.1.0"))
        assertFalse(DatasetPackageInstaller.isVersionAtLeast("0.1.0", "0.1.1"))
    }

    @Test
    fun `database filename uses normalized checksum prefix`() {
        val remote = RemoteDatasetVersion(
            datasetName = "network",
            version = "2026.09.16",
            schemaVersion = "android-v1",
            checksum = "ABCDEF0123456789FFFF",
            downloadUrl = "/api/v1/sync/network",
            sizeBytes = 100L,
            minimumAppVersion = "0.1.0",
            publishedAt = "2026-09-16T00:00:00Z",
        )

        assertEquals("ranah_siaga_abcdef0123456789.db", DatasetPackageInstaller.databaseNameFor(remote))
    }
}
