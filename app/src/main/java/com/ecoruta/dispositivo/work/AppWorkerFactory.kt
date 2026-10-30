package com.ecoruta.dispositivo.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ecoruta.dispositivo.di.AppContainer

/** Gives workers their dependencies through their constructors instead of a service locator. */
class AppWorkerFactory(private val container: AppContainer) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        SyncWorker::class.java.name ->
            SyncWorker(appContext, workerParameters, container.syncPendingData)
        else -> null // fall back to the default reflective factory
    }
}
