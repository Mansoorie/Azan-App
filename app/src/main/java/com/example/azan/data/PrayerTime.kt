package com.example.azan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Entity representing a prayer time in the database
 */
@Entity(tableName = "prayer_times")
data class PrayerTime(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // The date for this prayer time
    val date: String, // ISO-8601 format (YYYY-MM-DD)
    
    // Prayer times in 24-hour format (HH:MM)
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    
    // The calculation method used
    val calculationMethod: String,
    
    // When this entry was created/updated
    val lastUpdated: Long = System.currentTimeMillis()
)