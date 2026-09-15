package com.akusukaproject.siagapadang.data.remote

import com.akusukaproject.siagapadang.data.remote.model.ShelterCheckinRequestDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyApiClientTest {

    private class StubDeviceIdStorage : DeviceIdStorage {
        override fun getDeviceId(): String = "test-device-uuid-999"
        override fun saveDeviceId(deviceId: String) = Unit
    }

    @Test
    fun `mengembalikan kegagalan yang aman jika baseUrl kosong`() = runBlocking {
        val deviceIdProvider = AnonymousDeviceIdProvider(StubDeviceIdStorage())
        val client = EmergencyApiClient(baseUrl = "", deviceIdProvider = deviceIdProvider)

        val eventResult = client.getActiveEvent()
        assertTrue(eventResult.isFailure)
        assertEquals("Alamat backend belum dikonfigurasi.", eventResult.exceptionOrNull()?.message)

        val checkinResult = client.checkIn(
            ShelterCheckinRequestDto(
                evacuationPointExternalId = "TES_01",
                latitude = -0.95,
                longitude = 100.35,
                accuracyMeters = 10f,
            ),
        )
        assertTrue(checkinResult.isFailure)
        assertEquals("Alamat backend belum dikonfigurasi.", checkinResult.exceptionOrNull()?.message)

        val obstructionResult = client.getConfirmedObstructions()
        assertTrue(obstructionResult.isFailure)
        assertEquals("Alamat backend belum dikonfigurasi.", obstructionResult.exceptionOrNull()?.message)
    }
}
