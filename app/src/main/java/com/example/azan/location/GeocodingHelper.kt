package com.example.azan.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Helper class to get country information from coordinates using Android's Geocoder
 */
class GeocodingHelper(private val context: Context) {

    /**
     * Get country name from coordinates
     * @return country name or null if not found
     */
    suspend fun getCountryFromCoordinates(latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                
                // Different implementation based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // For Android 13 and above, use the new API with callback
                    getCountryNameApi33(geocoder, latitude, longitude)
                } else {
                    // For older Android versions
                    getCountryNameLegacy(geocoder, latitude, longitude)
                }
            } catch (e: Exception) {
                // Log the error and return null
                e.printStackTrace()
                null
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun getCountryNameLegacy(geocoder: Geocoder, latitude: Double, longitude: Double): String? {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        return if (!addresses.isNullOrEmpty()) {
            val countryName = addresses[0].countryName
            android.util.Log.d("GeocodingHelper", "Legacy API detected country: $countryName")
            countryName
        } else {
            android.util.Log.w("GeocodingHelper", "Legacy API could not detect country")
            null
        }
    }
    
    private suspend fun getCountryNameApi33(geocoder: Geocoder, latitude: Double, longitude: Double): String? {
        return suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (addresses.isNotEmpty()) {
                    val countryName = addresses[0].countryName
                    android.util.Log.d("GeocodingHelper", "API 33+ detected country: $countryName")
                    continuation.resume(countryName)
                } else {
                    android.util.Log.w("GeocodingHelper", "API 33+ could not detect country")
                    continuation.resume(null)
                }
            }
        }
    }
}