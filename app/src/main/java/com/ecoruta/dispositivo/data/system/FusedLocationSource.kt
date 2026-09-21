package com.ecoruta.dispositivo.data.system

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.ecoruta.dispositivo.domain.model.GeoPoint
import com.ecoruta.dispositivo.domain.model.LocationFix
import com.ecoruta.dispositivo.domain.system.LocationSource
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import kotlin.time.Duration

/**
 * Location backed by Google Play Services' fused provider.
 * Callers must hold the location permission; a missing permission closes the flow with an exception.
 */
@SuppressLint("MissingPermission")
class FusedLocationSource(context: Context) : LocationSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override fun updates(interval: Duration): Flow<LocationFix> = callbackFlow {
        val intervalMs = interval.inWholeMilliseconds
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .setWaitForAccurateLocation(false)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toFix()) }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    override suspend fun lastKnown(): LocationFix? = client.lastLocation.await()?.toFix()

    private fun Location.toFix() = LocationFix(
        point = GeoPoint(latitude, longitude),
        speedKmh = if (hasSpeed()) speed * MS_TO_KMH else 0f,
        accuracyMeters = if (hasAccuracy()) accuracy else UNKNOWN_ACCURACY_METERS,
        capturedAt = Instant.ofEpochMilli(time),
    )

    private companion object {
        const val MS_TO_KMH = 3.6f
        const val UNKNOWN_ACCURACY_METERS = 9_999f
    }
}
