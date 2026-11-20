package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.core.onSuccess
import com.ecoruta.dispositivo.domain.repository.RouteSignRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository

/**
 * Finishes the trip on the device at once, with or without network: the close is queued with the
 * real end time and delivered by [SyncPendingDataUseCase]. Capture stops by itself once no trip is
 * open; positions still queued are delivered too, and the server accepts them for a closed trip.
 *
 * A chosen route name lasts one trip, so the screen shows the usual route again at once; the server
 * clears it itself when the close arrives.
 */
class FinishTripUseCase(
    private val trips: TripRepository,
    private val signs: RouteSignRepository,
) {
    suspend operator fun invoke(): Outcome<Unit> = trips.finish().onSuccess { signs.resetLocal() }
}
