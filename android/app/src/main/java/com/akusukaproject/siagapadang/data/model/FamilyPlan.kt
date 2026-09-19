package com.akusukaproject.siagapadang.data.model

/**
 * Rencana titik temu keluarga yang disusun pada masa tenang dan disimpan hanya di perangkat.
 * Rencana ini tidak memuat dan tidak pernah menerima posisi anggota keluarga secara langsung.
 */
data class FamilyPlan(
    val meetingPointName: String? = null,
    val members: List<FamilyMember> = emptyList(),
    val updatedAtMillis: Long? = null,
) {
    val isEmpty: Boolean
        get() = meetingPointName == null && members.isEmpty()
}

data class FamilyMember(
    val id: String,
    val name: String,
    val routineLocation: String,
    val destinationName: String?,
)
