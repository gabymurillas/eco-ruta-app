package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.domain.model.OperationParams
import com.ecoruta.dispositivo.domain.model.TransmissionPolicy
import com.ecoruta.dispositivo.fakes.FixedParams
import com.ecoruta.dispositivo.fakes.T0
import com.ecoruta.dispositivo.fakes.fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TransmissionPolicyTest {

    private val policy = TransmissionPolicy(FixedParams(OperationParams()))

    @Test
    fun `first reading is always recorded`() {
        assertTrue(policy.shouldRecord(fix(secondsAfterStart = 0), lastRecordedAt = null))
    }

    @Test
    fun `moving vehicle records every 5 seconds`() {
        val moving = 30f
        assertFalse(policy.shouldRecord(fix(2, moving), lastRecordedAt = T0))
        assertTrue(policy.shouldRecord(fix(5, moving), lastRecordedAt = T0))
    }

    @Test
    fun `stopped vehicle records every 30 seconds`() {
        val stopped = 0f
        assertFalse(policy.shouldRecord(fix(5, stopped), lastRecordedAt = T0))
        assertFalse(policy.shouldRecord(fix(29, stopped), lastRecordedAt = T0))
        assertTrue(policy.shouldRecord(fix(30, stopped), lastRecordedAt = T0))
    }

    @Test
    fun `a vehicle that starts moving is recorded without waiting for the stopped interval`() {
        assertTrue(policy.shouldRecord(fix(5, speedKmh = 25f), lastRecordedAt = T0))
    }

    @Test
    fun `gps has no signal without a reading`() {
        assertEquals(GpsStatus.NO_SIGNAL, policy.gpsStatusOf(fix = null, now = T0))
    }

    @Test
    fun `gps has no signal once the last reading is older than the signal-loss threshold`() {
        assertEquals(GpsStatus.GOOD, policy.gpsStatusOf(fix(0), now = T0.plusSeconds(299)))
        assertEquals(GpsStatus.NO_SIGNAL, policy.gpsStatusOf(fix(0), now = T0.plusSeconds(301)))
    }

    @Test
    fun `emission rate follows the parameters currently in force`() {
        val tuned = TransmissionPolicy(FixedParams(OperationParams(movingInterval = 10.seconds)))

        assertFalse(tuned.shouldRecord(fix(5, speedKmh = 30f), lastRecordedAt = T0))
        assertTrue(tuned.shouldRecord(fix(10, speedKmh = 30f), lastRecordedAt = T0))
    }

    @Test
    fun `gps is weak when accuracy exceeds the threshold`() {
        assertEquals(GpsStatus.WEAK, policy.gpsStatusOf(fix(0, accuracyMeters = 80f), now = T0.plusSeconds(1)))
    }

    @Test
    fun `gps is good with a fresh accurate reading`() {
        assertEquals(GpsStatus.GOOD, policy.gpsStatusOf(fix(0, accuracyMeters = 8f), now = T0.plusSeconds(1)))
    }
}
