package com.ecoruta.dispositivo.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.ecoruta.dispositivo.domain.system.TelemetryController
import com.ecoruta.dispositivo.platform.LocationPermission

/** Starts [TelemetryService], the foreground service that keeps the device reporting. */
class ForegroundTelemetryController(private val context: Context) : TelemetryController {

    override fun start() {
        // The UI holds the app behind a permission screen and calls start() again once granted.
        if (!LocationPermission.isGranted(context)) {
            Log.w(TAG, "Location permission missing: telemetry service not started")
            return
        }
        ContextCompat.startForegroundService(context, Intent(context, TelemetryService::class.java))
    }

    private companion object {
        const val TAG = "TelemetryController"
    }
}
