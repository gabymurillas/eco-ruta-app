package com.ecoruta.dispositivo.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.EcoRutaApplication
import com.ecoruta.dispositivo.presentation.common.AppViewModelFactory
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.components.LocalThemeModeControl
import com.ecoruta.dispositivo.presentation.components.ThemeModeControl
import com.ecoruta.dispositivo.presentation.navigation.AppNavHost
import com.ecoruta.dispositivo.presentation.permission.LocationPermissionGate
import com.ecoruta.dispositivo.presentation.theme.EcoRutaTheme
import com.ecoruta.dispositivo.presentation.theme.ecoColors

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as EcoRutaApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container.kiosk.enter(this)

        val viewModelFactory = AppViewModelFactory(container)
        setContent {
            CompositionLocalProvider(LocalViewModelFactory provides viewModelFactory) {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: MainViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    EcoRutaTheme(themeMode) {
        CompositionLocalProvider(LocalThemeModeControl provides ThemeModeControl(themeMode, viewModel::onThemeModeChange)) {
            Box(Modifier.fillMaxSize().background(ecoColors.background)) {
                LocationPermissionGate(onGranted = viewModel::onLocationPermissionGranted) {
                    // Nothing to show until the persisted state is known; avoids flashing the pairing screen.
                    deviceState?.let { AppNavHost(it) }
                }
            }
        }
    }
}
