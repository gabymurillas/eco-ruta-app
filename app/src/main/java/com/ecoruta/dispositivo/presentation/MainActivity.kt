package com.ecoruta.dispositivo.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.EcoRutaApplication
import com.ecoruta.dispositivo.presentation.common.AppViewModelFactory
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.navigation.AppNavHost
import com.ecoruta.dispositivo.presentation.permission.LocationPermissionGate
import com.ecoruta.dispositivo.presentation.theme.EcoRutaTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as EcoRutaApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container.kiosk.enter(this)

        val viewModelFactory = AppViewModelFactory(container)
        setContent {
            EcoRutaTheme {
                CompositionLocalProvider(LocalViewModelFactory provides viewModelFactory) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: MainViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    Surface {
        LocationPermissionGate(onGranted = viewModel::onLocationPermissionGranted) {
            // Nothing to show until the persisted state is known; avoids flashing the pairing screen.
            deviceState?.let { AppNavHost(it) }
        }
    }
}
