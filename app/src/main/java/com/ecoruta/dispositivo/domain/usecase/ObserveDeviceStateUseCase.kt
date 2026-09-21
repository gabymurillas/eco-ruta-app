package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveDeviceStateUseCase(
    private val sessions: SessionRepository,
    private val trips: TripRepository,
) {
    operator fun invoke(): Flow<DeviceState> =
        combine(sessions.session, trips.activeTripId) { session, tripId ->
            when {
                session == null -> DeviceState.Unpaired
                tripId == null -> DeviceState.Idle(session)
                else -> DeviceState.OnTrip(session, tripId)
            }
        }
}
