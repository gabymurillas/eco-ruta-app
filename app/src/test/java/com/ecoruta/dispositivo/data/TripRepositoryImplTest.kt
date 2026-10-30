package com.ecoruta.dispositivo.data

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.local.prefs.PendingTripClose
import com.ecoruta.dispositivo.data.remote.UnitRouteResolver
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.dto.DeviceHealthDto
import com.ecoruta.dispositivo.data.remote.dto.FinishTripDto
import com.ecoruta.dispositivo.data.remote.dto.IncidentDto
import com.ecoruta.dispositivo.data.remote.dto.NewTripDto
import com.ecoruta.dispositivo.data.remote.dto.ParameterDto
import com.ecoruta.dispositivo.data.remote.dto.RegisterPositionsRequestDto
import com.ecoruta.dispositivo.data.remote.dto.RegisterPositionsResponseDto
import com.ecoruta.dispositivo.data.remote.dto.TripDto
import com.ecoruta.dispositivo.data.remote.dto.UnitRouteDto
import com.ecoruta.dispositivo.data.repository.TripRepositoryImpl
import com.ecoruta.dispositivo.domain.model.Trip
import com.ecoruta.dispositivo.domain.system.SyncScheduler
import com.ecoruta.dispositivo.fakes.T0
import com.ecoruta.dispositivo.fakes.testPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TripRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = FakeTripsApi()
    private val clock = MutableClock(T0)
    private var syncsScheduled = 0

    private val preferences by lazy { testPreferences(folder.root, storeScope) }

    private val repository by lazy {
        TripRepositoryImpl(
            preferences = preferences,
            api = api,
            routes = UnitRouteResolver(api),
            scheduler = object : SyncScheduler {
                override fun scheduleSync() {
                    syncsScheduled++
                }
            },
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    @Test
    fun `a new trip is opened on the unit's current route and remembered`() = runTest {
        val result = repository.start("unit-1")

        assertEquals(Outcome.Success(Trip("trip-new", "unit-1")), result)
        assertEquals(NewTripDto("unit-1", "route-1"), api.created.single())
        assertEquals("trip-new", repository.activeTripId.first())
    }

    @Test
    fun `a 409 adopts the trip already open for the unit instead of getting stuck`() = runTest {
        api.createFailure = httpError(409)
        api.openTrips = listOf(TripDto(id = "trip-lost-response", unitId = "unit-1"))

        val result = repository.start("unit-1")

        assertEquals(Outcome.Success(Trip("trip-lost-response", "unit-1")), result)
        assertEquals("eq.unit-1", api.openTripsQueriedFor)
        assertEquals("trip-lost-response", repository.activeTripId.first())
    }

    @Test
    fun `a 409 with no open trip left is an error, not a phantom trip`() = runTest {
        api.createFailure = httpError(409)
        api.openTrips = emptyList()

        val result = repository.start("unit-1")

        assertEquals(Outcome.Failure(AppError.Unexpected()), result)
        assertNull(repository.activeTripId.first())
    }

    @Test
    fun `other start failures are returned as they are`() = runTest {
        api.createFailure = IOException("no route to host")

        val result = repository.start("unit-1")

        assertEquals(Outcome.Failure(AppError.Offline), result)
        assertNull(api.openTripsQueriedFor)
        assertNull(repository.activeTripId.first())
    }

    @Test
    fun `finishing works offline and keeps the moment the driver really finished`() = runTest {
        preferences.setActiveTripId("trip-1")
        api.finishFailure = IOException("no route to host")
        clock.now = T0.plusSeconds(600)

        val result = repository.finish()

        assertEquals(Outcome.Success(Unit), result)
        assertNull(repository.activeTripId.first())
        assertEquals(PendingTripClose("trip-1", T0.plusSeconds(600)), preferences.pendingTripClose.first())
        assertTrue(repository.hasPendingClose.first())
        assertEquals(1, syncsScheduled)
        assertTrue(api.finished.isEmpty()) // nothing is sent inline: the scheduler does it
    }

    @Test
    fun `the queued close is sent later with the recorded end time, not the send time`() = runTest {
        preferences.setActiveTripId("trip-1")
        clock.now = T0.plusSeconds(600)
        repository.finish()

        clock.now = T0.plusSeconds(3_600) // the network came back an hour later
        val result = repository.syncPendingClose()

        assertEquals(Outcome.Success(Unit), result)
        assertEquals("eq.trip-1" to "2026-01-01T00:10:00Z", api.finished.single())
        assertFalse(repository.hasPendingClose.first())
    }

    @Test
    fun `a close that cannot be sent stays queued for the next attempt`() = runTest {
        preferences.setActiveTripId("trip-1")
        repository.finish()
        api.finishFailure = IOException("no route to host")

        val result = repository.syncPendingClose()

        assertEquals(Outcome.Failure(AppError.Offline), result)
        assertTrue(repository.hasPendingClose.first())
    }

    @Test
    fun `a close the server rejects is dropped so it cannot block every future trip`() = runTest {
        preferences.setActiveTripId("trip-1")
        repository.finish()
        api.finishFailure = httpError(400)

        val result = repository.syncPendingClose()

        assertEquals(Outcome.Success(Unit), result)
        assertFalse(repository.hasPendingClose.first())
    }

    @Test
    fun `finishing with no open trip is an error`() = runTest {
        assertEquals(Outcome.Failure(AppError.NoActiveTrip), repository.finish())
        assertEquals(0, syncsScheduled)
    }

    @Test
    fun `a new trip first delivers the previous trip's close`() = runTest {
        preferences.setActiveTripId("trip-1")
        repository.finish()

        repository.start("unit-1")

        assertEquals("eq.trip-1", api.finished.single().first)
        assertEquals("trip-new", repository.activeTripId.first())
    }

    @Test
    fun `no new trip opens while the previous close cannot reach the server`() = runTest {
        preferences.setActiveTripId("trip-1")
        repository.finish()
        api.finishFailure = IOException("no route to host")

        val result = repository.start("unit-1")

        assertEquals(Outcome.Failure(AppError.Offline), result)
        assertTrue(api.created.isEmpty())
        assertTrue(repository.hasPendingClose.first())
    }

    @Test
    fun `forgetting drops the open trip and a queued close`() = runTest {
        preferences.setActiveTripId("trip-1")
        repository.finish()
        preferences.setActiveTripId("trip-2")

        repository.forget()

        assertNull(repository.activeTripId.first())
        assertFalse(repository.hasPendingClose.first())
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}

private class MutableClock(var now: Instant) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneId.of("UTC")

    override fun withZone(zone: ZoneId?): Clock = this
}

private class FakeTripsApi : SupabaseRestApi {
    val created = mutableListOf<NewTripDto>()
    val finished = mutableListOf<Pair<String, String>>()
    var createFailure: Throwable? = null
    var finishFailure: Throwable? = null
    var openTrips: List<TripDto> = emptyList()
    var openTripsQueriedFor: String? = null
        private set

    override suspend fun getUnitRoute(idFilter: String) = listOf(UnitRouteDto(routeId = "route-1"))

    override suspend fun createTrip(body: NewTripDto): List<TripDto> {
        createFailure?.let { throw it }
        created += body
        return listOf(TripDto(id = "trip-new", unitId = body.unitId))
    }

    override suspend fun getOpenTrips(unitFilter: String): List<TripDto> {
        openTripsQueriedFor = unitFilter
        return openTrips
    }

    override suspend fun finishTrip(idFilter: String, body: FinishTripDto) {
        finishFailure?.let { throw it }
        finished += idFilter to body.endedAt
    }

    override suspend fun registerPositions(body: RegisterPositionsRequestDto): RegisterPositionsResponseDto =
        error("unused")

    override suspend fun reportDeviceHealth(body: DeviceHealthDto): Unit = error("unused")

    override suspend fun insertIncident(body: IncidentDto): Unit = error("unused")

    override suspend fun getParameters(): List<ParameterDto> = error("unused")
}
