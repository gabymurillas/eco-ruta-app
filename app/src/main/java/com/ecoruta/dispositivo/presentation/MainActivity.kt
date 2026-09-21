package com.ecoruta.dispositivo.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ecoruta.dispositivo.presentation.theme.EcoRutaTheme

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as EcoRutaApplication).container }

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* re-asked next launch */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permissionRequest.launch(requiredPermissions())
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

    private fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()
}

@Composable
private fun AppRoot(viewModel: MainViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    Surface {
        // Nothing to show until the persisted state is known; avoids flashing the pairing screen.
        deviceState?.let { AppNavHost(it) }
    }
}
