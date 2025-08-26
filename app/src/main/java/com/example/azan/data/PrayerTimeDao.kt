package com.example.azan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the PrayerTime entity
 */
@Dao
interface PrayerTimeDao {
    
    /**
     * Insert prayer times into the database
     * If there's a conflict (same date), replace the existing entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTimes: List<PrayerTime>)
    
    /**
     * Get prayer times for a specific date
     */
    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    fun getPrayerTimeForDate(date: String): Flow<PrayerTime?>
    
    /**
     * Get prayer times for today
     */
    @Query("SELECT * FROM prayer_times WHERE date = :todayDate LIMIT 1")
    fun getTodayPrayerTime(todayDate: String): Flow<PrayerTime?>
    
    /**
     * Get all prayer times in the database
     */
    @Query("SELECT * FROM prayer_times ORDER BY date ASC")
    fun getAllPrayerTimes(): Flow<List<PrayerTime>>
    
    /**
     * Delete all prayer times
     */
    @Query("DELETE FROM prayer_times")
    suspend fun deleteAllPrayerTimes()
    
    /**
     * Get the oldest prayer time entry date
     */
    @Query("SELECT MIN(date) FROM prayer_times")
    suspend fun getOldestPrayerTimeDate(): String?
    
    /**
     * Get the newest prayer time entry date
     */
    @Query("SELECT MAX(date) FROM prayer_times")
    suspend fun getNewestPrayerTimeDate(): String?
    
    /**
     * Count the number of prayer time entries
     */
    @Query("SELECT COUNT(*) FROM prayer_times")
    suspend fun countPrayerTimes(): Int
}