package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/** The theme in force and how to change it, provided once at the root for the top bar selector. */
data class ThemeModeControl(val mode: ThemeMode, val onChange: (ThemeMode) -> Unit)

val LocalThemeModeControl = staticCompositionLocalOf { ThemeModeControl(ThemeMode.SYSTEM) {} }

/** The selector in the top bar: follow the phone, decide by the hour, or force light or dark. */
@Composable
fun ThemeModeSelector() {
    val control = LocalThemeModeControl.current
    val colors = EcoTheme.colors
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.theme_selector, stringResource(control.mode.labelRes()))
    Box {
        HeaderIconButton(control.mode.icon(), description, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(mode.labelRes()), style = EcoTheme.type.body, color = colors.ink) },
                    leadingIcon = { Icon(mode.icon(), contentDescription = null, tint = colors.ink) },
                    trailingIcon = if (mode == control.mode) {
                        { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.theme_selected), tint = colors.brand) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        control.onChange(mode)
                    },
                    modifier = Modifier.heightIn(min = EcoTheme.size.controlSmall),
                )
            }
        }
    }
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Filled.PhoneAndroid
    ThemeMode.AUTO -> Icons.Filled.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Filled.LightMode
    ThemeMode.DARK -> Icons.Filled.DarkMode
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.AUTO -> R.string.theme_auto
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
