package com.ecoruta.dispositivo.data.remote

import kotlinx.serialization.json.Json

/** Shared JSON configuration for every backend call. */
val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
