package com.akusukaproject.siagapadang.ui.map

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import com.akusukaproject.siagapadang.R
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
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
            "raster-opacity": 0.90,
            "raster-saturation": -0.35,
            "raster-brightness-max": 0.88
          }
        }
      ]
    }
"""

@Suppress("DEPRECATION")
@Composable
fun OfflineMap(
    routeCoordinates: List<GeoCoordinate>,
    previousRouteCoordinates: List<List<GeoCoordinate>>,
    currentLocation: GeoCoordinate?,
    destinationLocation: GeoCoordinate?,
    destinationName: String?,
    destinationDistanceLabel: String?,
    deviceHeadingDegrees: Float?,
    followUserLocation: Boolean,
    recenterRequest: Int,
    routeOverviewRequest: Int,
    onUserMapGesture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestRoute = rememberUpdatedState(routeCoordinates)
    val latestPreviousRoutes = rememberUpdatedState(previousRouteCoordinates)
    val latestLocation = rememberUpdatedState(currentLocation)
    val latestDestination = rememberUpdatedState(destinationLocation)
    val latestDestinationName = rememberUpdatedState(destinationName)
    val latestDestinationDistance = rememberUpdatedState(destinationDistanceLabel)
    val latestHeading = rememberUpdatedState(deviceHeadingDegrees)
    val latestFollowUser = rememberUpdatedState(followUserLocation)
    val latestOnUserMapGesture = rememberUpdatedState(onUserMapGesture)
    val userMarkerBitmap = remember(context) { createUserMarkerBitmap(context) }
    val destinationAnnotationBitmap = remember(
        context,
        destinationName,
        destinationDistanceLabel,
    ) {
        createDestinationAnnotationBitmap(
            context = context,
            destinationName = destinationName ?: "Tujuan evakuasi",
            distanceLabel = destinationDistanceLabel.orEmpty(),
        )
    }
    val cameraTracker = remember { CameraTracker() }
    val mapView = remember {
        val mapOptions = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true)
        MapView(context, mapOptions).apply {
            onCreate(null)
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN ->
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_MOVE -> {
                        if (latestFollowUser.value) {
                            cameraTracker.isFollowing = false
                            latestOnUserMapGesture.value()
                        }
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(DEFAULT_PADANG_CENTER)
                    .zoom(DEFAULT_ZOOM)
                    .build()
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isRotateGesturesEnabled = true
                map.uiSettings.isScrollGesturesEnabled = true
                map.uiSettings.isHorizontalScrollGesturesEnabled = true
                map.uiSettings.isZoomGesturesEnabled = true
                map.uiSettings.isDoubleTapGesturesEnabled = true
                map.uiSettings.isQuickZoomGesturesEnabled = true
                map.addOnCameraMoveStartedListener { reason ->
                    if (
                        reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE &&
                        latestFollowUser.value
                    ) {
                        cameraTracker.isFollowing = false
                        latestOnUserMapGesture.value()
                    }
                }
                map.setStyle(Style.Builder().fromJson(DEVELOPMENT_MAP_STYLE)) { style ->
                    updateMapOverlays(
                        style = style,
                        routeCoordinates = latestRoute.value,
                        previousRouteCoordinates = latestPreviousRoutes.value,
                        currentLocation = latestLocation.value,
                        destinationLocation = latestDestination.value,
                        destinationAnnotationBitmap = createDestinationAnnotationBitmap(
                            context = context,
                            destinationName = latestDestinationName.value ?: "Tujuan evakuasi",
                            distanceLabel = latestDestinationDistance.value.orEmpty(),
                        ),
                        userMarkerBitmap = userMarkerBitmap,
                    )
                    if (latestFollowUser.value) latestLocation.value?.let { location ->
                        updateNavigationCamera(
                            map = map,
                            location = location,
                            headingDegrees = latestHeading.value,
                            tracker = cameraTracker,
                            animate = false,
                            force = true,
                        )
                        cameraTracker.isFollowing = true
                    }
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
                    updateMapOverlays(
                        style = style,
                        routeCoordinates = routeCoordinates,
                        previousRouteCoordinates = previousRouteCoordinates,
                        currentLocation = currentLocation,
                        destinationLocation = destinationLocation,
                        destinationAnnotationBitmap = destinationAnnotationBitmap,
                        userMarkerBitmap = userMarkerBitmap,
                    )
                }
                if (followUserLocation) currentLocation?.let { location ->
                    if (routeOverviewRequest != cameraTracker.routeOverviewRequest) {
                        showRouteOverview(map, routeCoordinates, location)
                        cameraTracker.isFollowing = false
                        cameraTracker.routeOverviewRequest = routeOverviewRequest
                    } else {
                        val forceRecenter = recenterRequest != cameraTracker.recenterRequest
                        updateNavigationCamera(
                            map = map,
                            location = location,
                            headingDegrees = deviceHeadingDegrees,
                            tracker = cameraTracker,
                            animate = true,
                            force = !cameraTracker.isFollowing || forceRecenter,
                        )
                        cameraTracker.isFollowing = true
                        cameraTracker.recenterRequest = recenterRequest
                    }
                } else {
                    cameraTracker.isFollowing = false
                    if (routeOverviewRequest != cameraTracker.routeOverviewRequest) {
                        showRouteOverview(map, routeCoordinates, currentLocation)
                        cameraTracker.routeOverviewRequest = routeOverviewRequest
                    }
                }
            }
        },
        modifier = modifier,
    )
}

private fun showRouteOverview(
    map: org.maplibre.android.maps.MapLibreMap,
    routeCoordinates: List<GeoCoordinate>,
    currentLocation: GeoCoordinate?,
) {
    if (routeCoordinates.size < 2) return
    val points = routeCoordinates.map { coordinate ->
        LatLng(coordinate.latitude, coordinate.longitude)
    }.toMutableList()
    currentLocation?.let { location ->
        points += LatLng(location.latitude, location.longitude)
    }
    val bounds = LatLngBounds.Builder().includes(points).build()
    map.easeCamera(
        CameraUpdateFactory.newLatLngBounds(bounds, ROUTE_OVERVIEW_PADDING_PX),
        ROUTE_OVERVIEW_ANIMATION_MILLIS,
    )
}

private fun updateMapOverlays(
    style: Style,
    routeCoordinates: List<GeoCoordinate>,
    previousRouteCoordinates: List<List<GeoCoordinate>>,
    currentLocation: GeoCoordinate?,
    destinationLocation: GeoCoordinate?,
    destinationAnnotationBitmap: Bitmap,
    userMarkerBitmap: Bitmap,
) {
    updatePreviousRouteOverlays(style, previousRouteCoordinates)

    if (routeCoordinates.size >= 2) {
        val geometry = LineString.fromLngLats(
            routeCoordinates.map { coordinate ->
                Point.fromLngLat(coordinate.longitude, coordinate.latitude)
            },
        )
        val existingRouteSource = style.getSource(ROUTE_SOURCE_ID) as? GeoJsonSource
        if (existingRouteSource == null) {
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, geometry))
            val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                lineColor("#7A7FFF"),
                lineWidth(7f),
                lineOpacity(0.96f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            )
            when {
                style.getLayer(DESTINATION_LAYER_ID) != null ->
                    style.addLayerBelow(routeLayer, DESTINATION_LAYER_ID)
                style.getLayer(LOCATION_LAYER_ID) != null ->
                    style.addLayerBelow(routeLayer, LOCATION_LAYER_ID)
                else -> style.addLayer(routeLayer)
            }
        } else {
            existingRouteSource.setGeoJson(geometry)
        }
    }

    destinationLocation?.let { coordinate ->
        val feature = Feature.fromGeometry(
            Point.fromLngLat(coordinate.longitude, coordinate.latitude),
        )
        val existingDestinationSource = style.getSource(DESTINATION_SOURCE_ID) as? GeoJsonSource
        style.addImage(DESTINATION_IMAGE_ID, destinationAnnotationBitmap)
        if (existingDestinationSource == null) {
            style.addSource(GeoJsonSource(DESTINATION_SOURCE_ID, feature))
            style.addLayer(
                SymbolLayer(DESTINATION_LAYER_ID, DESTINATION_SOURCE_ID).withProperties(
                    iconImage(DESTINATION_IMAGE_ID),
                    iconSize(1f),
                    iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                ),
            )
        } else {
            existingDestinationSource.setGeoJson(feature)
        }
    }

    currentLocation?.let { coordinate ->
        val feature = Feature.fromGeometry(
            Point.fromLngLat(coordinate.longitude, coordinate.latitude),
        )
        val existingLocationSource = style.getSource(LOCATION_SOURCE_ID) as? GeoJsonSource
        if (existingLocationSource == null) {
            style.addImage(USER_LOCATION_IMAGE_ID, userMarkerBitmap)
            style.addSource(GeoJsonSource(LOCATION_SOURCE_ID, feature))
            style.addLayer(
                SymbolLayer(LOCATION_LAYER_ID, LOCATION_SOURCE_ID).withProperties(
                    iconImage(USER_LOCATION_IMAGE_ID),
                    iconSize(1f),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true),
                    iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                ),
            )
        } else {
            existingLocationSource.setGeoJson(feature)
        }
    }
}

private fun updatePreviousRouteOverlays(
    style: Style,
    previousRouteCoordinates: List<List<GeoCoordinate>>,
) {
    val features = previousRouteCoordinates.mapNotNull { coordinates ->
        if (coordinates.size < 2) return@mapNotNull null
        Feature.fromGeometry(
            LineString.fromLngLats(
                coordinates.map { coordinate ->
                    Point.fromLngLat(coordinate.longitude, coordinate.latitude)
                },
            ),
        )
    }
    val featureCollection = FeatureCollection.fromFeatures(features)
    val existingSource = style.getSource(PREVIOUS_ROUTES_SOURCE_ID) as? GeoJsonSource
    if (existingSource == null) {
        if (features.isEmpty()) return
        style.addSource(GeoJsonSource(PREVIOUS_ROUTES_SOURCE_ID, featureCollection))
        val layer = LineLayer(PREVIOUS_ROUTES_LAYER_ID, PREVIOUS_ROUTES_SOURCE_ID).withProperties(
            lineColor("#7D8588"),
            lineWidth(5f),
            lineOpacity(0.72f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
        )
        if (style.getLayer(ROUTE_LAYER_ID) != null) {
            style.addLayerBelow(layer, ROUTE_LAYER_ID)
        } else {
            style.addLayer(layer)
        }
    } else {
        existingSource.setGeoJson(featureCollection)
    }
}

private fun updateNavigationCamera(
    map: org.maplibre.android.maps.MapLibreMap,
    location: GeoCoordinate,
    headingDegrees: Float?,
    tracker: CameraTracker,
    animate: Boolean,
    force: Boolean = false,
) {
    val normalizedHeading = ((headingDegrees ?: tracker.headingDegrees) % 360f + 360f) % 360f
    val movedMeters = tracker.location?.let { previous ->
        NearestNodeFinder.distanceMeters(previous, location)
    } ?: Double.POSITIVE_INFINITY
    val headingDelta = angularDifferenceDegrees(tracker.headingDegrees, normalizedHeading)
    if (!force && movedMeters < MIN_CAMERA_MOVE_METERS && headingDelta < MIN_CAMERA_TURN_DEGREES) return

    val cameraPosition = CameraPosition.Builder()
        .target(LatLng(location.latitude, location.longitude))
        .zoom(USER_LOCATION_ZOOM)
        .bearing(normalizedHeading.toDouble())
        .build()
    val update = CameraUpdateFactory.newCameraPosition(cameraPosition)
    if (animate && tracker.location != null) {
        map.easeCamera(update, CAMERA_ANIMATION_MILLIS)
    } else {
        map.moveCamera(update)
    }
    tracker.location = location
    tracker.headingDegrees = normalizedHeading
}

private fun angularDifferenceDegrees(first: Float, second: Float): Float {
    val difference = kotlin.math.abs(first - second) % 360f
    return minOf(difference, 360f - difference)
}

private fun createUserMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val outerSize = (USER_MARKER_OUTER_DP * density).toInt().coerceAtLeast(1)
    val leafSize = (USER_MARKER_LEAF_DP * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(outerSize, outerSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = outerSize / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(USER_MARKER_BACKGROUND_ALPHA, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center - density, paint)
    paint.apply {
        color = Color.rgb(9, 78, 255)
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    canvas.drawCircle(center, center, center - density, paint)

    context.getDrawable(R.drawable.ic_figma_map_arrow)?.let { drawable ->
        val inset = (outerSize - leafSize) / 2
        drawable.setBounds(inset, inset, inset + leafSize, inset + leafSize)
        drawable.draw(canvas)
    }
    return bitmap
}

private fun createDestinationAnnotationBitmap(
    context: Context,
    destinationName: String,
    distanceLabel: String,
): Bitmap {
    val density = context.resources.displayMetrics.density
    fun px(dp: Float): Float = dp * density

    val width = px(180f).toInt()
    val cardHeight = px(62f)
    val pointerHeight = px(10f)
    val pinSize = px(34f).toInt()
    val totalHeight = (cardHeight + pointerHeight + pinSize).toInt()
    val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = width / 2f
    val cardBounds = RectF(px(1f), px(1f), width - px(1f), cardHeight)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(px(3f), 0f, px(1f), Color.argb(70, 0, 0, 0))
    }
    canvas.drawRoundRect(cardBounds, px(10f), px(10f), cardPaint)
    val pointer = Path().apply {
        moveTo(centerX - px(9f), cardHeight - px(1f))
        lineTo(centerX, cardHeight + pointerHeight)
        lineTo(centerX + px(9f), cardHeight - px(1f))
        close()
    }
    canvas.drawPath(pointer, cardPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(90, 126, 128)
        style = Paint.Style.STROKE
        strokeWidth = px(1f)
    }
    canvas.drawRoundRect(cardBounds, px(10f), px(10f), borderPaint)
    canvas.drawPath(pointer, borderPaint)

    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(12, 24, 31)
        textSize = px(14f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val distancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 48, 73)
        textSize = px(13f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val textStart = px(13f)
    val textWidth = width - px(26f)
    canvas.drawText(
        fitText(destinationName.uppercase(), namePaint, textWidth),
        textStart,
        px(25f),
        namePaint,
    )
    if (distanceLabel.isNotBlank()) {
        canvas.drawText("≈ $distanceLabel", textStart, px(48f), distancePaint)
    }

    context.getDrawable(R.drawable.ic_figma_destination)?.let { drawable ->
        val left = ((width - pinSize) / 2f).toInt()
        val top = (cardHeight + pointerHeight).toInt()
        drawable.setBounds(left, top, left + pinSize, top + pinSize)
        drawable.draw(canvas)
    }
    return bitmap
}

private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    val ellipsis = "…"
    var end = text.length
    while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
        end--
    }
    return text.substring(0, end).trimEnd() + ellipsis
}

private class CameraTracker {
    var location: GeoCoordinate? = null
    var headingDegrees: Float = 0f
    var isFollowing: Boolean = false
    var recenterRequest: Int = -1
    var routeOverviewRequest: Int = 0
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
private const val ROUTE_OVERVIEW_PADDING_PX = 120
private const val ROUTE_OVERVIEW_ANIMATION_MILLIS = 900
private const val CAMERA_ANIMATION_MILLIS = 180
private const val MIN_CAMERA_MOVE_METERS = 1.5
private const val MIN_CAMERA_TURN_DEGREES = 2f
private const val USER_MARKER_OUTER_DP = 51f
private const val USER_MARKER_LEAF_DP = 39f
private const val USER_MARKER_BACKGROUND_ALPHA = 145
private const val ROUTE_SOURCE_ID = "evacuation-route-source"
private const val ROUTE_LAYER_ID = "evacuation-route-layer"
private const val PREVIOUS_ROUTES_SOURCE_ID = "previous-evacuation-routes-source"
private const val PREVIOUS_ROUTES_LAYER_ID = "previous-evacuation-routes-layer"
private const val LOCATION_SOURCE_ID = "user-location-source"
private const val LOCATION_LAYER_ID = "user-location-layer"
private const val USER_LOCATION_IMAGE_ID = "user-location-navigation-image"
private const val DESTINATION_SOURCE_ID = "evacuation-destination-source"
private const val DESTINATION_LAYER_ID = "evacuation-destination-layer"
private const val DESTINATION_IMAGE_ID = "evacuation-destination-annotation-image"
