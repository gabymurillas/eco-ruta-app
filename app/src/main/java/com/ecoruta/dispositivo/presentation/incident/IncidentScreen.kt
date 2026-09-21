package com.ecoruta.dispositivo.presentation.incident

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory

@Composable
fun IncidentRoute(
    onDone: () -> Unit,
    viewModel: IncidentViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                IncidentEvent.Sent -> onDone()
            }
        }
    }

    IncidentScreen(
        state = state,
        onTypeSelected = viewModel::onTypeSelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSend = viewModel::onSend,
        onCancel = onDone,
    )
}

@Composable
fun IncidentScreen(
    state: IncidentUiState,
    onTypeSelected: (IncidentType) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.incident_title), style = MaterialTheme.typography.headlineMedium)
        IncidentType.entries.forEach { type ->
            FilterChip(
                selected = state.selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(stringResource(type.labelRes())) },
            )
        }
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.incident_description_label)) },
        )
        state.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSend, enabled = state.canSend) { Text(stringResource(R.string.incident_send)) }
        Button(onClick = onCancel) { Text(stringResource(R.string.incident_cancel)) }
    }
}

@StringRes
private fun IncidentType.labelRes(): Int = when (this) {
    IncidentType.ROAD_CLOSED -> R.string.incident_road_closed
    IncidentType.MECHANICAL_FAILURE -> R.string.incident_mechanical_failure
    IncidentType.DETOUR -> R.string.incident_detour
    IncidentType.CONGESTION -> R.string.incident_congestion
}
