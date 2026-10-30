package com.ecoruta.dispositivo.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ecoruta.dispositivo.domain.model.DeviceSession
import com.ecoruta.dispositivo.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Instant

/** A trip closed on the device whose close has not reached the server yet. */
data class PendingTripClose(
    val tripId: String,
    /** When the driver actually finished it, which is what the server must record. */
    val endedAt: Instant,
)

/**
 * Durable device state: pairing identity, credentials, the trip in progress, cached parameters and
 * the theme chosen on screen.
 */
class DevicePreferences(
    private val store: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {
    // Decryption goes through the Keystore, so keep it off the main thread that collects this flow.
    val session: Flow<DeviceSession?> = store.data.map { it.toSession() }.flowOn(Dispatchers.Default)

    val activeTripId: Flow<String?> = store.data.map { it[ACTIVE_TRIP_ID] }.distinctUntilChanged()

    val pendingTripClose: Flow<PendingTripClose?> = store.data.map { it.toPendingClose() }.distinctUntilChanged()

    /** An unknown stored value (from a later version) falls back to following the phone. */
    val themeMode: Flow<ThemeMode> = store.data
        .map { prefs -> ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE] } ?: ThemeMode.SYSTEM }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    /** Unit of the last pairing, kept even after its session was revoked. */
    suspend fun lastPairedUnitId(): String? = store.data.first()[UNIT_ID]

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

    /**
     * Drops the credentials so the device falls back to the pairing screen. The unit and its trip
     * are kept: re-pairing to the same unit picks up exactly where it was.
     */
    suspend fun clearTokens() {
        store.edit {
            it.remove(ACCESS_TOKEN)
            it.remove(REFRESH_TOKEN)
        }
    }

    suspend fun setActiveTripId(tripId: String) {
        store.edit { it[ACTIVE_TRIP_ID] = tripId }
    }

    /**
     * Ends the active trip on the device in one atomic write: it stops being active and becomes a
     * close waiting to be sent. Returns the closed trip, or null when no trip was active.
     */
    suspend fun closeActiveTrip(endedAt: Instant): PendingTripClose? {
        var closed: PendingTripClose? = null
        store.edit {
            val tripId = it[ACTIVE_TRIP_ID] ?: return@edit
            it.remove(ACTIVE_TRIP_ID)
            it[PENDING_CLOSE_TRIP_ID] = tripId
            it[PENDING_CLOSE_AT_MS] = endedAt.toEpochMilli()
            closed = PendingTripClose(tripId, endedAt)
        }
        return closed
    }

    /** Only clears [tripId]'s close, in case another one was recorded meanwhile. */
    suspend fun clearPendingTripClose(tripId: String) {
        store.edit {
            if (it[PENDING_CLOSE_TRIP_ID] == tripId) {
                it.remove(PENDING_CLOSE_TRIP_ID)
                it.remove(PENDING_CLOSE_AT_MS)
            }
        }
    }

    /** Forgets the open trip and any pending close: they belong to a unit this device left. */
    suspend fun forgetTrips() {
        store.edit {
            it.remove(ACTIVE_TRIP_ID)
            it.remove(PENDING_CLOSE_TRIP_ID)
            it.remove(PENDING_CLOSE_AT_MS)
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

    private fun Preferences.toPendingClose(): PendingTripClose? {
        val tripId = this[PENDING_CLOSE_TRIP_ID] ?: return null
        val endedAtMs = this[PENDING_CLOSE_AT_MS] ?: return null
        return PendingTripClose(tripId, Instant.ofEpochMilli(endedAtMs))
    }

    private companion object {
        val UNIT_ID = stringPreferencesKey("unit_id")
        val UNIT_NAME = stringPreferencesKey("unit_name")
        val ROUTE_NAME = stringPreferencesKey("route_name")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val ACTIVE_TRIP_ID = stringPreferencesKey("active_trip_id")
        val PENDING_CLOSE_TRIP_ID = stringPreferencesKey("pending_close_trip_id")
        val PENDING_CLOSE_AT_MS = longPreferencesKey("pending_close_at_ms")
        val PARAMETERS_JSON = stringPreferencesKey("parameters_json")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
