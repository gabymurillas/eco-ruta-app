package com.ecoruta.dispositivo.presentation.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TripRoute(
    onReportIncident: () -> Unit,
    viewModel: TripViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TripScreen(state, viewModel::onFinishTrip, onReportIncident)
}

@Composable
fun TripScreen(
    state: TripUiState,
    onFinishTrip: () -> Unit,
    onReportIncident: () -> Unit,
) {
    val status = state.status
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.trip_transmitting), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.trip_gps, stringResource(status.gps.labelRes())))
        Text(stringResource(if (status.isOnline) R.string.trip_online else R.string.trip_offline))
        Text(stringResource(R.string.trip_battery, status.battery.percent))
        Text(stringResource(R.string.trip_last_sent, status.lastSentAt?.let(TIME_FORMAT::format) ?: "—"))
        Text(stringResource(R.string.trip_pending, status.pendingPositions))
        state.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onReportIncident) { Text(stringResource(R.string.trip_report_incident)) }
        Button(onClick = onFinishTrip, enabled = !state.isFinishing) { Text(stringResource(R.string.trip_finish)) }
    }
}

private fun GpsStatus.labelRes(): Int = when (this) {
    GpsStatus.NO_SIGNAL -> R.string.gps_no_signal
    GpsStatus.WEAK -> R.string.gps_weak
    GpsStatus.GOOD -> R.string.gps_good
}

private val TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
