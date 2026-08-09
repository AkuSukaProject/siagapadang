package com.akusukaproject.siagapadang.domain

class ArrivalConfirmationTracker(
    private val requiredConfirmations: Int = DEFAULT_REQUIRED_CONFIRMATIONS,
    private val arrivalRadiusMeters: Double = DEFAULT_ARRIVAL_RADIUS_METERS,
    private val maximumAccuracyMeters: Float = DEFAULT_MAXIMUM_ACCURACY_METERS,
) {
    private var confirmationCount = 0
    private var arrived = false

    fun update(distanceMeters: Double, accuracyMeters: Float?): Boolean {
        if (arrived) return true
        val hasReliableAccuracy = accuracyMeters == null || accuracyMeters <= maximumAccuracyMeters
        val isInsideArrivalRadius = distanceMeters <= arrivalRadiusMeters
        confirmationCount = if (hasReliableAccuracy && isInsideArrivalRadius) {
            confirmationCount + 1
        } else {
            0
        }
        arrived = confirmationCount >= requiredConfirmations
        return arrived
    }

    fun reset() {
        confirmationCount = 0
        arrived = false
    }

    private companion object {
        const val DEFAULT_REQUIRED_CONFIRMATIONS = 3
        const val DEFAULT_ARRIVAL_RADIUS_METERS = 20.0
        const val DEFAULT_MAXIMUM_ACCURACY_METERS = 35f
    }
}
