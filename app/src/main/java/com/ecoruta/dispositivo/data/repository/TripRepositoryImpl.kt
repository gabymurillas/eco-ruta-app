package com.ecoruta.dispositivo.data.repository

import android.util.Log
import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.core.map
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
import com.ecoruta.dispositivo.domain.system.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock

/**
 * Trips are opened online (the server assigns the id) but closed offline-first: the close is
 * written to disk with the moment the driver tapped "finish" and delivered when the network allows.
 */
class TripRepositoryImpl(
    private val preferences: DevicePreferences,
    private val api: SupabaseRestApi,
    private val routes: UnitRouteResolver,
    private val scheduler: SyncScheduler,
    private val clock: Clock,
) : TripRepository {

    private val closeLock = Mutex()

    override val activeTripId: Flow<String?> = preferences.activeTripId

    override val hasPendingClose: Flow<Boolean> = preferences.pendingTripClose.map { it != null }

    override suspend fun start(unitId: String): Outcome<Trip> {
        // While the previous trip is still open on the server, the new insert would 409 and the
        // adoption below would wrongly pick up the old trip.
        when (val previous = syncPendingClose()) {
            is Outcome.Failure -> return previous
            is Outcome.Success -> Unit
        }
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

    override suspend fun finish(): Outcome<Unit> {
        // The time is taken before anything can wait: it must be when the driver really finished.
        preferences.closeActiveTrip(endedAt = clock.instant()) ?: return Outcome.Failure(AppError.NoActiveTrip)
        // WorkManager sends it right away when online, or as soon as the network is back, and
        // survives the app or the phone restarting in between.
        scheduler.scheduleSync()
        return Outcome.Success(Unit)
    }

    override suspend fun syncPendingClose(): Outcome<Unit> = closeLock.withLock {
        val pending = preferences.pendingTripClose.first() ?: return Outcome.Success(Unit)
        val result = safeApiCall {
            api.finishTrip(eq(pending.tripId), FinishTripDto(pending.endedAt.toString()))
        }
        when {
            result is Outcome.Success -> preferences.clearPendingTripClose(pending.tripId)
            // A 4xx other than auth can never succeed; keeping it would block every future trip.
            result is Outcome.Failure && result.error is AppError.Rejected -> {
                Log.w(TAG, "Dropping close of trip ${pending.tripId} rejected by backend (${result.error.httpCode})")
                preferences.clearPendingTripClose(pending.tripId)
                return Outcome.Success(Unit)
            }
        }
        result
    }

    override suspend fun forget() {
        preferences.forgetTrips()
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

    private companion object {
        const val TAG = "TripRepository"
    }
}
