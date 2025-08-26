package com.example.azan.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.azan.data.LocationPreferences
import com.example.azan.data.PrayerTimeRepository
import kotlinx.coroutines.flow.first

/**
 * Worker to update prayer times in the background
 */
class PrayerTimeUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "PrayerTimeUpdateWorker"
    private val prayerTimeRepository = PrayerTimeRepository(context)
    private val locationPreferences = LocationPreferences(context)
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting prayer time update worker")
        
        try {
            // Check if we need to recalculate
            if (!prayerTimeRepository.shouldRecalculatePrayerTimes()) {
                Log.d(TAG, "No need to recalculate prayer times yet")
                return Result.success()
            }
            
            // Get saved location
            val hasLocationData = locationPreferences.hasLocationData.first()
            
            if (!hasLocationData) {
                Log.d(TAG, "No location data available, cannot update prayer times")
                return Result.failure()
            }
            
            val latitude = locationPreferences.latitude.first()
            val longitude = locationPreferences.longitude.first()
            
            // Get country name from preferences (default to a common method if not set)
            val countryName = locationPreferences.countryName.first()
            
            if (countryName.isNullOrBlank()) {
                Log.w(TAG, "Country name is null or blank, using default country: United States")
                // Calculate and store prayer times with default country
                prayerTimeRepository.calculateAndStorePrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    countryName = "United States"
                )
            } else {
                Log.d(TAG, "Using country from preferences: $countryName")
                // Calculate and store prayer times with saved country
                prayerTimeRepository.calculateAndStorePrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    countryName = countryName
                )
            }
            
            Log.d(TAG, "Successfully updated prayer times")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating prayer times", e)
            return Result.retry()
        }
    }
}