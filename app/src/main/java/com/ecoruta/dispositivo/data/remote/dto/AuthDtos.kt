package com.ecoruta.dispositivo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PairRequestDto(
    @SerialName("codigo") val code: String,
)

/** Response of `vincular-dispositivo` (see docs/contrato-vinculacion.md). */
@Serializable
data class PairResponseDto(
    @SerialName("unidad") val unit: PairedUnitDto,
    @SerialName("sesion") val session: PairedSessionDto,
)

@Serializable
data class PairedUnitDto(
    @SerialName("id") val id: String,
    @SerialName("nombre") val name: String,
    /** Display name of the route, e.g. "Ruta de prueba · Sector oeste". Not an id. */
    @SerialName("ruta") val routeName: String? = null,
)

@Serializable
data class PairedSessionDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshRequestDto(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)
