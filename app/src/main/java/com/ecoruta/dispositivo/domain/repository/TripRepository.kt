package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    /** Id of the trip currently in progress, or null when the device is idle. */
    val activeTripId: Flow<String?>

    /**
     * Opens a trip for the unit. If the server says one is already open (typically this device's
     * own, whose creation response was lost), that trip is adopted and returned instead.
     */
    suspend fun start(unitId: String): Outcome<Trip>

    /** Closes the active trip. */
    suspend fun finish(): Outcome<Unit>
}
