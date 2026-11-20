package com.ecoruta.dispositivo.data.remote.mock

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * A stand-in for the Supabase backend, only in the `demo` build: it answers every endpoint the app
 * calls with plausible data, so all screens can be walked through on a phone with no pairing code
 * and no network. Nothing above the HTTP client knows: pairing, trips, telemetry and incidents run
 * their real code paths.
 *
 * Pairing accepts any code except `ERROR`, which answers like an expired code to show that state.
 */
class MockBackendInterceptor : Interceptor {

    private val tripCounter = AtomicInteger()

    @Volatile private var openTripId: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Thread.sleep(LATENCY_MS) // Long enough to see the loading states.
        val path = request.url.encodedPath
        val method = request.method
        return when {
            path.endsWith("/functions/v1/vincular-dispositivo") -> pair(request)
            path.endsWith("/auth/v1/token") -> respond(request, 200, tokens())
            path.endsWith("/functions/v1/registrar-posicion") -> {
                val saved = Regex("\"lat\"").findAll(bodyOf(request)).count()
                respond(request, 200, """{"guardadas":$saved,"evaluada":true,"notificadas":[]}""")
            }
            path.endsWith("/rest/v1/unidades") -> respond(request, 200, """[{"ruta_id":"$ROUTE_ID"}]""")
            path.endsWith("/rest/v1/viajes") && method == "POST" -> openTrip(request)
            path.endsWith("/rest/v1/viajes") && method == "GET" -> respond(request, 200, openTripsJson())
            path.endsWith("/rest/v1/viajes") && method == "PATCH" -> {
                openTripId = null
                respond(request, 204, "")
            }
            path.endsWith("/rest/v1/rpc/registrar_salud_del_dispositivo") -> respond(request, 204, "")
            path.endsWith("/rest/v1/incidencias") -> respond(request, 201, "")
            path.endsWith("/rest/v1/parametros") -> respond(request, 200, "[]")
            else -> respond(request, 404, """{"code":"demo_sin_ruta","message":"El modo demo no simula $method $path"}""")
        }
    }

    private fun pair(request: Request): Response {
        val code = Regex("\"codigo\"\\s*:\\s*\"([^\"]*)\"").find(bodyOf(request))?.groupValues?.get(1).orEmpty()
        if (code.equals(INVALID_CODE, ignoreCase = true)) {
            return respond(request, 401, """{"code":"codigo_invalido","message":"Código inválido o vencido."}""")
        }
        return respond(
            request,
            200,
            """{"unidad":{"id":"$UNIT_ID","nombre":"Unidad 12","ruta":"Centro – UNERG"},"sesion":${tokens()}}""",
        )
    }

    private fun openTrip(request: Request): Response {
        if (openTripId != null) return respond(request, 409, """{"code":"23505","message":"Ya hay un viaje abierto."}""")
        val id = "viaje-demo-${tripCounter.incrementAndGet()}"
        openTripId = id
        return respond(request, 201, """[{"id":"$id","unidad_id":"$UNIT_ID"}]""")
    }

    private fun openTripsJson(): String = openTripId?.let { """[{"id":"$it","unidad_id":"$UNIT_ID"}]""" } ?: "[]"

    private fun tokens(): String = """{"access_token":"demo-access","refresh_token":"demo-refresh-${System.nanoTime()}"}"""

    private fun bodyOf(request: Request): String = request.body?.let { body ->
        Buffer().also { body.writeTo(it) }.readUtf8()
    }.orEmpty()

    private fun respond(request: Request, code: Int, json: String): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code < 400) "OK" else "Error")
        .body(json.toResponseBody(JSON))
        .build()

    private companion object {
        const val LATENCY_MS = 600L
        const val INVALID_CODE = "ERROR"
        const val UNIT_ID = "unidad-demo"
        const val ROUTE_ID = "ruta-demo"
        val JSON = "application/json".toMediaType()
    }
}
