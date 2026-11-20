package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.RouteSign
import com.ecoruta.dispositivo.domain.repository.RouteSignRepository

/**
 * Changes the route name the web shows for this unit, or goes back to its usual route with null.
 * Never queued: shown late, the web would announce a route the bus is no longer doing.
 */
class ChangeRouteSignUseCase(
    private val signs: RouteSignRepository,
    private val refreshSigns: RefreshRouteSignsUseCase,
) {
    suspend operator fun invoke(signId: String?): Outcome<RouteSign?> {
        val result = signs.change(signId)
        // The admin withdrew it: refresh so the picker stops offering it.
        if (result is Outcome.Failure && result.error == AppError.RouteSignNotAllowed) refreshSigns()
        return result
    }
}
