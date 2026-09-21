package com.ecoruta.dispositivo.domain.system

/** Starts the background component that keeps the device reporting for as long as it is paired. */
interface TelemetryController {
    /** Idempotent: calling it while already running has no effect. */
    fun start()
}
