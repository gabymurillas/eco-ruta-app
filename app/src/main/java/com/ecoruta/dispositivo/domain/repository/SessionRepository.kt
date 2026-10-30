package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.DeviceSession
import kotlinx.coroutines.flow.Flow

/** The device's identity: which unit it is, and the credentials that prove it. */
interface SessionRepository {
    /** Emits the paired session, or null while the device has not been paired yet. */
    val session: Flow<DeviceSession?>

    suspend fun current(): DeviceSession?

    /** Unit of the last pairing, even if its session has since been revoked. */
    suspend fun lastPairedUnitId(): String?

    /** Exchanges a one-time pairing code for the device identity and stores it. */
    suspend fun pair(code: String): Outcome<DeviceSession>
}
