package com.ecoruta.dispositivo.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.BatteryStatus
import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.domain.model.Readiness
import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.domain.model.TripStatus
import com.ecoruta.dispositivo.presentation.components.LocalThemeModeControl
import com.ecoruta.dispositivo.presentation.components.ThemeModeControl
import com.ecoruta.dispositivo.presentation.idle.IdleScreen
import com.ecoruta.dispositivo.presentation.idle.IdleUiState
import com.ecoruta.dispositivo.presentation.incident.IncidentScreen
import com.ecoruta.dispositivo.presentation.incident.IncidentUiState
import com.ecoruta.dispositivo.presentation.pairing.PairingScreen
import com.ecoruta.dispositivo.presentation.pairing.PairingUiState
import com.ecoruta.dispositivo.presentation.permission.LocationPermissionScreen
import com.ecoruta.dispositivo.presentation.theme.EcoRutaTheme
import com.ecoruta.dispositivo.presentation.trip.TripScreen
import com.ecoruta.dispositivo.presentation.trip.TripUiState
import java.time.Instant

/**
 * Debug-only catalog of every screen in every state, with sample data, for design review and for
 * the thesis screenshots. Real screens need a paired unit and live GPS; this needs neither.
 *
 * `adb shell am start -f 0x10008000 -n com.ecoruta.dispositivo/.catalog.DesignCatalogActivity --es screen trip --es theme dark`
 * (0x10008000 = new task + clear task: a fresh catalog in its own task, apart from the real app).
 * Screens: permission, pairing, pairing_error, idle, idle_offline, trip, trip_offline, trip_nogps,
 * trip_sent, incident. Themes: light, dark.
 */
class DesignCatalogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent.getStringExtra("screen") ?: "trip"
        val theme = if (intent.getStringExtra("theme") == "dark") ThemeMode.DARK else ThemeMode.LIGHT
        setContent {
            EcoRutaTheme(theme) {
                CompositionLocalProvider(LocalThemeModeControl provides ThemeModeControl(theme) {}) {
                    Sample(screen)
                }
            }
        }
    }
}

@Composable
private fun Sample(screen: String) {
    val now = Instant.now()
    val trip = TripStatus(
        gps = GpsStatus.GOOD,
        isOnline = true,
        isLocationEnabled = true,
        battery = BatteryStatus(percent = 76, isCharging = true),
        lastSentAt = now.minusSeconds(4),
        pendingPositions = 0,
    )
    val idle = IdleUiState(unitName = "Unidad 12", routeName = "Ruta Centro – UNERG")
    when (screen) {
        "permission" -> LocationPermissionScreen({}, {})
        "pairing" -> PairingScreen(PairingUiState(code = "4T9"), {}, {})
        "pairing_error" -> PairingScreen(
            PairingUiState(code = "4T9K2", sessionEnded = true, errorRes = R.string.error_invalid_pairing_code),
            {},
            {},
        )
        "idle" -> IdleScreen(idle) {}
        "idle_offline" -> IdleScreen(
            idle.copy(
                readiness = Readiness(isOnline = false, isLocationEnabled = true),
                hasPendingClose = true,
                errorRes = R.string.error_offline,
            ),
        ) {}
        "trip_offline" -> TripScreen(
            TripUiState(trip.copy(isOnline = false, pendingPositions = 37, battery = BatteryStatus(41, isCharging = false))),
            {},
            {},
        )
        "trip_nogps" -> TripScreen(TripUiState(trip.copy(gps = GpsStatus.NO_SIGNAL)), {}, {})
        "trip_sent" -> TripScreen(TripUiState(trip), {}, {}, showIncidentSent = true)
        "incident" -> IncidentScreen(
            IncidentUiState(selectedType = IncidentType.DETOUR, description = "Obras en la Av. Bolívar"),
            {},
            {},
            {},
            {},
        )
        else -> TripScreen(TripUiState(trip), {}, {})
    }
}
