package com.ecoruta.dispositivo.domain.model

/** Identity of a paired device: the unit it belongs to plus the credentials that prove it. */
data class DeviceSession(
    val unitId: String,
    val unitName: String,
    val routeName: String,
    val accessToken: String,
    val refreshToken: String,
)
