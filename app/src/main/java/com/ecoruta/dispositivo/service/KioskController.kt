package com.ecoruta.dispositivo.service

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.ecoruta.dispositivo.platform.LocationPermission
import com.ecoruta.dispositivo.receiver.KioskAdminReceiver

/** Pins the screen to this app (lock-task mode) when the app has been provisioned as device owner. */
class KioskController(private val context: Context) {

    private val policyManager = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, KioskAdminReceiver::class.java)

    /**
     * Allow-lists this app for lock-task and grants its own runtime permissions. Only possible for
     * the device owner; no-op otherwise. An unattended unit has nobody to answer a permission
     * dialog, and lock-task blocks the system settings where it could be granted by hand.
     */
    fun prepare() {
        if (!policyManager.isDeviceOwnerApp(context.packageName)) return
        policyManager.setLockTaskPackages(admin, arrayOf(context.packageName))
        LocationPermission.requested.forEach { permission ->
            policyManager.setPermissionGrantState(
                admin,
                context.packageName,
                permission,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED,
            )
        }
    }

    /** Enters kiosk mode. Skipped when not allow-listed, so normal debug installs are not pinned. */
    fun enter(activity: Activity) {
        if (policyManager.isLockTaskPermitted(context.packageName)) {
            activity.startLockTask()
        }
    }
}
