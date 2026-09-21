package com.ecoruta.dispositivo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ecoruta.dispositivo.presentation.MainActivity

/** Brings the app up after the device boots so it runs unattended, with no driver interaction. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Launched as the device owner, this is exempt from background activity start limits.
        // MainActivity then resumes telemetry if a trip was in progress.
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
