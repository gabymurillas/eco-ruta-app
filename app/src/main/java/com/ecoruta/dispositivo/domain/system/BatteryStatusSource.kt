package com.ecoruta.dispositivo.domain.system

import com.ecoruta.dispositivo.domain.model.BatteryStatus
import kotlinx.coroutines.flow.Flow

interface BatteryStatusSource {
    fun current(): BatteryStatus

    fun observe(): Flow<BatteryStatus>
}
