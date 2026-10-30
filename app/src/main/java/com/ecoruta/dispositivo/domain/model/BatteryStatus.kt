package com.ecoruta.dispositivo.domain.model

/** [percent] is null when the battery cannot be read; it is never guessed as 0. */
data class BatteryStatus(
    val percent: Int?,
    val isCharging: Boolean,
)
