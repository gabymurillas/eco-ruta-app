package com.ecoruta.dispositivo.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale, multiples of 4 (DESIGN.md §3.3). */
@Immutable
data class EcoSpacing(
    /** Label to value inside a tile. */
    val xxs: Dp = 4.dp,
    /** Icon to text. */
    val xs: Dp = 8.dp,
    /** Between tiles in a grid and between the bottom bar buttons. */
    val sm: Dp = 12.dp,
    /** Between blocks of a screen body. */
    val md: Dp = 14.dp,
    /** Padding inside cards and notices; bottom margin. */
    val lg: Dp = 16.dp,
    /** Screen side margin. */
    val xl: Dp = 20.dp,
    /** Padding of the status banner. */
    val xxl: Dp = 24.dp,
)

/** Corner radii (DESIGN.md §3.4). */
@Immutable
data class EcoRadius(
    /** Notices and reasons. */
    val sm: RoundedCornerShape = RoundedCornerShape(12.dp),
    /** 52–56 dp controls. */
    val md: RoundedCornerShape = RoundedCornerShape(14.dp),
    /** Cards, tiles, option rows and 84 dp buttons. */
    val lg: RoundedCornerShape = RoundedCornerShape(16.dp),
    /** The status banner. */
    val xl: RoundedCornerShape = RoundedCornerShape(20.dp),
    /** Badges and dots. */
    val full: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

/**
 * Sizes (DESIGN.md §3.5). Everything tappable is pressed from the driver's seat, so nothing goes
 * below [controlSmall].
 */
@Immutable
data class EcoSize(
    val iconButton: Dp = 52.dp,
    val controlSmall: Dp = 56.dp,
    val control: Dp = 72.dp,
    val primaryAction: Dp = 84.dp,
    val optionRow: Dp = 80.dp,
    val tileMin: Dp = 104.dp,
    val routeCardMin: Dp = 112.dp,
    val badgeMin: Dp = 36.dp,
    val icon: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val radioRing: Dp = 26.dp,
    val radioDot: Dp = 12.dp,
    val dot: Dp = 12.dp,
    val dotLarge: Dp = 16.dp,
    /** Width of the Eco mark beside the wordmark; its height follows the mark's proportions. */
    val brandMark: Dp = 30.dp,
    val progress: Dp = 28.dp,
    /** Height of the gradient that melts the welcome photo into the ground behind its text. */
    val photoFade: Dp = 140.dp,
    /** Width of the Eco mark used as a watermark behind the onboarding; it bleeds off the edge. */
    val watermark: Dp = 360.dp,
    /** How high the watermark sits above the bottom edge: behind the words, clear of the button. */
    val watermarkLift: Dp = 160.dp,
    /** Stroke of the route line on the onboarding map. */
    val routeStroke: Dp = 6.dp,
    /** Page indicator: an idle dot and the stretched pill of the current page. */
    val pageDot: Dp = 8.dp,
    val pageDotActive: Dp = 28.dp,
    /** One box of the pairing code: width to height. */
    val codeBoxAspect: Float = 0.82f,
)

/** Stroke widths (DESIGN.md §3.6). */
@Immutable
data class EcoBorder(
    val hairline: Dp = 1.dp,
    val field: Dp = 1.5.dp,
    val focus: Dp = 2.dp,
    val progress: Dp = 3.dp,
)

val LocalEcoSpacing = staticCompositionLocalOf { EcoSpacing() }
val LocalEcoRadius = staticCompositionLocalOf { EcoRadius() }
val LocalEcoSize = staticCompositionLocalOf { EcoSize() }
val LocalEcoBorder = staticCompositionLocalOf { EcoBorder() }
