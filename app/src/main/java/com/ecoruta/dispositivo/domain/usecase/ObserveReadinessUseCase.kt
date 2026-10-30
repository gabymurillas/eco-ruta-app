package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.model.Readiness
import com.ecoruta.dispositivo.domain.system.ConnectivityObserver
import com.ecoruta.dispositivo.domain.system.LocationSettingsObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Connectivity and location settings combined, for the idle screen before a trip starts. */
class ObserveReadinessUseCase(
    private val connectivity: ConnectivityObserver,
    private val locationSettings: LocationSettingsObserver,
) {
    operator fun invoke(): Flow<Readiness> =
        combine(connectivity.isOnline, locationSettings.isLocationEnabled, ::Readiness)
}
