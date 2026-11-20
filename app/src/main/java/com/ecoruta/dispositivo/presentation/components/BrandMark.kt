package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * The EcoRuta "Eco" mark: the unit (a solid disc) and, behind it, two fading echoes of the positions
 * it has sent. "Eco" is Spanish for echo. Same geometry as the masters in docs/brand/ and the
 * launcher icon; drawn here so its colours come from the theme tokens.
 *
 * Decorative: the wordmark next to it already names the brand.
 */
@Composable
fun BrandMark(
    echoColor: Color,
    unitColor: Color,
    modifier: Modifier = Modifier,
    width: Dp = EcoTheme.size.brandMark,
) {
    Canvas(modifier.width(width).aspectRatio(BOX_W / BOX_H)) {
        val k = size.width / BOX_W
        val r = RADIUS * k
        val cy = CENTRE_Y * k
        for ((x, thickness) in ECHOES) {
            drawArc(
                color = echoColor,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x * k - r, cy - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(width = thickness * k),
            )
        }
        drawCircle(unitColor, radius = r, center = Offset(UNIT_X * k, cy))
    }
}

// Geometry in the master's units (docs/brand/ecoruta-mark.svg, a 180 × 104 box).
private const val BOX_W = 180f
private const val BOX_H = 104f
private const val RADIUS = 46f
private const val CENTRE_Y = 52f
private const val UNIT_X = 134f

/** Centre x and thickness of each echo, nearest first: each one further back and thinner. */
private val ECHOES = listOf(90f to 12f, 50f to 8f)
