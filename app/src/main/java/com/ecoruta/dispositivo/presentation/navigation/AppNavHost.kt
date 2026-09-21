package com.ecoruta.dispositivo.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecoruta.dispositivo.domain.model.DeviceState
import com.ecoruta.dispositivo.presentation.idle.IdleRoute
import com.ecoruta.dispositivo.presentation.incident.IncidentRoute
import com.ecoruta.dispositivo.presentation.pairing.PairingRoute
import com.ecoruta.dispositivo.presentation.trip.TripRoute

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
        composable<Route.Pairing> { PairingRoute() }
        composable<Route.Idle> { IdleRoute() }
        composable<Route.Trip> {
            TripRoute(onReportIncident = { navController.navigate(Route.Incident) })
        }
        composable<Route.Incident> {
            IncidentRoute(onDone = { navController.popBackStack() })
        }
    }

    LaunchedEffect(deviceState) {
        val target = deviceState.toRoute()
        if (navController.currentDestination?.hasRoute(target::class) != true) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
