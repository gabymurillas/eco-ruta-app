package com.ecoruta.dispositivo.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/** Motion tokens (DESIGN.md §3.7): ease-out, no bounce. */
@Immutable
data class EcoMotion(
    /** State changes and selection. */
    val fastMillis: Int = 200,
    /** The slow "live" halo of the status banner. */
    val pulseMillis: Int = 2_000,
    /** Halo opacity at its start; it fades to nothing as it grows. */
    val pulseAlpha: Float = 0.45f,
    /** One cycle of an onboarding illustration's loop. */
    val loopMillis: Int = 4_000,
    /** Page change in the onboarding carousel. */
    val pageMillis: Int = 350,
    /** How far an illustration drifts behind the page as it slides, as a share of its width. */
    val parallax: Float = 0.35f,
    /** Opacity of a disabled control's content. */
    val disabledAlpha: Float = 0.45f,
)

val LocalEcoMotion = staticCompositionLocalOf { EcoMotion() }
