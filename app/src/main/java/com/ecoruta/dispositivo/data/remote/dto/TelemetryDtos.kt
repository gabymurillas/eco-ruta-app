package com.ecoruta.dispositivo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Column names mirror sql/01_schema.sql of the backend repository.

/**
 * Body of `registrar-posicion`, the Edge Function that stores readings AND evaluates geofences.
 * There is no `unidad_id`: the server takes the unit from the JWT.
 */
@Serializable
data class RegisterPositionsRequestDto(
    /** Null (omitted) when no trip is open. */
    @SerialName("viaje_id") val tripId: String?,
    @SerialName("posiciones") val positions: List<PositionPointDto>,
)

@Serializable
data class PositionPointDto(
    @SerialName("lat") val latitude: Double,
    @SerialName("lng") val longitude: Double,
    @SerialName("exactitud_m") val accuracyMeters: Float,
    @SerialName("velocidad_kmh") val speedKmh: Float,
    @SerialName("capturado_at") val capturedAt: String,
)

@Serializable
data class RegisterPositionsResponseDto(
    @SerialName("guardadas") val saved: Int,
    @SerialName("evaluada") val evaluated: Boolean = false,
    @SerialName("notificadas") val notified: List<String> = emptyList(),
)

/**
 * Body of the `registrar_salud_del_dispositivo` RPC. The last-signal timestamp is set by the server.
 * A null battery is omitted, and the server then keeps the last level it knew.
 */
@Serializable
data class DeviceHealthDto(
    @SerialName("bateria") val battery: Int?,
)

@Serializable
data class UnitRouteDto(
    @SerialName("ruta_id") val routeId: String? = null,
)

@Serializable
data class NewTripDto(
    @SerialName("unidad_id") val unitId: String,
    @SerialName("ruta_id") val routeId: String,
)

@Serializable
data class TripDto(
    @SerialName("id") val id: String,
    @SerialName("unidad_id") val unitId: String,
)

@Serializable
data class FinishTripDto(
    @SerialName("finalizado_at") val endedAt: String,
)

@Serializable
data class IncidentDto(
    @SerialName("ruta_id") val routeId: String,
    @SerialName("unidad_id") val unitId: String,
    @SerialName("tipo") val type: String,
    @SerialName("descripcion") val description: String?,
    @SerialName("ubicacion") val location: String,
)

/** Row of `parametros`: server-side calibration values the device must follow. */
@Serializable
data class ParameterDto(
    @SerialName("clave") val key: String,
    @SerialName("valor") val value: Double,
)
