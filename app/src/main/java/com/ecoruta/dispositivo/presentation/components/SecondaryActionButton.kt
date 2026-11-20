package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * An action inside a card ("Cambiar"): quiet teal fill and a chevron (DESIGN.md §4). Off, it turns
 * into a dashed outline with a lock, so it reads as locked rather than merely faded.
 */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String = text,
) {
    val colors = EcoTheme.colors
    val shape = EcoTheme.radius.md
    val dash = EcoTheme.border.field
    val content = if (enabled) colors.brandText else colors.inkMuted
    Row(
        modifier
            .heightIn(min = EcoTheme.size.controlSmall)
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.background(colors.brandTint)
                } else {
                    Modifier.drawBehind {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        val width = dash.toPx()
                        drawOutline(
                            outline,
                            colors.borderField,
                            style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(width * 4, width * 3))),
                        )
                    }
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = EcoTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xxs),
    ) {
        if (!enabled) Icon(Icons.Outlined.Lock, contentDescription = null, tint = content, modifier = Modifier.size(EcoTheme.size.icon))
        Text(text, color = content, style = EcoTheme.type.buttonSmall)
        if (enabled) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = content, modifier = Modifier.size(EcoTheme.size.icon))
        }
    }
}
