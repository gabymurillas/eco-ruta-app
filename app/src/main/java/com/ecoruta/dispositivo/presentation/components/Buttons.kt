package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/** The one main action of a screen: petrol fill (DESIGN.md §4, PrimaryButton). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = EcoTheme.size.primaryAction,
) {
    val colors = EcoTheme.colors
    val active = enabled && !loading
    // Faded but opaque: over a photo, a see-through button would let the picture show through.
    val fill = if (enabled) {
        colors.brandStrong
    } else {
        colors.brandStrong.copy(alpha = EcoTheme.motion.disabledAlpha).compositeOver(colors.background)
    }
    Box(
        modifier = modifier
            .heightIn(min = height)
            .clip(EcoTheme.radius.lg)
            .background(fill)
            .clickable(enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = EcoTheme.spacing.xl, vertical = EcoTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = colors.onBrandStrong,
                strokeWidth = EcoTheme.border.progress,
                modifier = Modifier.size(EcoTheme.size.progress),
            )
        } else {
            Text(text, color = colors.onBrandStrong, style = EcoTheme.type.button, textAlign = TextAlign.Center)
        }
    }
}

/** A secondary action: plain surface with a visible outline (DESIGN.md §4, SecondaryButton). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = EcoTheme.size.primaryAction,
) {
    val colors = EcoTheme.colors
    val shape = EcoTheme.radius.lg
    Box(
        modifier = modifier
            .heightIn(min = height)
            .clip(shape)
            .background(colors.surface)
            .border(EcoTheme.border.field, colors.borderField, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = EcoTheme.spacing.xl, vertical = EcoTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) colors.ink else colors.inkMuted,
            style = EcoTheme.type.button,
            textAlign = TextAlign.Center,
        )
    }
}
