package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.presentation.theme.ecoColors

/** How serious a message is. Decides the colour, never alone: the text always says it too. */
enum class Tone { INFO, WARNING, ERROR }

/** A short message on glass with a coloured edge, for errors and notices that are not the status. */
@Composable
fun NoticeBanner(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val colors = ecoColors
    val accent = when (tone) {
        Tone.INFO -> colors.info
        Tone.WARNING -> colors.warning
        Tone.ERROR -> colors.error
    }
    Row(
        modifier
            .fillMaxWidth()
            .glass(strong = true, shape = RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(width = 6.dp, height = 36.dp).clip(CircleShape).background(accent))
        Text(text, color = colors.ink, style = MaterialTheme.typography.bodyLarge)
    }
}
