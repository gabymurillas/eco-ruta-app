package com.ecoruta.dispositivo.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
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

/** Provides every EcoRuta token; screens and components read them through [EcoTheme]. */
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
    CompositionLocalProvider(
        LocalEcoColors provides colors,
        LocalEcoType provides DefaultEcoType,
        LocalEcoSpacing provides EcoSpacing(),
        LocalEcoRadius provides EcoRadius(),
        LocalEcoSize provides EcoSize(),
        LocalEcoBorder provides EcoBorder(),
        LocalEcoMotion provides EcoMotion(),
    ) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = DefaultEcoType.toMaterial(), content = content)
    }
}

/** The tokens in force (DESIGN.md §2): the only way a screen or component gets a value. */
object EcoTheme {
    val colors: EcoColors
        @Composable @ReadOnlyComposable get() = LocalEcoColors.current
    val type: EcoType
        @Composable @ReadOnlyComposable get() = LocalEcoType.current
    val spacing: EcoSpacing
        @Composable @ReadOnlyComposable get() = LocalEcoSpacing.current
    val radius: EcoRadius
        @Composable @ReadOnlyComposable get() = LocalEcoRadius.current
    val size: EcoSize
        @Composable @ReadOnlyComposable get() = LocalEcoSize.current
    val border: EcoBorder
        @Composable @ReadOnlyComposable get() = LocalEcoBorder.current
    val motion: EcoMotion
        @Composable @ReadOnlyComposable get() = LocalEcoMotion.current
}

/**
 * The app may be dark while the phone is light (or the reverse) when the theme is chosen on
 * screen, so the status bar icons follow the app, not the system. The header band is dark in both
 * themes, so its icons stay light either way.
 */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
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

/** Material components (text fields, menus, dialogs) pick these up. */
private fun EcoColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = brandStrong, onPrimary = onBrandStrong, background = background, onBackground = ink,
        surface = surface, onSurface = ink, onSurfaceVariant = inkMuted, outline = borderField, error = error,
        surfaceContainer = surface,
    )
} else {
    lightColorScheme(
        primary = brandStrong, onPrimary = onBrandStrong, background = background, onBackground = ink,
        surface = surface, onSurface = ink, onSurfaceVariant = inkMuted, outline = borderField, error = error,
        surfaceContainer = surface,
    )
}
