package com.ecoruta.dispositivo.domain

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.system.InMemoryTelemetryStatusStore
import com.ecoruta.dispositivo.domain.model.GeoPoint
import com.ecoruta.dispositivo.domain.model.Incident
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.repository.IncidentRepository
import com.ecoruta.dispositivo.domain.usecase.ReportIncidentUseCase
import com.ecoruta.dispositivo.fakes.FakeLocationSource
import com.ecoruta.dispositivo.fakes.FakeSessionRepository
import com.ecoruta.dispositivo.fakes.FixedParams
import com.ecoruta.dispositivo.fakes.T0
import com.ecoruta.dispositivo.fakes.fix
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.ZoneOffset

class ReportIncidentUseCaseTest {

    private val statusStore = InMemoryTelemetryStatusStore()
    private val reported = mutableListOf<Incident>()

    /** The clock sits 10 minutes after T0; the default signal-loss threshold is 5 minutes. */
    private fun reportIncident(providerFix: Int?) = ReportIncidentUseCase(
        sessions = FakeSessionRepository(),
        location = FakeLocationSource(listOfNotNull(providerFix?.let { fix(it.toLong()).moved() })),
        statusStore = statusStore,
        params = FixedParams(),
        clock = Clock.fixed(T0.plusSeconds(600), ZoneOffset.UTC),
        incidents = object : IncidentRepository {
            override suspend fun report(incident: Incident): Outcome<Unit> {
                reported += incident
                return Outcome.Success(Unit)
            }
        },
    )

    @Test
    fun `the capture pipeline's fresh reading wins over the provider's stale one`() = runTest {
        statusStore.onFix(fix(590))

        val result = reportIncident(providerFix = 0)(IncidentType.DETOUR, "  calle 5  ")

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(fix(590).point, reported.single().point)
        assertEquals("calle 5", reported.single().description)
    }

    @Test
    fun `the provider's reading is used when it is the fresher one`() = runTest {
        statusStore.onFix(fix(100))

        reportIncident(providerFix = 580)(IncidentType.CONGESTION, null)

        assertEquals(MOVED, reported.single().point)
    }

    @Test
    fun `a reading older than the signal-loss threshold is refused, not guessed`() = runTest {
        statusStore.onFix(fix(0))

        val result = reportIncident(providerFix = 10)(IncidentType.ROAD_CLOSED, null)

        assertEquals(Outcome.Failure(AppError.LocationUnavailable), result)
        assertTrue(reported.isEmpty())
    }

    private fun LocationFix.moved() = copy(point = MOVED)

    private companion object {
        val MOVED = GeoPoint(latitude = 10.5, longitude = -66.5)
    }
}
