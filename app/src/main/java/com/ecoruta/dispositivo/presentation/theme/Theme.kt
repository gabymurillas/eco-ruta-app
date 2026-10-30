package com.ecoruta.dispositivo.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ecoruta.dispositivo.domain.model.ThemeMode
import com.ecoruta.dispositivo.domain.model.isNightAt
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

@Composable
fun EcoRutaTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.AUTO -> isNightNow()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkEcoColors else LightEcoColors
    SystemBarIcons(dark)
    CompositionLocalProvider(LocalEcoColors provides colors) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = EcoTypography, content = content)
    }
}

/**
 * The app may be dark while the phone is light (or the reverse) when the theme is chosen on
 * screen, so the status bar icons follow the app, not the system.
 */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

/** Re-checked every minute, so the unit turns dark at dusk without anyone touching it. */
@Composable
private fun isNightNow(): Boolean {
    val night by produceState(initialValue = isNightAt(LocalTime.now())) {
        while (true) {
            delay(1.minutes)
            value = isNightAt(LocalTime.now())
        }
    }
    return night
}

/** Material components (text fields, dialogs) pick these up; the screens use [EcoColors] directly. */
private fun EcoColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = brandStrong, onPrimary = onBrand, background = background, onBackground = ink,
        surface = background, onSurface = ink, onSurfaceVariant = inkMuted, outline = border, error = error,
    )
} else {
    lightColorScheme(
        primary = brandStrong, onPrimary = onBrand, background = background, onBackground = ink,
        surface = background, onSurface = ink, onSurfaceVariant = inkMuted, outline = border, error = error,
    )
}

/** Shorthand for the EcoRuta palette in force. */
val ecoColors: EcoColors
    @Composable get() = LocalEcoColors.current
