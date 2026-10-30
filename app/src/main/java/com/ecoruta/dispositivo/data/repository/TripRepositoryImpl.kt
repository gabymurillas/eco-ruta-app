package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.core.map
import com.ecoruta.dispositivo.core.onSuccess
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.remote.UnitRouteResolver
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.api.eq
import com.ecoruta.dispositivo.data.remote.dto.FinishTripDto
import com.ecoruta.dispositivo.data.remote.dto.NewTripDto
import com.ecoruta.dispositivo.data.remote.dto.TripDto
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
        val created = safeApiCall { api.createTrip(NewTripDto(unitId, routeId)) }.map { it.firstOrNull() }
        val trip = if (created.isConflict()) openTripOf(unitId) else created
        return when (trip) {
            is Outcome.Failure -> trip
            is Outcome.Success -> {
                val opened = trip.value ?: return Outcome.Failure(AppError.Unexpected())
                preferences.setActiveTripId(opened.id)
                Outcome.Success(Trip(id = opened.id, unitId = opened.unitId))
            }
        }
    }

    /**
     * A 409 almost always means the trip this device just opened: the insert reached the server
     * but the response was lost (a timeout on mobile data), so the retry collides with it. A unit's
     * trips are opened from its own device, so the open one is adopted instead of leaving the
     * driver stuck with an error that only an admin could clear.
     */
    private suspend fun openTripOf(unitId: String): Outcome<TripDto?> =
        safeApiCall { api.getOpenTrips(eq(unitId)) }.map { it.firstOrNull() }

    private fun Outcome<*>.isConflict(): Boolean = this is Outcome.Failure && error is AppError.Conflict

    override suspend fun finish(): Outcome<Unit> {
        val tripId = preferences.activeTripId.first() ?: return Outcome.Failure(AppError.NoActiveTrip)
        return safeApiCall { api.finishTrip(eq(tripId), FinishTripDto(clock.instant().toString())) }
            .onSuccess { preferences.setActiveTripId(null) }
    }
}
