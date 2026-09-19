package com.akusukaproject.siagapadang.ui.familyplan

import com.akusukaproject.siagapadang.data.model.FamilyPlan

/**
 * Menyusun rencana keluarga menjadi teks biasa untuk dibagikan lewat SMS atau aplikasi pesan.
 * Teks tidak memuat koordinat maupun posisi siapa pun.
 */
object FamilyPlanShareText {
    fun build(plan: FamilyPlan): String = buildString {
        appendLine("RENCANA EVAKUASI TSUNAMI KELUARGA")
        appendLine()
        appendLine("Titik temu keluarga: ${plan.meetingPointName ?: "belum dipilih"}")
        if (plan.members.isNotEmpty()) {
            appendLine()
            appendLine("Tujuan tiap anggota:")
            plan.members.forEach { member ->
                val location = member.routineLocation.ifBlank { "lokasi belum diisi" }
                val destination = member.destinationName ?: "TES belum dipilih"
                appendLine("- ${member.name} ($location) -> $destination")
            }
        }
        appendLine()
        appendLine("Saat gempa kuat terasa: jangan kembali untuk menjemput.")
        appendLine(
            "Setiap orang berjalan cepat ke TES masing-masing, " +
                "lalu bertemu di titik temu setelah keadaan dinyatakan aman oleh petugas.",
        )
        append("Ikuti arahan petugas BPBD dan informasi resmi BMKG.")
    }
}
