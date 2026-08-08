package com.akusukaproject.siagapadang.ui.map

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val DEVELOPMENT_MAP_STYLE = """
    {
      "version": 8,
      "name": "Siaga Padang",
      "sources": {
        "openstreetmap": {
          "type": "raster",
          "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
          "tileSize": 256,
          "maxzoom": 19,
          "attribution": "© OpenStreetMap contributors"
        }
      },
      "layers": [
        {
          "id": "background",
          "type": "background",
          "paint": { "background-color": "#071A20" }
        },
        {
          "id": "openstreetmap",
          "type": "raster",
          "source": "openstreetmap",
          "paint": {
            "raster-opacity": 0.72,
            "raster-saturation": -0.55,
            "raster-brightness-max": 0.68
          }
        }
      ]
    }
"""

@Suppress("DEPRECATION")
@Composable
fun OfflineMap(
    routeCoordinates: List<GeoCoordinate>,
    currentLocation: GeoCoordinate?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestRoute = rememberUpdatedState(routeCoordinates)
    val latestLocation = rememberUpdatedState(currentLocation)
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(DEFAULT_PADANG_CENTER)
                    .zoom(DEFAULT_ZOOM)
                    .build()
                map.uiSettings.isCompassEnabled = false
                map.setStyle(Style.Builder().fromJson(DEVELOPMENT_MAP_STYLE)) { style ->
                    updateMapOverlays(
                        style = style,
                        routeCoordinates = latestRoute.value,
                        currentLocation = latestLocation.value,
                    )
                }
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
        update = { view ->
            view.getMapAsync { map ->
                map.style?.let { style ->
                    updateMapOverlays(style, routeCoordinates, currentLocation)
                }
                if (!hasCenteredOnUser && currentLocation != null) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(currentLocation.latitude, currentLocation.longitude),
                            USER_LOCATION_ZOOM,
                        ),
                        CAMERA_ANIMATION_MILLIS,
                    )
                    hasCenteredOnUser = true
                }
            }
        },
        modifier = modifier,
    )
}

private fun updateMapOverlays(
    style: Style,
    routeCoordinates: List<GeoCoordinate>,
    currentLocation: GeoCoordinate?,
) {
    if (routeCoordinates.size >= 2) {
        val geometry = LineString.fromLngLats(
            routeCoordinates.map { coordinate ->
                Point.fromLngLat(coordinate.longitude, coordinate.latitude)
            },
        )
        val existingRouteSource = style.getSource(ROUTE_SOURCE_ID) as? GeoJsonSource
        if (existingRouteSource == null) {
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, geometry))
            style.addLayer(
                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    lineColor("#FFC857"),
                    lineWidth(7f),
                    lineOpacity(0.96f),
                    lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND),
                ),
            )
        } else {
            existingRouteSource.setGeoJson(geometry)
        }
    }

    currentLocation?.let { coordinate ->
        val feature = Feature.fromGeometry(
            Point.fromLngLat(coordinate.longitude, coordinate.latitude),
        )
        val existingLocationSource = style.getSource(LOCATION_SOURCE_ID) as? GeoJsonSource
        if (existingLocationSource == null) {
            style.addSource(GeoJsonSource(LOCATION_SOURCE_ID, feature))
            style.addLayer(
                CircleLayer(LOCATION_LAYER_ID, LOCATION_SOURCE_ID).withProperties(
                    circleColor("#3DDCFF"),
                    circleRadius(8f),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(3f),
                ),
            )
        } else {
            existingLocationSource.setGeoJson(feature)
        }
    }
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

private val DEFAULT_PADANG_CENTER = LatLng(-0.9471, 100.4172)
private const val DEFAULT_ZOOM = 12.5
private const val USER_LOCATION_ZOOM = 16.0
private const val CAMERA_ANIMATION_MILLIS = 700
private const val ROUTE_SOURCE_ID = "evacuation-route-source"
private const val ROUTE_LAYER_ID = "evacuation-route-layer"
private const val LOCATION_SOURCE_ID = "user-location-source"
private const val LOCATION_LAYER_ID = "user-location-layer"
