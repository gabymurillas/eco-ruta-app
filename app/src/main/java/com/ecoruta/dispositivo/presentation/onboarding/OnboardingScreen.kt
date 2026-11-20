package com.ecoruta.dispositivo.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoruta.dispositivo.R
import com.ecoruta.dispositivo.presentation.common.LocalViewModelFactory
import com.ecoruta.dispositivo.presentation.components.BrandMark
import com.ecoruta.dispositivo.presentation.components.LightSystemBarIcons
import com.ecoruta.dispositivo.presentation.components.PrimaryButton
import com.ecoruta.dispositivo.presentation.theme.DarkEcoColors
import com.ecoruta.dispositivo.presentation.theme.EcoTheme
import com.ecoruta.dispositivo.presentation.theme.LocalEcoColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = viewModel(factory = LocalViewModelFactory.current)) {
    OnboardingScreen(onFinish = viewModel::onFinish)
}

/** One page of the carousel: its words and what draws above them. */
private enum class OnboardingPage(@StringRes val title: Int, @StringRes val text: Int) {
    WELCOME(R.string.onboarding_1_title, R.string.onboarding_1_text),
    LIVE(R.string.onboarding_2_title, R.string.onboarding_2_text),
    PAIRING(R.string.onboarding_3_title, R.string.onboarding_3_text),
}

/**
 * The welcome carousel, shown once per phone. It is always on the dark petrol ground, whatever the
 * theme: it opens over los Morros at dusk and the art is drawn in light on dark. The Eco mark sits
 * behind everything as a watermark and drifts a little with each page.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    CompositionLocalProvider(LocalEcoColors provides DarkEcoColors) {
        LightSystemBarIcons()
        val pages = OnboardingPage.entries
        // One page ahead stays composed, so the map is already loading while the welcome is read.
        val pager = rememberPagerState { pages.size }
        val scope = rememberCoroutineScope()
        val spacing = EcoTheme.spacing
        val isLast = pager.currentPage == pages.lastIndex
        Box(Modifier.fillMaxSize().background(EcoTheme.colors.header)) {
            Watermark(drift = { pager.currentPage + pager.currentPageOffsetFraction })
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                TopRow(showSkip = !isLast, onSkip = onFinish)
                HorizontalPager(state = pager, beyondViewportPageCount = 1, modifier = Modifier.weight(1f)) { index ->
                    PageContent(pages[index], pageOffset(pager, index), active = pager.currentPage == index)
                }
                Column(
                    Modifier.padding(horizontal = spacing.xl).padding(bottom = spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.xl),
                ) {
                    PageIndicator(pager.currentPage, pages.size)
                    PrimaryButton(
                        stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next),
                        onClick = {
                            if (isLast) {
                                onFinish()
                            } else {
                                scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** How far [page] is from the centre: 0 when it is the page shown, ±1 one page away. */
private fun pageOffset(pager: PagerState, page: Int): Float =
    (pager.currentPage - page) + pager.currentPageOffsetFraction

/**
 * The Eco mark, large and faint, bleeding off the right edge behind the words. [drift] is read while
 * drawing, not composing: it changes every frame of a swipe.
 */
@Composable
private fun Watermark(drift: () -> Float) {
    val colors = EcoTheme.colors
    val size = EcoTheme.size
    Box(Modifier.fillMaxSize()) {
        BrandMark(
            echoColor = colors.onHeader,
            unitColor = colors.onHeader,
            width = size.watermark,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = size.watermark * WATERMARK_BLEED, y = -size.watermarkLift)
                .graphicsLayer {
                    alpha = WATERMARK_ALPHA
                    translationX = drift() * WATERMARK_DRIFT.toPx()
                },
        )
    }
}

@Composable
private fun TopRow(showSkip: Boolean, onSkip: () -> Unit) {
    val colors = EcoTheme.colors
    val spacing = EcoTheme.spacing
    Row(
        Modifier.fillMaxWidth().heightIn(min = EcoTheme.size.iconButton + spacing.lg).padding(horizontal = spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        BrandMark(echoColor = colors.onHeader, unitColor = colors.brandOnHeader)
        Text(stringResource(R.string.brand_name), color = colors.onHeader, style = EcoTheme.type.brand, modifier = Modifier.weight(1f))
        if (showSkip) {
            TextButton(
                onClick = onSkip,
                shape = EcoTheme.radius.md,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onHeaderMuted),
                modifier = Modifier.heightIn(min = EcoTheme.size.controlSmall),
            ) {
                Text(stringResource(R.string.onboarding_skip), style = EcoTheme.type.buttonSmall)
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage, offset: Float, active: Boolean) {
    val colors = EcoTheme.colors
    val spacing = EcoTheme.spacing
    val parallax = EcoTheme.motion.parallax
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        // The art drifts behind the words as the page slides and fades out at the edge.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offset * size.width * parallax
                    alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.Center,
        ) {
            when (page) {
                OnboardingPage.WELCOME -> MorrosPhoto()
                OnboardingPage.LIVE -> LiveRouteArt(active)
                OnboardingPage.PAIRING -> PairingCodeIllustration()
            }
        }
        Column(Modifier.padding(horizontal = spacing.xl), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            if (page == OnboardingPage.WELCOME) {
                Text(stringResource(R.string.onboarding_place), color = colors.brand, style = EcoTheme.type.label)
            }
            Text(stringResource(page.title), color = colors.onHeader, style = EcoTheme.type.display)
            Text(stringResource(page.text), color = colors.onHeaderMuted, style = EcoTheme.type.body)
        }
    }
}

/** Los Morros at dusk, melting into the petrol ground under the words. */
@Composable
private fun MorrosPhoto() {
    val ground = EcoTheme.colors.header
    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.welcome_photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize(),
        )
        BottomFade(ground, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * A bus driving through San Juan on the OpenStreetMap map, the same one the web panel shows. Offline
 * the tiles stay plain ground and the route still draws over it.
 */
@Composable
private fun LiveRouteArt(active: Boolean) {
    val colors = EcoTheme.colors
    val spacing = EcoTheme.spacing
    Box(Modifier.fillMaxSize()) {
        OnboardingLiveRouteMap(
            routePoints = SanJuanDemoRoute,
            markerIconRes = R.drawable.ic_route_marker,
            routeColor = colors.brandOnHeader,
            groundColor = colors.header,
            durationMillis = ROUTE_LOOP_MILLIS,
            active = active,
            routeWidth = EcoTheme.size.routeStroke,
            modifier = Modifier.fillMaxSize(),
        )
        // Soft edges: the map melts into the ground at the top and under the words.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(EcoTheme.size.photoFade / 2)
                .background(Brush.verticalGradient(listOf(colors.header, Color.Transparent))),
        )
        BottomFade(colors.header, Modifier.align(Alignment.BottomCenter))
        // OpenStreetMap's licence requires the credit to be visible wherever its tiles show.
        Text(
            stringResource(R.string.map_attribution),
            color = colors.onHeaderMuted,
            style = EcoTheme.type.credit,
            modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = spacing.xl),
        )
        OfflineChip(Modifier.align(Alignment.BottomCenter).padding(bottom = spacing.lg))
    }
}

@Composable
private fun BottomFade(ground: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(EcoTheme.size.photoFade)
            .background(Brush.verticalGradient(listOf(Color.Transparent, ground))),
    )
}

/** "Sin señal también se guarda": the offline promise, as a Material tonal chip on the map. */
@Composable
private fun OfflineChip(modifier: Modifier = Modifier) {
    val colors = EcoTheme.colors
    val spacing = EcoTheme.spacing
    Surface(
        modifier = modifier,
        shape = EcoTheme.radius.full,
        color = colors.surface,
        contentColor = colors.onHeaderMuted,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = colors.brandOnHeader, modifier = Modifier.size(EcoTheme.size.icon))
            Text(stringResource(R.string.onboarding_offline_chip), style = EcoTheme.type.label)
        }
    }
}

/** Dots for the pages; the current one stretches into a teal pill. Read aloud as "Paso 2 de 3". */
@Composable
private fun PageIndicator(current: Int, count: Int) {
    val colors = EcoTheme.colors
    val size = EcoTheme.size
    val description = stringResource(R.string.onboarding_page, current + 1, count)
    Row(
        Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(EcoTheme.spacing.xs),
    ) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(
                if (active) size.pageDotActive else size.pageDot,
                tween(EcoTheme.motion.pageMillis),
                label = "dot",
            )
            val color by animateColorAsState(
                if (active) colors.brand else colors.onHeaderMuted.copy(alpha = INACTIVE_DOT_ALPHA),
                tween(EcoTheme.motion.pageMillis),
                label = "dotColor",
            )
            Box(Modifier.height(size.pageDot).width(width).clip(EcoTheme.radius.full).background(color))
        }
    }
}

private const val INACTIVE_DOT_ALPHA = 0.4f
/** Faint enough to stay behind the words: the mark is felt more than read. */
private const val WATERMARK_ALPHA = 0.07f
/** Share of the watermark that hangs past the right edge. */
private const val WATERMARK_BLEED = 0.32f
/** How far the watermark slides left with each page. */
private val WATERMARK_DRIFT = (-28).dp
/** One loop of the bus along the demo route (about 1.7 km). */
private const val ROUTE_LOOP_MILLIS = 18_000L
