package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Position
import kotlinx.coroutines.flow.Flow

/** The offline-first position queue (outbox): positions are stored first and delivered after. */
interface TelemetryRepository {
    /** Number of positions stored locally and not yet delivered. */
    val pendingCount: Flow<Int>

    /**
     * Stores the position durably and attempts delivery in the background.
     * Never fails because of connectivity: undelivered positions stay queued.
     */
    suspend fun enqueue(position: Position)

    /** Uploads everything queued. Returns a failure if delivery must be retried later. */
    suspend fun sync(): Outcome<Unit>

    /** Drops every queued position; used when the device moves to another unit. */
    suspend fun discardQueued()
}
