package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Trip
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository

/** Opens a trip. Capture starts by itself: the running service watches for an open trip. */
class StartTripUseCase(
    private val sessions: SessionRepository,
    private val trips: TripRepository,
) {
    suspend operator fun invoke(): Outcome<Trip> {
        val session = sessions.current() ?: return Outcome.Failure(AppError.NotPaired)
        return trips.start(session.unitId)
    }
}
