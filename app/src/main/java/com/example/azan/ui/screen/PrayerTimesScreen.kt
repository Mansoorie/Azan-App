package com.example.azan.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.azan.data.PrayerTime
import com.example.azan.ui.viewmodel.PrayerTimeViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(onNavigateToManualLocation: () -> Unit = {}) {
    val prayerTimeViewModel: PrayerTimeViewModel = viewModel()
    val todayPrayerTimes by prayerTimeViewModel.todayPrayerTimes.observeAsState()
    val isLoading by prayerTimeViewModel.isLoading.observeAsState(false)
    val errorMessage by prayerTimeViewModel.errorMessage.observeAsState()
    val availableCountries by prayerTimeViewModel.availableCountries.observeAsState(emptyList())
    val selectedCountry by prayerTimeViewModel.selectedCountry.observeAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error message in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            prayerTimeViewModel.clearErrorMessage()
        }
    }
    
    // Load today's prayer times
    LaunchedEffect(Unit) {
        prayerTimeViewModel.loadTodayPrayerTimes()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Azan - Prayer Times") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Only show country selection if no prayer times are available or no country is selected
            if (todayPrayerTimes == null || selectedCountry == null) {
                CountrySelectionDropdown(
                    countries = availableCountries,
                    selectedCountry = selectedCountry,
                    onCountrySelected = { prayerTimeViewModel.setCountry(it) }
                )
            } else {
                // Show selected country
                Text(
                    text = "Selected Country: $selectedCountry",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Manual location input button
            androidx.compose.material3.Button(
                onClick = onNavigateToManualLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enter Location Manually")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Today's date
            Text(
                text = "Today: ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                // Loading indicator
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (todayPrayerTimes != null) {
                // Prayer times display
                PrayerTimesDisplay(prayerTime = todayPrayerTimes!!)
            } else {
                // No prayer times available
                Text(
                    text = "No prayer times available. Please select your country and ensure location is enabled.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionDropdown(
    countries: List<String>,
    selectedCountry: String?,
    onCountrySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf(selectedCountry ?: "") }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select Your Country",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("Enter your country name") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            
            val filteredCountries = countries.filter {
                it.contains(searchText, ignoreCase = true)
            }
            
            if (filteredCountries.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filteredCountries.forEach { country ->
                        DropdownMenuItem(
                            text = { Text(country) },
                            onClick = {
                                searchText = country
                                expanded = false
                                onCountrySelected(country)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerTimesDisplay(prayerTime: PrayerTime) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prayer times cards
        PrayerTimeCard(name = "Fajr", time = prayerTime.fajr)
        PrayerTimeCard(name = "Sunrise", time = prayerTime.sunrise)
        PrayerTimeCard(name = "Dhuhr", time = prayerTime.dhuhr)
        PrayerTimeCard(name = "Asr", time = prayerTime.asr)
        PrayerTimeCard(name = "Maghrib", time = prayerTime.maghrib)
        PrayerTimeCard(name = "Isha", time = prayerTime.isha)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calculation method info
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = "Calculation Method: ${formatCalculationMethod(prayerTime.calculationMethod)}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PrayerTimeCard(name: String, time: String) {
    val formattedTime = try {
        LocalTime.parse(time).format(DateTimeFormatter.ofPattern("hh:mm a"))
    } catch (e: Exception) {
        time
    }
    
    val isCurrentPrayer = isCurrentPrayerTime(name, time)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = if (isCurrentPrayer) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isCurrentPrayer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = formattedTime,
                fontSize = 18.sp,
                color = if (isCurrentPrayer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Check if this is the current prayer time
 * 
 * This simplified implementation just checks if the current time is within 30 minutes after the prayer time
 */
fun isCurrentPrayerTime(name: String, timeString: String): Boolean {
    try {
        val now = LocalTime.now()
        val prayerTime = LocalTime.parse(timeString)
        
        // Highlight if current time is within 30 minutes after prayer time
        val thirtyMinutesAfterPrayer = prayerTime.plusMinutes(30)
        return now.isAfter(prayerTime) && now.isBefore(thirtyMinutesAfterPrayer)
    } catch (e: Exception) {
        return false
    }
}

/**
 * Format calculation method name for display
 */
fun formatCalculationMethod(method: String): String {
    return method.replace("_", " ").lowercase().split(" ").joinToString(" ") { it.capitalize() }
}

/**
 * Extension function to capitalize first letter of a string
 */
fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this[0].uppercase() + this.substring(1)
    } else {
        this
    }
}