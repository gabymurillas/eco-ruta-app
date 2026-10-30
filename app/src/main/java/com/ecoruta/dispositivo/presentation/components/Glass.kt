package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.presentation.theme.ecoColors

val GlassShape = RoundedCornerShape(22.dp)

/**
 * The "liquid glass" surface: a translucent fill, a rim that catches light at the top and fades at
 * the bottom, and a soft sheen, over the contour lines of [ScreenScaffold].
 *
 * There is no real backdrop blur on purpose: it only exists from Android 12 (the app supports 8)
 * and it costs frames on the cheap phones these units run on. Status that must be read at a
 * glance never sits on glass; it uses solid fills (see [StatusBanner]).
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    shape: Shape = GlassShape,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.glass(strong, shape).padding(contentPadding), content = content)
}

/** The glass look as a modifier, for surfaces that are not a plain column (buttons, inputs). */
@Composable
fun Modifier.glass(strong: Boolean = false, shape: Shape = GlassShape): Modifier {
    val colors = ecoColors
    val sheen = if (colors.isDark) SHEEN_DARK else SHEEN_LIGHT
    return clip(shape)
        .background(if (strong) colors.glassStrong else colors.glass)
        .background(Brush.verticalGradient(0f to colors.glassRimTop.copy(alpha = sheen), 0.45f to Color.Transparent))
        .border(1.dp, Brush.verticalGradient(listOf(colors.glassRimTop, colors.glassRimBottom)), shape)
}

private const val SHEEN_LIGHT = 0.55f
private const val SHEEN_DARK = 0.10f
