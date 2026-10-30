package com.ecoruta.dispositivo.domain.model

import java.time.Instant

/** Aggregated live view of the running trip, consumed by the UI. */
data class TripStatus(
    val gps: GpsStatus = GpsStatus.NO_SIGNAL,
    val isOnline: Boolean = false,
    val isLocationEnabled: Boolean = true,
    val battery: BatteryStatus = BatteryStatus(percent = null, isCharging = false),
    val lastSentAt: Instant? = null,
    val pendingPositions: Int = 0,
)
