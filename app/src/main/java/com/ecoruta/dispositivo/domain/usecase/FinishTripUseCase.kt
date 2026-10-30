package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.repository.TripRepository

/**
 * Finishes the trip on the device at once, with or without network: the close is queued with the
 * real end time and delivered by [SyncPendingDataUseCase]. Capture stops by itself once no trip is
 * open; positions still queued are delivered too, and the server accepts them for a closed trip.
 */
class FinishTripUseCase(private val trips: TripRepository) {
    suspend operator fun invoke(): Outcome<Unit> = trips.finish()
}
