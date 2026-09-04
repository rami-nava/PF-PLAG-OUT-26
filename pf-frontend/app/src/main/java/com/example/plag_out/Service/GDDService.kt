package com.example.plag_out.Service

import com.example.plag_out.AccountDeletionResponse
import com.example.plag_out.CargoResponse
import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoResponse
import com.example.plag_out.CreateTerrenoRequest
import com.example.plag_out.TerrenoCreateResponse
import com.example.plag_out.UpdateTerrenoRequest
import com.example.plag_out.CultivoResponse
import com.example.plag_out.CreatePlantacionRequest
import com.example.plag_out.MonitoreoRequest
import com.example.plag_out.UpdateMonitoreoRequest
import com.example.plag_out.UpdatePlantacionRequest
import com.example.plag_out.PlagaResponse
import com.example.plag_out.PlantacionCreateResponse
import com.example.plag_out.CreateUserRequest
import com.example.plag_out.CreateUserResponse
import com.example.plag_out.UsuarioResponse
import com.example.plag_out.UpdateUserRequest
import com.example.plag_out.DispositivoRequest
import com.example.plag_out.DispositivoResponse
import com.example.plag_out.MarcarLeidaResponse
import com.example.plag_out.NotificacionResponse
import com.example.plag_out.CreateReporteRequest
import com.example.plag_out.ReporteResponse
import com.example.plag_out.ReporteDetalleResponse
import com.example.plag_out.PrediccionConfirmacionRequest
import com.example.plag_out.PrediccionConfirmacionResponse
import com.example.plag_out.PrediccionDetalleResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.Path

interface GDDService {
    /*@POST("api/gdd/simulate-day")
    suspend fun simulateDay(@Body data: GDDSimulationRequest): Response<GDDSimulationResponse>
    */
    @GET("/monitoreos")
    suspend fun getMonitoreos(): Response<List<MonitoreoResponse>>

    @GET("/terrenos")
    suspend fun getTerrenos(): Response<List<TerrenoResponse>>

    @GET("/plantaciones")
    suspend fun getPlantaciones(): Response<List<PlantacionesResponse>>

    @POST("/terrenos")
    suspend fun createTerreno(@Body data: CreateTerrenoRequest): Response<TerrenoCreateResponse>

    @PATCH("/terrenos/{id}")
    suspend fun actualizarTerreno(
        @Path("id") id: Int,
        @Body data: UpdateTerrenoRequest
    ): Response<TerrenoResponse>

    @DELETE("/terrenos/{id}")
    suspend fun eliminarTerreno(@Path("id") id: Int): Response<Unit>

    @GET("/cultivos")
    suspend fun getCultivos(): Response<List<CultivoResponse>>

    @GET("/plagas")
    suspend fun getPlagas(): Response<List<PlagaResponse>>

    @POST("/plantaciones")
    suspend fun createPlantacion(@Body data: CreatePlantacionRequest): Response<PlantacionCreateResponse>

    @PATCH("/plantaciones/{id}")
    suspend fun actualizarPlantacion(
        @Path("id") id: Int,
        @Body data: UpdatePlantacionRequest
    ): Response<PlantacionesResponse>

    @DELETE("/plantaciones/{id}")
    suspend fun eliminarPlantacion(@Path("id") id: Int): Response<Unit>

    @POST("/monitoreos")
    suspend fun createMonitoreo(@Body data: MonitoreoRequest): Response<MonitoreoResponse>

    @GET("/monitoreos/{id}")
    suspend fun getMonitoreo(@Path("id") id: Int): Response<MonitoreoResponse>

    @PATCH("/monitoreos/{id}")
    suspend fun actualizarMonitoreo(
        @Path("id") id: Int,
        @Body data: UpdateMonitoreoRequest
    ): Response<MonitoreoResponse>

    @POST("/usuarios")
    suspend fun createUser(@Body data: CreateUserRequest): Response<CreateUserResponse>

    // El backend identifica al usuario por el JWT del header Authorization
    @GET("/usuarios/me")
    suspend fun getUsuarioActual(): Response<UsuarioResponse>

    @PATCH("/usuarios/me")
    suspend fun actualizarUsuario(@Body data: UpdateUserRequest): Response<UsuarioResponse>

    // Baja definitiva de la cuenta del usuario del JWT (y de todos sus datos en el backend)
    @DELETE("/usuarios/me")
    suspend fun eliminarCuenta(): Response<AccountDeletionResponse>

    // Registra (upsert) el token FCM del dispositivo para el usuario del JWT
    @POST("/usuarios/fcm-token")
    suspend fun registrarDispositivo(@Body data: DispositivoRequest): Response<DispositivoResponse>

    // Elimina el token FCM del usuario actual (logout)
    @DELETE("/usuarios/fcm-token/{fcm_token}")
    suspend fun eliminarDispositivo(@Path("fcm_token") fcmToken: String): Response<Unit>

    // Historial in-app del usuario del JWT. El backend devuelve solo las no leídas.
    @GET("/notificaciones")
    suspend fun getNotificaciones(): Response<List<NotificacionResponse>>

    @PATCH("/notificaciones/{id}")
    suspend fun marcarNotificacionLeida(@Path("id") id: Int): Response<MarcarLeidaResponse>

    @GET("/cargos")
    suspend fun getCargos(): Response<List<CargoResponse>>

    @POST("/reportes")
    suspend fun createReporte(@Body data: CreateReporteRequest): Response<ReporteResponse>

    @POST("/api/reports")
    suspend fun createReporteApi(@Body data: CreateReporteRequest): Response<ReporteResponse>

    @GET("/reportes/{id}")
    suspend fun getReporte(@Path("id") id: Int): Response<ReporteDetalleResponse>

    @GET("/reportes")
    suspend fun getReportes(): Response<List<ReporteDetalleResponse>>

    @GET("/api/v1/predicciones/{id}")
    suspend fun getPrediccion(@Path("id") id: Int): Response<PrediccionDetalleResponse>

    @POST("/api/v1/predicciones/{id}/confirmacion")
    suspend fun confirmarPrediccion(
        @Path("id") id: Int,
        @Body data: PrediccionConfirmacionRequest
    ): Response<PrediccionConfirmacionResponse>

    @GET("api/gdd/health")
    suspend fun health(): Response<Unit>
}
