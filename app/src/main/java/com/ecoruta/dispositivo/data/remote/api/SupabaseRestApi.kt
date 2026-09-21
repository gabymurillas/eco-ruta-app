package com.ecoruta.dispositivo.data.remote.api

import com.ecoruta.dispositivo.data.remote.dto.DeviceHealthDto
import com.ecoruta.dispositivo.data.remote.dto.FinishTripDto
import com.ecoruta.dispositivo.data.remote.dto.IncidentDto
import com.ecoruta.dispositivo.data.remote.dto.NewTripDto
import com.ecoruta.dispositivo.data.remote.dto.ParameterDto
import com.ecoruta.dispositivo.data.remote.dto.RegisterPositionsRequestDto
import com.ecoruta.dispositivo.data.remote.dto.RegisterPositionsResponseDto
import com.ecoruta.dispositivo.data.remote.dto.TripDto
import com.ecoruta.dispositivo.data.remote.dto.UnitRouteDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/** PostgREST endpoints. Row Level Security scopes every write to the device's own unit. */
interface SupabaseRestApi {

    /**
     * Stores readings and evaluates geofences in one call. Inserting straight into `posiciones` is
     * still allowed by RLS but evaluates nothing, and nothing would tell us. A 5xx means nothing
     * was saved, so the caller keeps the readings and retries.
     */
    @POST("functions/v1/registrar-posicion")
    suspend fun registerPositions(@Body body: RegisterPositionsRequestDto): RegisterPositionsResponseDto

    @GET("rest/v1/unidades?select=ruta_id")
    suspend fun getUnitRoute(@Query("id") idFilter: String): List<UnitRouteDto>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/viajes")
    suspend fun createTrip(@Body body: NewTripDto): List<TripDto>

    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/viajes")
    suspend fun finishTrip(@Query("id") idFilter: String, @Body body: FinishTripDto)

    /**
     * Device heartbeat. It is an RPC, not a PATCH on `dispositivos`: the function fixes which
     * columns are written, the unit comes from the JWT, and the timestamp is set by the server.
     */
    @POST("rest/v1/rpc/registrar_salud_del_dispositivo")
    suspend fun reportDeviceHealth(@Body body: DeviceHealthDto)

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/incidencias")
    suspend fun insertIncident(@Body body: IncidentDto)

    @GET("rest/v1/parametros?select=clave,valor")
    suspend fun getParameters(): List<ParameterDto>
}

/** PostgREST equality filter, e.g. `?id=eq.<uuid>`. */
fun eq(value: String): String = "eq.$value"
