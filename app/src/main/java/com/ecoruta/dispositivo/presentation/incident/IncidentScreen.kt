package com.ecoruta.dispositivo.presentation.incident

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.components.ChoiceRow
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.SecondaryButton
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

@Composable
fun IncidentRoute(
    onDone: () -> Unit,
    viewModel: IncidentViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                IncidentEvent.Sent -> {
                    // A system toast, not a card on the trip screen: it is the confirmation Android
                    // users already know, and it stays readable while the screen changes under it.
                    Toast.makeText(context, R.string.trip_incident_sent, Toast.LENGTH_LONG).show()
                    onDone()
                }
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
    ScreenScaffold(
        title = stringResource(R.string.incident_title),
        onBack = onCancel,
        bottomBar = {
            SecondaryButton(stringResource(R.string.incident_cancel), onCancel, Modifier.weight(1f), enabled = !state.isSending)
            PrimaryButton(
                stringResource(R.string.incident_send),
                onSend,
                Modifier.weight(1f),
                enabled = state.selectedType != null || state.description.isNotBlank(),
                loading = state.isSending,
            )
        },
    ) {
        Text(stringResource(R.string.incident_text), color = EcoTheme.colors.inkMuted, style = EcoTheme.type.body)
        // A plain choice, not a sequence: each option is recognised by its icon before its words.
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xs)) {
            IncidentType.choices.forEach { type ->
                ChoiceRow(
                    label = stringResource(type.labelRes()),
                    selected = state.selectedType == type,
                    onClick = { onTypeSelected(type) },
                    icon = type.icon(),
                )
            }
        }
        DescriptionField(state.description, onDescriptionChange)
        state.errorRes?.let { NoticeBanner(stringResource(it), Tone.ERROR) }
    }
}

/**
 * Optional, and never focused on its own: typing while driving is unsafe, so the keyboard only
 * opens if the driver taps the field, and the hint says when to do it.
 */
@Composable
private fun DescriptionField(value: String, onValueChange: (String) -> Unit) {
    val colors = EcoTheme.colors
    val shape = EcoTheme.radius.lg
    Column(verticalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xs)) {
        Text(stringResource(R.string.incident_description_label), color = colors.inkMuted, style = EcoTheme.type.label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = EcoTheme.type.body.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.brand),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = EcoTheme.size.control)
                .clip(shape)
                .background(colors.surface)
                .border(EcoTheme.border.field, colors.borderField, shape)
                .padding(EcoTheme.spacing.lg),
            decorationBox = { field ->
                Box {
                    if (value.isEmpty()) {
                        Text(stringResource(R.string.incident_description_hint), color = colors.inkMuted, style = EcoTheme.type.body)
                    }
                    field()
                }
            },
        )
    }
}

private fun IncidentType.icon(): ImageVector = when (this) {
    IncidentType.ROAD_CLOSED -> Icons.Filled.Block
    IncidentType.MECHANICAL_FAILURE -> Icons.Filled.Build
    IncidentType.DETOUR -> Icons.AutoMirrored.Filled.AltRoute
    IncidentType.CONGESTION -> Icons.Filled.Traffic
    IncidentType.OTHER -> Icons.AutoMirrored.Filled.Notes
}

@StringRes
private fun IncidentType.labelRes(): Int = when (this) {
    IncidentType.ROAD_CLOSED -> R.string.incident_road_closed
    IncidentType.MECHANICAL_FAILURE -> R.string.incident_mechanical_failure
    IncidentType.DETOUR -> R.string.incident_detour
    IncidentType.CONGESTION -> R.string.incident_congestion
    IncidentType.OTHER -> R.string.incident_other
}
