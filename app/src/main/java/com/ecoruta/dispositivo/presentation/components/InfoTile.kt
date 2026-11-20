package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/** A labelled value on a card, with an optional status dot in the top corner. */
@Composable
fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    valueColor: Color = EcoTheme.colors.ink,
    indicator: (@Composable () -> Unit)? = null,
) {
    val colors = EcoTheme.colors
    EcoCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = colors.inkMuted, style = EcoTheme.type.label, modifier = Modifier.weight(1f))
            indicator?.invoke()
        }
        Text(value, color = valueColor, style = EcoTheme.type.value)
        if (detail != null) {
            Text(detail, color = colors.inkMuted, style = EcoTheme.type.label)
        }
    }
}
