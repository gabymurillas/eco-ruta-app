package com.ecoruta.dispositivo.data.remote.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the Supabase `apikey` (publishable key) to every request, plus the device's bearer token
 * when it has one. Pairing carries no `Authorization`: the phone has no session yet, and knowing
 * a valid one-time code is what authorizes it.
 */
class SupabaseHeadersInterceptor(
    private val apiKey: String,
    private val accessToken: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().header(HEADER_API_KEY, apiKey)
        accessToken()?.let { request.header(HEADER_AUTHORIZATION, "Bearer $it") }
        return chain.proceed(request.build())
    }

    companion object {
        const val HEADER_API_KEY = "apikey"
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
