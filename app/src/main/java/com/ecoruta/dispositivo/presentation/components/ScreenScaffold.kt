package com.ecoruta.dispositivo.presentation.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.theme.Merriweather
import com.ecoruta.dispositivo.presentation.theme.ecoColors

/**
 * Common frame of every screen: warm background with contour lines, the top bar, a scrollable
 * body (large text settings must never cut content off) and the actions pinned at the bottom,
 * where the thumb reaches them.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ecoColors
    Box(modifier.fillMaxSize().background(colors.background)) {
        ContourBackground()
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp)) {
            TopBar()
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
private fun TopBar() {
    val colors = ecoColors
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Brand colour as identity only: the wordmark, never a status.
        Box(Modifier.size(10.dp).background(colors.brand, RoundedCornerShape(percent = 50)))
        Text(
            stringResource(R.string.brand_name),
            fontFamily = Merriweather,
            color = colors.brand,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        ThemeModeSelector()
    }
}
