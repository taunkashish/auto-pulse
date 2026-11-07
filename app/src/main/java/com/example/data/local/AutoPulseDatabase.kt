package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
abstract class AutoPulseDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: AutoPulseDatabase? = null

        fun getInstance(context: Context): AutoPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutoPulseDatabase::class.java,
                    "autopulse_trips.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
