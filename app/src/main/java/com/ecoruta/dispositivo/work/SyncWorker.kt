package com.ecoruta.dispositivo.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository

/** Delivers queued positions once the network is back, retrying with backoff until it succeeds. */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val telemetry: TelemetryRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (telemetry.sync()) {
        is Outcome.Success -> Result.success()
        is Outcome.Failure -> Result.retry()
    }
}
