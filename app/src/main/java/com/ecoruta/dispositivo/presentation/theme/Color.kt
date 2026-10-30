package com.ecoruta.dispositivo.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The EcoRuta design system (CLAUDE.md of the backend repository), converted from its oklch()
 * tokens to sRGB. The web only defines the light palette; the dark one keeps the same hues with
 * lightness flipped. Every text/fill pair below was checked against WCAG AA (4.5:1).
 *
 * Three palettes with separate jobs, never mixed: brand (terracotta, identity and the primary
 * action), status (live / warning / error) and the neutral warm surfaces.
 */
@Immutable
data class EcoColors(
    val background: Color,
    val ink: Color,
    val inkMuted: Color,
    val border: Color,

    /** Brand: identity accents. Never a status. */
    val brand: Color,
    /** Fill of the primary action; [onBrand] on top passes AA (plain terracotta does not). */
    val brandStrong: Color,
    val onBrand: Color,

    /** Status fills. Text on them is [onStatus], except on [warning], which takes [onWarning]. */
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val onStatus: Color,
    val onWarning: Color,
    /** Warning hue dark enough to be read as text on [background]. */
    val warningInk: Color,

    /** Liquid glass: translucent fill, a rim lighter at the top, and the contour lines behind. */
    val glass: Color,
    val glassStrong: Color,
    val glassRimTop: Color,
    val glassRimBottom: Color,
    val contour: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val LightEcoColors = EcoColors(
    background = Color(0xFFFBF4EA), // oklch(97% 0.015 75) crema
    ink = Color(0xFF291F18), // oklch(25% 0.02 60)
    inkMuted = Color(0xFF665B53), // oklch(48% 0.02 60), 6:1 on crema
    border = Color(0xFFD8CBC1), // oklch(85% 0.02 60)
    brand = Color(0xFFCB4A2A), // oklch(58% 0.17 35) terracota
    brandStrong = Color(0xFFA03317), // oklch(48% 0.15 35) terracota profundo, 7:1 with white
    onBrand = Color.White,
    success = Color(0xFF007E46), // oklch(52% 0.13 155)
    warning = Color(0xFFD28500), // oklch(68% 0.15 70)
    error = Color(0xFFB00A1D), // oklch(48% 0.19 25)
    info = Color(0xFF0070A6), // oklch(52% 0.12 240)
    onStatus = Color.White,
    onWarning = Color(0xFF291F18),
    warningInk = Color(0xFF6C4302), // oklch(42% 0.09 70)
    glass = Color(0x9EFFFBF6), // superficie at 62 %
    glassStrong = Color(0xE0FFFBF6), // superficie at 88 %
    glassRimTop = Color(0xFFFFFFFF),
    glassRimBottom = Color(0x33291F18),
    contour = Color(0x1A291F18),
    scrim = Color(0x59291F18),
    isDark = false,
)

val DarkEcoColors = EcoColors(
    background = Color(0xFF130E0A), // oklch(17% 0.012 60)
    ink = Color(0xFFF3EDE6), // oklch(95% 0.012 75)
    inkMuted = Color(0xFFBEB6AD), // oklch(78% 0.015 70), 9.6:1
    border = Color(0xFF3E3630),
    brand = Color(0xFFE57255), // oklch(68% 0.15 35)
    brandStrong = Color(0xFFD05F43), // oklch(62% 0.15 35), 5:1 with the dark ink
    onBrand = Color(0xFF130E0A),
    success = Color(0xFF4BC680), // oklch(74% 0.15 155)
    warning = Color(0xFFF2AF48), // oklch(80% 0.14 75)
    error = Color(0xFFF66D67), // oklch(70% 0.17 25)
    info = Color(0xFF5EADE2), // oklch(72% 0.11 240)
    onStatus = Color(0xFF130E0A),
    onWarning = Color(0xFF130E0A),
    warningInk = Color(0xFFF8C379),
    glass = Color(0x14FFFFFF), // white at 8 %
    glassStrong = Color(0x24FFFFFF), // white at 14 %
    glassRimTop = Color(0x40FFFFFF),
    glassRimBottom = Color(0x0FFFFFFF),
    contour = Color(0x14FFFFFF),
    scrim = Color(0x99000000),
    isDark = true,
)

val LocalEcoColors = staticCompositionLocalOf { LightEcoColors }
