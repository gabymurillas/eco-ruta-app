package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * The route the unit is doing and the way to change it (DESIGN.md §4, RouteCard). [routeName] is
 * what the web shows now; [normalRouteName], when given, is the unit's usual route underneath.
 * With [blockedReason] the button is off and the reason shows below it, never just a faded button.
 */
@Composable
fun RouteCard(
    label: String,
    routeName: String,
    onChange: () -> Unit,
    modifier: Modifier = Modifier,
    normalRouteName: String? = null,
    blockedReason: String? = null,
    nameStyle: TextStyle = EcoTheme.type.routeName,
    changeLabel: String,
    changeDescription: String,
) {
    val colors = EcoTheme.colors
    val spacing = EcoTheme.spacing
    EcoCard(modifier.heightIn(min = EcoTheme.size.routeCardMin)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(label, color = colors.inkMuted, style = EcoTheme.type.label)
                Text(routeName, color = colors.ink, style = nameStyle)
                if (normalRouteName != null) Text(normalRouteName, color = colors.inkMuted, style = EcoTheme.type.label)
            }
            SecondaryActionButton(
                text = changeLabel,
                onClick = onChange,
                enabled = blockedReason == null,
                description = changeDescription,
            )
        }
        if (blockedReason != null) DisabledReason(blockedReason, Modifier.padding(top = spacing.xs))
    }
}

/** Why a control is off, right under it: quiet info, not an error (DESIGN.md §4, DisabledReason). */
@Composable
fun DisabledReason(text: String, modifier: Modifier = Modifier) {
    val colors = EcoTheme.colors
    val spacing = EcoTheme.spacing
    Row(
        modifier
            .fillMaxWidth()
            .clip(EcoTheme.radius.sm)
            .background(colors.brandTint)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.brandText, modifier = Modifier.size(EcoTheme.size.icon))
        Text(text, color = colors.ink, style = EcoTheme.type.label)
    }
}
