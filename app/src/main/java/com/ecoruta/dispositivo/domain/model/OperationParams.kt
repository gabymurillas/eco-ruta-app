package com.ecoruta.dispositivo.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Operating parameters of the device.
 *
 * The calibrated values (emission intervals, accuracy threshold, signal-loss threshold) are owned
 * by the backend `parametros` table so the device never drifts from the server that evaluates
 * geofences. The defaults here are only the fallback used until they have been fetched once.
 */
data class OperationParams(
    /** `emision_movimiento_s`: emission period while the vehicle is moving. */
    val movingInterval: Duration = 5.seconds,
    /** `emision_detenido_s`: emission period while the vehicle is stopped. */
    val stoppedInterval: Duration = 30.seconds,
    /** `exactitud_maxima_m`: readings less accurate than this are flagged as weak signal. */
    val maxAccuracyMeters: Float = 50f,
    /** `sin_senal_umbral_s`: a GPS fix older than this counts as "no signal". */
    val gpsStaleAfter: Duration = 300.seconds,

    // Device-local settings, not calibrated on the server.
    /** Speed at or above which the vehicle is considered to be moving. */
    val movingSpeedThresholdKmh: Float = 3f,
    /** How long unsent positions are kept while offline. */
    val bufferRetention: Duration = 2.hours,
    /**
     * Max positions per upload request. The server evaluates geofences only against the newest
     * reading of each request, so a backlog must go out in one call; this is just a ceiling above
     * what the retention window can hold (2 h at 5 s is 1,440 readings).
     */
    val syncBatchSize: Int = 2_000,
)
