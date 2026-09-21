package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.DeviceSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    /** Emits the paired session, or null while the device has not been paired yet. */
    val session: Flow<DeviceSession?>

    suspend fun current(): DeviceSession?

    /** Exchanges a one-time pairing code for the device identity and stores it. */
    suspend fun pair(code: String): Outcome<DeviceSession>
}
