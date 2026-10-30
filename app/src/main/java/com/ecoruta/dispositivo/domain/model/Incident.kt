package com.ecoruta.dispositivo.domain.model

/**
 * The four events the PDF lists, plus [OTHER] for a note that fits none of them. Their wire values
 * (`calle_cerrada`, ..., `otro`) are the `tipo` check constraint of `incidencias`; the mapping
 * lives in the data layer.
 */
enum class IncidentType {
    ROAD_CLOSED,
    MECHANICAL_FAILURE,
    DETOUR,
    CONGESTION,

    /** Never picked from the list: it is what a description sent on its own goes out as. */
    OTHER,
    ;

    companion object {
        /** The options the driver picks from. */
        val choices: List<IncidentType> = entries - OTHER
    }
}

/** An event reported from the road. Its position is the vehicle's at the moment of the report. */
data class Incident(
    val unitId: String,
    val type: IncidentType,
    val description: String?,
    val point: GeoPoint,
)
