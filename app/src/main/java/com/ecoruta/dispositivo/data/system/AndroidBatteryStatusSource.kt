package com.ecoruta.dispositivo.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.ecoruta.dispositivo.domain.model.BatteryStatus
import com.ecoruta.dispositivo.domain.system.BatteryStatusSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Battery level and charging state from the sticky `ACTION_BATTERY_CHANGED` broadcast. */
class AndroidBatteryStatusSource(private val context: Context) : BatteryStatusSource {

    private val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    // ACTION_BATTERY_CHANGED is sticky: registering without a receiver just returns the last value.
    override fun current(): BatteryStatus =
        context.registerReceiver(null, filter)?.toBatteryStatus() ?: UNKNOWN

    override fun observe(): Flow<BatteryStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent.toBatteryStatus())
            }
        }
        // The sticky value is delivered immediately on registration.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun Intent.toBatteryStatus(): BatteryStatus {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) level * PERCENT_MAX / scale else UNKNOWN.percent
        val isCharging = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        return BatteryStatus(percent, isCharging)
    }

    private companion object {
        const val PERCENT_MAX = 100
        val UNKNOWN = BatteryStatus(percent = null, isCharging = false)
    }
}
