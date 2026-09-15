package com.akusukaproject.siagapadang.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnonymousDeviceIdProviderTest {

    private class InMemoryDeviceIdStorage : DeviceIdStorage {
        private var storedId: String? = null

        override fun getDeviceId(): String? = storedId

        override fun saveDeviceId(deviceId: String) {
            storedId = deviceId
        }
    }

    @Test
    fun `menghasilkan UUID baru jika belum ada ID tersimpan`() {
        val storage = InMemoryDeviceIdStorage()
        val provider = AnonymousDeviceIdProvider(storage)

        val deviceId = provider.getDeviceId()

        assertNotNull(deviceId)
        assertTrue(Regex("^[0-9a-fA-F-]{36}$").matches(deviceId))
        assertEquals(deviceId, storage.getDeviceId())
    }

    @Test
    fun `menggunakan ID yang sudah tersimpan pada pemanggilan berikutnya`() {
        val storage = InMemoryDeviceIdStorage()
        storage.saveDeviceId("existing-uuid-12345")

        val provider = AnonymousDeviceIdProvider(storage)

        val deviceId1 = provider.getDeviceId()
        val deviceId2 = provider.getDeviceId()

        assertEquals("existing-uuid-12345", deviceId1)
        assertEquals("existing-uuid-12345", deviceId2)
    }
}
