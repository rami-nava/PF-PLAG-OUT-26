package com.example.plag_out.Service

import com.example.plag_out.GDDSimulationRequest
import com.example.plag_out.GDDSimulationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GDDService {
    @POST("api/gdd/simulate-day")
    suspend fun simulateDay(@Body data: GDDSimulationRequest): Response<GDDSimulationResponse>

    @GET("api/gdd/health")
    suspend fun health(): Response<Unit>
}