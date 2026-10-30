package com.ecoruta.dispositivo.domain.model

/**
 * Health signal so the admin panel can spot silent or low-battery units. The server timestamps it.
 * A null [batteryPercent] keeps the last known level: reporting 0 would fire a false low-battery alert.
 */
data class DeviceStatus(
    val batteryPercent: Int?,
)
