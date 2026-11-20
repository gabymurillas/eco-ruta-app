package com.ecoruta.dispositivo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ecoruta.dispositivo.presentation.theme.EcoTheme

/**
 * The flat surface every block sits on: white (or the dark surface), a 1 dp border and no shadow;
 * depth comes from tone, as in the web panel.
 */
@Composable
fun EcoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = EcoTheme.radius.lg
    Column(
        modifier = modifier
            .clip(shape)
            .background(EcoTheme.colors.surface)
            .border(EcoTheme.border.hairline, EcoTheme.colors.border, shape)
            .padding(EcoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xxs),
        content = content,
    )
}
