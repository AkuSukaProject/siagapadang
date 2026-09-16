package com.akusukaproject.siagapadang.data.local

import android.content.Context
import com.akusukaproject.siagapadang.data.model.LocalDatasetManifest
import org.json.JSONObject
import java.io.File

class DatasetStorage(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private var sessionDatabaseName =
        preferences.getString(KEY_ACTIVE_DATABASE, null) ?: DEFAULT_DATABASE_NAME
    private var sessionManifest = readPersistedManifest()

    fun activeDatabaseName(): String = sessionDatabaseName

    fun cleanupInterruptedDownloads() {
        appContext.getDatabasePath(DEFAULT_DATABASE_NAME).parentFile
            ?.listFiles { file -> file.name.endsWith(DOWNLOAD_SUFFIX) }
            ?.forEach(File::delete)
    }

    fun localManifest(): LocalDatasetManifest = sessionManifest

    private fun readPersistedManifest(): LocalDatasetManifest {
        val storedVersion = preferences.getString(KEY_VERSION, null)
        val storedChecksum = preferences.getString(KEY_CHECKSUM, null)
        val storedSize = preferences.getLong(KEY_SIZE_BYTES, -1L)
        if (storedVersion != null && storedChecksum != null && storedSize >= 0L) {
            return LocalDatasetManifest(
                version = storedVersion,
                checksum = storedChecksum,
                sizeBytes = storedSize,
            )
        }
        return readBundledManifest()
    }

    fun activateOnNextLaunch(databaseName: String, manifest: LocalDatasetManifest): Boolean {
        val currentDatabase = activeDatabaseName()
        val currentManifest = localManifest()
        return preferences.edit()
            .putString(KEY_PREVIOUS_DATABASE, currentDatabase)
            .putString(KEY_PREVIOUS_VERSION, currentManifest.version)
            .putString(KEY_PREVIOUS_CHECKSUM, currentManifest.checksum)
            .putLong(KEY_PREVIOUS_SIZE_BYTES, currentManifest.sizeBytes)
            .putString(KEY_ACTIVE_DATABASE, databaseName)
            .putString(KEY_VERSION, manifest.version)
            .putString(KEY_CHECKSUM, manifest.checksum)
            .putLong(KEY_SIZE_BYTES, manifest.sizeBytes)
            .commit()
    }

    fun rollbackFailedActivation(failedDatabaseName: String): String {
        val previousVersion = preferences.getString(KEY_PREVIOUS_VERSION, null)
        val previousChecksum = preferences.getString(KEY_PREVIOUS_CHECKSUM, null)
        val previousSize = preferences.getLong(KEY_PREVIOUS_SIZE_BYTES, -1L)
        val requestedFallback = preferences.getString(KEY_PREVIOUS_DATABASE, null)
        val hasPreviousMetadata =
            previousVersion != null && previousChecksum != null && previousSize >= 0L
        val fallback = requestedFallback?.takeIf {
            it == DEFAULT_DATABASE_NAME || hasPreviousMetadata
        } ?: DEFAULT_DATABASE_NAME
        val editor = preferences.edit()
            .putString(KEY_ACTIVE_DATABASE, fallback)
            .remove(KEY_PREVIOUS_DATABASE)

        editor
            .remove(KEY_PREVIOUS_VERSION)
            .remove(KEY_PREVIOUS_CHECKSUM)
            .remove(KEY_PREVIOUS_SIZE_BYTES)

        if (fallback == DEFAULT_DATABASE_NAME) {
            editor
                .remove(KEY_VERSION)
                .remove(KEY_CHECKSUM)
                .remove(KEY_SIZE_BYTES)
        } else if (previousVersion != null && previousChecksum != null && previousSize >= 0L) {
            editor
                .putString(KEY_VERSION, previousVersion)
                .putString(KEY_CHECKSUM, previousChecksum)
                .putLong(KEY_SIZE_BYTES, previousSize)
        }
        check(editor.commit()) { "Status rollback dataset tidak dapat disimpan." }
        sessionDatabaseName = fallback
        sessionManifest = if (fallback == DEFAULT_DATABASE_NAME) {
            readBundledManifest()
        } else {
            LocalDatasetManifest(
                version = requireNotNull(previousVersion),
                checksum = requireNotNull(previousChecksum),
                sizeBytes = previousSize,
            )
        }
        if (failedDatabaseName != DEFAULT_DATABASE_NAME) {
            appContext.deleteDatabase(failedDatabaseName)
        }
        return fallback
    }

    private fun readBundledManifest(): LocalDatasetManifest {
        val json = appContext.assets.open(MANIFEST_ASSET).bufferedReader().use {
            JSONObject(it.readText())
        }
        return LocalDatasetManifest(
            version = json.getString("version"),
            checksum = json.getString("checksum"),
            sizeBytes = json.getLong("size_bytes"),
        )
    }

    companion object {
        const val DEFAULT_DATABASE_NAME = "ranah_siaga_hybrid.db"
        private const val MANIFEST_ASSET = "dataset_manifest.json"
        private const val PREFERENCES_NAME = "dataset_storage"
        private const val KEY_ACTIVE_DATABASE = "active_database"
        private const val KEY_PREVIOUS_DATABASE = "previous_database"
        private const val KEY_PREVIOUS_VERSION = "previous_version"
        private const val KEY_PREVIOUS_CHECKSUM = "previous_checksum"
        private const val KEY_PREVIOUS_SIZE_BYTES = "previous_size_bytes"
        private const val KEY_VERSION = "version"
        private const val KEY_CHECKSUM = "checksum"
        private const val KEY_SIZE_BYTES = "size_bytes"
        private const val DOWNLOAD_SUFFIX = ".download"
    }
}
