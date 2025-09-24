# Azan - Islamic Prayer Times App

## Overview
Azan is an Android application that calculates and displays Islamic prayer times using the [Adhan Kotlin library](https://github.com/batoulapps/adhan-kotlin). The app calculates prayer times for the next 60 days based on the user's location and country-specific calculation method, storing them in a local database for offline access.

## Features
- Calculates prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha) for the next 60 days
- Supports country-specific prayer calculation methods
- Stores prayer times in a local Room database for offline access
- Automatically recalculates prayer times every 40 days using WorkManager
- Supports local time zones and daylight savings time
- Modern UI built with Jetpack Compose

## Technical Details

### Libraries Used
- **Adhan Kotlin**: For accurate prayer time calculations
- **Jetpack Compose**: For modern, declarative UI
- **Room**: For local database storage
- **WorkManager**: For background prayer time updates
- **DataStore**: For storing user preferences
- **KotlinX DateTime**: For date and time handling

### Architecture
- **MVVM (Model-View-ViewModel)** architecture pattern
- **Repository Pattern** for data operations
- **Dependency Injection** for better testability

### Prayer Time Calculation
The app uses the [Adhan Kotlin library](https://github.com/batoulapps/adhan-kotlin) to calculate prayer times. Different countries use different calculation methods, which are stored in a JSON file (`country_prayer_methods.json`). The app allows users to select their country, which determines the calculation method used.

### Background Updates
The app uses WorkManager to schedule background updates of prayer times every 40 days, ensuring that the prayer times are always up-to-date even if the user doesn't open the app frequently.

## Setup and Installation
1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app on an Android device or emulator


## Acknowledgements
- [Adhan Kotlin](https://github.com/batoulapps/adhan-kotlin) for the prayer time calculation library
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for the modern UI toolkit
