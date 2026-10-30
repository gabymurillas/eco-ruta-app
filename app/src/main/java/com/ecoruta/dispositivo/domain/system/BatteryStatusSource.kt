package com.ecoruta.dispositivo.domain.system

import com.ecoruta.dispositivo.domain.model.BatteryStatus
import kotlinx.coroutines.flow.Flow

/** Battery level and charging state, for the heartbeat and the trip screen. */
interface BatteryStatusSource {
    fun current(): BatteryStatus

    fun observe(): Flow<BatteryStatus>
}
