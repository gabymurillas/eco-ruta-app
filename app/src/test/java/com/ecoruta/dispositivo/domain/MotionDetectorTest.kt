package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.domain.model.GeoPoint
import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.model.OperationParams
import com.ecoruta.dispositivo.domain.policy.MotionDetector
import com.ecoruta.dispositivo.fakes.FixedParams
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MotionDetectorTest {

    private val detector = MotionDetector(FixedParams(OperationParams(movingSpeedThresholdKmh = 3f)))
    private val t0 = Instant.parse("2026-11-20T12:00:00Z")

    private fun fix(speedKmh: Float, at: Instant = t0) = LocationFix(GeoPoint(9.9, -67.35), speedKmh, 8f, at)

    @Test
    fun `a reading at or above the threshold means moving`() {
        assertTrue(detector.next(MotionDetector.State(), fix(3f), t0).isMoving)
    }

    @Test
    fun `a parked unit is not moving`() {
        assertFalse(detector.next(MotionDetector.State(), fix(0.5f), t0).isMoving)
    }

    @Test
    fun `slowing down only counts as stopped after a few seconds below the threshold`() {
        var state = detector.next(MotionDetector.State(), fix(30f), t0)

        state = detector.next(state, fix(1f, t0.plusSeconds(1)), t0.plusSeconds(1))
        assertTrue("a slow corner must not unlock it", state.isMoving)

        state = detector.next(state, fix(1f, t0.plusSeconds(6)), t0.plusSeconds(6))
        assertFalse(state.isMoving)
    }

    @Test
    fun `speeding up again cancels the stop countdown`() {
        var state = detector.next(MotionDetector.State(), fix(30f), t0)
        state = detector.next(state, fix(1f, t0.plusSeconds(1)), t0.plusSeconds(1))
        state = detector.next(state, fix(25f, t0.plusSeconds(2)), t0.plusSeconds(2))
        state = detector.next(state, fix(1f, t0.plusSeconds(5)), t0.plusSeconds(5))

        assertTrue("the countdown restarts at the second slow reading", state.isMoving)
    }

    @Test
    fun `with no recent reading it never locks the driver out`() {
        val moving = detector.next(MotionDetector.State(), fix(40f), t0)

        assertFalse(detector.next(moving, null, t0).isMoving)
        assertFalse(detector.next(moving, fix(40f), t0.plusSeconds(3_600)).isMoving)
    }
}
