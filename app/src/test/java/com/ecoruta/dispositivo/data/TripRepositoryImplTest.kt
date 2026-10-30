package com.ecoruta.dispositivo.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.local.prefs.TokenCipher
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
import com.ecoruta.dispositivo.fakes.T0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.ZoneOffset

class TripRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = FakeTripsApi()

    private val preferences by lazy {
        DevicePreferences(
            store = PreferenceDataStoreFactory.create(scope = storeScope) {
                folder.newFile("device.preferences_pb").apply { delete() }
            },
            cipher = TokenCipher(), // never touched: trips store no secrets
        )
    }

    private val repository by lazy {
        TripRepositoryImpl(preferences, api, UnitRouteResolver(api), Clock.fixed(T0, ZoneOffset.UTC))
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
    fun `other failures are returned as they are`() = runTest {
        api.createFailure = IOException("no route to host")

        val result = repository.start("unit-1")

        assertEquals(Outcome.Failure(AppError.Offline), result)
        assertNull(api.openTripsQueriedFor)
        assertNull(repository.activeTripId.first())
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}

private class FakeTripsApi : SupabaseRestApi {
    val created = mutableListOf<NewTripDto>()
    var createFailure: Throwable? = null
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

    override suspend fun registerPositions(body: RegisterPositionsRequestDto): RegisterPositionsResponseDto =
        error("unused")

    override suspend fun finishTrip(idFilter: String, body: FinishTripDto): Unit = error("unused")

    override suspend fun reportDeviceHealth(body: DeviceHealthDto): Unit = error("unused")

    override suspend fun insertIncident(body: IncidentDto): Unit = error("unused")

    override suspend fun getParameters(): List<ParameterDto> = error("unused")
}
