package com.ecoruta.dispositivo.domain.model

/** What the driver must know before setting off: whether the unit could transmit right now. */
data class Readiness(
    val isOnline: Boolean,
    val isLocationEnabled: Boolean,
)
