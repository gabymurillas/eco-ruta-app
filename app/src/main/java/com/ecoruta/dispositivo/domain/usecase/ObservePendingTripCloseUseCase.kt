package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow

/**
 * True while the last trip was finished on the device but its close has not reached the server.
 * The driver sees it on the idle screen: a new trip cannot open until that close goes out.
 */
class ObservePendingTripCloseUseCase(private val trips: TripRepository) {
    operator fun invoke(): Flow<Boolean> = trips.hasPendingClose
}
