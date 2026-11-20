package com.ecoruta.dispositivo.presentation.onboarding

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on Earth, in degrees. Plain Kotlin, so the route maths runs in JVM tests. */
data class LatLon(val lat: Double, val lon: Double)

/** Where the bus is at one instant of the loop. [heading] is degrees clockwise from north, 0–360. */
data class RoutePose(val position: LatLon, val heading: Float, val segment: Int)

/**
 * A fixed route walked at constant speed: a fraction of the loop maps to a distance along the line,
 * not to a point index, so long and short segments take the time their length deserves.
 */
class RouteTrack(val allPoints: List<LatLon>) {

    private val points get() = allPoints

    init {
        require(points.size >= 2) { "A route needs at least two points" }
    }

    /** Distance from the start to each point, in metres. */
    private val cumulative: DoubleArray = DoubleArray(points.size).also { acc ->
        for (i in 1 until points.size) acc[i] = acc[i - 1] + distanceMeters(points[i - 1], points[i])
    }

    val lengthMeters: Double get() = cumulative.last()

    fun poseAt(fraction: Float): RoutePose {
        val target = fraction.coerceIn(0f, 1f) * lengthMeters
        var segment = 0
        while (segment < points.size - 2 && cumulative[segment + 1] < target) segment++
        val from = points[segment]
        val to = points[segment + 1]
        val span = cumulative[segment + 1] - cumulative[segment]
        val t = if (span > 0) (target - cumulative[segment]) / span else 0.0
        // Segments are a few metres to a few hundred: straight-line interpolation is exact enough.
        return RoutePose(
            position = LatLon(from.lat + (to.lat - from.lat) * t, from.lon + (to.lon - from.lon) * t),
            heading = bearingDegrees(from, to),
            segment = segment,
        )
    }

    /** The stretch already driven: every point passed, then the bus itself. */
    fun travelled(pose: RoutePose): List<LatLon> = points.subList(0, pose.segment + 1) + pose.position
}

/** Great-circle distance (haversine). */
fun distanceMeters(a: LatLon, b: LatLon): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val h = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).let { it * it }
    return 2 * EARTH_RADIUS_M * asin(sqrt(h))
}

/** Initial bearing from [a] to [b], degrees clockwise from north, 0–360. */
fun bearingDegrees(a: LatLon, b: LatLon): Float {
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return normalizeDegrees(Math.toDegrees(atan2(y, x)).toFloat())
}

/** Turns [current] toward [target] the short way round, by [share] (0–1) of the gap. */
fun approachDegrees(current: Float, target: Float, share: Float): Float {
    val gap = ((target - current + 540f) % 360f) - 180f
    return normalizeDegrees(current + gap * share.coerceIn(0f, 1f))
}

fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

private const val EARTH_RADIUS_M = 6_371_009.0
