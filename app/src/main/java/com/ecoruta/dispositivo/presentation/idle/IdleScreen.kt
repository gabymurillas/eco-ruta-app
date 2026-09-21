package com.ecoruta.dispositivo.presentation.idle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory

@Composable
fun IdleRoute(viewModel: IdleViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IdleScreen(state, viewModel::onStartTrip)
}

@Composable
fun IdleScreen(state: IdleUiState, onStartTrip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
    ) {
        Text(state.routeName, style = MaterialTheme.typography.headlineMedium)
        Text(state.unitName, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.idle_status))
        state.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(onClick = onStartTrip, enabled = !state.isStarting) {
            Text(stringResource(R.string.idle_start_trip))
        }
    }
}
