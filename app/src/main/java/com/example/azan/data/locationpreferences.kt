package com.example.azan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for storing and retrieving location coordinates using DataStore
 */
class LocationPreferences(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("location_prefs")
        private val LATITUDE_KEY = doublePreferencesKey("latitude")
        private val LONGITUDE_KEY = doublePreferencesKey("longitude")
        private val COUNTRY_NAME_KEY = stringPreferencesKey("country_name")
        private val COUNTRY_KEY = stringPreferencesKey("country")
    }
    
    /**
     * Save location data
     */
    suspend fun saveLocation(latitude: Double, longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[LATITUDE_KEY] = latitude
            preferences[LONGITUDE_KEY] = longitude
        }
    }
    
    /**
     * Save location data with country name
     */
    suspend fun saveLocationWithCountry(latitude: Double, longitude: Double, country: String) {
        val validCountry = country.trim()
        android.util.Log.d("LocationPreferences", "Saving location with country: $validCountry")
        
        context.dataStore.edit { preferences ->
            preferences[LATITUDE_KEY] = latitude
            preferences[LONGITUDE_KEY] = longitude
            preferences[COUNTRY_KEY] = validCountry
            preferences[COUNTRY_NAME_KEY] = validCountry  // Also save to COUNTRY_NAME_KEY for compatibility
            
            // Log the saved values for debugging
            android.util.Log.d("LocationPreferences", "Saved values - Lat: $latitude, Lng: $longitude, Country: $validCountry")
        }
    }
    
    /**
     * Save country name to DataStore
     */
    suspend fun saveCountryName(countryName: String) {
        val validCountry = countryName.trim()
        android.util.Log.d("LocationPreferences", "Saving country name: $validCountry")
        
        context.dataStore.edit { preferences ->
            preferences[COUNTRY_NAME_KEY] = validCountry
            preferences[COUNTRY_KEY] = validCountry  // Also update COUNTRY_KEY for consistency
            android.util.Log.d("LocationPreferences", "Country name saved: $validCountry")
        }
    }
    
    /**
     * Get saved latitude as a Flow
     */
    val latitude: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[LATITUDE_KEY] ?: 0.0
    }
    
    /**
     * Get saved longitude as a Flow
     */
    val longitude: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[LONGITUDE_KEY] ?: 0.0
    }
    
    /**
     * Get saved country name as a Flow
     */
    val countryName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[COUNTRY_NAME_KEY]
    }
    
    /**
     * Check if location data exists
     */
    val hasLocationData: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences.contains(LATITUDE_KEY) && preferences.contains(LONGITUDE_KEY)
    }
}