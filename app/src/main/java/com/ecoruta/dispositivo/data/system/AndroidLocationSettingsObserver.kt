package com.ecoruta.dispositivo.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.ecoruta.dispositivo.domain.system.LocationSettingsObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/** Watches the location master switch through `PROVIDERS_CHANGED_ACTION`. */
class AndroidLocationSettingsObserver(
    private val context: Context,
    scope: CoroutineScope,
) : LocationSettingsObserver {

    private val manager = context.getSystemService(LocationManager::class.java)

    override val isLocationEnabled: StateFlow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(LocationManagerCompat.isLocationEnabled(manager))
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_EXPORTED, // sent by the system
        )
        awaitClose { context.unregisterReceiver(receiver) }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, initialValue = LocationManagerCompat.isLocationEnabled(manager))
}
