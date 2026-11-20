package com.ecoruta.dispositivo.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens (DESIGN.md §3.1). They are the web admin panel's palette (`.tema-admin` in
 * ecoruta-web/src/app/globals.css), so the device and the panel read as one product.
 *
 * Three groups with separate jobs, never mixed: neutral surfaces and ink, the brand (petrol for the
 * header and the main action, teal as accent) and status (success / warning / error). Every text
 * on fill pair below passes WCAG AA; the ratios are listed in DESIGN.md.
 */
@Immutable
data class EcoColors(
    val background: Color,
    val surface: Color,
    val border: Color,
    /** Outlines that must be seen: secondary buttons, fields, radio rings (3:1 against [surface]). */
    val borderField: Color,
    val ink: Color,
    val inkMuted: Color,

    /** The top bar band. */
    val header: Color,
    val onHeader: Color,
    val onHeaderMuted: Color,

    /** Brand accent: focus and identity. Never a status. */
    val brand: Color,
    /** The unit in the Eco mark on the petrol header: the light teal, so it reads in both themes. */
    val brandOnHeader: Color,
    /** Fill of the main action and of a chosen option. */
    val brandStrong: Color,
    val onBrandStrong: Color,
    /** Quiet brand fill: secondary actions inside a card and informative notices. */
    val brandTint: Color,
    val brandText: Color,

    /** Status dots and icons. */
    val success: Color,
    /** The panel's green fails AA under white text, so solid status fills use this deeper one. */
    val successStrong: Color,
    val onSuccessStrong: Color,
    val warning: Color,
    val onWarning: Color,
    val warningSurface: Color,
    val warningBorder: Color,
    val error: Color,
    val onError: Color,
    val errorSurface: Color,
    val errorBorder: Color,

    /** Dims the background photo in the dark theme. */
    val photoScrim: Color,
    val isDark: Boolean,
)

val LightEcoColors = EcoColors(
    background = Color(0xFFF1F5F6),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFDCE4E7),
    borderField = Color(0xFF78898F),
    ink = Color(0xFF10232A),
    inkMuted = Color(0xFF4B5E65),
    header = Color(0xFF0E2A33),
    onHeader = Color(0xFFFFFFFF),
    onHeaderMuted = Color(0xFFCFE0E4),
    brand = Color(0xFF197878),
    brandOnHeader = Color(0xFF4FC1BE),
    brandStrong = Color(0xFF0E2A33),
    onBrandStrong = Color(0xFFFFFFFF),
    brandTint = Color(0xFFE8EFF1),
    brandText = Color(0xFF145F60),
    success = Color(0xFF1E8E5A),
    successStrong = Color(0xFF137049),
    onSuccessStrong = Color(0xFFFFFFFF),
    warning = Color(0xFFB4531A),
    onWarning = Color(0xFFFFFFFF),
    warningSurface = Color(0xFFF7F5F2),
    warningBorder = Color(0xFFE2DDD6),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorSurface = Color(0xFFFBEAE8),
    errorBorder = Color(0xFFF1C7C2),
    photoScrim = Color(0x00000000),
    isDark = false,
)

val DarkEcoColors = EcoColors(
    background = Color(0xFF0B1B21),
    surface = Color(0xFF11262D),
    border = Color(0xFF24414A),
    borderField = Color(0xFF6E878E),
    ink = Color(0xFFE4EDEF),
    inkMuted = Color(0xFFA9BCC2),
    header = Color(0xFF06141A),
    onHeader = Color(0xFFFFFFFF),
    onHeaderMuted = Color(0xFFCFE0E4),
    brand = Color(0xFF4FC1BE),
    brandOnHeader = Color(0xFF4FC1BE),
    brandStrong = Color(0xFFE4EDEF),
    onBrandStrong = Color(0xFF0B1B21),
    brandTint = Color(0xFF173A44),
    brandText = Color(0xFF4FC1BE),
    success = Color(0xFF3CC283),
    successStrong = Color(0xFF3CC283),
    onSuccessStrong = Color(0xFF0B1B21),
    warning = Color(0xFFF08A4B),
    onWarning = Color(0xFF0B1B21),
    warningSurface = Color(0xFF152A31),
    warningBorder = Color(0xFF5A4130),
    error = Color(0xFFF28B82),
    onError = Color(0xFF0B1B21),
    errorSurface = Color(0xFF2A1B1E),
    errorBorder = Color(0xFF5C2B2B),
    photoScrim = Color(0x73000000),
    isDark = true,
)

/** Text over the welcome photo: always white, since the photo does not change with the theme. */
val OnPhoto = Color.White

/** Secondary text over the photo. */
val OnPhotoMuted = Color(0xCCFFFFFF)

/** The dark ground at the foot of the welcome photo, in both themes. */
val PhotoGround = Color(0xFF0D110D)

val LocalEcoColors = staticCompositionLocalOf { LightEcoColors }
