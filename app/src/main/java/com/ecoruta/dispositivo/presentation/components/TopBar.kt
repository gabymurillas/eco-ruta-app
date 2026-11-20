package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * The petrol band at the top of every screen, as in the web panel's menu (DESIGN.md §4, TopBar).
 * It runs behind the status bar, so the system icons stay light in both themes.
 *
 * Without [title] it shows the wordmark; with it, a secondary screen's name and, given [onBack], a
 * back button. [action] goes at the end (the theme selector).
 */
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = EcoTheme.colors
    val spacing = EcoTheme.spacing
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.header)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = EcoTheme.size.iconButton + spacing.xxl)
            .padding(horizontal = spacing.xl, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (onBack != null) {
            HeaderIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
        }
        if (title != null) {
            Text(title, color = colors.onHeader, style = EcoTheme.type.screenTitle, modifier = Modifier.weight(1f))
        } else {
            // Brand colour as identity only: the wordmark, never a status.
            BrandMark(echoColor = colors.onHeader, unitColor = colors.brandOnHeader)
            Text(
                stringResource(R.string.brand_name),
                color = colors.onHeader,
                style = EcoTheme.type.brand,
                modifier = Modifier.weight(1f),
            )
        }
        action?.invoke()
    }
}

/** A square icon button on the header band, with an accessible name. */
@Composable
fun HeaderIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = EcoTheme.colors
    val shape = EcoTheme.radius.md
    Box(
        modifier
            .size(EcoTheme.size.iconButton)
            .clip(shape)
            .border(EcoTheme.border.hairline, colors.onHeaderMuted.copy(alpha = HEADER_OUTLINE_ALPHA), shape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.onHeader)
    }
}

/** The outline only marks the button's edge on the dark band; the icon carries the contrast. */
private const val HEADER_OUTLINE_ALPHA = 0.35f
