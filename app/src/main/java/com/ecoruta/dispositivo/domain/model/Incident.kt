package com.ecoruta.dispositivo.domain.model

enum class IncidentType {
    ROAD_CLOSED,
    MECHANICAL_FAILURE,
    DETOUR,
    CONGESTION,
}

data class Incident(
    val unitId: String,
    val type: IncidentType,
    val description: String?,
    val point: GeoPoint,
)
