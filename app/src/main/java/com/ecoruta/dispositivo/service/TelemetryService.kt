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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Foreground service (type `location`) that keeps telemetry running with the screen off.
 * The persistent notification is required by Android and doubles as proof the device is working.
 */
class TelemetryService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sessionJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this,
                TelemetryNotification.ID,
                TelemetryNotification.build(this),
                FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } catch (e: SecurityException) {
            // Location permission revoked while the service was scheduled; crashing would only
            // loop. The app restarts the service once the permission screen gets it back.
            return giveUp(e)
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException: a start from the background was refused.
            return giveUp(e)
        }
        if (sessionJob?.isActive != true) sessionJob = scope.launch { runSession() }
        // If the system kills the process mid-trip, restart so capture resumes.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun giveUp(cause: Exception): Int {
        Log.e(TAG, "Could not start the telemetry service in the foreground", cause)
        stopSelf()
        return START_NOT_STICKY
    }

    /**
     * The session only ever ends by cancellation. Anything else is a bug, and stopping here would
     * silence the unit for good (in kiosk mode nothing reopens the app to start it again), so it
     * is logged and the session starts over.
     */
    private suspend fun runSession() {
        val container = (application as EcoRutaApplication).container
        while (true) {
            try {
                container.runTelemetrySession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Telemetry session failed; restarting", e)
            }
            delay(RESTART_DELAY)
        }
    }

    private companion object {
        const val TAG = "TelemetryService"
        val RESTART_DELAY = 10.seconds

        // Compile-time constant, inlined by the compiler, so it is safe on API < 29; ServiceCompat
        // ignores the type on those versions.
        @SuppressLint("InlinedApi")
        const val FOREGROUND_SERVICE_TYPE_LOCATION = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
    }
}
