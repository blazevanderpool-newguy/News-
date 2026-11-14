package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ArticleEntity::class], version = 1, exportSchema = false)
abstract class PulseWireDatabase : RoomDatabase() {
  abstract fun articleDao(): ArticleDao

  companion object {
    @Volatile private var INSTANCE: PulseWireDatabase? = null

    fun getInstance(context: Context): PulseWireDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                PulseWireDatabase::class.java,
                "pulsewire_cache.db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
