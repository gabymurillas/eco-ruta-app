package com.ecoruta.dispositivo.domain.policy

import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.repository.OperationParamsSource
import java.time.Duration
import java.time.Instant
import kotlin.time.toJavaDuration

/**
 * Whether the unit is moving, to lock controls the driver should not use while driving.
 *
 * It starts moving at the first reading at or above `movimiento_umbral_kmh`, and only counts as
 * stopped after [STOP_CONFIRM] below it, so the lock does not flicker at every slow corner. With no
 * recent reading it is not considered moving: a GPS without signal must not lock the driver out.
 */
class MotionDetector(private val paramsSource: OperationParamsSource) {

    data class State(val isMoving: Boolean = false, val slowSince: Instant? = null)

    fun next(state: State, fix: LocationFix?, now: Instant): State {
        val params = paramsSource.current
        val stale = fix == null || Duration.between(fix.capturedAt, now) > params.gpsStaleAfter.toJavaDuration()
        return when {
            stale -> State()
            fix.speedKmh >= params.movingSpeedThresholdKmh -> State(isMoving = true)
            !state.isMoving -> State()
            else -> {
                val slowSince = state.slowSince ?: fix.capturedAt
                if (Duration.between(slowSince, now) >= STOP_CONFIRM) State() else State(isMoving = true, slowSince = slowSince)
            }
        }
    }

    companion object {
        /** How long below the threshold before the unit counts as stopped. */
        val STOP_CONFIRM: Duration = Duration.ofSeconds(4)
    }
}
