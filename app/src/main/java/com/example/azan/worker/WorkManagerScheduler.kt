package com.example.azan.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utility class to schedule background work for prayer time updates
 */
object WorkManagerScheduler {
    
    private const val PRAYER_TIME_UPDATE_WORK = "prayer_time_update_work"
    
    // Schedule to run every 40 days
    private const val REPEAT_INTERVAL = 40L
    private val REPEAT_INTERVAL_TIME_UNIT = TimeUnit.DAYS
    
    // Flexible interval of 1 day to allow WorkManager to optimize battery usage
    private const val FLEX_INTERVAL = 1L
    private val FLEX_INTERVAL_TIME_UNIT = TimeUnit.DAYS
    
    /**
     * Schedule periodic prayer time updates
     */
    fun schedulePrayerTimeUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Require network connection
            .setRequiresBatteryNotLow(true) // Don't run when battery is low
            .build()
        
        val periodicWorkRequest = PeriodicWorkRequestBuilder<PrayerTimeUpdateWorker>(
            REPEAT_INTERVAL, REPEAT_INTERVAL_TIME_UNIT,
            FLEX_INTERVAL, FLEX_INTERVAL_TIME_UNIT
        )
            .setConstraints(constraints)
            .build()
        
        // Enqueue the work request
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PRAYER_TIME_UPDATE_WORK,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            periodicWorkRequest
        )
    }
    
    /**
     * Cancel scheduled prayer time updates
     */
    fun cancelPrayerTimeUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PRAYER_TIME_UPDATE_WORK)
    }
}