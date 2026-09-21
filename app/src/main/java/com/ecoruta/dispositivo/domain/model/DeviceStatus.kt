package com.ecoruta.dispositivo.domain.model

import java.time.Instant

data class BatteryStatus(
    val percent: Int,
    val isCharging: Boolean,
)

/** Health signal so the admin panel can spot silent or low-battery units. The server timestamps it. */
data class DeviceStatus(
    val batteryPercent: Int,
)

enum class GpsStatus { NO_SIGNAL, WEAK, GOOD }

/** Aggregated live view of the running trip, consumed by the UI. */
data class TripStatus(
    val gps: GpsStatus = GpsStatus.NO_SIGNAL,
    val isOnline: Boolean = false,
    val battery: BatteryStatus = BatteryStatus(percent = 0, isCharging = false),
    val lastSentAt: Instant? = null,
    val pendingPositions: Int = 0,
)
