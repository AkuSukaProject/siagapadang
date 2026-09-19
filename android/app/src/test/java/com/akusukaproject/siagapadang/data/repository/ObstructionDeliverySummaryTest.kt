package com.akusukaproject.siagapadang.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObstructionDeliverySummaryTest {
    @Test
    fun `dataset mismatch is stated instead of promising a later send`() {
        val message = ObstructionDeliverySummary(isDatasetMismatch = true).message()!!

        assertTrue(message.contains("berbeda versi"))
        assertTrue(!message.contains("dikirim otomatis"))
    }

    @Test
    fun `offline keeps report and says it will be sent when connected`() {
        val message = ObstructionDeliverySummary(offlineCount = 1, remainingCount = 1).message()!!

        assertTrue(message.startsWith("Belum terhubung"))
        assertTrue(message.contains("(1)"))
    }

    @Test
    fun `server error is not described as missing signal`() {
        val message = ObstructionDeliverySummary(serverErrorCount = 1, remainingCount = 1).message()!!

        assertTrue(message.startsWith("Server posko sedang bermasalah"))
    }

    @Test
    fun `rejection carries the server reason`() {
        val message = ObstructionDeliverySummary(
            rejectedCount = 1,
            rejectionReason = "Lokasi laporan terlalu jauh (>50m) dari ruas jalan yang dilaporkan.",
        ).message()

        assertEquals(
            "Laporan ditolak server posko: Lokasi laporan terlalu jauh (>50m) dari ruas jalan yang dilaporkan.",
            message,
        )
    }

    @Test
    fun `confirmation outranks plain success`() {
        val message = ObstructionDeliverySummary(sentCount = 2, confirmedCount = 1).message()

        assertEquals("Ruas jalan kini ditandai terhalang untuk semua pengguna.", message)
    }

    @Test
    fun `nothing happened gives no message`() {
        assertNull(ObstructionDeliverySummary().message())
    }
}
