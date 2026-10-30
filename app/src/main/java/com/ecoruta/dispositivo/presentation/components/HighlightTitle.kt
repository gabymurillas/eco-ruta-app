package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.presentation.theme.ecoColors

/**
 * A headline set as highlighted blocks, one per line: the device's signature look, in Merriweather.
 * Ink blocks with the page colour as text, so it reads at a distance in both themes.
 */
@Composable
fun HighlightTitle(vararg lines: String, modifier: Modifier = Modifier) {
    val colors = ecoColors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            Text(
                line,
                color = colors.background,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.ink)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}
