package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.system.ConnectivityObserver
import com.ecoruta.dispositivo.domain.system.LocationSettingsObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** What the driver must know before setting off: whether the unit could transmit right now. */
data class Readiness(
    val isOnline: Boolean,
    val isLocationEnabled: Boolean,
)

class ObserveReadinessUseCase(
    private val connectivity: ConnectivityObserver,
    private val locationSettings: LocationSettingsObserver,
) {
    operator fun invoke(): Flow<Readiness> =
        combine(connectivity.isOnline, locationSettings.isLocationEnabled, ::Readiness)
}
