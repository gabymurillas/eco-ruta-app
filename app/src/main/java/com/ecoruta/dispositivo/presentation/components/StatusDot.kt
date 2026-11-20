package com.ecoruta.dispositivo.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/** A coloured dot; [pulse] adds the slow halo EcoRuta uses for "live". */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, pulse: Boolean = false, size: Dp = EcoTheme.size.dot) {
    val motion = EcoTheme.motion
    val halo = if (pulse) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(motion.pulseMillis), RepeatMode.Restart),
            label = "halo",
        )
        progress
    } else {
        null
    }
    Box(
        modifier
            .size(size)
            .drawBehind {
                if (halo != null) {
                    drawCircle(
                        color.copy(alpha = motion.pulseAlpha * (1 - halo)),
                        radius = this.size.minDimension / 2 * (1 + HALO_GROWTH * halo),
                    )
                }
                drawCircle(color)
            },
    )
}

/** How far the halo grows past the dot, as a multiple of its radius. */
private const val HALO_GROWTH = 1.6f
