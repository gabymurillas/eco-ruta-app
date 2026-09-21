package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.data.system.InMemoryTelemetryStatusStore
import com.ecoruta.dispositivo.domain.model.OperationParams
import com.ecoruta.dispositivo.domain.model.TransmissionPolicy
import com.ecoruta.dispositivo.domain.usecase.CaptureTelemetryUseCase
import com.ecoruta.dispositivo.domain.usecase.RunTelemetrySessionUseCase
import com.ecoruta.dispositivo.domain.usecase.SendHeartbeatUseCase
import com.ecoruta.dispositivo.fakes.FakeBatteryStatusSource
import com.ecoruta.dispositivo.fakes.FakeDeviceStatusRepository
import com.ecoruta.dispositivo.fakes.FakeLocationSource
import com.ecoruta.dispositivo.fakes.FakeParametersRepository
import com.ecoruta.dispositivo.fakes.FakeSessionRepository
import com.ecoruta.dispositivo.fakes.FakeTelemetryRepository
import com.ecoruta.dispositivo.fakes.FakeTripRepository
import com.ecoruta.dispositivo.fakes.FixedParams
import com.ecoruta.dispositivo.fakes.fix
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// runCurrent/advanceTimeBy are the way to drive virtual time in a test of an endless loop.
@OptIn(ExperimentalCoroutinesApi::class)
class RunTelemetrySessionUseCaseTest {

    private val params = OperationParams()
    private val trips = FakeTripRepository(activeTripId = null)
    private val location = FakeLocationSource(listOf(fix(0)))
    private val telemetry = FakeTelemetryRepository()
    private val heartbeats = FakeDeviceStatusRepository()
    private val parameters = FakeParametersRepository(params)

    private val runSession = RunTelemetrySessionUseCase(
        captureTelemetry = CaptureTelemetryUseCase(
            location = location,
            sessions = FakeSessionRepository(),
            trips = trips,
            telemetry = telemetry,
            statusStore = InMemoryTelemetryStatusStore(),
            policy = TransmissionPolicy(FixedParams(params)),
        ),
        sendHeartbeat = SendHeartbeatUseCase(FakeSessionRepository(), FakeBatteryStatusSource(83), heartbeats),
        trips = trips,
        parameters = parameters,
    )

    @Test
    fun `first heartbeat goes out immediately even with no trip open`() = runTest {
        val session = launch { runSession() }
        runCurrent()

        assertEquals(listOf(83), heartbeats.reports.map { it.batteryPercent })
        session.cancel()
    }

    @Test
    fun `heartbeat repeats at the stopped emission interval`() = runTest {
        val session = launch { runSession() }
        runCurrent()

        advanceTimeBy(params.stoppedInterval.inWholeMilliseconds + 1)
        runCurrent()

        assertEquals(2, heartbeats.reports.size)
        session.cancel()
    }

    @Test
    fun `gps capture waits for a trip to open`() = runTest {
        val session = launch { runSession() }
        runCurrent()
        assertNull(location.requestedInterval)

        trips.activeTripId.value = "trip-1"
        runCurrent()

        assertNotNull(location.requestedInterval)
        assertEquals("trip-1", telemetry.enqueued.single().tripId)
        session.cancel()
    }

    @Test
    fun `parameters are refreshed before capture starts`() = runTest {
        val session = launch { runSession() }
        runCurrent()

        assertEquals(1, parameters.refreshCount)
        session.cancel()
    }
}
