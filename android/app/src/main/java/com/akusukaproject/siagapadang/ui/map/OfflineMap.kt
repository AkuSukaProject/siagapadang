package com.akusukaproject.siagapadang.ui.map

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val OFFLINE_BASE_STYLE = """
    {
      "version": 8,
      "name": "Siaga Padang Offline",
      "sources": {},
      "layers": [
        {
          "id": "background",
          "type": "background",
          "paint": { "background-color": "#071A20" }
        }
      ]
    }
"""

// MapLibre masih memakai callback tekanan memori Android yang didepresiasi.
@Suppress("DEPRECATION")
@Composable
fun OfflineMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(OFFLINE_BASE_STYLE))
            }
        }
    }
    val lifecycleController = remember(mapView) { MapViewLifecycleController(mapView) }

    DisposableEffect(lifecycle, lifecycleController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> lifecycleController.start()
                Lifecycle.Event.ON_RESUME -> lifecycleController.resume()
                Lifecycle.Event.ON_PAUSE -> lifecycleController.pause()
                Lifecycle.Event.ON_STOP -> lifecycleController.stop()
                Lifecycle.Event.ON_DESTROY -> lifecycleController.destroy()
                else -> Unit
            }
        }
        val applicationContext = context.applicationContext
        val memoryCallbacks = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                    mapView.onLowMemory()
                }
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onLowMemory() {
                mapView.onLowMemory()
            }

            override fun onConfigurationChanged(newConfig: Configuration) = Unit
        }

        lifecycle.addObserver(observer)
        applicationContext.registerComponentCallbacks(memoryCallbacks)
        lifecycleController.synchronizeWith(lifecycle.currentState)
        onDispose {
            lifecycle.removeObserver(observer)
            applicationContext.unregisterComponentCallbacks(memoryCallbacks)
            lifecycleController.destroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private class MapViewLifecycleController(
    private val mapView: MapView,
) {
    private var started = false
    private var resumed = false
    private var destroyed = false

    fun synchronizeWith(state: Lifecycle.State) {
        when {
            state == Lifecycle.State.DESTROYED -> destroy()
            state.isAtLeast(Lifecycle.State.RESUMED) -> resume()
            state.isAtLeast(Lifecycle.State.STARTED) -> start()
        }
    }

    fun start() {
        if (!destroyed && !started) {
            mapView.onStart()
            started = true
        }
    }

    fun resume() {
        if (!destroyed && !resumed) {
            start()
            mapView.onResume()
            resumed = true
        }
    }

    fun pause() {
        if (!destroyed && resumed) {
            mapView.onPause()
            resumed = false
        }
    }

    fun stop() {
        if (!destroyed && started) {
            pause()
            mapView.onStop()
            started = false
        }
    }

    fun destroy() {
        if (!destroyed) {
            stop()
            mapView.onDestroy()
            destroyed = true
        }
    }
}
