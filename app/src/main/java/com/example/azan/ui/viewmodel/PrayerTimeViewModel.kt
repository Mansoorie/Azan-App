package com.example.azan.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.azan.data.LocationPreferences
import com.example.azan.data.PrayerTime
import com.example.azan.data.PrayerTimeRepository
import com.example.azan.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for prayer times
 */
class PrayerTimeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prayerTimeRepository = PrayerTimeRepository(application)
    private val locationPreferences = LocationPreferences(application)
    
    private val _todayPrayerTimes = MutableLiveData<PrayerTime?>()
    val todayPrayerTimes: LiveData<PrayerTime?> = _todayPrayerTimes
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _availableCountries = MutableLiveData<List<String>>()
    val availableCountries: LiveData<List<String>> = _availableCountries
    
    private val _selectedCountry = MutableLiveData<String?>()
    val selectedCountry: LiveData<String?> = _selectedCountry
    
    init {
        loadAvailableCountries()
        loadSelectedCountry()
        
        // Check if we need to load prayer times
        viewModelScope.launch {
            val hasLocationData = locationPreferences.hasLocationData.first()
            if (hasLocationData) {
                loadTodayPrayerTimes()
            }
        }
    }
    
    /**
     * Load available countries from the repository
     */
    private fun loadAvailableCountries() {
        viewModelScope.launch {
            try {
                val countries = prayerTimeRepository.getAvailableCountries()
                _availableCountries.value = countries
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load countries: ${e.message}"
            }
        }
    }
    
    /**
     * Load the selected country from preferences
     */
    private fun loadSelectedCountry() {
        viewModelScope.launch {
            try {
                val country = locationPreferences.countryName.first()
                _selectedCountry.value = country
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load selected country: ${e.message}"
            }
        }
    }
    
    /**
     * Set the selected country and recalculate prayer times
     */
    fun setCountry(countryName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Validate country name
                val validCountryName = countryName.trim()
                android.util.Log.d("PrayerTimeViewModel", "Setting country: $validCountryName")
                
                // Save country name to preferences
                locationPreferences.saveCountryName(validCountryName)
                _selectedCountry.value = validCountryName
                
                // Get location data
                val hasLocationData = locationPreferences.hasLocationData.first()
                
                if (hasLocationData) {
                    val latitude = locationPreferences.latitude.first()
                    val longitude = locationPreferences.longitude.first()
                    
                    // Calculate and store prayer times
                    if (countryName.isNullOrBlank()) {
                        android.util.Log.w("PrayerTimeViewModel", "Country name is null or blank, using default country: United States")
                        prayerTimeRepository.calculateAndStorePrayerTimes(
                            latitude = latitude,
                            longitude = longitude,
                            countryName = "United States"
                        )
                    } else {
                        android.util.Log.d("PrayerTimeViewModel", "Recalculating prayer times with country: $countryName")
                        prayerTimeRepository.calculateAndStorePrayerTimes(
                            latitude = latitude,
                            longitude = longitude,
                            countryName = countryName
                        )
                    }
                    
                    // Schedule periodic updates
                    WorkManagerScheduler.schedulePrayerTimeUpdates(getApplication())
                    
                    // Load today's prayer times
                    loadTodayPrayerTimes()
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set country: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Recalculate prayer times with current location and country
     */
    fun recalculatePrayerTimes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Check if we have location data
                val hasLocationData = locationPreferences.hasLocationData.first()
                
                if (hasLocationData) {
                    // Recalculate prayer times with the current location and country
                    val latitude = locationPreferences.latitude.first()
                    val longitude = locationPreferences.longitude.first()
                    val country = locationPreferences.countryName.first()
                    
                    if (country.isNullOrBlank()) {
                        android.util.Log.w("PrayerTimeViewModel", "Country name is null or blank, using default country: United States")
                        prayerTimeRepository.calculateAndStorePrayerTimes(
                            latitude = latitude,
                            longitude = longitude,
                            countryName = "United States"
                        )
                    } else {
                        android.util.Log.d("PrayerTimeViewModel", "Recalculating prayer times with country: $country")
                        prayerTimeRepository.calculateAndStorePrayerTimes(
                            latitude = latitude,
                            longitude = longitude,
                            countryName = country
                        )
                    }
                    
                    // Schedule periodic updates
                    WorkManagerScheduler.schedulePrayerTimeUpdates(getApplication())
                    
                    // Load today's prayer times
                    loadTodayPrayerTimes()
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to recalculate prayer times: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load today's prayer times from the repository
     */
    fun loadTodayPrayerTimes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val prayerTime = prayerTimeRepository.getPrayerTimeForDate(today)
                _todayPrayerTimes.value = prayerTime
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load prayer times: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}