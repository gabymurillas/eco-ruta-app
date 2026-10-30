package com.ecoruta.dispositivo.presentation.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.components.LightSystemBarIcons
import com.ecoruta.dispositivo.presentation.theme.LightEcoColors
import com.ecoruta.dispositivo.presentation.theme.OnPhoto
import com.ecoruta.dispositivo.presentation.theme.PhotoGround

/**
 * First thing a new device shows: los Morros at dusk across the screen and, at its foot, what the
 * device is for. It looks the same in both themes, since the photo does not change. The only
 * gradient is the one that melts the photo into the ground behind the text; the title itself
 * sits on that dark ground, so it needs no box of its own.
 */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    LightSystemBarIcons()
    Column(Modifier.fillMaxSize().background(PhotoGround)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Image(
                painterResource(R.drawable.welcome_photo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(FADE_HEIGHT)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, PhotoGround))),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.welcome_place).uppercase(),
                color = OnPhoto.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
            )
            Text(stringResource(R.string.welcome_title), color = OnPhoto, style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.welcome_text), color = OnPhoto.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge)
            StartButton(onStart, Modifier.padding(top = 10.dp))
        }
    }
}

/** Cream on the dark ground: the brand's terracotta would sink into the dusk colours. */
@Composable
private fun StartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(LightEcoColors.background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.welcome_start),
            color = LightEcoColors.ink,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

private val FADE_HEIGHT = 140.dp
