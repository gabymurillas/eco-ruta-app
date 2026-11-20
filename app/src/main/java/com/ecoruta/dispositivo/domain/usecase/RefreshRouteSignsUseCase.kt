package com.ecoruta.dispositivo.domain.usecase

import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.domain.model.RouteSignOptions
import com.ecoruta.dispositivo.domain.repository.RouteSignRepository
import com.ecoruta.dispositivo.domain.repository.TripRepository
import kotlinx.coroutines.flow.first

/**
 * The options for the picker. [cached] answers at once (also offline); [invoke] asks the backend,
 * which also catches a sign the admin withdrew meanwhile.
 *
 * While a trip close is still on its way the server keeps that trip's sign, and adopting it would
 * bring back the name the driver just left behind; the device keeps its own until the close lands.
 */
class RefreshRouteSignsUseCase(
    private val signs: RouteSignRepository,
    private val trips: TripRepository,
) {
    suspend fun cached(): RouteSignOptions? = signs.cachedOptions()

    suspend operator fun invoke(): Outcome<RouteSignOptions> =
        signs.refresh(adoptCurrent = !trips.hasPendingClose.first())
}
