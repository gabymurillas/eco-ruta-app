package com.ecoruta.dispositivo.presentation.onboarding

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.provider.Settings
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import kotlin.math.exp

/**
 * A bus driving a fixed route on a live OpenStreetMap map, in a loop: the full route as a faint
 * line, the driven stretch growing over it, the marker turning with the street and the map turning
 * under it, heading-up and tilted like a navigator.
 *
 * Same tiles as the web panel (Leaflet + tile.openstreetmap.org): no key, no account. The tiles are
 * tinted into the night palette of the onboarding. Offline, the tiles that are missing paint as
 * [groundColor], so the route and the bus still read on a plain ground.
 *
 * Decorative: no gestures, no controls. It only moves while [active]; pass false when its page is
 * off screen and it holds still where it was. With animations turned off in the system settings it
 * shows one still frame. The OpenStreetMap credit is the caller's to show, over the map.
 */
@Composable
fun OnboardingLiveRouteMap(
    routePoints: List<LatLon>,
    @DrawableRes markerIconRes: Int,
    routeColor: Color,
    groundColor: Color,
    durationMillis: Long,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    trackColor: Color = routeColor.copy(alpha = TRACK_ALPHA),
    routeWidth: Dp = 6.dp,
) {
    val context = LocalContext.current
    val widthPx = with(LocalDensity.current) { routeWidth.toPx() }
    val reduceMotion = remember { animationsDisabled(context) }
    val scene = remember(routePoints, markerIconRes, routeColor, groundColor, trackColor, widthPx) {
        RouteMapScene(
            context = context,
            track = RouteTrack(routePoints),
            markerIconRes = markerIconRes,
            routeColor = routeColor.toArgb(),
            trackColor = trackColor.toArgb(),
            groundColor = groundColor.toArgb(),
            routeWidthPx = widthPx,
        ).also { it.show(if (reduceMotion) STILL_FRAME else 0f) }
    }

    // osmdroid wants the host's resume/pause (tile threads) and a detach when it leaves for good.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, scene) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> scene.map.onResume()
                Lifecycle.Event.ON_PAUSE -> scene.map.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            scene.map.onDetach()
        }
    }

    LaunchedEffect(scene, active, reduceMotion, durationMillis) {
        if (!active || reduceMotion) return@LaunchedEffect
        while (true) {
            // Resume from where it stopped: leaving the page and coming back does not restart it.
            val first = withFrameNanos { it }
            val start = first - (scene.progress * durationMillis * NANOS_PER_MILLI).toLong()
            var last = first
            while (scene.progress < 1f) {
                val now = withFrameNanos { it }
                val progress = ((now - start) / NANOS_PER_MILLI / durationMillis).toFloat().coerceAtMost(1f)
                // Ease into each new street instead of snapping at the corner.
                val elapsedMs = (now - last) / NANOS_PER_MILLI
                last = now
                scene.show(progress, turnShare = 1f - exp(-elapsedMs / TURN_MILLIS).toFloat())
            }
            delay(END_PAUSE_MILLIS)
            scene.show(0f)
        }
    }

    Box(modifier.clipToBounds().clearAndSetSemantics {}) {
        AndroidView(
            factory = { scene.map },
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    // The map is a plain View, so a layer tilt gives the 3D look. Scaled up so the
                    // far edge, narrowed by the perspective, still covers the box.
                    rotationX = TILT_DEGREES
                    scaleX = TILT_COVER_SCALE
                    scaleY = TILT_COVER_SCALE
                },
        )
        // The map is a picture here: this layer takes every touch so the MapView never sees one,
        // and the swipe still reaches the pager around it.
        Box(Modifier.matchParentSize().pointerInput(Unit) {})
    }
}

/** The MapView and its three overlays, driven frame by frame without recomposing. */
private class RouteMapScene(
    context: Context,
    private val track: RouteTrack,
    @DrawableRes markerIconRes: Int,
    @ColorInt routeColor: Int,
    @ColorInt trackColor: Int,
    @ColorInt groundColor: Int,
    routeWidthPx: Float,
) {
    var progress = 0f
        private set
    private var heading = track.poseAt(0f).heading

    val map: MapView

    private val driven: Polyline
    private val bus: Marker

    init {
        configureOsmdroid(context)
        map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setBackgroundColor(groundColor)
            overlayManager.tilesOverlay.apply {
                setLoadingBackgroundColor(groundColor)
                setLoadingLineColor(groundColor)
                setColorFilter(nightTint(groundColor))
            }
            controller.setZoom(ZOOM)
        }
        val full = Polyline(map).apply {
            setPoints(track.allPoints.map(LatLon::toGeoPoint))
            outlinePaint.styleAsRoute(trackColor, routeWidthPx)
        }
        driven = Polyline(map).apply { outlinePaint.styleAsRoute(routeColor, routeWidthPx) }
        bus = Marker(map).apply {
            icon = ContextCompat.getDrawable(context, markerIconRes)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            // Flat: it turns with the map, so its rotation is a compass bearing (counter-clockwise
            // in osmdroid, hence the minus in show()).
            isFlat = true
        }
        map.overlays.addAll(listOf(full, driven, bus))
    }

    /** Puts the bus at [progress] of the loop; [turnShare] 1 snaps the heading, less eases it. */
    fun show(progress: Float, turnShare: Float = 1f) {
        this.progress = progress
        val pose = track.poseAt(progress)
        heading = approachDegrees(heading, pose.heading, turnShare)
        val here = pose.position.toGeoPoint()
        driven.setPoints(track.travelled(pose).map(LatLon::toGeoPoint))
        bus.position = here
        bus.rotation = -heading
        // Heading-up: turn the map so the street ahead points to the top of the screen.
        map.setMapOrientation(-heading, false)
        map.controller.setCenter(here)
        map.invalidate()
    }
}

private fun LatLon.toGeoPoint() = GeoPoint(lat, lon)

private fun Paint.styleAsRoute(@ColorInt color: Int, width: Float) {
    this.color = color
    strokeWidth = width
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    isAntiAlias = true
}

/**
 * OpenStreetMap's tile policy asks every app to identify itself, and the cache belongs in the app's
 * own folder (no storage permission). Global osmdroid state, so it is set before each MapView.
 */
private fun configureOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(osmdroidBasePath, "tiles")
    }
}

/**
 * Turns the light OpenStreetMap tiles into the night palette: the cream land lands on [ground],
 * the white streets a little lighter, and blocks, water and text fall into darker shades of it.
 * A luminance ramp, steep on purpose: the light tiles only span a narrow band of greys.
 */
private fun nightTint(@ColorInt ground: Int): ColorMatrixColorFilter {
    fun row(channel: Int) = floatArrayOf(
        LUMA_R * TINT_SLOPE, LUMA_G * TINT_SLOPE, LUMA_B * TINT_SLOPE, 0f,
        channel - TINT_SLOPE * LAND_LUMA,
    )
    val r = android.graphics.Color.red(ground)
    val g = android.graphics.Color.green(ground)
    val b = android.graphics.Color.blue(ground)
    return ColorMatrixColorFilter(ColorMatrix(row(r) + row(g) + row(b) + floatArrayOf(0f, 0f, 0f, 1f, 0f)))
}

private fun animationsDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** Whole zoom levels keep the raster tiles sharp (the web panel snaps to them for the same reason). */
private const val ZOOM = 17.0
/** Degrees the map leans back: depth without the far edge turning to a sliver. */
private const val TILT_DEGREES = 40f
private const val TILT_COVER_SCALE = 1.7f
/** Time constant of the bus and the map turning toward a new street. */
private const val TURN_MILLIS = 350.0
/** Rest at the last stop before the loop starts over. */
private const val END_PAUSE_MILLIS = 1_200L
/** Where the still frame sits when animations are off: mid-route, the driven stretch visible. */
private const val STILL_FRAME = 0.6f
private const val TRACK_ALPHA = 0.35f
private const val NANOS_PER_MILLI = 1_000_000.0

private const val LUMA_R = 0.299f
private const val LUMA_G = 0.587f
private const val LUMA_B = 0.114f
/** Luminance (0–255) of the OSM land colour, the one that should come out as the ground. */
private const val LAND_LUMA = 240f
private const val TINT_SLOPE = 1.5f
