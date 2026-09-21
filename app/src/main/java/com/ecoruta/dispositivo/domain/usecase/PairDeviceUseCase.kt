package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.core.onSuccess
import com.ecoruta.dispositivo.domain.model.DeviceSession
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.system.TelemetryController

class PairDeviceUseCase(
    private val sessions: SessionRepository,
    private val telemetry: TelemetryController,
) {
    suspend operator fun invoke(rawCode: String): Outcome<DeviceSession> {
        val code = rawCode.trim().uppercase()
        if (!PAIRING_CODE.matches(code)) return Outcome.Failure(AppError.InvalidPairingCode)
        // Starting the service sends the first heartbeat right away, so the panel never shows a
        // freshly paired unit as down.
        return sessions.pair(code).onSuccess { telemetry.start() }
    }

    private companion object {
        val PAIRING_CODE = Regex("[A-Z0-9-]+")
    }
}
