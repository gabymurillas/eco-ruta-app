package com.ecoruta.dispositivo.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ecoruta.dispositivo.domain.system.TelemetryController

/** Starts [TelemetryService], the foreground service that keeps the device reporting. */
class ForegroundTelemetryController(private val context: Context) : TelemetryController {

    override fun start() {
        ContextCompat.startForegroundService(context, Intent(context, TelemetryService::class.java))
    }
}
