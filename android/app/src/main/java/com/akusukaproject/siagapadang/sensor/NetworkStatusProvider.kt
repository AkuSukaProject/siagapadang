package com.akusukaproject.siagapadang.sensor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkStatusProvider(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    fun availability(): Flow<Boolean> = callbackFlow {
        fun publishCurrentStatus() {
            trySend(hasValidatedNetwork())
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = publishCurrentStatus()

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                        ),
                )
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        publishCurrentStatus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            connectivityManager.registerNetworkCallback(request, callback)
        }
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    private fun hasValidatedNetwork(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
