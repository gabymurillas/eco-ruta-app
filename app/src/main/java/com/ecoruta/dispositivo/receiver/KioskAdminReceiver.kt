package com.ecoruta.dispositivo.receiver

import android.app.admin.DeviceAdminReceiver

/**
 * Entry point that lets the app be provisioned as device owner (required for lock-task kiosk mode):
 * `adb shell dpm set-device-owner com.ecoruta.dispositivo/.receiver.KioskAdminReceiver`
 */
class KioskAdminReceiver : DeviceAdminReceiver()
