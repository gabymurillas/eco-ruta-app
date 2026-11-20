package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.domain.usecase.CompleteOnboardingUseCase
import com.ecoruta.dispositivo.domain.usecase.ObserveDeviceStateUseCase
import com.ecoruta.dispositivo.fakes.FakeSessionRepository
import com.ecoruta.dispositivo.fakes.FakeSettingsRepository
import com.ecoruta.dispositivo.fakes.FakeTripRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveDeviceStateUseCaseTest {

    private val trips = FakeTripRepository(activeTripId = null)

    @Test
    fun `a brand new phone opens on the welcome carousel`() = runTest {
        val state = ObserveDeviceStateUseCase(FakeSessionRepository(stored = null), trips, FakeSettingsRepository())().first()

        assertEquals(DeviceState.Unpaired(firstTime = true), state)
    }

    @Test
    fun `once the carousel was seen it never shows again, even unpaired`() = runTest {
        val settings = FakeSettingsRepository()
        val observe = ObserveDeviceStateUseCase(FakeSessionRepository(stored = null), trips, settings)

        CompleteOnboardingUseCase(settings)()

        assertEquals(DeviceState.Unpaired(firstTime = false), observe().first())
    }

    @Test
    fun `a phone that was paired before skips the carousel after an update`() = runTest {
        val sessions = FakeSessionRepository(stored = null, lastUnitId = "unit-1")

        val state = ObserveDeviceStateUseCase(sessions, trips, FakeSettingsRepository())().first()

        assertEquals(DeviceState.Unpaired(firstTime = false), state)
    }
}
