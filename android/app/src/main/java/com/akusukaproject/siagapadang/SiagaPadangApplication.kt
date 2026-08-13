package com.akusukaproject.siagapadang

import android.app.Application
import android.util.Log
import com.akusukaproject.siagapadang.data.local.SiagaPadangDatabase
import com.akusukaproject.siagapadang.data.repository.EvacuationRepository
import com.akusukaproject.siagapadang.data.repository.ZoneRepository
import com.akusukaproject.siagapadang.sensor.CompassProvider
import com.akusukaproject.siagapadang.sensor.LocationProvider
import com.akusukaproject.siagapadang.sensor.NetworkStatusProvider
import org.maplibre.android.MapLibre
import org.maplibre.android.offline.OfflineManager

class SiagaPadangApplication : Application() {
    val database by lazy { SiagaPadangDatabase.getInstance(this) }
    val evacuationRepository by lazy { EvacuationRepository(database.evacuationDao()) }
    val zoneRepository by lazy { ZoneRepository(database.zoneDao()) }
    val locationProvider by lazy { LocationProvider(this) }
    val compassProvider by lazy { CompassProvider(this) }
    val networkStatusProvider by lazy { NetworkStatusProvider(this) }

    override fun onCreate() {
        super.onCreate()
        removeLegacyDatabaseCopy()
        MapLibre.getInstance(this)
        limitMapLibreAmbientCache()
    }

    private fun removeLegacyDatabaseCopy() {
        val legacyDatabase = getDatabasePath(LEGACY_DATABASE_NAME)
        if (legacyDatabase.exists() && !deleteDatabase(LEGACY_DATABASE_NAME)) {
            Log.w(LOG_TAG, "Salinan database lama gagal dihapus")
        }
    }

    private fun limitMapLibreAmbientCache() {
        OfflineManager.getInstance(this).setMaximumAmbientCacheSize(
            MAX_AMBIENT_CACHE_BYTES,
            object : OfflineManager.FileSourceCallback {
                override fun onSuccess() = Unit

                override fun onError(message: String) {
                    Log.w(LOG_TAG, "Batas cache MapLibre gagal diterapkan: $message")
                }
            },
        )
    }

    private companion object {
        const val LOG_TAG = "SiagaPadangApplication"
        const val LEGACY_DATABASE_NAME = "ranah_siaga.db"
        const val MAX_AMBIENT_CACHE_BYTES = 16L * 1024L * 1024L
    }
}
