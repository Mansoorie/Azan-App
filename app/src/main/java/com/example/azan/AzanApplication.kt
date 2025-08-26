package com.example.azan

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

/**
 * Application class for initializing components
 */
class AzanApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
    }

    /**
     * Provide WorkManager configuration
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}