package com.akusukaproject.siagapadang

import android.app.Application
import com.akusukaproject.siagapadang.data.local.SiagaPadangDatabase
import com.akusukaproject.siagapadang.data.repository.EvacuationRepository
import com.akusukaproject.siagapadang.sensor.CompassProvider
import com.akusukaproject.siagapadang.sensor.LocationProvider
import org.maplibre.android.MapLibre

class SiagaPadangApplication : Application() {
    val database by lazy { SiagaPadangDatabase.getInstance(this) }
    val evacuationRepository by lazy { EvacuationRepository(database.evacuationDao()) }
    val locationProvider by lazy { LocationProvider(this) }
    val compassProvider by lazy { CompassProvider(this) }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
