package com.ecoruta.dispositivo.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.presentation.theme.ecoColors

/** Every tappable control is at least this tall: it is pressed from the driver's seat. */
val MinTouchHeight = 72.dp

private val PillShape = RoundedCornerShape(percent = 50)

/** The one main action of a screen, in deep terracotta (plain terracotta fails AA with white). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = MinTouchHeight,
) {
    val colors = ecoColors
    val active = enabled && !loading
    Box(
        modifier = modifier
            .heightIn(min = height)
            .clip(PillShape)
            .background(colors.brandStrong.copy(alpha = if (enabled) 1f else DISABLED_ALPHA))
            .clickable(enabled = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = colors.onBrand, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        } else {
            Text(text, color = colors.onBrand, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

/** A secondary action, on glass. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = MinTouchHeight,
) {
    val colors = ecoColors
    Box(
        modifier = modifier
            .heightIn(min = height)
            .glass(strong = true, shape = PillShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = colors.ink.copy(alpha = if (enabled) 1f else DISABLED_ALPHA),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

/** A coloured dot; [pulse] adds the slow 2 s halo EcoRuta uses for "live". */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, pulse: Boolean = false, size: Dp = 12.dp) {
    val halo = if (pulse) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Restart),
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
                    drawCircle(color.copy(alpha = 0.45f * (1 - halo)), radius = this.size.minDimension / 2 * (1 + 1.6f * halo))
                }
                drawCircle(color)
            },
    )
}

/**
 * A headline set as highlighted blocks, one per line: the device's signature look, in Merriweather.
 * Ink blocks with the page colour as text, so it reads at a distance in both themes.
 */
@Composable
fun HighlightTitle(vararg lines: String, modifier: Modifier = Modifier) {
    val colors = ecoColors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            Text(
                line,
                color = colors.background,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.ink)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}

/** A labelled value on glass, with an optional indicator in the top corner. */
@Composable
fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    valueColor: Color = ecoColors.ink,
    indicator: (@Composable () -> Unit)? = null,
) {
    val colors = ecoColors
    GlassPanel(modifier, contentPadding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = colors.inkMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            indicator?.invoke()
        }
        Text(value, color = valueColor, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
        if (detail != null) {
            Text(detail, color = colors.inkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** How serious a message is. Decides the colour, never alone: the text always says it too. */
enum class Tone { INFO, WARNING, ERROR }

/** A short message on glass with a coloured edge, for errors and notices that are not the status. */
@Composable
fun NoticeBanner(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val colors = ecoColors
    val accent = when (tone) {
        Tone.INFO -> colors.info
        Tone.WARNING -> colors.warning
        Tone.ERROR -> colors.error
    }
    Row(
        modifier
            .fillMaxWidth()
            .glass(strong = true, shape = RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(width = 6.dp, height = 36.dp).clip(CircleShape).background(accent))
        Text(text, color = colors.ink, style = MaterialTheme.typography.bodyLarge)
    }
}

private const val DISABLED_ALPHA = 0.45f
private const val PULSE_MILLIS = 2_000
