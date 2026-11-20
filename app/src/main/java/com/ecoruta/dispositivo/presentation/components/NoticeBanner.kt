package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/** How serious a message is. Decides colour and icon, never alone: the text always says it too. */
enum class Tone { INFO, WARNING, ERROR }

/**
 * A short message on a solid, softly tinted fill with an icon (DESIGN.md §4, NoticeBanner): it
 * reads in direct sunlight, unlike a translucent panel. Warnings and errors are announced.
 */
@Composable
fun NoticeBanner(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val colors = EcoTheme.colors
    val shape = EcoTheme.radius.sm
    val (fill, outline, iconTint) = when (tone) {
        Tone.INFO -> Triple(colors.brandTint, colors.brandTint, colors.brandText)
        Tone.WARNING -> Triple(colors.warningSurface, colors.warningBorder, colors.warning)
        Tone.ERROR -> Triple(colors.errorSurface, colors.errorBorder, colors.error)
    }
    val icon = when (tone) {
        Tone.INFO -> Icons.Outlined.Info
        Tone.WARNING -> Icons.Outlined.WarningAmber
        Tone.ERROR -> Icons.Outlined.ErrorOutline
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(EcoTheme.border.hairline, outline, shape)
            .padding(EcoTheme.spacing.lg)
            .then(if (tone == Tone.INFO) Modifier else Modifier.semantics { liveRegion = LiveRegionMode.Polite }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(EcoTheme.size.icon))
        Text(text, color = colors.ink, style = EcoTheme.type.body, modifier = Modifier.weight(1f))
    }
}
