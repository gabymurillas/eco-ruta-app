package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.presentation.theme.ecoColors

/** A labelled value on glass, with an optional indicator in the top corner. */
@Composable
fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    detail: String? = null,
    valueColor: Color = ecoColors.ink,
    indicator: (@Composable () -> Unit)? = null,
) {
    val colors = ecoColors
    GlassPanel(modifier, strong = strong, contentPadding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = colors.inkMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            indicator?.invoke()
        }
        Text(value, color = valueColor, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
        if (detail != null) {
            Text(detail, color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
