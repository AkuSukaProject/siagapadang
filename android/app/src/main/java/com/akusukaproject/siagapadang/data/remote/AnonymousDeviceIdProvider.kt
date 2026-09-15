package com.akusukaproject.siagapadang.data.remote

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Abstraksi penyimpanan ID perangkat untuk mempermudah pengujian unit.
 */
interface DeviceIdStorage {
    fun getDeviceId(): String?
    fun saveDeviceId(deviceId: String)
}

class SharedPreferencesDeviceIdStorage(context: Context) : DeviceIdStorage {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getDeviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)

    override fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    companion object {
        const val PREFS_NAME = "siaga_padang_device_prefs"
        const val KEY_DEVICE_ID = "anonymous_device_id"
    }
}

/**
 * Menyediakan ID perangkat acak dan stabil untuk keperluan keamanan rate-limiting
 * dan konsensus pelaporan backend, tanpa mengumpulkan identitas pribadi pengguna.
 */
class AnonymousDeviceIdProvider(
    private val storage: DeviceIdStorage,
) {
    constructor(context: Context) : this(SharedPreferencesDeviceIdStorage(context))

    @Volatile
    private var cachedDeviceId: String? = null

    fun getDeviceId(): String {
        cachedDeviceId?.let { return it }

        synchronized(this) {
            cachedDeviceId?.let { return it }
            val existingId = storage.getDeviceId()
            if (!existingId.isNullOrBlank()) {
                cachedDeviceId = existingId
                return existingId
            }

            val newId = UUID.randomUUID().toString()
            storage.saveDeviceId(newId)
            cachedDeviceId = newId
            return newId
        }
    }
}
