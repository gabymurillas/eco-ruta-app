package com.ecoruta.dispositivo.presentation.incident

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.IncidentType
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.components.GlassButton
import com.ecoruta.dispositivo.presentation.components.GlassPanel
import com.ecoruta.dispositivo.presentation.components.HighlightTitle
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.components.glass
import com.ecoruta.dispositivo.presentation.theme.ecoColors

@Composable
fun IncidentRoute(
    onDone: (sent: Boolean) -> Unit,
    viewModel: IncidentViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                IncidentEvent.Sent -> onDone(true)
            }
        }
    }

    IncidentScreen(
        state = state,
        onTypeSelected = viewModel::onTypeSelected,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSend = viewModel::onSend,
        onCancel = { onDone(false) },
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
        bottomBar = {
            GlassButton(stringResource(R.string.incident_cancel), onCancel, Modifier.weight(1f), enabled = !state.isSending)
            PrimaryButton(
                stringResource(R.string.incident_send),
                onSend,
                Modifier.weight(1f),
                enabled = state.selectedType != null,
                loading = state.isSending,
            )
        },
    ) {
        HighlightTitle(stringResource(R.string.incident_title_1), stringResource(R.string.incident_title_2))
        Text(stringResource(R.string.incident_text), color = ecoColors.inkMuted, style = MaterialTheme.typography.bodyLarge)
        // A plain choice, not a sequence: each option is recognised by its icon before its words.
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IncidentType.entries.forEach { type ->
                IncidentOption(type, selected = state.selectedType == type, onClick = { onTypeSelected(type) })
            }
        }
        DescriptionField(state.description, onDescriptionChange)
        state.errorRes?.let { NoticeBanner(stringResource(it), Tone.ERROR) }
    }
}

@Composable
private fun IncidentOption(type: IncidentType, selected: Boolean, onClick: () -> Unit) {
    val colors = ecoColors
    val shape = RoundedCornerShape(22.dp)
    val content = if (selected) colors.onBrand else colors.ink
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .then(if (selected) Modifier.clip(shape).background(colors.brandStrong) else Modifier.glass(shape = shape))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(type.icon(), contentDescription = null, tint = content, modifier = Modifier.size(32.dp))
        Text(stringResource(type.labelRes()), color = content, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Optional, and never focused on its own: typing while driving is unsafe, so the keyboard only
 * opens if the driver taps the field, and the hint says when to do it.
 */
@Composable
private fun DescriptionField(value: String, onValueChange: (String) -> Unit) {
    val colors = ecoColors
    GlassPanel(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.incident_description_label), color = colors.inkMuted, style = MaterialTheme.typography.labelMedium)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.brand),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 2,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            decorationBox = { field ->
                Box {
                    if (value.isEmpty()) {
                        Text(stringResource(R.string.incident_description_hint), color = colors.inkMuted, style = MaterialTheme.typography.bodyLarge)
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
}

@StringRes
private fun IncidentType.labelRes(): Int = when (this) {
    IncidentType.ROAD_CLOSED -> R.string.incident_road_closed
    IncidentType.MECHANICAL_FAILURE -> R.string.incident_mechanical_failure
    IncidentType.DETOUR -> R.string.incident_detour
    IncidentType.CONGESTION -> R.string.incident_congestion
}
