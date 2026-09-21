package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    /** Id of the trip currently in progress, or null when the device is idle. */
    val activeTripId: Flow<String?>

    /** Opens a trip for the unit. Fails with `Conflict` if the unit already has an open trip. */
    suspend fun start(unitId: String): Outcome<Trip>

    /** Closes the active trip. */
    suspend fun finish(): Outcome<Unit>
}
