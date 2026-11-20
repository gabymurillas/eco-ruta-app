package com.ecoruta.dispositivo.presentation.idle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.components.InfoTile
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PhotoTitle
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.UnitRouteCard
import com.ecoruta.dispositivo.presentation.components.StatusBadge
import com.ecoruta.dispositivo.presentation.components.StatusDot
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

@Composable
fun IdleRoute(
    onChangeRoute: () -> Unit,
    viewModel: IdleViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IdleScreen(state, viewModel::onStartTrip, onChangeRoute)
}

@Composable
fun IdleScreen(state: IdleUiState, onStartTrip: () -> Unit, onChangeRoute: () -> Unit) {
    val spacing = EcoTheme.spacing
    ScreenScaffold(
        photo = painterResource(R.drawable.welcome_photo),
        showThemeSelector = true,
        bottomBar = {
            PrimaryButton(
                stringResource(R.string.idle_start_trip),
                onStartTrip,
                Modifier.weight(1f),
                loading = state.isStarting,
            )
        },
    ) {
        // Neutral, not amber: waiting is the normal state, not something to look at.
        StatusBadge(stringResource(R.string.idle_status), EcoTheme.colors.inkMuted)
        PhotoTitle(stringResource(R.string.idle_title_1), stringResource(R.string.idle_title_2))
        // No GPS runs while waiting, so the change is never locked here: the unit is parked.
        UnitRouteCard(state.routeName, state.signName, onChangeRoute, Modifier.fillMaxWidth(), large = true)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
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
    val colors = EcoTheme.colors
    val online = state.readiness.isOnline
    InfoTile(
        label = stringResource(R.string.tile_connection),
        value = stringResource(if (online) R.string.value_online else R.string.value_offline),
        modifier = modifier,
        indicator = { StatusDot(if (online) colors.success else colors.warning) },
    )
}
