package com.ecoruta.dispositivo.domain.model

/** A run from "start" to "finish". The server assigns the id; every position is tied to one. */
data class Trip(
    val id: String,
    val unitId: String,
)
