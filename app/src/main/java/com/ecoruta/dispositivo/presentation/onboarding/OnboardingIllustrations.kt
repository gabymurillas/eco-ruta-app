package com.ecoruta.dispositivo.presentation.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.ecoruta.dispositivo.presentation.theme.EcoTheme
import kotlin.math.floor

// Decorative art for the welcome carousel; colours, strokes and sizes come from the tokens.

/** 0→1 over one loop, forever. */
@Composable
private fun loopProgress(label: String): Float {
    val transition = rememberInfiniteTransition(label = label)
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(EcoTheme.motion.loopMillis, easing = LinearEasing), RepeatMode.Restart),
        label = label,
    )
    return progress
}

/** The pairing code types itself in, one box at a time, and is confirmed with a check. */
@Composable
fun PairingCodeIllustration(modifier: Modifier = Modifier) {
    val colors = EcoTheme.colors
    val border = EcoTheme.border
    val t = loopProgress("code")
    val typed = floor(t / TYPING_SHARE * SAMPLE_CODE.length).toInt().coerceIn(0, SAMPLE_CODE.length)
    val done = typed == SAMPLE_CODE.length
    val checkScale = if (done) ((t - TYPING_SHARE) / (1 - TYPING_SHARE) * CHECK_POP).coerceIn(0f, 1f) else 0f
    Column(
        modifier.padding(horizontal = EcoTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xxl),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xs)) {
            SAMPLE_CODE.forEachIndexed { index, char ->
                val isNext = index == typed
                val shape = EcoTheme.radius.lg
                Box(
                    Modifier
                        .width(EcoTheme.size.iconButton)
                        .aspectRatio(EcoTheme.size.codeBoxAspect)
                        .clip(shape)
                        .background(colors.surface)
                        .border(
                            if (isNext || done) border.focus else border.field,
                            if (done) colors.success else if (isNext) colors.brand else colors.borderField,
                            shape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index < typed) Text(char.toString(), color = colors.ink, style = EcoTheme.type.code)
                }
            }
        }
        Box(
            Modifier.size(EcoTheme.size.iconButton).scale(checkScale).clip(EcoTheme.radius.full).background(colors.success),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onSuccessStrong, modifier = Modifier.size(EcoTheme.size.icon))
        }
    }
}

private const val SAMPLE_CODE = "4T9K2"
/** Share of the loop spent typing; the rest holds the finished code with its check. */
private const val TYPING_SHARE = 0.6f
/** How fast the check pops in, relative to the time left in the loop. */
private const val CHECK_POP = 4f
