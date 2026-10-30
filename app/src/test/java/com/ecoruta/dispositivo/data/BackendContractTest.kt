package com.ecoruta.dispositivo.data

import com.ecoruta.dispositivo.data.mapper.toDomain
import com.ecoruta.dispositivo.data.mapper.toOperationParams
import com.ecoruta.dispositivo.data.remote.ApiJson
import com.ecoruta.dispositivo.data.remote.dto.DeviceHealthDto
import com.ecoruta.dispositivo.data.remote.dto.PairResponseDto
import com.ecoruta.dispositivo.data.remote.dto.ParameterDto
import com.ecoruta.dispositivo.data.remote.dto.PositionPointDto
import com.ecoruta.dispositivo.data.remote.dto.RegisterPositionsRequestDto
import com.ecoruta.dispositivo.data.remote.dto.RegisterPositionsResponseDto
import com.ecoruta.dispositivo.domain.model.OperationParams
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Pins the app to the payloads documented in the backend's docs/contrato-vinculacion.md. */
class BackendContractTest {

    @Test
    fun `pairing response from the contract maps to a device session`() {
        val body = """
            {
              "unidad": {
                "id": "d97c008a-9dc7-4251-947b-feb3196a5bc7",
                "nombre": "Unidad 12",
                "ruta": "Ruta de prueba · Sector oeste"
              },
              "sesion": {
                "access_token": "eyJ-access",
                "refresh_token": "refresh-1",
                "expires_at": 1758067200
              }
            }
        """.trimIndent()

        val session = ApiJson.decodeFromString<PairResponseDto>(body).toDomain()

        assertEquals("d97c008a-9dc7-4251-947b-feb3196a5bc7", session.unitId)
        assertEquals("Unidad 12", session.unitName)
        assertEquals("Ruta de prueba · Sector oeste", session.routeName)
        assertEquals("eyJ-access", session.accessToken)
        assertEquals("refresh-1", session.refreshToken)
    }

    @Test
    fun `parametros rows override the emission and signal settings`() {
        val rows = ApiJson.decodeFromString<List<ParameterDto>>(
            """
            [
              {"clave":"emision_movimiento_s","valor":8},
              {"clave":"emision_detenido_s","valor":45},
              {"clave":"exactitud_maxima_m","valor":35},
              {"clave":"sin_senal_umbral_s","valor":120},
              {"clave":"bateria_alerta_pct","valor":15}
            ]
            """.trimIndent(),
        )

        val params = rows.associate { it.key to it.value }.toOperationParams()

        assertEquals(8.seconds, params.movingInterval)
        assertEquals(45.seconds, params.stoppedInterval)
        assertEquals(35f, params.maxAccuracyMeters)
        assertEquals(120.seconds, params.gpsStaleAfter)
    }

    @Test
    fun `position upload matches the registrar-posicion contract`() {
        val request = RegisterPositionsRequestDto(
            tripId = "fb3e56a3-0000-0000-0000-000000000000",
            positions = listOf(PositionPointDto(9.9165, -67.379, 12f, 38f, "2026-09-21T01:55:00Z")),
        )

        val root = ApiJson.parseToJsonElement(ApiJson.encodeToString(request)).jsonObject

        // No unidad_id: the server takes the unit from the JWT.
        assertEquals(setOf("viaje_id", "posiciones"), root.keys)
        val point = root.getValue("posiciones").jsonArray.single().jsonObject
        assertEquals(setOf("lat", "lng", "exactitud_m", "velocidad_kmh", "capturado_at"), point.keys)
        assertEquals("-67.379", point.getValue("lng").jsonPrimitive.content)
    }

    @Test
    fun `viaje_id is omitted when there is no open trip`() {
        val request = RegisterPositionsRequestDto(tripId = null, positions = emptyList())

        val root = ApiJson.parseToJsonElement(ApiJson.encodeToString(request)).jsonObject

        assertEquals(setOf("posiciones"), root.keys)
    }

    @Test
    fun `registrar-posicion response from the contract is understood`() {
        val response = ApiJson.decodeFromString<RegisterPositionsResponseDto>(
            """{ "guardadas": 1, "evaluada": true, "notificadas": ["Parada 1"] }""",
        )

        assertEquals(1, response.saved)
        assertTrue(response.evaluated)
        assertEquals(listOf("Parada 1"), response.notified)
    }

    @Test
    fun `heartbeat sends the battery, and omits it when unknown so the server keeps the last level`() {
        assertEquals("""{"bateria":83}""", ApiJson.encodeToString(DeviceHealthDto(battery = 83)))
        assertEquals("{}", ApiJson.encodeToString(DeviceHealthDto(battery = null)))
    }

    @Test
    fun `zero, negative or broken parameters are ignored so a typo cannot flood the server`() {
        val params = mapOf(
            "emision_movimiento_s" to 0.0,
            "emision_detenido_s" to -30.0,
            "exactitud_maxima_m" to Double.NaN,
            "sin_senal_umbral_s" to 240.0,
        ).toOperationParams()

        assertEquals(OperationParams().movingInterval, params.movingInterval)
        assertEquals(OperationParams().stoppedInterval, params.stoppedInterval)
        assertEquals(OperationParams().maxAccuracyMeters, params.maxAccuracyMeters)
        assertEquals(240.seconds, params.gpsStaleAfter)
    }

    @Test
    fun `parameters the server does not define keep their defaults`() {
        val params = mapOf("emision_movimiento_s" to 7.0).toOperationParams()

        assertEquals(7.seconds, params.movingInterval)
        assertEquals(OperationParams().stoppedInterval, params.stoppedInterval)
    }
}
