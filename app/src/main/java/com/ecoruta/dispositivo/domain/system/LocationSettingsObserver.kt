package com.ecoruta.dispositivo.domain.system

import kotlinx.coroutines.flow.StateFlow

/** The phone-wide location switch, which no permission can override. */
interface LocationSettingsObserver {
    /**
     * True while location is switched on in the phone's settings. With it off the GPS delivers
     * nothing at all, and the screen must say so instead of an ambiguous "no signal".
     */
    val isLocationEnabled: StateFlow<Boolean>
}
