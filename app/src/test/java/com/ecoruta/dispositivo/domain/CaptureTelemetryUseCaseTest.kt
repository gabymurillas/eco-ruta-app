package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.data.system.InMemoryTelemetryStatusStore
import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.model.OperationParams
import com.ecoruta.dispositivo.domain.model.TransmissionPolicy
import com.ecoruta.dispositivo.domain.usecase.CaptureTelemetryUseCase
import com.ecoruta.dispositivo.fakes.FakeLocationSource
import com.ecoruta.dispositivo.fakes.FakeSessionRepository
import com.ecoruta.dispositivo.fakes.FakeTelemetryRepository
import com.ecoruta.dispositivo.fakes.FakeTripRepository
import com.ecoruta.dispositivo.fakes.FixedParams
import com.ecoruta.dispositivo.fakes.T0
import com.ecoruta.dispositivo.fakes.fix
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureTelemetryUseCaseTest {

    private val params = OperationParams()
    private val telemetry = FakeTelemetryRepository()
    private val statusStore = InMemoryTelemetryStatusStore()

    private fun captureFrom(fixes: List<LocationFix>, activeTripId: String? = "trip-1"): FakeLocationSource {
        val location = FakeLocationSource(fixes)
        val captureTelemetry = CaptureTelemetryUseCase(
            location = location,
            sessions = FakeSessionRepository(),
            trips = FakeTripRepository(activeTripId),
            telemetry = telemetry,
            statusStore = statusStore,
            policy = TransmissionPolicy(FixedParams(params)),
        )
        runTest { captureTelemetry() }
        return location
    }

    @Test
    fun `positions are thinned to 5 seconds while moving and 30 seconds while stopped`() {
        captureFrom(
            listOf(
                fix(0, speedKmh = 30f), // recorded: first reading
                fix(5, speedKmh = 30f), // recorded: moving, 5 s elapsed
                fix(7, speedKmh = 30f), // skipped: only 2 s since last
                fix(10, speedKmh = 0f), // skipped: stopped, only 5 s since last
                fix(40, speedKmh = 0f), // recorded: stopped, 35 s since last
            ),
        )

        val recordedAt = telemetry.enqueued.map { it.capturedAt }
        assertEquals(listOf(T0, T0.plusSeconds(5), T0.plusSeconds(40)), recordedAt)
    }

    @Test
    fun `positions carry the unit and the active trip`() {
        captureFrom(listOf(fix(0)))

        val position = telemetry.enqueued.single()
        assertEquals("unit-1", position.unitId)
        assertEquals("trip-1", position.tripId)
    }

    @Test
    fun `nothing is recorded when no trip is active but the gps status still updates`() {
        captureFrom(listOf(fix(0)), activeTripId = null)

        assertTrue(telemetry.enqueued.isEmpty())
        assertEquals(fix(0), statusStore.lastFix.value)
    }

    @Test
    fun `location is requested at the fast moving interval`() {
        val location = captureFrom(emptyList())

        assertEquals(params.movingInterval, location.requestedInterval)
    }
}
