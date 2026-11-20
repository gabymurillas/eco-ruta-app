package com.ecoruta.dispositivo.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ecoruta.dispositivo.R

/**
 * Figtree, the admin panel's typeface. Bundled, not downloadable: a unit may boot for the first
 * time with no network.
 */
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
    Font(R.font.figtree_extrabold, FontWeight.ExtraBold),
)

/**
 * Type roles (DESIGN.md §3.2), sized to be read from the driver's seat at a glance: the status line
 * is the largest thing on screen, values come next, and even labels stay at 16 sp.
 */
@Immutable
data class EcoType(
    val display: TextStyle,
    val status: TextStyle,
    val screenTitle: TextStyle,
    val value: TextStyle,
    val routeName: TextStyle,
    val routeNameLarge: TextStyle,
    val option: TextStyle,
    val brand: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    /** One character per box of the pairing code; tabular figures keep the boxes even. */
    val code: TextStyle,
    /** Map credits only: a licence note nobody reads while driving, so it may go below 16. */
    val credit: TextStyle,
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Figtree,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

val DefaultEcoType = EcoType(
    display = style(34, 40, FontWeight.ExtraBold, -0.02),
    status = style(32, 38, FontWeight.ExtraBold, -0.02),
    screenTitle = style(26, 32, FontWeight.ExtraBold, -0.01),
    value = style(28, 34, FontWeight.Bold),
    routeName = style(24, 30, FontWeight.ExtraBold, -0.01),
    routeNameLarge = style(26, 32, FontWeight.ExtraBold, -0.01),
    option = style(22, 28, FontWeight.Bold),
    brand = style(22, 28, FontWeight.ExtraBold, -0.01),
    button = style(20, 24, FontWeight.ExtraBold),
    buttonSmall = style(18, 22, FontWeight.Bold),
    body = style(18, 26, FontWeight.Medium),
    label = style(16, 20, FontWeight.SemiBold),
    code = style(34, 40, FontWeight.Bold).copy(fontFeatureSettings = "tnum"),
    credit = style(12, 16, FontWeight.Medium),
)

/** Material components (text fields, menus, dialogs) pick these up. */
internal fun EcoType.toMaterial() = Typography(
    displaySmall = display,
    headlineLarge = status,
    headlineSmall = screenTitle,
    titleLarge = value,
    titleMedium = option,
    bodyLarge = body,
    bodyMedium = label,
    labelLarge = button,
    labelMedium = label,
)

/**
 * A soft shadow under white text on the photo: it keeps the letters legible where the sky turns
 * lighter, without a box over the picture.
 */
val PhotoTextShadow = Shadow(color = Color(0x80000000), offset = Offset(0f, 2f), blurRadius = 12f)

val LocalEcoType = staticCompositionLocalOf { DefaultEcoType }
