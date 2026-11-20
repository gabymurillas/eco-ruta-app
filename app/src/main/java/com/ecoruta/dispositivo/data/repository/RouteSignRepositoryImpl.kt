package com.ecoruta.dispositivo.data.repository

import com.ecoruta.dispositivo.core.AppError
import com.ecoruta.dispositivo.core.Outcome
import com.ecoruta.dispositivo.core.map
import com.ecoruta.dispositivo.data.local.prefs.DevicePreferences
import com.ecoruta.dispositivo.data.remote.ApiJson
import com.ecoruta.dispositivo.data.remote.api.SupabaseRestApi
import com.ecoruta.dispositivo.data.remote.dto.RouteSignDto
import com.ecoruta.dispositivo.data.remote.dto.RouteSignsDto
import com.ecoruta.dispositivo.data.remote.safeApiCall
import com.ecoruta.dispositivo.domain.model.RouteSign
import com.ecoruta.dispositivo.domain.model.RouteSignOptions
import com.ecoruta.dispositivo.domain.repository.RouteSignRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Route names through the backend's two `letreros` RPCs, with the options and the sign in force
 * cached in DataStore: the screens read the cache, so they show the right name with no signal.
 */
class RouteSignRepositoryImpl(
    private val preferences: DevicePreferences,
    private val api: SupabaseRestApi,
) : RouteSignRepository {

    override val current: Flow<RouteSign?> = preferences.routeSign.map { it?.let { (id, name) -> RouteSign(id, name) } }

    override suspend fun cachedOptions(): RouteSignOptions? {
        val json = preferences.cachedRouteSignOptions() ?: return null
        val cached = try {
            ApiJson.decodeFromString<RouteSignsDto>(json)
        } catch (e: SerializationException) {
            return null
        }
        return cached.toDomain()
    }

    override suspend fun refresh(adoptCurrent: Boolean): Outcome<RouteSignOptions> =
        safeApiCall { api.getRouteSigns(JsonObject(emptyMap())) }.map { dto ->
            preferences.cacheRouteSignOptions(ApiJson.encodeToString(RouteSignsDto.serializer(), dto))
            if (adoptCurrent) preferences.setRouteSign(dto.current?.let { it.id to it.name })
            dto.toDomain()
        }

    override suspend fun change(signId: String?): Outcome<RouteSign?> {
        val body = JsonObject(mapOf(PARAM_SIGN_ID to (signId?.let(::JsonPrimitive) ?: JsonNull)))
        val result = safeApiCall { api.changeRouteSign(body) }
        return when {
            // The RPC answers 400 only for a sign this unit may not use (inactive or withdrawn).
            result is Outcome.Failure && result.error == AppError.Rejected(HTTP_BAD_REQUEST) ->
                Outcome.Failure(AppError.RouteSignNotAllowed)
            else -> result.map { dto ->
                // What the server says is in force is what the web shows; the screen shows the same.
                val sign = dto.current?.toDomain()
                preferences.setRouteSign(sign?.let { it.id to it.name })
                sign
            }
        }
    }

    override suspend fun resetLocal() = preferences.setRouteSign(null)

    private fun RouteSignsDto.toDomain() = RouteSignOptions(current?.toDomain(), options.map { it.toDomain() })

    private fun RouteSignDto.toDomain() = RouteSign(id, name)

    private companion object {
        const val PARAM_SIGN_ID = "p_letrero_id"
        const val HTTP_BAD_REQUEST = 400
    }
}
