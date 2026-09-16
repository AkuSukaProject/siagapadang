package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.InundationZoneStatus

class ZoneExitConfirmationTracker(
    private val requiredConfirmations: Int = DEFAULT_REQUIRED_CONFIRMATIONS,
    private val maximumAccuracyMeters: Float = DEFAULT_MAXIMUM_ACCURACY_METERS,
) {
    private var observedInsideZone = false
    private var outsideConfirmationCount = 0
    private var exitConfirmed = false

    fun update(status: InundationZoneStatus, accuracyMeters: Float?): Boolean {
        if (exitConfirmed) return true
        if (accuracyMeters == null || accuracyMeters > maximumAccuracyMeters) {
            outsideConfirmationCount = 0
            return false
        }

        when (status) {
            InundationZoneStatus.DataUnavailable -> outsideConfirmationCount = 0
            is InundationZoneStatus.InsideRecordedZone -> {
                observedInsideZone = true
                outsideConfirmationCount = 0
            }
            InundationZoneStatus.OutsideRecordedZone -> {
                outsideConfirmationCount = if (observedInsideZone) {
                    outsideConfirmationCount + 1
                } else {
                    0
                }
            }
        }
        exitConfirmed = observedInsideZone &&
            outsideConfirmationCount >= requiredConfirmations
        return exitConfirmed
    }

    fun reset() {
        observedInsideZone = false
        outsideConfirmationCount = 0
        exitConfirmed = false
    }

    companion object {
        const val DEFAULT_REQUIRED_CONFIRMATIONS = 3
        const val DEFAULT_MAXIMUM_ACCURACY_METERS = 35f
    }
}
