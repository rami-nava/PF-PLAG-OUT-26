package com.example.plag_out.Service

import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoResponse
import com.example.plag_out.CreateTerrenoRequest
import com.example.plag_out.TerrenoCreateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

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

    @GET("api/gdd/health")
    suspend fun health(): Response<Unit>
}