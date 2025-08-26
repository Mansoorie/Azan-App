package com.example.azan.data

import android.content.Context
import android.util.Log
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.data.DateComponents
import com.batoulapps.adhan2.PrayerTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone as JavaTimeZone



/**
 * Repository for prayer time calculations and storage
 */
class PrayerTimeRepository(private val context: Context) {
    
    private val prayerTimeDao = AzanDatabase.getDatabase(context).prayerTimeDao()
    private val TAG = "PrayerTimeRepository"
    
    /**
     * Get prayer times for today
     */
    fun getTodayPrayerTimes(): Flow<PrayerTime?> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val formattedDate = today.toString() // ISO-8601 format (YYYY-MM-DD)
        return prayerTimeDao.getTodayPrayerTime(formattedDate)
    }
    
    /**
     * Get prayer times for a specific date
     */
    suspend fun getPrayerTimeForDate(date: LocalDate): PrayerTime? {
        val formattedDate = date.toString() // ISO-8601 format (YYYY-MM-DD)
        return prayerTimeDao.getPrayerTimeForDate(formattedDate).first()
    }
    
    /**
     * Get all prayer times
     */
    fun getAllPrayerTimes(): Flow<List<PrayerTime>> {
        return prayerTimeDao.getAllPrayerTimes()
    }
    
    /**
     * Get list of available countries from the JSON file
     */
    suspend fun getAvailableCountries(): List<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("country_prayer_methods.json").bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonString)
            
            // Get all country names
            val countries = mutableListOf<String>()
            val keys = jsonObject.keys()
            
            while (keys.hasNext()) {
                val country = keys.next()
                countries.add(country)
            }
            
            // Sort countries alphabetically
            return@withContext countries.sorted()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading countries from JSON", e)
            // Return empty list if there's an error
            return@withContext emptyList()
        }
    }
    
    /**
     * Calculate and store prayer times for the next 60 days
     */
    suspend fun calculateAndStorePrayerTimes(
        latitude: Double,
        longitude: Double,
        countryName: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Validate country name
            val validCountryName = countryName.trim()
            Log.d(TAG, "Calculating prayer times for coordinates: $latitude, $longitude, country: $validCountryName")
            
            // Get the calculation method for the country
            val calculationMethod = getCalculationMethodForCountry(validCountryName)
            Log.d(TAG, "Using calculation method: $calculationMethod for country: $validCountryName")
            
            // Create coordinates
            val coordinates = Coordinates(latitude, longitude)
            
            // Get calculation parameters
            val parameters = getCalculationParameters(calculationMethod)
            Log.d(TAG, "Calculation parameters: $parameters")
            
            // Get current date
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            // Calculate prayer times for the next 60 days
            val prayerTimes = mutableListOf<PrayerTime>()
            
            for (i in 0 until 60) {
                // ✅ Correct usage: pass Int and use DateBased.Day
                val date = today.plus(i.toLong(), DateTimeUnit.DAY)

                val dateComponents = DateComponents(date.year, date.monthNumber, date.dayOfMonth)
                
                // Calculate prayer times using Adhan library
                val adhanPrayerTimes = PrayerTimes(coordinates, dateComponents, parameters)
                
                // Format times to 24-hour format
                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                formatter.timeZone = JavaTimeZone.getDefault()
                
                // Create PrayerTime entity
                val prayerTime = PrayerTime(
                    date = date.toString(),
                    fajr = formatter.format(adhanPrayerTimes.fajr.toDate()),
                    sunrise = formatter.format(adhanPrayerTimes.sunrise.toDate()),
                    dhuhr = formatter.format(adhanPrayerTimes.dhuhr.toDate()),
                    asr = formatter.format(adhanPrayerTimes.asr.toDate()),
                    maghrib = formatter.format(adhanPrayerTimes.maghrib.toDate()),
                    isha = formatter.format(adhanPrayerTimes.isha.toDate()),
                    calculationMethod = calculationMethod
                )
                
                prayerTimes.add(prayerTime)
            }
            
            // Store in database
            prayerTimeDao.insertPrayerTimes(prayerTimes)
            
            Log.d(TAG, "Stored ${prayerTimes.size} prayer times in the database")
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating prayer times", e)
            throw e
        }
    }
    
    /**
     * Get the calculation method for a country from the JSON file
     */
    private fun getCalculationMethodForCountry(countryName: String): String {
        return try {
            val jsonString = context.assets.open("country_prayer_methods.json").bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonString)
            
            // Normalize country name for comparison
            val normalizedCountryName = normalizeCountryName(countryName)
            Log.d(TAG, "Normalized country name: $normalizedCountryName from original: $countryName")
            
            // Show toast with detected country name
            showCountryToast(countryName)
            
            // Check if country exists in the JSON
            val method = if (jsonObject.has(normalizedCountryName)) {
                val calculationMethod = jsonObject.getString(normalizedCountryName)
                Log.d(TAG, "Found calculation method for $normalizedCountryName: $calculationMethod")
                calculationMethod
            } else {
                // Try to find a matching country name
                val keys = jsonObject.keys()
                var foundMatch = false
                var matchedCountry = ""
                var calculationMethod = "MUSLIM_WORLD_LEAGUE"
                
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (normalizeCountryName(key).equals(normalizedCountryName, ignoreCase = true)) {
                        calculationMethod = jsonObject.getString(key)
                        foundMatch = true
                        matchedCountry = key
                        break
                    }
                }
                
                if (foundMatch) {
                    Log.d(TAG, "Found matching country: $matchedCountry for $normalizedCountryName with method: $calculationMethod")
                    calculationMethod
                } else {
                    Log.w(TAG, "Country not found in prayer methods JSON: $normalizedCountryName, using default MUSLIM_WORLD_LEAGUE")
                    "MUSLIM_WORLD_LEAGUE" // Default if country not found
                }
            }
            
            method
        } catch (e: Exception) {
            Log.e(TAG, "Error reading country prayer methods", e)
            "MUSLIM_WORLD_LEAGUE" // Default
        }
    }
    
    /**
     * Normalize country name for better matching
     */
    private fun normalizeCountryName(countryName: String): String {
        val normalized = countryName.trim()
        
        // Handle special cases for common country name variations
        return when {
            normalized.equals("Republic of India", ignoreCase = true) -> "India"
            normalized.equals("United States of America", ignoreCase = true) -> "United States"
            normalized.equals("USA", ignoreCase = true) -> "United States"
            normalized.equals("UK", ignoreCase = true) -> "United Kingdom"
            normalized.equals("UAE", ignoreCase = true) -> "United Arab Emirates"
            normalized.equals("KSA", ignoreCase = true) -> "Saudi Arabia"
            normalized.contains("India", ignoreCase = true) -> "India"
            normalized.contains("United States", ignoreCase = true) -> "United States"
            normalized.contains("Saudi", ignoreCase = true) -> "Saudi Arabia"
            else -> normalized
        }
    }
    
    /**
     * Show toast with detected country name
     */
    private fun showCountryToast(countryName: String) {
        android.widget.Toast.makeText(
            context,
            "Detected Country: $countryName",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
    
    /**
     * Get calculation parameters based on the method name
     */
    private fun getCalculationParameters(methodName: String): CalculationParameters {
        return when (methodName) {
            "MUSLIM_WORLD_LEAGUE" -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            "EGYPTIAN" -> CalculationMethod.EGYPTIAN.parameters
            "KARACHI" -> CalculationMethod.KARACHI.parameters
            "UMM_AL_QURA" -> CalculationMethod.UMM_AL_QURA.parameters
            "DUBAI" -> CalculationMethod.DUBAI.parameters
            "QATAR" -> CalculationMethod.QATAR.parameters
            "KUWAIT" -> CalculationMethod.KUWAIT.parameters
            "SINGAPORE" -> CalculationMethod.SINGAPORE.parameters
            "NORTH_AMERICA" -> CalculationMethod.NORTH_AMERICA.parameters
            else -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        }
    }
    
    /**
     * Check if we need to recalculate prayer times
     * Returns true if there are no prayer times or if the newest entry is older than 40 days
     */
    suspend fun shouldRecalculatePrayerTimes(): Boolean = withContext(Dispatchers.IO) {
        val count = prayerTimeDao.countPrayerTimes()
        
        if (count == 0) {
            return@withContext true
        }
        
        val newestDate = prayerTimeDao.getNewestPrayerTimeDate() ?: return@withContext true
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        // Parse the newest date
        val parts = newestDate.split("-").map { it.toInt() }
        val newestLocalDate = LocalDate(parts[0], parts[1], parts[2])
        
        // Calculate days between
        val daysBetween = today.toEpochDays() - newestLocalDate.toEpochDays()
        
        // Recalculate if more than 40 days old
        return@withContext daysBetween >= 40
    }
}

// Extension function to convert kotlinx.datetime.Instant to java.util.Date
private fun kotlinx.datetime.Instant.toDate(): Date {
    return Date(this.toEpochMilliseconds())
}