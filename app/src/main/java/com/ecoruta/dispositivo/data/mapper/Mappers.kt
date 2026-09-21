package com.ecoruta.dispositivo.data.mapper

import com.ecoruta.dispositivo.data.local.db.PendingPositionEntity
import com.ecoruta.dispositivo.data.remote.dto.DeviceHealthDto
import com.ecoruta.dispositivo.data.remote.dto.IncidentDto
import com.ecoruta.dispositivo.data.remote.dto.PairResponseDto
import com.ecoruta.dispositivo.data.remote.dto.PositionPointDto
import com.ecoruta.dispositivo.data.remote.dto.RegisterPositionsRequestDto
import com.ecoruta.dispositivo.domain.model.DeviceSession
import com.ecoruta.dispositivo.domain.model.DeviceStatus
import com.ecoruta.dispositivo.domain.model.GeoPoint
import com.ecoruta.dispositivo.domain.model.Incident
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.domain.model.OperationParams
import com.ecoruta.dispositivo.domain.model.Position
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

fun PairResponseDto.toDomain() = DeviceSession(
    unitId = unit.id,
    unitName = unit.name,
    routeName = unit.routeName.orEmpty(),
    accessToken = session.accessToken,
    refreshToken = session.refreshToken,
)

fun Position.toEntity() = PendingPositionEntity(
    unitId = unitId,
    tripId = tripId,
    latitude = point.latitude,
    longitude = point.longitude,
    speedKmh = speedKmh,
    accuracyMeters = accuracyMeters,
    capturedAtMs = capturedAt.toEpochMilli(),
)

fun PendingPositionEntity.toDto() = PositionPointDto(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    speedKmh = speedKmh,
    capturedAt = Instant.ofEpochMilli(capturedAtMs).toString(),
)

/** All [positions] belong to [tripId]; the server orders them, so capture order is not required. */
fun toRegisterRequest(tripId: String, positions: List<PendingPositionEntity>) =
    RegisterPositionsRequestDto(tripId = tripId, positions = positions.map { it.toDto() })

fun DeviceStatus.toDto() = DeviceHealthDto(battery = batteryPercent)

fun Incident.toDto(routeId: String) = IncidentDto(
    routeId = routeId,
    unitId = unitId,
    type = type.toWireValue(),
    description = description,
    location = point.toEwkt(),
)

/** Applies the backend's `parametros` on top of [base]; keys the server does not define keep [base]. */
fun Map<String, Double>.toOperationParams(base: OperationParams = OperationParams()) = base.copy(
    movingInterval = this[KEY_MOVING_INTERVAL_S]?.seconds ?: base.movingInterval,
    stoppedInterval = this[KEY_STOPPED_INTERVAL_S]?.seconds ?: base.stoppedInterval,
    maxAccuracyMeters = this[KEY_MAX_ACCURACY_M]?.toFloat() ?: base.maxAccuracyMeters,
    gpsStaleAfter = this[KEY_SIGNAL_LOST_S]?.seconds ?: base.gpsStaleAfter,
)

/** PostGIS expects longitude first: `SRID=4326;POINT(lon lat)`. */
private fun GeoPoint.toEwkt(): String = "SRID=4326;POINT($longitude $latitude)"

private fun IncidentType.toWireValue(): String = when (this) {
    IncidentType.ROAD_CLOSED -> "calle_cerrada"
    IncidentType.MECHANICAL_FAILURE -> "falla_mecanica"
    IncidentType.DETOUR -> "desvio"
    IncidentType.CONGESTION -> "congestion"
}

private const val KEY_MOVING_INTERVAL_S = "emision_movimiento_s"
private const val KEY_STOPPED_INTERVAL_S = "emision_detenido_s"
private const val KEY_MAX_ACCURACY_M = "exactitud_maxima_m"
private const val KEY_SIGNAL_LOST_S = "sin_senal_umbral_s"
