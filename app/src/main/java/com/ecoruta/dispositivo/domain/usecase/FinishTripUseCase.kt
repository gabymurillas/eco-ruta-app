package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository

/** Closes the trip. Capture stops by itself once no trip is open. */
class FinishTripUseCase(
    private val trips: TripRepository,
    private val telemetry: TelemetryRepository,
) {
    suspend operator fun invoke(): Outcome<Unit> {
        // Best effort: deliver what is queued before closing. Leftovers are still synced later.
        telemetry.sync()
        return trips.finish()
    }
}
