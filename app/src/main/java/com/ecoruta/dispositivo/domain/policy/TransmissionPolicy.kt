package com.ecoruta.dispositivo.domain.policy

import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.repository.OperationParamsSource
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import java.time.Duration as JavaDuration

/**
 * Decides how often readings are transmitted and how good the GPS signal is.
 *
 * The receiver always runs at the fast [locationRequestInterval] so a stopped vehicle that starts
 * moving is noticed immediately; readings are then thinned out to the adaptive emission rate.
 * Parameters are read on every call so a refresh from the backend takes effect right away.
 */
class TransmissionPolicy(private val paramsSource: OperationParamsSource) {

    private val params get() = paramsSource.current

    val locationRequestInterval: Duration get() = params.movingInterval

    fun shouldRecord(fix: LocationFix, lastRecordedAt: Instant?): Boolean {
        if (lastRecordedAt == null) return true
        val elapsed = JavaDuration.between(lastRecordedAt, fix.capturedAt)
        return elapsed >= (intervalFor(fix.speedKmh) - EMISSION_TOLERANCE).toJavaDuration()
    }

    fun gpsStatusOf(fix: LocationFix?, now: Instant): GpsStatus = when {
        fix == null -> GpsStatus.NO_SIGNAL
        JavaDuration.between(fix.capturedAt, now) > params.gpsStaleAfter.toJavaDuration() -> GpsStatus.NO_SIGNAL
        fix.accuracyMeters > params.maxAccuracyMeters -> GpsStatus.WEAK
        else -> GpsStatus.GOOD
    }

    private fun intervalFor(speedKmh: Float): Duration =
        if (speedKmh >= params.movingSpeedThresholdKmh) params.movingInterval else params.stoppedInterval

    private companion object {
        /** Location providers may deliver a fix slightly earlier than the requested period. */
        val EMISSION_TOLERANCE = 500.milliseconds
    }
}
