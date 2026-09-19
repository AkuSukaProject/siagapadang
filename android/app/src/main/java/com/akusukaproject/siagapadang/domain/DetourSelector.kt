package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.EvacuationRoute

/**
 * Memilih jalan memutar ke TES yang sama lewat simpang tetangga (satu langkah).
 *
 * Setiap kandidat adalah rute prakomputasi milik simpang tetangga yang diawali satu ruas dari
 * simpang asal. Pemilihan hanya membandingkan data yang sudah ada; tidak ada pencarian graf.
 */
object DetourSelector {
    fun select(
        startNodeId: Long,
        destinationName: String,
        candidates: List<EvacuationRoute>,
        blockedEdgeIds: Set<Long>,
    ): EvacuationRoute? = candidates
        .asSequence()
        .filter { it.destinationName == destinationName }
        // Rute tetangga dihitung sebelum ada hambatan, jadi bisa saja kembali lewat ruas terhalang.
        .filter { candidate -> candidate.edgeIds.none { it in blockedEdgeIds } }
        // Tolak rute yang kembali melewati simpang asal (putar balik ke titik semula).
        .filter { candidate -> candidate.nodeIds.count { it == startNodeId } == 1 }
        .minByOrNull { it.estimatedSeconds }
}
