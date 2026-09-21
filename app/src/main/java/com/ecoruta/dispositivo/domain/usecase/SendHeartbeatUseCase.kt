package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.DeviceStatus
import com.ecoruta.dispositivo.domain.repository.DeviceStatusRepository
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.system.BatteryStatusSource

/** Reports the battery level so remote monitoring can tell the device is alive. */
class SendHeartbeatUseCase(
    private val sessions: SessionRepository,
    private val battery: BatteryStatusSource,
    private val statusRepository: DeviceStatusRepository,
) {
    suspend operator fun invoke(): Outcome<Unit> {
        // An unpaired device has no session token yet, so there is nothing to report.
        if (sessions.current() == null) return Outcome.Success(Unit)
        return statusRepository.report(DeviceStatus(battery.current().percent))
    }
}
