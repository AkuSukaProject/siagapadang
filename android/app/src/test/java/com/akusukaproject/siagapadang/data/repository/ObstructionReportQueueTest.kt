package com.akusukaproject.siagapadang.data.repository

import com.akusukaproject.siagapadang.data.remote.model.ObstructionReportRequestDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObstructionReportQueueTest {

    private class MemoryQueueStorage : ObstructionQueueStorage {
        var json: String? = null
        override fun loadReportsJson(): String? = json
        override fun saveReportsJson(json: String) {
            this.json = json
        }
    }

    @Test
    fun `antrean awal kosong`() {
        val storage = MemoryQueueStorage()
        val queue = ObstructionReportQueue(storage)
        assertTrue(queue.getPendingReports().isEmpty())
    }

    @Test
    fun `enqueue menyimpan dan memuat ulang laporan dengan benar`() {
        val storage = MemoryQueueStorage()
        val queue = ObstructionReportQueue(storage)

        val report = ObstructionReportRequestDto(
            latitude = -0.95,
            longitude = 100.35,
            datasetVersionId = 1,
            edgeExternalId = "1001",
            description = "Pohon tumbang",
        )
        queue.enqueue(report)

        val pending = queue.getPendingReports()
        assertEquals(1, pending.size)
        assertEquals("1001", pending[0].edgeExternalId)
        assertEquals(-0.95, pending[0].latitude, 0.0001)
        assertEquals(100.35, pending[0].longitude, 0.0001)
        assertEquals(1, pending[0].datasetVersionId)
        assertEquals("Pohon tumbang", pending[0].description)
    }

    @Test
    fun `enqueue mencegah duplikasi ruas yang sama`() {
        val storage = MemoryQueueStorage()
        val queue = ObstructionReportQueue(storage)

        val report1 = ObstructionReportRequestDto(
            latitude = -0.95,
            longitude = 100.35,
            datasetVersionId = 1,
            edgeExternalId = "1001",
        )
        val report2 = ObstructionReportRequestDto(
            latitude = -0.951,
            longitude = 100.351,
            datasetVersionId = 1,
            edgeExternalId = "1001",
        )
        queue.enqueue(report1)
        queue.enqueue(report2)

        val pending = queue.getPendingReports()
        assertEquals(1, pending.size)
        assertEquals("1001", pending[0].edgeExternalId)
    }

    @Test
    fun `remove menghapus laporan berdasarkan edgeExternalId`() {
        val storage = MemoryQueueStorage()
        val queue = ObstructionReportQueue(storage)

        queue.enqueue(
            ObstructionReportRequestDto(
                latitude = -0.95,
                longitude = 100.35,
                datasetVersionId = 1,
                edgeExternalId = "1001",
            ),
        )
        queue.enqueue(
            ObstructionReportRequestDto(
                latitude = -0.96,
                longitude = 100.36,
                datasetVersionId = 1,
                edgeExternalId = "1002",
            ),
        )

        assertEquals(2, queue.getPendingReports().size)

        queue.remove("1001")
        val remaining = queue.getPendingReports()
        assertEquals(1, remaining.size)
        assertEquals("1002", remaining[0].edgeExternalId)
    }

    @Test
    fun `clear mengosongkan seluruh antrean`() {
        val storage = MemoryQueueStorage()
        val queue = ObstructionReportQueue(storage)

        queue.enqueue(
            ObstructionReportRequestDto(
                latitude = -0.95,
                longitude = 100.35,
                datasetVersionId = 1,
                edgeExternalId = "1001",
            ),
        )
        queue.clear()
        assertTrue(queue.getPendingReports().isEmpty())
    }

    @Test
    fun `data json korup ditangani dengan aman tanpa crash`() {
        val storage = MemoryQueueStorage()
        storage.json = "{ bukan-json-array }"
        val queue = ObstructionReportQueue(storage)
        assertTrue(queue.getPendingReports().isEmpty())
    }
}
