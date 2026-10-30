package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Incident
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.repository.IncidentRepository
import com.ecoruta.dispositivo.domain.repository.OperationParamsSource
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.system.LocationSource
import com.ecoruta.dispositivo.domain.system.TelemetryStatusStore
import java.time.Clock
import java.time.Duration
import kotlin.time.toJavaDuration

/**
 * Reports an incident at the vehicle's current position. The incident recalculates arrival
 * estimates for the stops ahead, so a position from far back on the route would penalize the
 * wrong stretch: a reading older than the signal-loss threshold is refused instead of guessed.
 */
class ReportIncidentUseCase(
    private val sessions: SessionRepository,
    private val location: LocationSource,
    private val statusStore: TelemetryStatusStore,
    private val params: OperationParamsSource,
    private val clock: Clock,
    private val incidents: IncidentRepository,
) {
    suspend operator fun invoke(type: IncidentType, description: String?): Outcome<Unit> {
        val session = sessions.current() ?: return Outcome.Failure(AppError.NotPaired)
        val fix = freshestFix() ?: return Outcome.Failure(AppError.LocationUnavailable)
        val incident = Incident(
            unitId = session.unitId,
            type = type,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            point = fix.point,
        )
        return incidents.report(incident)
    }

    /** The capture pipeline's last reading is normally the freshest; the provider's is a fallback. */
    private suspend fun freshestFix(): LocationFix? {
        val newest = listOfNotNull(statusStore.lastFix.value, location.lastKnown()).maxByOrNull { it.capturedAt }
            ?: return null
        val age = Duration.between(newest.capturedAt, clock.instant())
        return newest.takeIf { age <= params.current.gpsStaleAfter.toJavaDuration() }
    }
}
