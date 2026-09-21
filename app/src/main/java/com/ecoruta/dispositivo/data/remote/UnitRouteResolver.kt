package com.ecoruta.dispositivo.data.remote

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.api.eq

/**
 * `viajes` and `incidencias` require `ruta_id`, which the pairing response does not carry.
 * Units are publicly readable, so it is looked up fresh each time: an admin may reassign the unit.
 */
class UnitRouteResolver(private val api: SupabaseRestApi) {

    suspend fun routeIdOf(unitId: String): Outcome<String> =
        when (val result = safeApiCall { api.getUnitRoute(eq(unitId)) }) {
            is Outcome.Failure -> result
            is Outcome.Success -> result.value.firstOrNull()?.routeId
                ?.let { Outcome.Success(it) }
                ?: Outcome.Failure(AppError.NoRouteAssigned)
        }
}
