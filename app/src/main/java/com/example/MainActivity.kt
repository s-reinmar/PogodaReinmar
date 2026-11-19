package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.location.AndroidLocationProvider
import com.example.data.preferences.DataStoreFavoriteCitiesPreferences
import com.example.data.preferences.DataStoreLanguagePreferences
import com.example.data.repository.HybridWeatherRepositoryImpl
import com.example.presentation.WeatherViewModel
import com.example.ui.theme.PogodaPlTheme
import com.example.ui.weather.WeatherScreen

class MainActivity : ComponentActivity() {

    private val weatherRepository by lazy { HybridWeatherRepositoryImpl() }
    private val locationProvider by lazy { AndroidLocationProvider(applicationContext) }
    private val languagePreferences by lazy { DataStoreLanguagePreferences(applicationContext) }
    private val favoriteCitiesPreferences by lazy { DataStoreFavoriteCitiesPreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            PogodaPlTheme {
                val weatherViewModel: WeatherViewModel = viewModel(
                    factory = WeatherViewModel.Factory(
                        weatherRepository = weatherRepository,
                        locationGateway = locationProvider,
                        languagePreferences = languagePreferences,
                        favoriteCitiesGateway = favoriteCitiesPreferences,
                    ),
                )
                WeatherScreen(
                    viewModel = weatherViewModel,
                    hasLocationPermission = locationProvider::hasLocationPermission,
                )
            }
        }
    }
}
