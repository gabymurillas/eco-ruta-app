package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.usecase.PairDeviceUseCase
import com.ecoruta.dispositivo.fakes.FakeSessionRepository
import com.ecoruta.dispositivo.fakes.FakeTelemetryController
import com.ecoruta.dispositivo.fakes.FakeTelemetryRepository
import com.ecoruta.dispositivo.fakes.FakeTripRepository
import com.ecoruta.dispositivo.fakes.TEST_SESSION
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairDeviceUseCaseTest {

    private var sessions = FakeSessionRepository(stored = null)
    private val trips = FakeTripRepository()
    private val telemetry = FakeTelemetryRepository()
    private val controller = FakeTelemetryController()

    private fun pairDevice() = PairDeviceUseCase(sessions, trips, telemetry, controller)

    @Test
    fun `blank code is rejected without calling the backend`() = runTest {
        val result = pairDevice()("   ")

        assertEquals(Outcome.Failure(AppError.InvalidPairingCode), result)
        assertNull(sessions.pairedWithCode)
        assertEquals(0, controller.startCount)
    }

    @Test
    fun `code with illegal characters is rejected`() = runTest {
        val result = pairDevice()("AB C!")

        assertEquals(Outcome.Failure(AppError.InvalidPairingCode), result)
        assertNull(sessions.pairedWithCode)
    }

    @Test
    fun `code is trimmed and upper-cased before pairing`() = runTest {
        val result = pairDevice()("  ab12-cd  ")

        assertTrue(result is Outcome.Success)
        assertEquals("AB12-CD", sessions.pairedWithCode)
    }

    @Test
    fun `pairing starts the service so the first heartbeat goes out at once`() = runTest {
        pairDevice()("4T9K2")

        assertEquals(1, controller.startCount)
    }

    @Test
    fun `re-pairing to the same unit keeps its open trip and queued positions`() = runTest {
        sessions = FakeSessionRepository(stored = null, lastUnitId = TEST_SESSION.unitId)

        pairDevice()("4T9K2")

        assertFalse(trips.forgotten)
        assertFalse(telemetry.discarded)
    }

    @Test
    fun `moving to another unit drops the old unit's trip and queued positions`() = runTest {
        sessions = FakeSessionRepository(stored = null, lastUnitId = "another-unit")

        pairDevice()("4T9K2")

        assertTrue(trips.forgotten)
        assertTrue(telemetry.discarded)
    }
}
