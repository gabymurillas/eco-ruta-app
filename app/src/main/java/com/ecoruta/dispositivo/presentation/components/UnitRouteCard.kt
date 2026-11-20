package com.ecoruta.dispositivo.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * The unit's route as the web shows it, shared by "En espera" and "En recorrido": the chosen name
 * with the usual route under it, or just the usual route when nothing was chosen.
 */
@Composable
fun UnitRouteCard(
    normalRouteName: String,
    signName: String?,
    onChange: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    label: String = stringResource(R.string.route_label),
    blockedReason: String? = null,
) {
    val shown = signName ?: normalRouteName
    RouteCard(
        label = label,
        routeName = shown,
        onChange = onChange,
        modifier = modifier,
        normalRouteName = signName?.let { stringResource(R.string.route_normal_detail, normalRouteName) },
        blockedReason = blockedReason,
        nameStyle = if (large) EcoTheme.type.routeNameLarge else EcoTheme.type.routeName,
        changeLabel = stringResource(R.string.route_change),
        changeDescription = stringResource(R.string.route_change_description, shown),
    )
}
