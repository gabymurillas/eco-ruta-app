package com.ecoruta.dispositivo.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ecoruta.dispositivo.domain.system.SyncScheduler
import java.util.concurrent.TimeUnit

/** One unique job that waits for any network and retries with backoff; it survives reboots. */
class WorkManagerSyncScheduler(context: Context) : SyncScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // KEEP: if a sync is already pending or running, it will drain everything queued so far.
        workManager.enqueueUniqueWork(SYNC_WORK, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val SYNC_WORK = "telemetry-sync"
        const val BACKOFF_SECONDS = 30L
    }
}
