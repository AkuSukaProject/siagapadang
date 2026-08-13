package com.akusukaproject.siagapadang.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DeviceLocation(
    val coordinate: GeoCoordinate,
    val accuracyMeters: Float,
)

class LocationProvider(context: Context) {
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun lastKnownLocation(): DeviceLocation? = suspendCancellableCoroutine { continuation ->
        client.lastLocation
            .addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(
                        location?.let {
                            DeviceLocation(
                                coordinate = GeoCoordinate(it.latitude, it.longitude),
                                accuracyMeters = it.accuracy,
                            )
                        },
                    )
                }
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
            .addOnCanceledListener { continuation.cancel() }
    }

    @SuppressLint("MissingPermission")
    suspend fun currentOrLastKnownLocation(): DeviceLocation? {
        val current = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSource = CancellationTokenSource()
                continuation.invokeOnCancellation { cancellationSource.cancel() }
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setDurationMillis(CURRENT_LOCATION_TIMEOUT_MILLIS)
                    .build()

                client.getCurrentLocation(request, cancellationSource.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(
                                location?.let {
                                    DeviceLocation(
                                        coordinate = GeoCoordinate(it.latitude, it.longitude),
                                        accuracyMeters = it.accuracy,
                                    )
                                },
                            )
                        }
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                    .addOnCanceledListener { continuation.cancel() }
            }
        }
        return current ?: runCatching { lastKnownLocation() }.getOrNull()
    }

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
        const val CURRENT_LOCATION_TIMEOUT_MILLIS = 5_000L
    }
}
