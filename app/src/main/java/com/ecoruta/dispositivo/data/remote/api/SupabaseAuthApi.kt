package com.ecoruta.dispositivo.data.remote.api

import com.ecoruta.dispositivo.data.remote.dto.PairRequestDto
import com.ecoruta.dispositivo.data.remote.dto.PairResponseDto
import com.ecoruta.dispositivo.data.remote.dto.RefreshRequestDto
import com.ecoruta.dispositivo.data.remote.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/** Endpoints that do not need (or cannot have) a device session yet. */
interface SupabaseAuthApi {

    /** Edge Function that validates the one-time code and returns the device identity + tokens. */
    @POST("functions/v1/vincular-dispositivo")
    suspend fun pair(@Body body: PairRequestDto): PairResponseDto

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenResponseDto
}
