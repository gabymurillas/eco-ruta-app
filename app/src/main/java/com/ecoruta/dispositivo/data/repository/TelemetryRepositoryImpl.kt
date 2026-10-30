package com.ecoruta.dispositivo.data.repository

import android.util.Log
import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.local.db.PendingPositionDao
import com.ecoruta.dispositivo.data.local.db.PendingPositionEntity
import com.ecoruta.dispositivo.data.mapper.toEntity
import com.ecoruta.dispositivo.data.mapper.toRegisterRequest
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.safeApiCall
import com.ecoruta.dispositivo.domain.model.Position
import com.ecoruta.dispositivo.domain.repository.OperationParamsSource
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository
import com.ecoruta.dispositivo.domain.system.ConnectivityObserver
import com.ecoruta.dispositivo.domain.system.SyncScheduler
import com.ecoruta.dispositivo.domain.system.TelemetryStatusStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import kotlin.time.toJavaDuration

/**
 * Outbox pattern: every position is written to the local queue first, so nothing is lost when the
 * network drops. Delivery is attempted right away when online and otherwise deferred to WorkManager.
 */
class TelemetryRepositoryImpl(
    private val dao: PendingPositionDao,
    private val api: SupabaseRestApi,
    private val connectivity: ConnectivityObserver,
    private val scheduler: SyncScheduler,
    private val statusStore: TelemetryStatusStore,
    private val paramsSource: OperationParamsSource,
    private val clock: Clock,
    private val appScope: CoroutineScope,
) : TelemetryRepository {

    private val uploadLock = Mutex()

    override val pendingCount: Flow<Int> = dao.observeCount()

    override suspend fun enqueue(position: Position) {
        dao.insert(position.toEntity())
        dao.deleteCapturedBefore(retentionThresholdMs())
        // Delivery must not delay the next capture, so it runs outside the caller's coroutine.
        appScope.launch { deliverOrDefer() }
    }

    override suspend fun sync(): Outcome<Unit> = uploadLock.withLock { uploadPending() }

    override suspend fun discardQueued() = uploadLock.withLock { dao.deleteAll() }

    private suspend fun deliverOrDefer() {
        // If an upload is already draining the queue, it will pick up the new row too.
        if (!uploadLock.tryLock()) return
        try {
            val delivered = connectivity.isOnline.value && uploadPending() is Outcome.Success
            if (!delivered) scheduler.scheduleSync()
        } finally {
            uploadLock.unlock()
        }
    }

    /**
     * Drains the queue, one request per trip, oldest trip first. Rows are deleted only once the
     * server acknowledged them: a 5xx means nothing was saved, so it stops here and retries later.
     */
    private suspend fun uploadPending(): Outcome<Unit> {
        while (true) {
            dao.deleteCapturedBefore(retentionThresholdMs())
            val batch = nextTripBatch()
            if (batch.isEmpty()) return Outcome.Success(Unit)

            val request = toRegisterRequest(batch.first().tripId, batch)
            when (val result = safeApiCall { api.registerPositions(request) }) {
                is Outcome.Success -> statusStore.onSent(clock.instant())
                is Outcome.Failure -> {
                    // A 4xx (other than auth) means retrying can never succeed; drop the batch so
                    // one bad row cannot block the whole queue.
                    val error = result.error as? AppError.Rejected ?: return result
                    logDroppedBatch(batch.size, error)
                }
            }
            dao.deleteByIds(batch.map { it.id })
        }
    }

    /**
     * The oldest trip's queued readings. They go out together on purpose: the server evaluates
     * geofences only against the newest reading of each request, so splitting a backlog would make
     * every older slice evaluate a position the unit has already left behind.
     */
    private suspend fun nextTripBatch(): List<PendingPositionEntity> {
        val queued = dao.oldest(paramsSource.current.syncBatchSize)
        val tripId = queued.firstOrNull()?.tripId ?: return emptyList()
        return queued.takeWhile { it.tripId == tripId }
    }

    /**
     * A 403 means RLS refused the unit: the token does not match the unit, so the pairing is broken
     * and the device must be paired again. It is not a bad reading, so it must not read like one.
     */
    private fun logDroppedBatch(size: Int, error: AppError.Rejected) {
        if (error.httpCode == HTTP_FORBIDDEN) {
            Log.e(TAG, "Backend refused $size positions for this unit (403): the device needs re-pairing")
        } else {
            Log.w(TAG, "Dropping $size positions rejected by backend (${error.httpCode})")
        }
    }

    private fun retentionThresholdMs(): Long =
        clock.instant().minus(paramsSource.current.bufferRetention.toJavaDuration()).toEpochMilli()

    private companion object {
        const val TAG = "TelemetryRepository"
        const val HTTP_FORBIDDEN = 403
    }
}
