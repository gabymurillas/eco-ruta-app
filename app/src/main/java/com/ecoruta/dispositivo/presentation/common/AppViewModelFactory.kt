package com.ecoruta.dispositivo.presentation.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.ecoruta.dispositivo.di.AppContainer
import com.ecoruta.dispositivo.presentation.MainViewModel
import com.ecoruta.dispositivo.presentation.idle.IdleViewModel
import com.ecoruta.dispositivo.presentation.incident.IncidentViewModel
import com.ecoruta.dispositivo.presentation.pairing.PairingViewModel
import com.ecoruta.dispositivo.presentation.trip.TripViewModel

/** Single place that knows how to build each ViewModel from the [AppContainer]. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = when (modelClass) {
        MainViewModel::class.java -> MainViewModel(container.observeDeviceState, container.resumeTelemetry)
        PairingViewModel::class.java -> PairingViewModel(container.pairDevice)
        IdleViewModel::class.java -> IdleViewModel(container.observeDeviceState, container.startTrip)
        TripViewModel::class.java -> TripViewModel(container.observeTripStatus, container.finishTrip)
        IncidentViewModel::class.java -> IncidentViewModel(container.reportIncident)
        else -> error("Unknown ViewModel class: ${modelClass.name}")
    } as T
}

val LocalViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("No ViewModelProvider.Factory provided")
}
