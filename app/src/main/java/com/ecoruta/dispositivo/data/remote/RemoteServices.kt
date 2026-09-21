package com.ecoruta.dispositivo.data.remote

import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.remote.api.SupabaseAuthApi
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.auth.SessionAuthenticator
import com.ecoruta.dispositivo.data.remote.auth.SupabaseHeadersInterceptor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

data class SupabaseConfig(
    val baseUrl: String,
    val anonKey: String,
    val enableHttpLogging: Boolean,
)

/** Builds the HTTP stack: one client for pairing/refresh, one authenticated client for everything else. */
class RemoteServices(
    private val config: SupabaseConfig,
    preferences: DevicePreferences,
) {
    val authApi: SupabaseAuthApi = retrofit(
        client = baseClient(accessToken = { null }).build(),
    ).create(SupabaseAuthApi::class.java)

    val restApi: SupabaseRestApi = retrofit(
        client = baseClient(accessToken = { currentAccessToken(preferences) })
            .authenticator(SessionAuthenticator(preferences, authApi))
            .build(),
    ).create(SupabaseRestApi::class.java)

    private fun baseClient(accessToken: () -> String?): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(SupabaseHeadersInterceptor(config.anonKey, accessToken))
            .apply { if (config.enableHttpLogging) addInterceptor(loggingInterceptor()) }

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader(SupabaseHeadersInterceptor.HEADER_AUTHORIZATION)
        redactHeader(SupabaseHeadersInterceptor.HEADER_API_KEY)
    }

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()

    /** OkHttp interceptors run on a network thread, so blocking on the (cached) DataStore read is fine. */
    private fun currentAccessToken(preferences: DevicePreferences): String? =
        runBlocking { preferences.session.first()?.accessToken }

    private companion object {
        const val TIMEOUT_SECONDS = 20L
    }
}
