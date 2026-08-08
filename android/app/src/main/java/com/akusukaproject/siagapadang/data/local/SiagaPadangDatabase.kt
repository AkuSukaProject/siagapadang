package com.akusukaproject.siagapadang.data.local

import android.content.Context
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

    companion object {
        @Volatile
        private var instance: SiagaPadangDatabase? = null

        fun getInstance(context: Context): SiagaPadangDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SiagaPadangDatabase::class.java,
                    "ranah_siaga.db",
                )
                    .createFromAsset("ranah_siaga.db")
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build()
                    .also { instance = it }
            }
    }
}
