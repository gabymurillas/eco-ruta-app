package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * Common frame of every screen: the petrol [TopBar], a scrollable body (large text settings must
 * never cut content off) and the actions pinned at the bottom, where the thumb reaches them.
 *
 * With a [photo] the body sits on that picture instead of the plain background, dimmed in the dark
 * theme. It does not move when the keyboard opens: only the body does.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    photo: Painter? = null,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    showThemeSelector: Boolean = false,
    bottomBar: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = EcoTheme.colors
    val spacing = EcoTheme.spacing
    Column(modifier.fillMaxSize().background(colors.background)) {
        TopBar(
            title = title,
            onBack = onBack,
            action = if (showThemeSelector) ({ ThemeModeSelector() }) else null,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (photo != null) PhotoBackground(photo)
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = spacing.xl),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    content = content,
                )
                if (bottomBar != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = spacing.xs, bottom = spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        content = bottomBar,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoBackground(photo: Painter) {
    LightSystemBarIcons()
    Image(
        photo,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize(),
    )
    Box(Modifier.fillMaxSize().background(EcoTheme.colors.photoScrim))
}
