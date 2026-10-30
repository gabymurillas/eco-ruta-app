package com.ecoruta.dispositivo.presentation.navigation

import com.ecoruta.dispositivo.domain.model.DeviceState
import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. */
sealed interface Route {
    @Serializable data object Welcome : Route
    @Serializable data object Pairing : Route
    @Serializable data object Idle : Route
    @Serializable data object Trip : Route
    @Serializable data object Incident : Route
}

/**
 * The primary screen for each device state. [Route.Incident] is only ever pushed on top of
 * [Route.Trip], and [Route.Pairing] on top of [Route.Welcome].
 */
fun DeviceState.toRoute(): Route = when (this) {
    is DeviceState.Unpaired -> if (firstTime) Route.Welcome else Route.Pairing
    is DeviceState.Idle -> Route.Idle
    is DeviceState.OnTrip -> Route.Trip
}
