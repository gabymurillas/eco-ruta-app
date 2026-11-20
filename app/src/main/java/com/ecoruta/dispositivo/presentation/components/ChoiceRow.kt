package com.ecoruta.dispositivo.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * One option of a single choice, big enough to hit from the seat (DESIGN.md §4, ChoiceRow). The
 * chosen one fills with petrol; a radio ring and the label say it too, so colour is never alone.
 * Put the rows in a `Modifier.selectableGroup()` column.
 */
@Composable
fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    val colors = EcoTheme.colors
    val size = EcoTheme.size
    val shape = EcoTheme.radius.lg
    val fast = tween<Color>(EcoTheme.motion.fastMillis)
    val fill by animateColorAsState(if (selected) colors.brandStrong else colors.surface, fast, label = "fill")
    val content by animateColorAsState(if (selected) colors.onBrandStrong else colors.ink, fast, label = "content")
    val muted = if (selected) colors.onBrandStrong else colors.inkMuted
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = size.optionRow)
            .clip(shape)
            .background(fill)
            .border(EcoTheme.border.hairline, if (selected) colors.brandStrong else colors.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = EcoTheme.spacing.xl, vertical = EcoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.lg),
    ) {
        RadioRing(selected, ringColor = if (selected) colors.onBrandStrong else colors.borderField, dotColor = content)
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size.iconLarge))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = content, style = EcoTheme.type.option)
            if (subtitle != null) Text(subtitle, color = muted, style = EcoTheme.type.label)
        }
    }
}

@Composable
private fun RadioRing(selected: Boolean, ringColor: Color, dotColor: Color) {
    val size = EcoTheme.size
    Box(
        Modifier
            .size(size.radioRing)
            .clip(EcoTheme.radius.full)
            .border(EcoTheme.border.focus, ringColor, EcoTheme.radius.full),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(size.radioDot).clip(EcoTheme.radius.full).background(dotColor))
    }
}
