package com.ecoruta.dispositivo.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.ecoruta.dispositivo.EcoRutaApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service (type `location`) that keeps telemetry running with the screen off.
 * The persistent notification is required by Android and doubles as proof the device is working.
 */
class TelemetryService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            TelemetryNotification.ID,
            TelemetryNotification.build(this),
            FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        if (sessionJob?.isActive != true) sessionJob = scope.launch { runSession() }
        // If the system kills the process mid-trip, restart so capture resumes.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runSession() {
        val container = (application as EcoRutaApplication).container
        try {
            container.runTelemetrySession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Telemetry session failed", e)
            stopSelf()
        }
    }

    private companion object {
        const val TAG = "TelemetryService"

        // Compile-time constant, inlined by the compiler, so it is safe on API < 29; ServiceCompat
        // ignores the type on those versions.
        @SuppressLint("InlinedApi")
        const val FOREGROUND_SERVICE_TYPE_LOCATION = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
    }
}
