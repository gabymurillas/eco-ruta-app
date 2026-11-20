package com.ecoruta.dispositivo.domain.repository

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.RouteSign
import com.ecoruta.dispositivo.domain.model.RouteSignOptions
import kotlinx.coroutines.flow.Flow

/**
 * The route name the unit shows and the choices the admin gave it. Both are cached on the device,
 * so the screens and the picker work with no signal; changing it always needs the network.
 */
interface RouteSignRepository {
    /** The sign shown now; null is the unit's usual route. */
    val current: Flow<RouteSign?>

    /** The last options fetched, or null if they were never fetched. */
    suspend fun cachedOptions(): RouteSignOptions?

    /**
     * Fetches options and the current sign from the backend and caches them. With [adoptCurrent]
     * false the sign shown on the device is kept (see [com.ecoruta.dispositivo.domain.usecase.RefreshRouteSignsUseCase]).
     */
    suspend fun refresh(adoptCurrent: Boolean): Outcome<RouteSignOptions>

    /**
     * Shows [signId] from now on, or the usual route when null. Fails with
     * [com.ecoruta.dispositivo.core.AppError.RouteSignNotAllowed] when the admin no longer allows it.
     */
    suspend fun change(signId: String?): Outcome<RouteSign?>

    /** Back to the usual route on this device only: the server does it itself when the trip closes. */
    suspend fun resetLocal()
}
