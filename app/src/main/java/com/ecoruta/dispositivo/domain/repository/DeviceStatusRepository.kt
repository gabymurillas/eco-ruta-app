package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.DeviceStatus

/** Where the heartbeat goes. */
interface DeviceStatusRepository {
    /** Reports the device as alive. The backend identifies the unit from the session token. */
    suspend fun report(status: DeviceStatus): Outcome<Unit>
}
