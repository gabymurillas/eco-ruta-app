package com.ecoruta.dispositivo.presentation.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun PairingRoute(viewModel: PairingViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PairingScreen(state, viewModel::onCodeChange, viewModel::onSubmit)
}

@Composable
fun PairingScreen(
    state: PairingUiState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.pairing_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.code,
            onValueChange = onCodeChange,
            label = { Text(stringResource(R.string.pairing_code_label)) },
            singleLine = true,
            isError = state.errorRes != null,
            supportingText = state.errorRes?.let { { Text(stringResource(it)) } },
        )
        Button(onClick = onSubmit, enabled = !state.isLoading && state.code.isNotBlank()) {
            Text(stringResource(R.string.pairing_submit))
        }
    }
}
