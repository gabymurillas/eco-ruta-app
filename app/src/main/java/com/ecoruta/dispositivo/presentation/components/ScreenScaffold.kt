package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.presentation.theme.Merriweather
import com.ecoruta.dispositivo.presentation.theme.ecoColors

/** The theme in force and how to change it, provided once at the root for the top bar selector. */
data class ThemeModeControl(val mode: ThemeMode, val onChange: (ThemeMode) -> Unit)

val LocalThemeModeControl = staticCompositionLocalOf { ThemeModeControl(ThemeMode.SYSTEM) {} }

/**
 * Common frame of every screen: warm background with contour lines, the top bar, a scrollable
 * body (large text settings must never cut content off) and the actions pinned at the bottom,
 * where the thumb reaches them.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ecoColors
    Box(modifier.fillMaxSize().background(colors.background)) {
        ContourBackground()
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp)) {
            TopBar()
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
            if (bottomBar != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    content = bottomBar,
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    val colors = ecoColors
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Brand colour as identity only: the wordmark, never a status.
        Box(Modifier.size(10.dp).background(colors.brand, RoundedCornerShape(percent = 50)))
        Text(
            stringResource(R.string.brand_name),
            fontFamily = Merriweather,
            color = colors.brand,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        ThemeModeButton()
    }
}

/** The small selector: follow the phone, decide by the hour, or force light or dark. */
@Composable
private fun ThemeModeButton() {
    val control = LocalThemeModeControl.current
    val colors = ecoColors
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.theme_selector, stringResource(control.mode.labelRes()))
    Box {
        Box(
            Modifier
                .size(52.dp)
                .glass(strong = true, shape = RoundedCornerShape(percent = 50))
                .clickable(role = Role.Button, onClickLabel = description) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(control.mode.icon(), contentDescription = description, tint = colors.ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(mode.labelRes()), style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { Icon(mode.icon(), contentDescription = null) },
                    trailingIcon = if (mode == control.mode) {
                        { Text("✓", style = MaterialTheme.typography.bodyLarge) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        control.onChange(mode)
                    },
                    modifier = Modifier.padding(vertical = 4.dp),
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
