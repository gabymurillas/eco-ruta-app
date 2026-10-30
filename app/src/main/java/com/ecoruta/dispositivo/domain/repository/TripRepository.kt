package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    /** Id of the trip currently in progress, or null when the device is idle. */
    val activeTripId: Flow<String?>

    /** True while a trip finished on the device has not been closed on the server yet. */
    val hasPendingClose: Flow<Boolean>

    /**
     * Opens a trip for the unit. If the server says one is already open (typically this device's
     * own, whose creation response was lost), that trip is adopted and returned instead.
     *
     * A pending close is delivered first: until the server closes the previous trip, the unit
     * cannot open another. If it cannot be delivered (offline), the new trip is not opened.
     */
    suspend fun start(unitId: String): Outcome<Trip>

    /**
     * Finishes the active trip on the device right away, recording the current moment as its real
     * end. Never fails for lack of network: the close is queued and sent when connectivity allows,
     * with that recorded time, not the time it happened to be sent.
     */
    suspend fun finish(): Outcome<Unit>

    /** Sends a queued close, if any. Returns a failure if it must be retried later. */
    suspend fun syncPendingClose(): Outcome<Unit>

    /** Drops the open trip and any queued close; used when the device moves to another unit. */
    suspend fun forget()
}
