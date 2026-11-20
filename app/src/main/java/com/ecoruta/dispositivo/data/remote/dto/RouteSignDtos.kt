package com.ecoruta.dispositivo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The `letreros` contract of the backend (sql/21_letreros.sql): two RPCs, the unit from the JWT.

/** One route name the admin allows for this unit. */
@Serializable
data class RouteSignDto(
    @SerialName("id") val id: String,
    @SerialName("nombre") val name: String,
)

/** Answer of `letreros_disponibles()`: the sign in force (null: the usual route) and the choices. */
@Serializable
data class RouteSignsDto(
    @SerialName("actual") val current: RouteSignDto? = null,
    @SerialName("opciones") val options: List<RouteSignDto> = emptyList(),
)

/** Answer of `cambiar_letrero(p_letrero_id)`: the sign now in force. */
@Serializable
data class ChangedRouteSignDto(
    @SerialName("actual") val current: RouteSignDto? = null,
)
