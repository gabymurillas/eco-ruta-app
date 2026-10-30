package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Unpaired, idle or on a trip, derived from persisted state only: it drives which screen shows,
 * so a reboot lands on the right one with nobody touching the phone.
 */
class ObserveDeviceStateUseCase(
    private val sessions: SessionRepository,
    private val trips: TripRepository,
) {
    operator fun invoke(): Flow<DeviceState> =
        combine(sessions.session, trips.activeTripId) { session, tripId ->
            when {
                session == null -> DeviceState.Unpaired(firstTime = sessions.lastPairedUnitId() == null)
                tripId == null -> DeviceState.Idle(session)
                else -> DeviceState.OnTrip(session, tripId)
            }
        }
}
