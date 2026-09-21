package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.core.onSuccess
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.remote.UnitRouteResolver
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.api.eq
import com.ecoruta.dispositivo.data.remote.dto.FinishTripDto
import com.ecoruta.dispositivo.data.remote.dto.NewTripDto
import com.ecoruta.dispositivo.data.remote.safeApiCall
import com.ecoruta.dispositivo.domain.model.Trip
import com.ecoruta.dispositivo.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Clock

class TripRepositoryImpl(
    private val preferences: DevicePreferences,
    private val api: SupabaseRestApi,
    private val routes: UnitRouteResolver,
    private val clock: Clock,
) : TripRepository {

    override val activeTripId: Flow<String?> = preferences.activeTripId

    override suspend fun start(unitId: String): Outcome<Trip> {
        val routeId = when (val route = routes.routeIdOf(unitId)) {
            is Outcome.Failure -> return route
            is Outcome.Success -> route.value
        }
        return when (val result = safeApiCall { api.createTrip(NewTripDto(unitId, routeId)) }) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                val created = result.value.firstOrNull() ?: return Outcome.Failure(AppError.Unexpected())
                preferences.setActiveTripId(created.id)
                Outcome.Success(Trip(id = created.id, unitId = created.unitId))
            }
        }
    }

    override suspend fun finish(): Outcome<Unit> {
        val tripId = preferences.activeTripId.first() ?: return Outcome.Failure(AppError.NoActiveTrip)
        return safeApiCall { api.finishTrip(eq(tripId), FinishTripDto(clock.instant().toString())) }
            .onSuccess { preferences.setActiveTripId(null) }
    }
}
