package com.example.plag_out

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GDDSimulationRequest(
    val latitude: Double,
    val longitude: Double,
    val startDate: String,
    val currentDate: String,
    val initialGDD: Int,
    val targetGDD: Int,
    val baseTemperature: Double,
    val cropName: String = "Unknown",
    val notes: String = ""
)

@Serializable
data class GDDSimulationResponse(
    @SerializedName("current_gdd")
    val currentGDD: Int,
    @SerializedName("target_gdd")
    val targetGDD: Int,
    @SerializedName("progress_percentage")
    val progressPercentage: Float,
    val date: String,
    @SerializedName("avg_temp")
    val avgTemp: Float,
    @SerializedName("gdd_gained")
    val gddGained: Float,
    @SerializedName("target_reached")
    val targetReached: Boolean,
    val message: String
)

@Serializable
data class HealthResponse(
    val status: String
)