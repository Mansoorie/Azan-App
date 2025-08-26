package com.example.azan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for the Azan app
 */
@Database(entities = [PrayerTime::class], version = 1, exportSchema = false)
abstract class AzanDatabase : RoomDatabase() {
    
    /**
     * Get the DAO for prayer times
     */
    abstract fun prayerTimeDao(): PrayerTimeDao
    
    companion object {
        @Volatile
        private var INSTANCE: AzanDatabase? = null
        
        /**
         * Get the singleton instance of the database
         */
        fun getDatabase(context: Context): AzanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AzanDatabase::class.java,
                    "azan_database"
                ).build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}