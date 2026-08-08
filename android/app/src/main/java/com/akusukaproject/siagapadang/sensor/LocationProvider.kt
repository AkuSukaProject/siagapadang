package com.akusukaproject.siagapadang.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class DeviceLocation(
    val coordinate: GeoCoordinate,
    val accuracyMeters: Float,
)

class LocationProvider(context: Context) {
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    fun locations(): Flow<DeviceLocation> = callbackFlow {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MILLIS,
        )
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        DeviceLocation(
                            coordinate = GeoCoordinate(
                                latitude = location.latitude,
                                longitude = location.longitude,
                            ),
                            accuracyMeters = location.accuracy,
                        ),
                    )
                }
            }
        }

        client.lastLocation.addOnSuccessListener { location ->
            location?.let {
                trySend(
                    DeviceLocation(
                        coordinate = GeoCoordinate(it.latitude, it.longitude),
                        accuracyMeters = it.accuracy,
                    ),
                )
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { error -> close(error) }

        awaitClose { client.removeLocationUpdates(callback) }
    }.conflate()

    private companion object {
        const val UPDATE_INTERVAL_MILLIS = 1_000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 500L
        const val MIN_UPDATE_DISTANCE_METERS = 2f
    }
}

