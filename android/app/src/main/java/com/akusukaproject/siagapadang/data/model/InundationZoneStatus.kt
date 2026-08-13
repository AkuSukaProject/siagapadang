package com.akusukaproject.siagapadang.data.model

sealed interface InundationZoneStatus {
    data object DataUnavailable : InundationZoneStatus

    data object OutsideRecordedZone : InundationZoneStatus

    data class InsideRecordedZone(
        val zoneName: String,
        val dangerLevel: String,
    ) : InundationZoneStatus
}
