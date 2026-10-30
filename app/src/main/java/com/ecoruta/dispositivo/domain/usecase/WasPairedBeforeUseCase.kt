package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.repository.SessionRepository

/**
 * True when the device shows the pairing screen because its pairing ended (the session was
 * revoked), not because it is new. The screen then explains why it is asking for a code again.
 */
class WasPairedBeforeUseCase(private val sessions: SessionRepository) {
    suspend operator fun invoke(): Boolean = sessions.current() == null && sessions.lastPairedUnitId() != null
}
