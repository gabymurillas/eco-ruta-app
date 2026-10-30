package com.ecoruta.dispositivo.presentation.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ecoruta.dispositivo.presentation.theme.ecoColors

/**
 * White system bar icons while a screen with a dark photo behind them shows (it is dark in both
 * themes), and the theme's own icons again once it leaves.
 */
@Composable
fun LightSystemBarIcons() {
    val view = LocalView.current
    val dark = ecoColors.isDark
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    DisposableEffect(window, dark) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
}
