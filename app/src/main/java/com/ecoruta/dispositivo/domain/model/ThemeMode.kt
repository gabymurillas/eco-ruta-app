package com.ecoruta.dispositivo.domain.model

import java.time.LocalTime

/** How the app picks light or dark. Chosen from the small selector on screen. */
enum class ThemeMode {
    /** Follows the phone's own setting (the default). */
    SYSTEM,

    /** Decides by itself from the time of day, whatever the phone is set to. */
    AUTO,
    LIGHT,
    DARK,
}

/**
 * Night as [ThemeMode.AUTO] understands it. San Juan de los Morros sits near the equator, so
 * sunrise and sunset barely move through the year (about 06:15 and 18:30): fixed hours are enough
 * and need no location or network.
 */
fun isNightAt(time: LocalTime): Boolean = time >= NIGHT_STARTS || time < DAY_STARTS

private val NIGHT_STARTS: LocalTime = LocalTime.of(18, 30)
private val DAY_STARTS: LocalTime = LocalTime.of(6, 0)
