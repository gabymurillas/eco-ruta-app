package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.mapper.toDto
import com.ecoruta.dispositivo.data.remote.UnitRouteResolver
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.safeApiCall
import com.ecoruta.dispositivo.domain.model.Incident
import com.ecoruta.dispositivo.domain.repository.IncidentRepository

class IncidentRepositoryImpl(
    private val api: SupabaseRestApi,
    private val routes: UnitRouteResolver,
) : IncidentRepository {

    override suspend fun report(incident: Incident): Outcome<Unit> {
        val routeId = when (val route = routes.routeIdOf(incident.unitId)) {
            is Outcome.Failure -> return route
            is Outcome.Success -> route.value
        }
        return safeApiCall { api.insertIncident(incident.toDto(routeId)) }
    }
}
