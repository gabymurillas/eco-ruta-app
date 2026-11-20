package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/** A small pill naming a state, like the web's "En vivo" badge: a dot plus words, never colour alone. */
@Composable
fun StatusBadge(text: String, dotColor: Color, modifier: Modifier = Modifier) {
    val colors = EcoTheme.colors
    val shape = EcoTheme.radius.full
    Row(
        modifier
            .heightIn(min = EcoTheme.size.badgeMin)
            .clip(shape)
            .background(colors.surface)
            .border(EcoTheme.border.hairline, colors.border, shape)
            .padding(horizontal = EcoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xs),
    ) {
        StatusDot(dotColor)
        Text(text, color = colors.ink, style = EcoTheme.type.label)
    }
}
