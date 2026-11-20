package com.ecoruta.dispositivo.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ecoruta.dispositivo.presentation.theme.EcoTheme
import com.ecoruta.dispositivo.presentation.theme.OnPhoto
import com.ecoruta.dispositivo.presentation.theme.PhotoTextShadow

/** The headline of a screen with a photo behind it: white, with a soft shadow, one line per argument. */
@Composable
fun PhotoTitle(vararg lines: String, modifier: Modifier = Modifier) {
    Text(
        lines.joinToString("\n"),
        color = OnPhoto,
        style = EcoTheme.type.display.copy(shadow = PhotoTextShadow),
        modifier = modifier,
    )
}
