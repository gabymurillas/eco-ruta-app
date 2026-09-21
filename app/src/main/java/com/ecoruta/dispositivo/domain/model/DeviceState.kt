package com.ecoruta.dispositivo.domain.model

/** Coarse lifecycle of the device; drives which screen is shown. */
sealed interface DeviceState {
    data object Unpaired : DeviceState
    data class Idle(val session: DeviceSession) : DeviceState
    data class OnTrip(val session: DeviceSession, val tripId: String) : DeviceState
}
