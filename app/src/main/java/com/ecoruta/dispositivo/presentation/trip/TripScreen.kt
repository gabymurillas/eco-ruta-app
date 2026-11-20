package com.ecoruta.dispositivo.presentation.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.domain.model.TripStatus
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.common.agoText
import com.ecoruta.dispositivo.presentation.common.rememberNow
import com.ecoruta.dispositivo.presentation.components.InfoTile
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.SecondaryButton
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.StatusDot
import com.ecoruta.dispositivo.presentation.components.UnitRouteCard
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.theme.EcoColors
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

@Composable
fun TripRoute(
    onReportIncident: () -> Unit,
    onChangeRoute: () -> Unit,
    viewModel: TripViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TripScreen(state, viewModel::onFinishTrip, onReportIncident, onChangeRoute)
}

@Composable
fun TripScreen(
    state: TripUiState,
    onFinishTrip: () -> Unit,
    onReportIncident: () -> Unit,
    onChangeRoute: () -> Unit,
) {
    var confirmFinish by remember { mutableStateOf(false) }
    val status = state.status

    ScreenScaffold(
        bottomBar = {
            SecondaryButton(stringResource(R.string.trip_report_incident), onReportIncident, Modifier.weight(1f))
            PrimaryButton(
                stringResource(R.string.trip_finish),
                onClick = { confirmFinish = true },
                modifier = Modifier.weight(1f),
                loading = state.isFinishing,
            )
        },
    ) {
        StatusBanner(status)
        UnitRouteCard(
            normalRouteName = state.normalRouteName,
            signName = state.signName,
            onChange = onChangeRoute,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.route_current_label),
            blockedReason = if (state.isMoving) stringResource(R.string.route_blocked_moving) else null,
        )
        // A common height keeps each row even; the values stay large enough to read from the seat.
        Row(horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.sm)) {
            GpsTile(status, Modifier.weight(1f).heightIn(min = EcoTheme.size.tileMin))
            ConnectionTile(status, Modifier.weight(1f).heightIn(min = EcoTheme.size.tileMin))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.sm)) {
            PowerTile(status, Modifier.weight(1f).heightIn(min = EcoTheme.size.tileMin))
            UploadsTile(status, Modifier.weight(1f).heightIn(min = EcoTheme.size.tileMin))
        }
        state.errorRes?.let { NoticeBanner(stringResource(it), Tone.ERROR) }
    }

    if (confirmFinish) {
        FinishDialog(
            onConfirm = {
                confirmFinish = false
                onFinishTrip()
            },
            onDismiss = { confirmFinish = false },
        )
    }
}

/**
 * The one thing to read from the seat. A solid fill, never translucent: it has to hold its contrast in
 * direct sunlight. Colour, dot and words all say the same, so colour is never the only signal.
 */
@Composable
private fun StatusBanner(status: TripStatus) {
    val colors = EcoTheme.colors
    val headline = TripHeadline.of(status)
    val (fill, onFill) = headline.fill(colors)
    val now = rememberNow()
    val detail = when (headline) {
        TripHeadline.TRANSMITTING -> status.lastSentAt
            ?.let { stringResource(R.string.headline_last_sent, agoText(it, now)) }
            ?: stringResource(R.string.headline_waiting_first)
        TripHeadline.OFFLINE -> stringResource(R.string.headline_offline_detail)
        TripHeadline.WEAK_GPS -> stringResource(R.string.headline_weak_detail)
        TripHeadline.SEARCHING_GPS -> stringResource(R.string.headline_searching_detail)
        TripHeadline.LOCATION_OFF -> stringResource(R.string.headline_location_off_detail)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(EcoTheme.radius.xl)
            .background(fill)
            .padding(EcoTheme.spacing.xxl)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.md)) {
            StatusDot(onFill, pulse = headline == TripHeadline.TRANSMITTING, size = EcoTheme.size.dotLarge)
            Text(stringResource(headline.titleRes()), color = onFill, style = EcoTheme.type.status)
        }
        Text(detail, color = onFill, style = EcoTheme.type.body)
    }
}

@Composable
private fun GpsTile(status: TripStatus, modifier: Modifier) {
    val colors = EcoTheme.colors
    val (valueRes, tint) = when {
        !status.isLocationEnabled -> R.string.value_location_off to colors.error
        status.gps == GpsStatus.GOOD -> R.string.value_gps_good to colors.success
        status.gps == GpsStatus.WEAK -> R.string.value_gps_weak to colors.warning
        else -> R.string.value_gps_none to colors.error
    }
    InfoTile(
        label = stringResource(R.string.tile_gps),
        value = stringResource(valueRes),
        modifier = modifier,
        indicator = { StatusDot(tint) },
    )
}

@Composable
private fun ConnectionTile(status: TripStatus, modifier: Modifier) {
    val colors = EcoTheme.colors
    InfoTile(
        label = stringResource(R.string.tile_connection),
        value = stringResource(if (status.isOnline) R.string.value_online else R.string.value_offline),
        modifier = modifier,
        indicator = { StatusDot(if (status.isOnline) colors.success else colors.warning) },
    )
}

/**
 * The unit runs plugged into the vehicle, so the percentage matters less than whether it is still
 * charging: a cable that came loose is what drains it, and that is what turns amber.
 */
@Composable
private fun PowerTile(status: TripStatus, modifier: Modifier) {
    val colors = EcoTheme.colors
    val charging = status.battery.isCharging
    InfoTile(
        label = stringResource(R.string.tile_power),
        value = stringResource(if (charging) R.string.value_charging else R.string.value_not_charging),
        detail = status.battery.percent?.let { stringResource(R.string.value_battery_percent, it) }
            ?: stringResource(R.string.value_battery_unknown),
        modifier = modifier,
        indicator = { StatusDot(if (charging) colors.success else colors.warning) },
    )
}

@Composable
private fun UploadsTile(status: TripStatus, modifier: Modifier) {
    val colors = EcoTheme.colors
    val queued = status.pendingPositions
    InfoTile(
        label = stringResource(R.string.tile_uploads),
        value = if (queued == 0) stringResource(R.string.value_up_to_date) else stringResource(R.string.value_queued, queued),
        detail = if (queued == 0) null else stringResource(R.string.value_queued_detail),
        modifier = modifier,
        indicator = { StatusDot(if (queued == 0) colors.success else colors.warning) },
    )
}

/** Big, stacked buttons: this is answered from the seat, and the confirm is not a "good" green. */
@Composable
private fun FinishDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = EcoTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(EcoTheme.radius.xl)
                .background(colors.surface)
                .padding(EcoTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EcoTheme.spacing.md),
        ) {
            Text(stringResource(R.string.finish_dialog_title), color = colors.ink, style = EcoTheme.type.screenTitle)
            Text(stringResource(R.string.finish_dialog_text), color = colors.inkMuted, style = EcoTheme.type.body)
            PrimaryButton(stringResource(R.string.finish_dialog_confirm), onConfirm, Modifier.fillMaxWidth(), height = EcoTheme.size.control)
            SecondaryButton(stringResource(R.string.finish_dialog_back), onDismiss, Modifier.fillMaxWidth(), height = EcoTheme.size.control)
        }
    }
}

/** The banner's fill and the text colour on it. */
private fun TripHeadline.fill(colors: EcoColors): Pair<Color, Color> = when (severity) {
    TripHeadline.Severity.OK -> colors.successStrong to colors.onSuccessStrong
    TripHeadline.Severity.WARNING -> colors.warning to colors.onWarning
    TripHeadline.Severity.ERROR -> colors.error to colors.onError
}

private fun TripHeadline.titleRes(): Int = when (this) {
    TripHeadline.TRANSMITTING -> R.string.headline_transmitting
    TripHeadline.OFFLINE -> R.string.headline_offline
    TripHeadline.WEAK_GPS -> R.string.headline_weak_gps
    TripHeadline.SEARCHING_GPS -> R.string.headline_searching_gps
    TripHeadline.LOCATION_OFF -> R.string.headline_location_off
}
