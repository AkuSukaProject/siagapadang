package com.akusukaproject.siagapadang.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalConfirmationTrackerTest {
    @Test
    fun `kedatangan dikunci setelah tiga pembacaan akurat di dalam radius`() {
        val tracker = ArrivalConfirmationTracker()

        assertFalse(tracker.update(distanceMeters = 15.0, accuracyMeters = 8f))
        assertFalse(tracker.update(distanceMeters = 12.0, accuracyMeters = 7f))
        assertTrue(tracker.update(distanceMeters = 10.0, accuracyMeters = 6f))
        assertTrue(tracker.update(distanceMeters = 50.0, accuracyMeters = 6f))
    }

    @Test
    fun `pembacaan di luar radius mengulang konfirmasi`() {
        val tracker = ArrivalConfirmationTracker()

        assertFalse(tracker.update(distanceMeters = 10.0, accuracyMeters = 5f))
        assertFalse(tracker.update(distanceMeters = 40.0, accuracyMeters = 5f))
        assertFalse(tracker.update(distanceMeters = 10.0, accuracyMeters = 5f))
        assertFalse(tracker.update(distanceMeters = 10.0, accuracyMeters = 5f))
        assertTrue(tracker.update(distanceMeters = 10.0, accuracyMeters = 5f))
    }

    @Test
    fun `akurasi GPS buruk tidak mengonfirmasi kedatangan`() {
        val tracker = ArrivalConfirmationTracker()

        repeat(4) {
            assertFalse(tracker.update(distanceMeters = 5.0, accuracyMeters = 80f))
        }
    }
}
