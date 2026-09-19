package com.akusukaproject.siagapadang.data.repository

/**
 * Ringkasan satu putaran pengiriman antrean laporan jalur terhalang. Pesan yang dihasilkan
 * menyebut penyebab sebenarnya: tidak terhubung, server bermasalah, laporan ditolak, atau
 * data jalan di perangkat berbeda versi dengan server.
 */
data class ObstructionDeliverySummary(
    val sentCount: Int = 0,
    val confirmedCount: Int = 0,
    val rejectedCount: Int = 0,
    val rejectionReason: String? = null,
    val serverErrorCount: Int = 0,
    val offlineCount: Int = 0,
    val remainingCount: Int = 0,
    val isDatasetMismatch: Boolean = false,
) {
    fun message(): String? = when {
        isDatasetMismatch ->
            "Laporan tidak dikirim: data jalan di HP berbeda versi dengan server posko. " +
                "Rute di HP tetap sudah berganti."
        confirmedCount > 0 -> "Ruas jalan kini ditandai terhalang untuk semua pengguna."
        rejectedCount > 0 ->
            "Laporan ditolak server posko" + (rejectionReason?.let { ": $it" } ?: ".")
        serverErrorCount > 0 ->
            "Server posko sedang bermasalah. Laporan disimpan di HP ($remainingCount) " +
                "dan dicoba lagi nanti."
        offlineCount > 0 ->
            "Belum terhubung ke server posko. Laporan disimpan di HP ($remainingCount) " +
                "dan dikirim otomatis saat terhubung."
        sentCount > 0 -> "Laporan jalan terhalang diterima posko bencana."
        else -> null
    }
}
