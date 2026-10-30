package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.usecase.SyncPendingDataUseCase
import com.ecoruta.dispositivo.fakes.FakeTelemetryRepository
import com.ecoruta.dispositivo.fakes.FakeTripRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPendingDataUseCaseTest {

    private val telemetry = FakeTelemetryRepository()
    private val trips = FakeTripRepository()
    private val syncPendingData = SyncPendingDataUseCase(telemetry, trips)

    @Test
    fun `succeeds when positions and the trip close both went out`() = runTest {
        assertEquals(Outcome.Success(Unit), syncPendingData())
    }

    @Test
    fun `asks for a retry when the trip close could not be sent`() = runTest {
        trips.closeResult = Outcome.Failure(AppError.Offline)

        assertEquals(Outcome.Failure(AppError.Offline), syncPendingData())
    }

    @Test
    fun `asks for a retry when positions could not be sent`() = runTest {
        telemetry.syncResult = Outcome.Failure(AppError.Server)

        assertEquals(Outcome.Failure(AppError.Server), syncPendingData())
    }
}
