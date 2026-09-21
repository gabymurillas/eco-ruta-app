package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.system.TelemetryController

/** Makes sure the background service is running whenever the device is paired, e.g. after a reboot. */
class ResumeTelemetryUseCase(
    private val sessions: SessionRepository,
    private val telemetry: TelemetryController,
) {
    suspend operator fun invoke() {
        if (sessions.current() != null) telemetry.start()
    }
}
