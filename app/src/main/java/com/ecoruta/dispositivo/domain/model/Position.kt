package com.ecoruta.dispositivo.domain.model

import java.time.Instant

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

/** A raw reading from the location provider. */
data class LocationFix(
    val point: GeoPoint,
    val speedKmh: Float,
    val accuracyMeters: Float,
    val capturedAt: Instant,
)

/** A reading attributed to a unit and trip, ready to be transmitted. */
data class Position(
    val unitId: String,
    val tripId: String,
    val point: GeoPoint,
    val speedKmh: Float,
    val accuracyMeters: Float,
    val capturedAt: Instant,
)
