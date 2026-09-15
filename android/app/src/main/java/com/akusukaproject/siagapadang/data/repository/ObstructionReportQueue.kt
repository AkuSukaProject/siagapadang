package com.akusukaproject.siagapadang.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.akusukaproject.siagapadang.data.remote.model.ObstructionReportRequestDto
import org.json.JSONArray

/**
 * Abstraksi media penyimpanan antrean laporan jalan terhalang untuk memudahkan pengujian unit.
 */
interface ObstructionQueueStorage {
    fun loadReportsJson(): String?
    fun saveReportsJson(json: String)
}

class SharedPreferencesObstructionQueueStorage(context: Context) : ObstructionQueueStorage {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadReportsJson(): String? = prefs.getString(KEY_REPORTS, null)

    override fun saveReportsJson(json: String) {
        prefs.edit().putString(KEY_REPORTS, json).apply()
    }

    companion object {
        const val PREFS_NAME = "siaga_padang_obstruction_queue"
        const val KEY_REPORTS = "pending_obstruction_reports"
    }
}

/**
 * Antrean lokal laporan hambatan jalan. Menyimpan laporan secara persisten di perangkat
 * saat koneksi internet mati atau lambat, dan mengirimkannya saat online.
 */
class ObstructionReportQueue(
    private val storage: ObstructionQueueStorage,
) {
    constructor(context: Context) : this(SharedPreferencesObstructionQueueStorage(context))

    @Synchronized
    fun enqueue(report: ObstructionReportRequestDto) {
        val currentList = getPendingReports().toMutableList()
        if (currentList.none { it.edgeExternalId == report.edgeExternalId }) {
            currentList.add(report)
            persist(currentList)
        }
    }

    @Synchronized
    fun getPendingReports(): List<ObstructionReportRequestDto> {
        val jsonStr = storage.loadReportsJson() ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ObstructionReportRequestDto>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ObstructionReportRequestDto(
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        datasetVersionId = obj.getInt("dataset_version_id"),
                        edgeExternalId = obj.getString("edge_external_id"),
                        description = obj.optString("description").takeIf { it.isNotBlank() },
                    ),
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun remove(edgeExternalId: String) {
        val currentList = getPendingReports().filterNot { it.edgeExternalId == edgeExternalId }
        persist(currentList)
    }

    @Synchronized
    fun clear() {
        storage.saveReportsJson("[]")
    }

    private fun persist(reports: List<ObstructionReportRequestDto>) {
        val array = JSONArray()
        for (report in reports) {
            array.put(report.toJson())
        }
        storage.saveReportsJson(array.toString())
    }
}
