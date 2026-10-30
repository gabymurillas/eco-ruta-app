package com.ecoruta.dispositivo.domain.model

/**
 * The four events the PDF lists. Their wire values (`calle_cerrada`, ...) are the `tipo` check
 * constraint of `incidencias`; the mapping lives in the data layer.
 */
enum class IncidentType {
    ROAD_CLOSED,
    MECHANICAL_FAILURE,
    DETOUR,
    CONGESTION,
}

/** An event reported from the road. Its position is the vehicle's at the moment of the report. */
data class Incident(
    val unitId: String,
    val type: IncidentType,
    val description: String?,
    val point: GeoPoint,
)
