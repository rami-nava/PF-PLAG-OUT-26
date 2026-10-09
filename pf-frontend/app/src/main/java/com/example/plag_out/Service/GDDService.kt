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
import com.example.plag_out.ReportesMapaResponse
import com.example.plag_out.PrediccionConfirmacionRequest
import com.example.plag_out.PrediccionConfirmacionResponse
import com.example.plag_out.PrediccionDetalleResponse
import com.example.plag_out.ConsentimientoModeloRequest
import com.example.plag_out.ConsentimientoModeloResponse
import com.example.plag_out.PaginaUsuarios
import com.example.plag_out.PlagaAdmin
import com.example.plag_out.PlagaAdminRequest
import com.example.plag_out.UsuarioAdmin
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

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

    @PATCH("/monitoreos/{id}")
    suspend fun actualizarUmbralAlertaMl(
        @Path("id") id: Int,
        @Body data: JsonObject
    ): Response<MonitoreoResponse>

    @DELETE("/monitoreos/{id}")
    suspend fun eliminarMonitoreo(@Path("id") id: Int): Response<Unit>

    @POST("/usuarios")
    suspend fun createUser(@Body data: CreateUserRequest): Response<CreateUserResponse>

    // El backend identifica al usuario por el JWT del header Authorization
    @GET("/usuarios/me")
    suspend fun getUsuarioActual(): Response<UsuarioResponse>

    @PATCH("/usuarios/me")
    suspend fun actualizarUsuario(@Body data: UpdateUserRequest): Response<UsuarioResponse>

    @GET("/api/v1/usuarios/me/consentimiento-modelo")
    suspend fun getConsentimientoModelo(): Response<ConsentimientoModeloResponse>

    @PATCH("/api/v1/usuarios/me/consentimiento-modelo")
    suspend fun actualizarConsentimientoModelo(
        @Body data: ConsentimientoModeloRequest
    ): Response<ConsentimientoModeloResponse>

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

    @GET("/reportes/{id}")
    suspend fun getReporte(@Path("id") id: Int): Response<ReporteDetalleResponse>

    @GET("/reportes")
    suspend fun getReportes(
        @retrofit2.http.Query("fecha_desde") fechaDesde: String? = null,
        @retrofit2.http.Query("fecha_hasta") fechaHasta: String? = null
    ): Response<List<ReporteDetalleResponse>>

    @GET("/mapa/reportes")
    suspend fun getReportesMapa(
        @Query("sur") sur: Double,
        @Query("oeste") oeste: Double,
        @Query("norte") norte: Double,
        @Query("este") este: Double,
        @Query("zoom") zoom: Int,
        @Query("nivel") nivel: String,
        @Query("celda_grados") celdaGrados: Double? = null,
        @Query("limite") limite: Int? = null,
        @Query("ambito") ambito: String? = null,
        @Query("radio_km") radioKm: Int? = null,
        @Query("dias") dias: Int? = null,
        @Query("severidad") severidad: String? = null,
        @Query("plaga") plaga: String? = null,
        @Query("cultivo") cultivo: String? = null
    ): Response<ReportesMapaResponse>

    @DELETE("/reportes/{id}")
    suspend fun deleteReporte(@Path("id") reporteId: Int): Response<Unit>

    @GET("/api/v1/predicciones/{id}")
    suspend fun getPrediccion(@Path("id") id: Int): Response<PrediccionDetalleResponse>

    @POST("/api/v1/predicciones/{id}/confirmacion")
    suspend fun confirmarPrediccion(
        @Path("id") id: Int,
        @Body data: PrediccionConfirmacionRequest
    ): Response<PrediccionConfirmacionResponse>

    @POST("/monitoreos/{id}/observaciones")
    suspend fun registrarBiofix(@Path("id") id: Int, @Body data: com.example.plag_out.BiofixRequest): Response<com.example.plag_out.BiofixResult>

    @GET("/ciclos/{id}")
    suspend fun getCiclo(@Path("id") id: Int): Response<com.example.plag_out.GddCicloResponse>

    @POST("/ciclos/{id}/archivar")
    suspend fun archivarCiclo(@Path("id") id: Int): Response<com.example.plag_out.GddCicloResponse>

    @GET("api/gdd/health")
    suspend fun health(): Response<Unit>

    // ── Administración ──────────────────────────────────────────────────────
    // El backend valida que el usuario del JWT sea admin: 403 {"detail": "requiere_admin"} si no.

    @GET("/admin/plagas")
    suspend fun adminGetPlagas(
        @Query("incluir_inactivas") incluirInactivas: Boolean = true
    ): Response<List<PlagaAdmin>>

    @POST("/admin/plagas")
    suspend fun adminCrearPlaga(@Body data: PlagaAdminRequest): Response<PlagaAdmin>

    @PATCH("/admin/plagas/{id}")
    suspend fun adminActualizarPlaga(
        @Path("id") id: Int,
        @Body data: PlagaAdminRequest
    ): Response<PlagaAdmin>

    // Baja lógica: la plaga queda con activo = false. Se reactiva con PATCH {"activo": true}.
    @DELETE("/admin/plagas/{id}")
    suspend fun adminDesactivarPlaga(@Path("id") id: Int): Response<PlagaAdmin>

    // Solo lista usuarios base: los admins no se gestionan desde la app.
    @GET("/admin/usuarios")
    suspend fun adminGetUsuarios(
        @Query("q") q: String? = null,
        @Query("activo") activo: Boolean? = null,
        @Query("pagina") pagina: Int = 1,
        @Query("tamanio") tamanio: Int = 20
    ): Response<PaginaUsuarios>

    @POST("/admin/usuarios/{id}/suspender")
    suspend fun adminSuspenderUsuario(@Path("id") id: String): Response<UsuarioAdmin>

    @POST("/admin/usuarios/{id}/reactivar")
    suspend fun adminReactivarUsuario(@Path("id") id: String): Response<UsuarioAdmin>

    @DELETE("/admin/usuarios/{id}")
    suspend fun adminEliminarUsuario(@Path("id") id: String): Response<AccountDeletionResponse>
}
