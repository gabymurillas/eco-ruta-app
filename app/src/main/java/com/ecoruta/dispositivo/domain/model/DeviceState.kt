package com.ecoruta.dispositivo.domain.model

/** Coarse lifecycle of the device; drives which screen is shown. */
sealed interface DeviceState {
    /** [firstTime]: never paired before, so it opens on the welcome screen instead of the code. */
    data class Unpaired(val firstTime: Boolean) : DeviceState
    data class Idle(val session: DeviceSession) : DeviceState
    data class OnTrip(val session: DeviceSession, val tripId: String) : DeviceState
}
