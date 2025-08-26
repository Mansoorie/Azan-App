package com.example.azan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.azan.data.LocationPreferences
import com.example.azan.location.LocationManager
import com.example.azan.location.LocationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel to manage location data and UI state
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val locationManager = LocationManager(application)
    private val locationPreferences = LocationPreferences(application)
    
    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Loading)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()
    
    init {
        // Check if we already have location data stored
        viewModelScope.launch {
            locationPreferences.hasLocationData.collect { hasData ->
                if (hasData) {
                    // If we have location data, use it
                    locationPreferences.latitude.collect { latitude ->
                        locationPreferences.longitude.collect { longitude ->
                            if (latitude != 0.0 && longitude != 0.0) {
                                _uiState.value = LocationUiState.Success(latitude, longitude)
                                return@collect
                            }
                        }
                    }
                } else {
                    // Only request permissions if we don't have location data
                    if (locationManager.hasLocationPermissions()) {
                        updateLocation()
                    } else {
                        _uiState.value = LocationUiState.PermissionRequired
                    }
                }
            }
        }
        
        // Observe location state changes
        viewModelScope.launch {
            locationManager.locationState.collectLatest { state ->
                when (state) {
                    is LocationState.Success -> {
                        _uiState.value = LocationUiState.Success(
                            latitude = state.latitude,
                            longitude = state.longitude
                        )
                    }
                    is LocationState.Error -> {
                        _uiState.value = LocationUiState.Error(state.message)
                    }
                    is LocationState.PermissionRequired -> {
                        _uiState.value = LocationUiState.PermissionRequired
                    }
                    is LocationState.Loading -> {
                        _uiState.value = LocationUiState.Loading
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Update location data by requesting current location
     */
    fun updateLocation() {
        locationManager.getCurrentLocation()
    }
    
    /**
     * Called when permissions are granted
     */
    fun onPermissionsGranted() {
        updateLocation()
    }
    
    override fun onCleared() {
        super.onCleared()
        locationManager.stopLocationUpdates()
    }
}

/**
 * UI state for location data
 */
sealed class LocationUiState {
    object Loading : LocationUiState()
    object PermissionRequired : LocationUiState()
    data class Success(val latitude: Double, val longitude: Double) : LocationUiState()
    data class Error(val message: String) : LocationUiState()
}