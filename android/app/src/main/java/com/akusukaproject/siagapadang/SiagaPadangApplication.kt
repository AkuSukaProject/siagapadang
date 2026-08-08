package com.akusukaproject.siagapadang

import android.app.Application
import org.maplibre.android.MapLibre

class SiagaPadangApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}

