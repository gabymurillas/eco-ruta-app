package com.ecoruta.dispositivo.presentation.routepicker

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.components.ChoiceRow
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.SecondaryButton
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

@Composable
fun RoutePickerRoute(
    onDone: () -> Unit,
    viewModel: RoutePickerViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RoutePickerEvent.Changed -> {
                    // The same confirmation the incident report uses: the screen changes under it.
                    Toast.makeText(context, R.string.route_changed, Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }
        }
    }
    RoutePickerScreen(state, viewModel::onSelect, viewModel::onConfirm, onDone)
}

/** Picks the route name the web shows for this unit, from the ones the admin allowed. */
@Composable
fun RoutePickerScreen(
    state: RoutePickerUiState,
    onSelect: (String?) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    ScreenScaffold(
        title = stringResource(R.string.route_picker_title),
        onBack = onCancel,
        bottomBar = {
            SecondaryButton(stringResource(R.string.route_picker_cancel), onCancel, Modifier.weight(1f), enabled = !state.isSaving)
            PrimaryButton(
                stringResource(R.string.route_picker_confirm),
                onConfirm,
                Modifier.weight(1f),
                enabled = state.canConfirm,
                loading = state.isSaving,
            )
        },
    ) {
        Text(stringResource(R.string.route_picker_text), color = EcoTheme.colors.inkMuted, style = EcoTheme.type.body)
        // Why Confirm is off, before the list, where the driver looks first.
        if (!state.isOnline) NoticeBanner(stringResource(R.string.route_picker_offline), Tone.WARNING)
        if (state.isOnline && state.isStale) NoticeBanner(stringResource(R.string.route_picker_stale), Tone.INFO)
        state.errorRes?.let { NoticeBanner(stringResource(it), Tone.ERROR) }
        if (state.isLoading) {
            Text(stringResource(R.string.route_picker_loading), color = EcoTheme.colors.inkMuted, style = EcoTheme.type.body)
        } else {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xs)) {
                ChoiceRow(
                    label = stringResource(R.string.route_picker_normal),
                    subtitle = state.normalRouteName.takeIf { it.isNotBlank() },
                    selected = state.selectedId == null,
                    onClick = { onSelect(null) },
                )
                state.options.forEach { sign ->
                    ChoiceRow(label = sign.name, selected = state.selectedId == sign.id, onClick = { onSelect(sign.id) })
                }
            }
            if (state.options.isEmpty()) NoticeBanner(stringResource(R.string.route_picker_empty), Tone.INFO)
        }
    }
}
