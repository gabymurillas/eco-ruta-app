package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private const val DISABLED_ALPHA = 0.45f
