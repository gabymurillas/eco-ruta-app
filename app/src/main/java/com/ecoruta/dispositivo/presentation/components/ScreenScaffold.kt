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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.theme.Merriweather
import com.ecoruta.dispositivo.presentation.theme.OnPhoto
import com.ecoruta.dispositivo.presentation.theme.PhotoNightScrim
import com.ecoruta.dispositivo.presentation.theme.ecoColors

/**
 * Common frame of every screen: warm background with contour lines, the top bar, a scrollable
 * body (large text settings must never cut content off) and the actions pinned at the bottom,
 * where the thumb reaches them.
 *
 * With a [photo] the background is that picture instead, dimmed in the dark theme, with the top bar
 * and system bar icons in white. It does not move when the keyboard opens: only the body does.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    photo: Painter? = null,
    bottomBar: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ecoColors
    Box(modifier.fillMaxSize().background(colors.background)) {
        if (photo == null) {
            ContourBackground()
        } else {
            PhotoBackground(photo)
        }
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp)) {
            TopBar(onPhoto = photo != null)
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
            if (bottomBar != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    content = bottomBar,
                )
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
    if (ecoColors.isDark) Box(Modifier.fillMaxSize().background(PhotoNightScrim))
}

/** Brand and theme selector. Over a photo the wordmark turns white: terracotta does not read on it. */
@Composable
private fun TopBar(onPhoto: Boolean) {
    val colors = ecoColors
    val brandColor = if (onPhoto) OnPhoto else colors.brand
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Brand colour as identity only: the wordmark, never a status.
        Box(Modifier.size(10.dp).background(brandColor, RoundedCornerShape(percent = 50)))
        Text(
            stringResource(R.string.brand_name),
            fontFamily = Merriweather,
            color = brandColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        ThemeModeSelector()
    }
}
