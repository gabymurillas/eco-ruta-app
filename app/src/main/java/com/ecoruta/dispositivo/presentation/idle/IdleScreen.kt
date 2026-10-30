package com.ecoruta.dispositivo.presentation.idle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.ecoruta.dispositivo.presentation.components.HighlightTitle
import com.ecoruta.dispositivo.presentation.components.InfoTile
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.RouteIllustration
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.StatusDot
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.theme.ecoColors

@Composable
fun IdleRoute(viewModel: IdleViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IdleScreen(state, viewModel::onStartTrip)
}

@Composable
fun IdleScreen(state: IdleUiState, onStartTrip: () -> Unit) {
    val colors = ecoColors
    ScreenScaffold(
        bottomBar = {
            PrimaryButton(
                stringResource(R.string.idle_start_trip),
                onStartTrip,
                Modifier.weight(1f),
                loading = state.isStarting,
                height = 84.dp,
            )
        },
    ) {
        // Neutral, not amber: waiting is the normal state, not something to look at.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusDot(colors.inkMuted)
            Text(stringResource(R.string.idle_status), color = colors.inkMuted, style = MaterialTheme.typography.labelMedium)
        }
        HighlightTitle(stringResource(R.string.idle_title_1), stringResource(R.string.idle_title_2))
        // Notices outrank decoration: with any of them showing, the route drawing gives up its room
        // so nothing the driver must read ends up below the fold.
        val hasNotices = !state.readiness.isLocationEnabled || state.hasPendingClose || state.errorRes != null
        if (!hasNotices) RouteIllustration(Modifier.fillMaxWidth().height(130.dp))
        InfoTile(label = stringResource(R.string.idle_route), value = state.routeName, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoTile(stringResource(R.string.idle_unit), state.unitName, Modifier.weight(1f))
            ReadinessTile(state, Modifier.weight(1f))
        }
        if (!state.readiness.isLocationEnabled) NoticeBanner(stringResource(R.string.idle_location_off), Tone.ERROR)
        if (state.hasPendingClose) NoticeBanner(stringResource(R.string.idle_pending_close), Tone.WARNING)
        state.errorRes?.let { NoticeBanner(stringResource(it), Tone.ERROR) }
    }
}

@Composable
private fun ReadinessTile(state: IdleUiState, modifier: Modifier) {
    val colors = ecoColors
    val online = state.readiness.isOnline
    InfoTile(
        label = stringResource(R.string.tile_connection),
        value = stringResource(if (online) R.string.value_online else R.string.value_offline),
        modifier = modifier,
        valueColor = if (online) colors.ink else colors.warningInk,
        indicator = { StatusDot(if (online) colors.success else colors.warning) },
    )
}
