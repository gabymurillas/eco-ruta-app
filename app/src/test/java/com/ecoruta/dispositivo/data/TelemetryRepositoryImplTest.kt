package com.ecoruta.dispositivo.data

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.local.db.PendingPositionDao
import com.ecoruta.dispositivo.data.local.db.PendingPositionEntity
import com.ecoruta.dispositivo.data.mapper.toEntity
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
import com.ecoruta.dispositivo.data.repository.TelemetryRepositoryImpl
import com.ecoruta.dispositivo.data.system.InMemoryTelemetryStatusStore
import com.ecoruta.dispositivo.domain.model.Position
import com.ecoruta.dispositivo.domain.system.ConnectivityObserver
import com.ecoruta.dispositivo.domain.system.SyncScheduler
import com.ecoruta.dispositivo.fakes.FixedParams
import com.ecoruta.dispositivo.fakes.T0
import com.ecoruta.dispositivo.fakes.fix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.ZoneOffset

class TelemetryRepositoryImplTest {

    private val dao = FakePendingPositionDao()
    private val api = FakePositionsApi()
    private val statusStore = InMemoryTelemetryStatusStore()

    private val repository = TelemetryRepositoryImpl(
        dao = dao,
        api = api,
        connectivity = object : ConnectivityObserver {
            override val isOnline: StateFlow<Boolean> = MutableStateFlow(true)
        },
        scheduler = object : SyncScheduler {
            override fun scheduleSync() = Unit
        },
        statusStore = statusStore,
        paramsSource = FixedParams(),
        clock = Clock.fixed(T0.plusSeconds(120), ZoneOffset.UTC),
        appScope = CoroutineScope(Job()),
    )

    private suspend fun queue(tripId: String, vararg secondsAfterStart: Long) {
        secondsAfterStart.forEach { seconds ->
            val reading = fix(seconds)
            dao.insert(
                Position(
                    unitId = "unit-1",
                    tripId = tripId,
                    point = reading.point,
                    speedKmh = reading.speedKmh,
                    accuracyMeters = reading.accuracyMeters,
                    capturedAt = reading.capturedAt,
                ).toEntity(),
            )
        }
    }

    @Test
    fun `a backlog of one trip goes out in a single call`() = runTest {
        queue("trip-1", 0, 5, 10, 15, 20)

        val result = repository.sync()

        assertEquals(Outcome.Success(Unit), result)
        val request = api.requests.single()
        assertEquals("trip-1", request.tripId)
        assertEquals(5, request.positions.size)
        assertTrue(dao.rows.isEmpty())
        assertNotNull(statusStore.lastSentAt.value)
    }

    @Test
    fun `each trip gets its own request, oldest first`() = runTest {
        queue("trip-1", 0, 5)
        queue("trip-2", 10, 15)

        repository.sync()

        assertEquals(listOf("trip-1", "trip-2"), api.requests.map { it.tripId })
        assertEquals(listOf(2, 2), api.requests.map { it.positions.size })
    }

    @Test
    fun `readings are kept when the server fails so nothing is lost`() = runTest {
        queue("trip-1", 0, 5, 10)
        api.failWith = httpError(500)

        val result = repository.sync()

        assertEquals(Outcome.Failure(AppError.Server), result)
        assertEquals(3, dao.rows.size)
        assertNull(statusStore.lastSentAt.value)
    }

    @Test
    fun `readings are kept when the network is down`() = runTest {
        queue("trip-1", 0, 5)
        api.failWith = IOException("no route to host")

        val result = repository.sync()

        assertEquals(Outcome.Failure(AppError.Offline), result)
        assertEquals(2, dao.rows.size)
    }

    @Test
    fun `a rejected batch is dropped so it cannot block the queue forever`() = runTest {
        queue("trip-1", 0, 5)
        api.failWith = httpError(400)

        val result = repository.sync()

        assertEquals(Outcome.Success(Unit), result)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `readings older than the retention window are discarded`() = runTest {
        // The clock sits at T0+120s; retention is 2 h, so only readings older than that go.
        queue("trip-1", -7_300, 0)

        repository.sync()

        assertEquals(1, api.requests.single().positions.size)
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}

private class FakePendingPositionDao : PendingPositionDao {
    val rows = mutableListOf<PendingPositionEntity>()
    private var nextId = 1L

    override suspend fun insert(position: PendingPositionEntity) {
        rows += position.copy(id = nextId++)
    }

    override suspend fun oldest(limit: Int): List<PendingPositionEntity> =
        rows.sortedWith(compareBy({ it.capturedAtMs }, { it.id })).take(limit)

    override suspend fun deleteByIds(ids: List<Long>) {
        rows.removeAll { it.id in ids }
    }

    override suspend fun deleteCapturedBefore(thresholdMs: Long) {
        rows.removeAll { it.capturedAtMs < thresholdMs }
    }

    override fun observeCount(): Flow<Int> = flowOf(rows.size)
}

private class FakePositionsApi : SupabaseRestApi {
    val requests = mutableListOf<RegisterPositionsRequestDto>()
    var failWith: Throwable? = null

    override suspend fun registerPositions(body: RegisterPositionsRequestDto): RegisterPositionsResponseDto {
        failWith?.let { throw it }
        requests += body
        return RegisterPositionsResponseDto(saved = body.positions.size)
    }

    override suspend fun getUnitRoute(idFilter: String): List<UnitRouteDto> = error("unused")

    override suspend fun createTrip(body: NewTripDto): List<TripDto> = error("unused")

    override suspend fun finishTrip(idFilter: String, body: FinishTripDto): Unit = error("unused")

    override suspend fun reportDeviceHealth(body: DeviceHealthDto): Unit = error("unused")

    override suspend fun insertIncident(body: IncidentDto): Unit = error("unused")

    override suspend fun getParameters(): List<ParameterDto> = error("unused")
}
