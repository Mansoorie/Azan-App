package com.example.azan.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.azan.data.LocationPreferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages location access efficiently to minimize battery consumption
 */
class LocationManager(private val context: Context) {
    
    private val locationPreferences = LocationPreferences(context)
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Initial)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            // Log the received location result
            android.util.Log.d("LocationManager", "Location update received: ${result.locations.size} locations")
            
            result.lastLocation?.let { location ->
                val latitude = location.latitude
                val longitude = location.longitude
                
                // Log the location data
                android.util.Log.d("LocationManager", "Updated location: $latitude, $longitude, accuracy: ${location.accuracy}m")
                
                // Save location to preferences with country information
                coroutineScope.launch {
                    saveLocationData(latitude, longitude)
                }
                
                // Update state
                _locationState.value = LocationState.Success(latitude, longitude)
                
                // Stop location updates after getting a result
                stopLocationUpdates()
            } ?: run {
                // Log that no location was received
                android.util.Log.w("LocationManager", "Location result received but lastLocation is null")
                
                // Check if there are any locations in the result
                if (result.locations.isNotEmpty()) {
                    val location = result.locations[0]
                    val latitude = location.latitude
                    val longitude = location.longitude
                    
                    // Log the location data
                    android.util.Log.d("LocationManager", "Using first location from result: $latitude, $longitude, accuracy: ${location.accuracy}m")
                    
                    // Save location to preferences with country information
                    coroutineScope.launch {
                        saveLocationData(latitude, longitude)
                    }
                    
                    // Update state
                    _locationState.value = LocationState.Success(latitude, longitude)
                    
                    // Stop location updates after getting a result
                    stopLocationUpdates()
                }
            }
        }
    }
    
    /**
     * Check if location permissions are granted
     * @return true if either fine or coarse location permission is granted
     */
    fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if precise location permission is granted
     * @return true if fine location permission is granted
     */
    private fun hasPreciseLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Save location data to preferences
     */
    private suspend fun saveLocationData(latitude: Double, longitude: Double) {
        // Try to get country name from coordinates
        val geocodingHelper = GeocodingHelper(context)
        val countryName = geocodingHelper.getCountryFromCoordinates(latitude, longitude)
        
        // Save location with country name if available
        if (countryName != null) {
            locationPreferences.saveLocationWithCountry(latitude, longitude, countryName)
        } else {
            locationPreferences.saveLocation(latitude, longitude)
        }
    }
    
    /**
     * Get the last known location if available, otherwise request a location update
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        if (!hasLocationPermissions()) {
            _locationState.value = LocationState.PermissionRequired
            return
        }
        
        _locationState.value = LocationState.Loading
        
        // Log the permission status to help with debugging
        val permissionStatus = "Fine location: ${hasPreciseLocationPermission()}, Coarse location: ${hasLocationPermissions()}"
        android.util.Log.d("LocationManager", "Permission status: $permissionStatus")
        
        // Try to get last location first (most battery efficient)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Use last known location if available
                    val latitude = location.latitude
                    val longitude = location.longitude
                    
                    // Log the location data
                    android.util.Log.d("LocationManager", "Last known location: $latitude, $longitude")
                    
                    // Save location to preferences
                    coroutineScope.launch {
                        saveLocationData(latitude, longitude)
                    }
                    
                    // Update state
                    _locationState.value = LocationState.Success(latitude, longitude)
                } else {
                    // Log that we need to request location updates
                    android.util.Log.d("LocationManager", "No last known location, requesting updates")
                    
                    // Request a single location update if last known location is not available
                    requestLocationUpdates()
                }
            }
            .addOnFailureListener { e ->
                // Log the error
                android.util.Log.e("LocationManager", "Error getting last location: ${e.message}")
                _locationState.value = LocationState.Error(e.message ?: "Unknown error")
            }
    }
    
    /**
     * Request a single location update
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (!hasLocationPermissions()) {
            _locationState.value = LocationState.PermissionRequired
            return
        }
        
        // Set priority based on available permissions
        // Use HIGH_ACCURACY only if we have precise location permission
        val priority = if (hasPreciseLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            // Fall back to balanced accuracy if only coarse location is available
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        
        // Updated location request settings for better compatibility with Android 13+ devices
        val locationRequest = LocationRequest.Builder(10000) // 10 seconds interval
            .setPriority(priority)
            .setWaitForAccurateLocation(hasPreciseLocationPermission()) // Only wait for accurate location if we have precise permission
            .setMinUpdateIntervalMillis(5000) // 5 seconds minimum interval
            .setMaxUpdateDelayMillis(15000) // 15 seconds maximum delay
            .setMaxUpdates(3) // Try up to 3 updates to get a good location
            .build()
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    
    /**
     * Stop location updates to save battery
     */
    fun stopLocationUpdates() {
        android.util.Log.d("LocationManager", "Stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnSuccessListener {
                android.util.Log.d("LocationManager", "Location updates stopped successfully")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("LocationManager", "Failed to stop location updates: ${e.message}")
            }
    }
}

/**
 * Represents the state of location data
 */
sealed class LocationState {
    object Initial : LocationState()
    object Loading : LocationState()
    object PermissionRequired : LocationState()
    data class Success(val latitude: Double, val longitude: Double) : LocationState()
    data class Error(val message: String) : LocationState()
}