package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.presentation.theme.ecoColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Topographic contour lines behind every screen: what the glass panels show through. Drawn once
 * per size and cached, so it costs nothing per frame.
 */
@Composable
fun ContourBackground(modifier: Modifier = Modifier, seed: Float = 1.3f) {
    val color = ecoColors.contour
    Box(
        modifier.fillMaxSize().drawWithCache {
            val base = min(size.width, size.height)
            val hills = listOf(
                hill(Offset(size.width * 0.15f, size.height * 0.42f), base * 0.08f, base * 0.075f, 9, seed),
                hill(Offset(size.width * 0.95f, size.height * 0.12f), base * 0.06f, base * 0.07f, 7, seed + 1.4f),
            )
            val stroke = Stroke(width = 1.dp.toPx())
            onDrawBehind { hills.forEach { drawPath(it, color, style = stroke) } }
        },
    )
}

private fun hill(center: Offset, start: Float, step: Float, rings: Int, seed: Float): Path = Path().apply {
    repeat(rings) { ring ->
        val radius = start + step * ring
        for (i in 0..SEGMENTS) {
            val angle = i.toFloat() / SEGMENTS * 2 * PI.toFloat()
            val wobble = 1 + 0.14f * sin(3 * angle + seed + ring * 0.25f) +
                0.08f * cos(5 * angle - seed * 1.7f) + 0.05f * sin(7 * angle + ring * 0.4f)
            val x = center.x + cos(angle) * radius * wobble * 1.15f
            val y = center.y + sin(angle) * radius * wobble
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/**
 * A stylised route with its stops, for the pairing and idle screens. Purely decorative: it is not
 * the unit's real route, so it is kept off the trip screen, where it could pass for a map.
 */
@Composable
fun RouteIllustration(modifier: Modifier = Modifier) {
    val colors = ecoColors
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val line = Path().apply {
            moveTo(-w * 0.05f, h * 0.15f)
            cubicTo(w * 0.35f, -h * 0.05f, w * 0.75f, h * 0.2f, w * 0.6f, h * 0.55f)
            cubicTo(w * 0.5f, h * 0.8f, w * 0.8f, h, w * 1.05f, h * 0.9f)
        }
        drawPath(line, colors.brand.copy(alpha = 0.7f), style = Stroke(width = 2.5.dp.toPx()))
        STOPS.forEach { (x, y) ->
            val center = Offset(w * x, h * y)
            drawCircle(colors.glassStrong, radius = 13.dp.toPx(), center = center)
            drawCircle(colors.border, radius = 13.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
            drawCircle(colors.ink, radius = 3.dp.toPx(), center = center)
        }
    }
}

private const val SEGMENTS = 72
private val STOPS = listOf(0.28f to 0.07f, 0.66f to 0.3f, 0.6f to 0.58f, 0.84f to 0.92f)
