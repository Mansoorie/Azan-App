package com.example.azan

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.azan.ui.LocationUiState
import com.example.azan.ui.LocationViewModel
import com.example.azan.ui.screen.ManualLocationScreen
import com.example.azan.ui.screen.PrayerTimesScreen
import com.example.azan.ui.theme.AzanTheme
import com.example.azan.ui.viewmodel.ManualLocationViewModel
import com.example.azan.worker.WorkManagerScheduler
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule background prayer time updates
        WorkManagerScheduler.schedulePrayerTimeUpdates(this)
        
        setContent {
            AzanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

/**
 * Main screen that handles navigation between different screens
 */
@Composable
fun MainScreen() {
    // Simple navigation state
    var currentScreen by remember { mutableStateOf(Screen.PrayerTimes) }
    
    // Initialize LocationViewModel once to ensure it starts requesting permissions if needed
    val locationViewModel: LocationViewModel = viewModel()
    val locationUiState by locationViewModel.uiState.collectAsState()
    
    // Check if we need to request location permissions on first launch
    LaunchedEffect(locationUiState) {
        when (locationUiState) {
            is LocationUiState.PermissionRequired -> {
                currentScreen = Screen.Location
            }
            is LocationUiState.Success -> {
                // If we have location data, go directly to prayer times screen
                currentScreen = Screen.PrayerTimes
            }
            else -> {}
        }
    }
    
    when (currentScreen) {
        Screen.PrayerTimes -> {
            PrayerTimesScreen(
                onNavigateToManualLocation = { currentScreen = Screen.ManualLocation }
            )
        }
        Screen.ManualLocation -> {
            val manualLocationViewModel: ManualLocationViewModel = viewModel()
            ManualLocationScreen(
                viewModel = manualLocationViewModel,
                onLocationSaved = { currentScreen = Screen.PrayerTimes }
            )
        }
        Screen.Location -> {
            LocationScreen(
                viewModel = locationViewModel,
                onBackClick = { currentScreen = Screen.PrayerTimes }
            )
        }
    }
}

/**
 * Enum representing different screens in the app
 */
enum class Screen {
    PrayerTimes,
    ManualLocation,
    Location
}

@Composable
fun LocationScreen(viewModel: LocationViewModel, onBackClick: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val locationPermissionsGranted = permissions.entries.any { it.value }
            if (locationPermissionsGranted) {
                viewModel.onPermissionsGranted()
            }
        }
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.location_coordinates),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        when (uiState) {
            is LocationUiState.Loading -> {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.getting_location),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            is LocationUiState.PermissionRequired -> {
                Text(
                    text = stringResource(R.string.permission_required),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                
                // Add more detailed explanation for Android 13+ users
                Text(
                    text = "For accurate prayer times, please allow precise location access. This app only uses your location to calculate prayer times and does not share this data.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Button(
                    onClick = {
                        // Request both permissions, but Android 13+ will handle them appropriately
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackClick) {
                    Text("Back")
                }
            }
            
            is LocationUiState.Success -> {
                val state = uiState as LocationUiState.Success
                
                CoordinatesCard(
                    latitude = state.latitude,
                    longitude = state.longitude,
                    onUpdateClick = { viewModel.updateLocation() }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackClick) {
                    Text("Back")
                }
            }
            
            is LocationUiState.Error -> {
                val state = uiState as LocationUiState.Error
                
                Text(
                    text = stringResource(R.string.error_format, state.message),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                
                Button(
                    onClick = { viewModel.updateLocation() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.try_again))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackClick) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
fun CoordinatesCard(
    latitude: Double,
    longitude: Double,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.current_location),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.latitude_format, String.format("%.6f", latitude)),
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.longitude_format, String.format("%.6f", longitude)),
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onUpdateClick) {
                Text(stringResource(R.string.update_location))
            }
        }
    }
}