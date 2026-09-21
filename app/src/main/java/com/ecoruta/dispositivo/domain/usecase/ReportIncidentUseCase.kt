package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.Incident
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.domain.repository.IncidentRepository
import com.ecoruta.dispositivo.domain.repository.SessionRepository
import com.ecoruta.dispositivo.domain.system.LocationSource

class ReportIncidentUseCase(
    private val sessions: SessionRepository,
    private val location: LocationSource,
    private val incidents: IncidentRepository,
) {
    suspend operator fun invoke(type: IncidentType, description: String?): Outcome<Unit> {
        val session = sessions.current() ?: return Outcome.Failure(AppError.NotPaired)
        val fix = location.lastKnown() ?: return Outcome.Failure(AppError.LocationUnavailable)
        val incident = Incident(
            unitId = session.unitId,
            type = type,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            point = fix.point,
        )
        return incidents.report(incident)
    }
}
