package com.ecoruta.dispositivo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.presentation.idle.IdleRoute
import com.ecoruta.dispositivo.presentation.incident.IncidentRoute
import com.ecoruta.dispositivo.presentation.pairing.PairingRoute
import com.ecoruta.dispositivo.presentation.trip.TripRoute
import com.ecoruta.dispositivo.presentation.welcome.WelcomeScreen

/**
 * Screen flow is driven by [deviceState], not by button callbacks: pairing, starting and finishing
 * a trip just change persisted state and the right screen follows. This also makes recovery after
 * a reboot automatic.
 */
@Composable
fun AppNavHost(deviceState: DeviceState) {
    val navController = rememberNavController()
    val startDestination = remember { deviceState.toRoute() }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Welcome> { WelcomeScreen(onStart = { navController.navigate(Route.Pairing) }) }
        composable<Route.Pairing> { PairingRoute() }
        composable<Route.Idle> { IdleRoute() }
        composable<Route.Trip> { entry ->
            // The incident screen leaves this flag behind so the trip screen can confirm the send.
            val incidentSent by entry.savedStateHandle.getStateFlow(INCIDENT_SENT, false).collectAsStateWithLifecycle()
            TripRoute(
                onReportIncident = { navController.navigate(Route.Incident) },
                showIncidentSent = incidentSent,
                onIncidentSentShown = { entry.savedStateHandle[INCIDENT_SENT] = false },
            )
        }
        composable<Route.Incident> {
            IncidentRoute(
                onDone = { sent ->
                    if (sent) navController.previousBackStackEntry?.savedStateHandle?.set(INCIDENT_SENT, true)
                    navController.popBackStack()
                },
            )
        }
    }

    // Keyed on the screen, not on the state: the state also changes when the session token is
    // renewed (about hourly), and reacting to that would yank the driver out of an incident report.
    val target = deviceState.toRoute()
    LaunchedEffect(target) {
        val current = navController.currentDestination
        // Pairing is where the welcome screen leads, so it already satisfies that target.
        val satisfied = current?.hasRoute(target::class) == true ||
            (target == Route.Welcome && current?.hasRoute(Route.Pairing::class) == true)
        if (!satisfied) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

private const val INCIDENT_SENT = "incident_sent"
