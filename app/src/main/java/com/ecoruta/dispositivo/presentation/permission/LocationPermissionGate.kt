package com.ecoruta.dispositivo.presentation.permission

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.platform.LocationPermission

/**
 * Shows [content] only once precise location is granted, and reports it through [onGranted].
 *
 * Nothing behind the gate may run without it: pairing starts the location service, and on
 * Android 14+ that service crashes the app if the permission is missing. On a provisioned kiosk
 * unit the device owner grants it itself, so this screen is only ever seen on a dev install.
 */
@Composable
fun LocationPermissionGate(onGranted: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(LocationPermission.isGranted(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = LocationPermission.isGranted(context)
    }

    // The permission may have been granted (or revoked) from the system settings meanwhile.
    LifecycleResumeEffect(Unit) {
        granted = LocationPermission.isGranted(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(granted) {
        if (granted) onGranted() else launcher.launch(LocationPermission.requested)
    }

    if (granted) {
        content()
    } else {
        LocationPermissionScreen(
            onRequest = { launcher.launch(LocationPermission.requested) },
            onOpenSettings = { context.openAppSettings() },
        )
    }
}

@Composable
fun LocationPermissionScreen(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.permission_text))
        Button(onClick = onRequest) { Text(stringResource(R.string.permission_request)) }
        // After "don't ask again" the system dialog no longer shows; settings is the only way back.
        OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.permission_open_settings)) }
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
