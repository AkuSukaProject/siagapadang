package com.akusukaproject.siagapadang.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NodeEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SiagaPadangDatabase : RoomDatabase() {
    abstract fun evacuationDao(): EvacuationDao
    abstract fun zoneDao(): ZoneDao

    companion object {
        @Volatile
        private var instance: SiagaPadangDatabase? = null

        fun getInstance(context: Context, storage: DatasetStorage): SiagaPadangDatabase =
            instance ?: synchronized(this) {
                instance ?: openActiveDatabase(context.applicationContext, storage)
                    .also { instance = it }
            }

        private fun openActiveDatabase(
            context: Context,
            storage: DatasetStorage,
        ): SiagaPadangDatabase {
            val activeName = storage.activeDatabaseName()
            var candidate: SiagaPadangDatabase? = null
            return try {
                candidate = buildDatabase(context, activeName)
                candidate.openHelper.writableDatabase
                candidate
            } catch (error: Exception) {
                candidate?.close()
                if (activeName == DatasetStorage.DEFAULT_DATABASE_NAME) throw error
                Log.e(LOG_TAG, "Dataset aktif gagal dibuka; kembali ke dataset sebelumnya", error)
                val fallbackName = storage.rollbackFailedActivation(activeName)
                buildDatabase(context, fallbackName).also { database ->
                    database.openHelper.writableDatabase
                }
            }
        }

        private fun buildDatabase(context: Context, databaseName: String): SiagaPadangDatabase {
            val builder = Room.databaseBuilder(
                context,
                SiagaPadangDatabase::class.java,
                databaseName,
            ).setJournalMode(JournalMode.TRUNCATE)
            if (databaseName == DatasetStorage.DEFAULT_DATABASE_NAME) {
                builder.createFromAsset("ranah_siaga.db")
            }
            return builder.build()
        }

        private const val LOG_TAG = "SiagaPadangDatabase"
    }
}
