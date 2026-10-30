package com.ecoruta.dispositivo.fakes

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.BatteryStatus
import com.ecoruta.dispositivo.domain.model.DeviceSession
import com.ecoruta.dispositivo.domain.model.DeviceStatus
import com.ecoruta.dispositivo.domain.model.GeoPoint
import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.model.OperationParams
import com.ecoruta.dispositivo.domain.model.Position
import com.ecoruta.dispositivo.domain.model.Trip
import com.ecoruta.dispositivo.domain.repository.DeviceStatusRepository
import com.ecoruta.dispositivo.domain.repository.OperationParamsSource
import com.ecoruta.dispositivo.domain.repository.ParametersRepository
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.repository.TelemetryRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import com.ecoruta.dispositivo.domain.system.BatteryStatusSource
import com.ecoruta.dispositivo.domain.system.LocationSource
import com.ecoruta.dispositivo.domain.system.TelemetryController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import kotlin.time.Duration

val T0: Instant = Instant.parse("2026-01-01T00:00:00Z")

val TEST_SESSION = DeviceSession(
    unitId = "unit-1",
    unitName = "Unidad 1",
    routeName = "Ruta A",
    accessToken = "access",
    refreshToken = "refresh",
)

fun fix(
    secondsAfterStart: Long,
    speedKmh: Float = 30f,
    accuracyMeters: Float = 5f,
) = LocationFix(
    point = GeoPoint(latitude = 10.0, longitude = -66.0),
    speedKmh = speedKmh,
    accuracyMeters = accuracyMeters,
    capturedAt = T0.plusSeconds(secondsAfterStart),
)

class FixedParams(override val current: OperationParams = OperationParams()) : OperationParamsSource

class FakeSessionRepository(
    private var stored: DeviceSession? = TEST_SESSION,
) : SessionRepository {
    var pairedWithCode: String? = null
        private set

    override val session: Flow<DeviceSession?> get() = flowOf(stored)

    override suspend fun current(): DeviceSession? = stored

    override suspend fun pair(code: String): Outcome<DeviceSession> {
        pairedWithCode = code
        return Outcome.Success(TEST_SESSION).also { stored = TEST_SESSION }
    }
}

class FakeTripRepository(activeTripId: String? = "trip-1") : TripRepository {
    override val activeTripId = MutableStateFlow(activeTripId)

    override suspend fun start(unitId: String): Outcome<Trip> = Outcome.Success(Trip("trip-1", unitId))

    override suspend fun finish(): Outcome<Unit> = Outcome.Success(Unit)
}

class FakeTelemetryRepository : TelemetryRepository {
    val enqueued = mutableListOf<Position>()

    override val pendingCount: Flow<Int> = flowOf(0)

    override suspend fun enqueue(position: Position) {
        enqueued += position
    }

    override suspend fun sync(): Outcome<Unit> = Outcome.Success(Unit)
}

/** The first [failures] subscriptions fail like a provider error or a revoked permission would. */
class FakeLocationSource(
    private val fixes: List<LocationFix>,
    private var failures: Int = 0,
) : LocationSource {
    var requestedInterval: Duration? = null
        private set
    var subscriptions = 0
        private set

    override fun updates(interval: Duration): Flow<LocationFix> {
        requestedInterval = interval
        return flow {
            subscriptions++
            if (failures > 0) {
                failures--
                throw SecurityException("location permission revoked")
            }
            emitAll(fixes.asFlow())
        }
    }

    override suspend fun lastKnown(): LocationFix? = fixes.lastOrNull()
}

class FakeTelemetryController : TelemetryController {
    var startCount = 0
        private set

    override fun start() {
        startCount++
    }
}

class FakeBatteryStatusSource(private val percent: Int? = 83) : BatteryStatusSource {
    override fun current() = BatteryStatus(percent, isCharging = true)

    override fun observe(): Flow<BatteryStatus> = flowOf(current())
}

/** The first [failures] reports throw, like an unexpected bug in the network layer would. */
class FakeDeviceStatusRepository(private var failures: Int = 0) : DeviceStatusRepository {
    val reports = mutableListOf<DeviceStatus>()

    override suspend fun report(status: DeviceStatus): Outcome<Unit> {
        reports += status
        if (failures > 0) {
            failures--
            error("unexpected failure")
        }
        return Outcome.Success(Unit)
    }
}

class FakeParametersRepository(override var current: OperationParams = OperationParams()) : ParametersRepository {
    var refreshCount = 0
        private set

    override suspend fun refresh() {
        refreshCount++
    }
}
