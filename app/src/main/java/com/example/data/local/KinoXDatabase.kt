package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VideoHistoryEntity::class,
        PinnedFolderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KinoXDatabase : RoomDatabase() {
    abstract fun kinoXDao(): KinoXDao

    companion object {
        @Volatile
        private var INSTANCE: KinoXDatabase? = null

        fun getInstance(context: Context): KinoXDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KinoXDatabase::class.java,
                    "kinox_media.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
