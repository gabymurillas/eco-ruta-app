package com.ecoruta.dispositivo.presentation.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.domain.model.TripStatus
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.common.agoText
import com.ecoruta.dispositivo.presentation.common.rememberNow
import com.ecoruta.dispositivo.presentation.components.GlassButton
import com.ecoruta.dispositivo.presentation.components.GlassPanel
import com.ecoruta.dispositivo.presentation.components.InfoTile
import com.ecoruta.dispositivo.presentation.components.NoticeBanner
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.components.ScreenScaffold
import com.ecoruta.dispositivo.presentation.components.StatusDot
import com.ecoruta.dispositivo.presentation.components.Tone
import com.ecoruta.dispositivo.presentation.theme.EcoColors
import com.ecoruta.dispositivo.presentation.theme.Merriweather
import com.ecoruta.dispositivo.presentation.theme.ecoColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun TripRoute(
    onReportIncident: () -> Unit,
    showIncidentSent: Boolean,
    onIncidentSentShown: () -> Unit,
    viewModel: TripViewModel = viewModel(factory = LocalViewModelFactory.current),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TripScreen(state, viewModel::onFinishTrip, onReportIncident, showIncidentSent, onIncidentSentShown)
}

@Composable
fun TripScreen(
    state: TripUiState,
    onFinishTrip: () -> Unit,
    onReportIncident: () -> Unit,
    showIncidentSent: Boolean = false,
    onIncidentSentShown: () -> Unit = {},
) {
    var confirmFinish by remember { mutableStateOf(false) }
    val status = state.status

    ScreenScaffold(
        bottomBar = {
            GlassButton(stringResource(R.string.trip_report_incident), onReportIncident, Modifier.weight(1f), height = 84.dp)
            PrimaryButton(
                stringResource(R.string.trip_finish),
                onClick = { confirmFinish = true },
                modifier = Modifier.weight(1f),
                loading = state.isFinishing,
                height = 84.dp,
            )
        },
    ) {
        StatusBanner(status)
        if (showIncidentSent) IncidentSentNotice(onIncidentSentShown)
        // Tall tiles use the height the decorative route used to take: more to read from the seat.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GpsTile(status, Modifier.weight(1f).heightIn(min = TILE_HEIGHT))
            ConnectionTile(status, Modifier.weight(1f).heightIn(min = TILE_HEIGHT))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PowerTile(status, Modifier.weight(1f).heightIn(min = TILE_HEIGHT))
            UploadsTile(status, Modifier.weight(1f).heightIn(min = TILE_HEIGHT))
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
 * The one thing to read from the seat. A solid fill, never glass: it has to hold its contrast in
 * direct sunlight. Colour, dot and words all say the same, so colour is never the only signal.
 */
@Composable
private fun StatusBanner(status: TripStatus) {
    val colors = ecoColors
    val headline = TripHeadline.of(status)
    val fill = headline.fill(colors)
    val onFill = if (headline.severity == TripHeadline.Severity.WARNING) colors.onWarning else colors.onStatus
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
            .clip(RoundedCornerShape(26.dp))
            .background(fill)
            .padding(horizontal = 22.dp, vertical = 26.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatusDot(onFill, pulse = headline == TripHeadline.TRANSMITTING, size = 16.dp)
            Text(stringResource(headline.titleRes()), color = onFill, fontFamily = Merriweather, style = MaterialTheme.typography.headlineLarge)
        }
        Text(detail, color = onFill, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GpsTile(status: TripStatus, modifier: Modifier) {
    val colors = ecoColors
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
    val colors = ecoColors
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
    val colors = ecoColors
    val charging = status.battery.isCharging
    InfoTile(
        label = stringResource(R.string.tile_power),
        value = stringResource(if (charging) R.string.value_charging else R.string.value_not_charging),
        detail = status.battery.percent?.let { stringResource(R.string.value_battery_percent, it) }
            ?: stringResource(R.string.value_battery_unknown),
        modifier = modifier,
        valueColor = if (charging) colors.ink else colors.warningInk,
        indicator = { StatusDot(if (charging) colors.success else colors.warning) },
    )
}

@Composable
private fun UploadsTile(status: TripStatus, modifier: Modifier) {
    val colors = ecoColors
    val queued = status.pendingPositions
    InfoTile(
        label = stringResource(R.string.tile_uploads),
        value = if (queued == 0) stringResource(R.string.value_up_to_date) else stringResource(R.string.value_queued, queued),
        detail = if (queued == 0) null else stringResource(R.string.value_queued_detail),
        modifier = modifier,
        indicator = { StatusDot(if (queued == 0) colors.success else colors.warning) },
    )
}

/** Confirms the report went out, then gets out of the way. */
@Composable
private fun IncidentSentNotice(onShown: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(NOTICE_DURATION)
        onShown()
    }
    GlassPanel(Modifier.fillMaxWidth(), strong = true, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusDot(ecoColors.success)
            Text(stringResource(R.string.trip_incident_sent), color = ecoColors.ink, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Big, stacked buttons: this is answered from the seat, and the confirm is not a "good" green. */
@Composable
private fun FinishDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = ecoColors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(colors.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.finish_dialog_title), color = colors.ink, fontFamily = Merriweather, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.finish_dialog_text), color = colors.inkMuted, style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(stringResource(R.string.finish_dialog_confirm), onConfirm, Modifier.fillMaxWidth())
            GlassButton(stringResource(R.string.finish_dialog_back), onDismiss, Modifier.fillMaxWidth())
        }
    }
}

private fun TripHeadline.fill(colors: EcoColors): Color = when (severity) {
    TripHeadline.Severity.OK -> colors.success
    TripHeadline.Severity.WARNING -> colors.warning
    TripHeadline.Severity.ERROR -> colors.error
}

private fun TripHeadline.titleRes(): Int = when (this) {
    TripHeadline.TRANSMITTING -> R.string.headline_transmitting
    TripHeadline.OFFLINE -> R.string.headline_offline
    TripHeadline.WEAK_GPS -> R.string.headline_weak_gps
    TripHeadline.SEARCHING_GPS -> R.string.headline_searching_gps
    TripHeadline.LOCATION_OFF -> R.string.headline_location_off
}

private val NOTICE_DURATION = 4.seconds
private val TILE_HEIGHT = 150.dp
