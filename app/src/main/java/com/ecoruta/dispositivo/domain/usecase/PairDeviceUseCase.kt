package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.core.onSuccess
import com.ecoruta.dispositivo.domain.model.DeviceSession
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import com.ecoruta.dispositivo.domain.system.TelemetryController

/** Pairs the device with a one-time code from the admin panel and starts reporting at once. */
class PairDeviceUseCase(
    private val sessions: SessionRepository,
    private val trips: TripRepository,
    private val telemetry: TelemetryRepository,
    private val controller: TelemetryController,
) {
    suspend operator fun invoke(rawCode: String): Outcome<DeviceSession> {
        val code = rawCode.trim().uppercase()
        if (!PAIRING_CODE.matches(code)) return Outcome.Failure(AppError.InvalidPairingCode)
        val previousUnitId = sessions.lastPairedUnitId()
        return sessions.pair(code).onSuccess { session ->
            // Re-paired to the same unit (its session was revoked): pick up the trip and the
            // queue where they were. Moved to another unit: they belong to the old one, and
            // sending them with the new token would attribute them to the wrong vehicle.
            if (previousUnitId != null && previousUnitId != session.unitId) {
                trips.forget()
                telemetry.discardQueued()
            }
            // Starting the service sends the first heartbeat right away, so the panel never shows
            // a freshly paired unit as down.
            controller.start()
        }
    }

    private companion object {
        val PAIRING_CODE = Regex("[A-Z0-9-]+")
    }
}
