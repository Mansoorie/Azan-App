package com.example.azan.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.azan.data.LocationPreferences
import com.example.azan.data.PrayerTimeRepository
import com.example.azan.location.GeocodingHelper
import com.example.azan.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for manual location input
 */
class ManualLocationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val locationPreferences = LocationPreferences(application)
    private val prayerTimeRepository = PrayerTimeRepository(application)
    
        // State to track if country selection is needed
    private val _needCountrySelection = MutableStateFlow(false)
    val needCountrySelection: StateFlow<Boolean> = _needCountrySelection.asStateFlow()
    
    // State to hold available countries
    private val _availableCountries = MutableStateFlow<List<String>>(emptyList())
    val availableCountries: StateFlow<List<String>> = _availableCountries.asStateFlow()
    
    // State for operation result
    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()
    
    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    init {
        // Load available countries
        viewModelScope.launch {
            try {
                _availableCountries.value = prayerTimeRepository.getAvailableCountries()
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("Failed to load countries: ${e.message}")
            }
        }
    }
    
    /**
     * Save manually entered location coordinates
     */
    fun saveManualLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                // Try to detect country from coordinates
                val geocodingHelper = GeocodingHelper(getApplication())
                val detectedCountry = geocodingHelper.getCountryFromCoordinates(latitude, longitude)
                
                if (detectedCountry == null) {
                    // Country detection failed, prompt user to select country
                    _needCountrySelection.value = true
                    // Save coordinates temporarily
                    _tempLatitude = latitude
                    _tempLongitude = longitude
                    // Also save to preferences as backup
                    locationPreferences.saveLocation(latitude, longitude)
                    android.util.Log.d(TAG, "Country detection failed, saved temp coordinates: $latitude, $longitude")
                    _operationResult.value = OperationResult.CountrySelectionNeeded
                } else {
                    // Country detected successfully
                    android.util.Log.d(TAG, "Country detected: $detectedCountry for coordinates: $latitude, $longitude")
                    completeLocationSave(latitude, longitude, detectedCountry)
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("Failed to save location: ${e.message}")
            }
        }
    }
    
    /**
     * Save location with manually selected country
     */
    // Temporary variables to store coordinates when country selection is needed
    private var _tempLatitude: Double? = null
    private var _tempLongitude: Double? = null
    private val TAG = "ManualLocationViewModel"
    
    fun saveSelectedCountry(countryName: String) {
        viewModelScope.launch {
            try {
                // Validate country name
                val validCountryName = countryName.trim()
                android.util.Log.d(TAG, "Saving selected country: $validCountryName")
                
                if (validCountryName.isEmpty()) {
                    _operationResult.value = OperationResult.Error("Country name cannot be empty")
                    return@launch
                }
                
                // Get the coordinates that were previously entered
                val latitude = _tempLatitude
                val longitude = _tempLongitude
                
                if (latitude != null && longitude != null) {
                    android.util.Log.d(TAG, "Using temporary coordinates: $latitude, $longitude with country: $validCountryName")
                    completeLocationSave(latitude, longitude, validCountryName)
                    _needCountrySelection.value = false
                    _tempLatitude = null
                    _tempLongitude = null
                } else {
                    // Fallback to preferences if temp coordinates are null
                    android.util.Log.d(TAG, "Temp coordinates null, falling back to preferences")
                    val prefLatitude = locationPreferences.latitude.first()
                    val prefLongitude = locationPreferences.longitude.first()
                    
                    if (prefLatitude != null && prefLongitude != null) {
                        android.util.Log.d(TAG, "Using coordinates from preferences: $prefLatitude, $prefLongitude with country: $validCountryName")
                        completeLocationSave(prefLatitude, prefLongitude, validCountryName)
                        _needCountrySelection.value = false
                    } else {
                        android.util.Log.e(TAG, "No coordinates available from any source")
                        _operationResult.value = OperationResult.Error("No coordinates available. Please try again.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error saving selected country", e)
                _operationResult.value = OperationResult.Error("Failed to save country: ${e.message}")
            }
        }
    }
    
    /**
     * Complete the location saving process with country information
     */
    private suspend fun completeLocationSave(latitude: Double, longitude: Double, countryName: String) {
        // Validate country name
        val validatedCountryName = countryName.trim()
        
        if (validatedCountryName.isEmpty()) {
            _operationResult.value = OperationResult.Error("Country name cannot be empty")
            return
        }
        
        // Log the country being saved
        android.util.Log.d("ManualLocationViewModel", "Saving location with country: $validatedCountryName")
        
        // Save location with country to preferences
        locationPreferences.saveLocationWithCountry(latitude, longitude, validatedCountryName)
        
        // Calculate and store prayer times
        try {
            prayerTimeRepository.calculateAndStorePrayerTimes(
                latitude = latitude,
                longitude = longitude,
                countryName = validatedCountryName
            )
            
            // Schedule periodic updates
            WorkManagerScheduler.schedulePrayerTimeUpdates(getApplication())
            
            _operationResult.value = OperationResult.Success("Location saved successfully with country: $validatedCountryName")
        } catch (e: Exception) {
            android.util.Log.e("ManualLocationViewModel", "Error calculating prayer times", e)
            _operationResult.value = OperationResult.Error("Failed to calculate prayer times: ${e.message}")
        }
    }
}

/**
 * Represents the result of an operation in the ManualLocationViewModel
 */
sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class Error(val message: String) : OperationResult()
    object CountrySelectionNeeded : OperationResult()
}