package com.ecoruta.dispositivo.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ecoruta.dispositivo.domain.model.DeviceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Durable device state: pairing identity, credentials, the trip in progress and cached parameters. */
class DevicePreferences(
    private val store: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {
    // Decryption goes through the Keystore, so keep it off the main thread that collects this flow.
    val session: Flow<DeviceSession?> = store.data.map { it.toSession() }.flowOn(Dispatchers.Default)

    val activeTripId: Flow<String?> = store.data.map { it[ACTIVE_TRIP_ID] }

    suspend fun saveSession(session: DeviceSession) {
        store.edit {
            it[UNIT_ID] = session.unitId
            it[UNIT_NAME] = session.unitName
            it[ROUTE_NAME] = session.routeName
            it[ACCESS_TOKEN] = cipher.encrypt(session.accessToken)
            it[REFRESH_TOKEN] = cipher.encrypt(session.refreshToken)
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        store.edit {
            it[ACCESS_TOKEN] = cipher.encrypt(accessToken)
            it[REFRESH_TOKEN] = cipher.encrypt(refreshToken)
        }
    }

    suspend fun setActiveTripId(tripId: String?) {
        store.edit {
            if (tripId == null) it.remove(ACTIVE_TRIP_ID) else it[ACTIVE_TRIP_ID] = tripId
        }
    }

    /** Last `parametros` fetched from the backend, so a device that boots offline keeps its calibration. */
    suspend fun cachedParameters(): Map<String, Double>? {
        val json = store.data.first()[PARAMETERS_JSON] ?: return null
        return try {
            Json.decodeFromString<Map<String, Double>>(json)
        } catch (e: SerializationException) {
            null
        }
    }

    suspend fun saveParameters(values: Map<String, Double>) {
        store.edit { it[PARAMETERS_JSON] = Json.encodeToString(values) }
    }

    private fun Preferences.toSession(): DeviceSession? {
        val unitId = this[UNIT_ID] ?: return null
        val accessToken = this[ACCESS_TOKEN]?.let(cipher::decrypt) ?: return null
        val refreshToken = this[REFRESH_TOKEN]?.let(cipher::decrypt) ?: return null
        return DeviceSession(
            unitId = unitId,
            unitName = this[UNIT_NAME].orEmpty(),
            routeName = this[ROUTE_NAME].orEmpty(),
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    private companion object {
        val UNIT_ID = stringPreferencesKey("unit_id")
        val UNIT_NAME = stringPreferencesKey("unit_name")
        val ROUTE_NAME = stringPreferencesKey("route_name")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val ACTIVE_TRIP_ID = stringPreferencesKey("active_trip_id")
        val PARAMETERS_JSON = stringPreferencesKey("parameters_json")
    }
}
