package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.domain.model.RouteSign
import com.ecoruta.dispositivo.domain.repository.RouteSignRepository
import kotlinx.coroutines.flow.Flow

/** The route name the unit shows now; null is its usual route. */
class ObserveRouteSignUseCase(private val signs: RouteSignRepository) {
    operator fun invoke(): Flow<RouteSign?> = signs.current
}
