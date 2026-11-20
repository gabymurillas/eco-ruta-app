package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.RouteSign
import com.ecoruta.dispositivo.domain.usecase.ChangeRouteSignUseCase
import com.ecoruta.dispositivo.domain.usecase.FinishTripUseCase
import com.ecoruta.dispositivo.domain.usecase.RefreshRouteSignsUseCase
import com.ecoruta.dispositivo.fakes.FakeRouteSignRepository
import com.ecoruta.dispositivo.fakes.FakeTripRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteSignUseCasesTest {

    private val villa = RouteSign("s1", "Terminal – Villa Olímpica")
    private val signs = FakeRouteSignRepository()
    private val trips = FakeTripRepository(activeTripId = null)
    private val refresh = RefreshRouteSignsUseCase(signs, trips)
    private val change = ChangeRouteSignUseCase(signs, refresh)

    @Test
    fun `choosing a route shows it on the device`() = runTest {
        val result = change(villa.id)

        assertEquals(Outcome.Success(villa), result)
        assertEquals(villa, signs.current.value)
    }

    @Test
    fun `null goes back to the usual route`() = runTest {
        signs.current.value = villa

        change(null)

        assertNull(signs.current.value)
    }

    @Test
    fun `a route the admin withdrew refreshes the list so the picker stops offering it`() = runTest {
        signs.changeResult = Outcome.Failure(AppError.RouteSignNotAllowed)

        val result = change(villa.id)

        assertEquals(Outcome.Failure(AppError.RouteSignNotAllowed), result)
        assertEquals(listOf(true), signs.refreshes)
    }

    @Test
    fun `while a trip close is pending the server's old route is not adopted`() = runTest {
        // Finished offline: the server still shows the old trip's route until the close lands.
        trips.hasPendingClose.value = true

        refresh()

        assertEquals(listOf(false), signs.refreshes)
    }

    @Test
    fun `finishing the trip shows the usual route again at once`() = runTest {
        signs.current.value = villa

        FinishTripUseCase(FakeTripRepository(), signs)()

        assertNull(signs.current.value)
    }
}
