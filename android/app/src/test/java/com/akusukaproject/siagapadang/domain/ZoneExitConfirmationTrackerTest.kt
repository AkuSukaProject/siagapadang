package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.InundationZoneStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneExitConfirmationTrackerTest {
    private val inside = InundationZoneStatus.InsideRecordedZone(
        zoneName = "Zona rendaman",
        dangerLevel = "Tinggi",
    )

    @Test
    fun `mulai aplikasi di luar zona tidak dianggap sebagai transisi keluar`() {
        val tracker = ZoneExitConfirmationTracker()

        repeat(5) {
            assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
        }
    }

    @Test
    fun `tiga pembacaan akurat setelah berada di dalam mengonfirmasi keluar zona`() {
        val tracker = ZoneExitConfirmationTracker()

        assertFalse(tracker.update(inside, 7f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 9f))
        assertTrue(tracker.update(InundationZoneStatus.OutsideRecordedZone, 10f))
    }

    @Test
    fun `pembacaan batas yang masih diklasifikasikan di dalam tidak menambah konfirmasi`() {
        val tracker = ZoneExitConfirmationTracker()

        assertFalse(tracker.update(inside, 7f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
        assertFalse(tracker.update(inside, 8f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
        assertTrue(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
    }

    @Test
    fun `akurasi GPS buruk memutus rangkaian konfirmasi`() {
        val tracker = ZoneExitConfirmationTracker()

        assertFalse(tracker.update(inside, 7f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 60f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
        assertFalse(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
        assertTrue(tracker.update(InundationZoneStatus.OutsideRecordedZone, 8f))
    }
}
