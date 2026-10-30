package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository

/**
 * Delivers everything the device stored while it could not: queued positions, then a queued trip
 * close. Positions go first so the trip's last readings are attributed to it before it closes.
 */
class SyncPendingDataUseCase(
    private val telemetry: TelemetryRepository,
    private val trips: TripRepository,
) {
    /** Returns a failure if anything is left to retry later. Both are attempted either way. */
    suspend operator fun invoke(): Outcome<Unit> {
        val positions = telemetry.sync()
        val close = trips.syncPendingClose()
        return positions as? Outcome.Failure ?: close
    }
}
