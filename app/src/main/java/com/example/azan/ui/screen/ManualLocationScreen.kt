package com.example.azan.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.azan.R
import com.example.azan.ui.viewmodel.ManualLocationViewModel
import com.example.azan.ui.viewmodel.OperationResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionScreen(
    countries: List<String>,
    onCountrySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }
    
    val filteredCountries = if (searchText.isEmpty()) {
        countries
    } else {
        countries.filter { it.contains(searchText, ignoreCase = true) }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Country Selection Required",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "We couldn't automatically detect your country from the coordinates. Please select your country to continue.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Select Country") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredCountries.forEach { country ->
                        DropdownMenuItem(
                            text = { Text(country) },
                            onClick = {
                                selectedCountry = country
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLocationScreen(
    viewModel: ManualLocationViewModel,
    onLocationSaved: () -> Unit
) {
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var latitudeError by remember { mutableStateOf(false) }
    var longitudeError by remember { mutableStateOf(false) }
    
    // Get state from ViewModel
    val needCountrySelection by viewModel.needCountrySelection.collectAsState()
    val availableCountries by viewModel.availableCountries.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Handle operation results
    LaunchedEffect(operationResult) {
        operationResult?.let { result ->
            when (result) {
                is OperationResult.Success -> {
                    snackbarHostState.showSnackbar(message = result.message)
                    viewModel.clearOperationResult()
                    onLocationSaved()
                }
                is OperationResult.Error -> {
                    snackbarHostState.showSnackbar(message = result.message)
                    viewModel.clearOperationResult()
                }
                is OperationResult.CountrySelectionNeeded -> {
                    // This is handled by the UI state
                    viewModel.clearOperationResult()
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_location)) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (needCountrySelection) {
                // Country selection screen
                CountrySelectionScreen(
                    countries = availableCountries,
                    onCountrySelected = { country ->
                        viewModel.saveSelectedCountry(country)
                    }
                )
            } else {
                // Coordinate input screen
                Text(
                    text = stringResource(R.string.enter_manual_location),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Latitude input
                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { 
                        latitudeText = it
                        latitudeError = false
                    },
                    label = { Text(stringResource(R.string.enter_latitude)) },
                    placeholder = { Text(stringResource(R.string.latitude_hint)) },
                    isError = latitudeError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Longitude input
                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { 
                        longitudeText = it 
                        longitudeError = false
                    },
                    label = { Text(stringResource(R.string.enter_longitude)) },
                    placeholder = { Text(stringResource(R.string.longitude_hint)) },
                    isError = longitudeError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        val latitude = latitudeText.toDoubleOrNull()
                        val longitude = longitudeText.toDoubleOrNull()
                        
                        latitudeError = latitude == null || latitude < -90 || latitude > 90
                        longitudeError = longitude == null || longitude < -180 || longitude > 180
                        if (!latitudeError && !longitudeError && latitude != null && longitude != null) {
                            viewModel.saveManualLocation(latitude, longitude)
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message = "Invalid coordinates")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_location))
                }
            }
        }
    }
}