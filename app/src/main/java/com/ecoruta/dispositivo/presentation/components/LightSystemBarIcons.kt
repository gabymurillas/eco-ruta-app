package com.ecoruta.dispositivo.presentation.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * White navigation bar icons while a screen with the dark photo behind them shows (it is dark in
 * both themes), and the theme's own icons again once it leaves.
 */
@Composable
fun LightSystemBarIcons() {
    val view = LocalView.current
    val dark = EcoTheme.colors.isDark
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    DisposableEffect(window, dark) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightNavigationBars = false
        // The status bar always sits on the dark header band, so only the navigation bar changes.
        onDispose { controller.isAppearanceLightNavigationBars = !dark }
    }
}
