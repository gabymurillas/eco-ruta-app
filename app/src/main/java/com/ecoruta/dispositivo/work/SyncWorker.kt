package com.ecoruta.dispositivo.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.usecase.SyncPendingDataUseCase

/**
 * Delivers queued positions and trip closes once the network is back, retrying with backoff until
 * it succeeds.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncPendingData: SyncPendingDataUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (syncPendingData()) {
        is Outcome.Success -> Result.success()
        is Outcome.Failure -> Result.retry()
    }
}
