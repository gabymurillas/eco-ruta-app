package com.ecoruta.dispositivo.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * The runtime permissions the device needs. Shared by the UI that asks for them and by the service
 * that cannot start without them: on Android 14+ a `location` foreground service started without
 * the location permission throws and takes the whole process down.
 */
object LocationPermission {

    /** Requested together; only [isGranted] decides whether the app may go on. */
    val requested: Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        // Without it the service still runs; the persistent notification is just hidden.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    /**
     * Precise location is required, not just approximate: an approximate fix is off by kilometres,
     * so every reading would be flagged unusable and no geofence would ever be evaluated.
     */
    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
