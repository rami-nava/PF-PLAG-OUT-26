package com.example.plag_out

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.io.Serial
import java.time.LocalDate

/*
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
)*/
/*
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
)*/

@Serializable
data class HealthResponse(
    val status: String
)
/*
data class Monitoreo(
    val id: Int,
    val plantacion_id: Int,
    val plaga_id: Int,
    val fecha: Date,
    val gdd_diario: Float,
    val gdd_acumulado: Float,
    val estadio_biologico: String,
    val porc_riesgo_calculado: Float,
    val nivel_alerta: Int
)*/

/*
@Serializable
data class MonitoreoResponse(
    @SerializedName("plantacion")
    val plantacion: PlantacionResponse,
    @SerializedName("monitoreos")
    val monitoreos: List<Monitoreo>
)*/

@Entity(tableName = "monitoreos")
@Serializable
data class MonitoreoResponse(
    @SerializedName("id")
    @PrimaryKey
    val monitoreo_id: Int,
    @SerializedName("plantacion_id")
    val plantacion_id: Int,
    @SerializedName("terreno_id")
    val terreno_id: Int,
    @SerializedName("terreno_nombre")
    val terreno_nombre: String,
    @SerializedName("cultivo_nombre")
    val cultivo_nombre: String,
    @SerializedName("plaga_nombre")
    val plaga_nombre: String,
    @SerializedName("gdd_diario")
    val gdd_diario: Float,
    @SerializedName("gdd_acumulado")
    val gdd_acumulado: Float,
    @SerializedName("gdd_objetivo")
    val gdd_objetivo: Float,
    @SerializedName("progreso")
    val progreso: Float,
    @SerializedName("nivel_alerta")
    val nivel_alerta: Int,
    @SerializedName("fecha_actualizacion")
    val fecha_actualizacion: LocalDate,
    @SerializedName("umbral_riesgo")
    val umbral_riesgo: Int? = null,
    @SerializedName("activo")
    val activo: Boolean = true,
    @SerializedName("fecha_inicio")
    val fecha_inicio: LocalDate? = null,
    @SerializedName("plaga_nombre_cientifico")
    val plaga_nombre_cientifico: String? = null
)

@Entity(tableName = "terrenos")
@Serializable
data class TerrenoResponse(
    @SerializedName("id")
    @PrimaryKey
    val terreno_id: Int,
    @SerializedName("terreno_nombre")
    val terreno_nombre: String,
    @SerializedName("terreno_latitud")
    val terreno_latitud: Float,
    @SerializedName("terreno_longitud")
    val terreno_longitud: Float,
    @SerializedName("terreno_area")
    val terreno_area: Float
)

@Entity(tableName = "plantaciones")
@Serializable
data class PlantacionesResponse(
    @SerializedName("id")
    @PrimaryKey
    val plantacion_id: Int,
    @SerializedName("terreno_id")
    val terreno_id: Int,
    @SerializedName("terreno_nombre")
    val terreno_nombre: String,
    @SerializedName("cultivo_nombre")
    val cultivo_nombre: String,
    @SerializedName("cultivo_nombre_cientifico")
    val cultivo_nombre_cientifico: String,
    @SerializedName("fecha_siembra")
    val fecha_siembra: LocalDate,
    @SerializedName("activa")
    val activa: Boolean
)

@Serializable
data class CreateTerrenoRequest(
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("latitud")
    val latitud: Double,
    @SerializedName("longitud")
    val longitud: Double,
    @SerializedName("area_hectareas")
    val area_hectareas: Double
)

@Serializable
data class TerrenoCreateResponse(
    @SerializedName("id")
    val terreno_id: Int,
    @SerializedName("nombre")
    val terreno_nombre: String,
    @SerializedName("latitud")
    val terreno_latitud: Float,
    @SerializedName("longitud")
    val terreno_longitud: Float,
    @SerializedName("area_hectareas")
    val terreno_area: Float
)

@Serializable
data class CultivoResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("nombre_cientifico")
    val nombre_cientifico: String
)

@Serializable
data class CreatePlantacionRequest(
    @SerializedName("terreno_id")
    val terreno_id: Int,
    @SerializedName("cultivo_id")
    val cultivo_id: Int,
    @SerializedName("fecha_siembra")
    val fecha_siembra: String,
    @SerializedName("activa")
    val activa: Boolean
)

@Serializable
data class PlantacionCreateResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("terreno_id")
    val terreno_id: Int,
    @SerializedName("cultivo_id")
    val cultivo_id: Int,
    @SerializedName("fecha_siembra")
    val fecha_siembra: LocalDate,
    @SerializedName("activa")
    val activa: Boolean,
    @SerializedName("cultivo")
    val cultivo: CultivoResponse
)

@Serializable
data class PlagaResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("nombre_cientifico")
    val nombre_cientifico: String
)

@Serializable
data class MonitoreoRequest(
    @SerializedName("terreno_id")
    val terreno_id: Int,
    @SerializedName("plantacion_id")
    val plantacion_id: Int,
    @SerializedName("plaga_id")
    val plaga_id: Int,
    @SerializedName("umbral_riesgo")
    val umbral_riesgo: Int
)

/**
 * Edición parcial de un monitoreo. Un solo endpoint sirve para las dos acciones de la pantalla de
 * detalle (cambiar umbral y finalizar) — se manda solo el campo que cambió.
 */
@Serializable
data class UpdateMonitoreoRequest(
    @SerializedName("umbral_riesgo")
    val umbral_riesgo: Int? = null,
    @SerializedName("activo")
    val activo: Boolean? = null
)

/** Edición parcial de una plantación. */
@Serializable
data class UpdatePlantacionRequest(
    @SerializedName("activa")
    val activa: Boolean? = null
)

@Serializable
data class CreateUserRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("apellido")
    val apellido: String,
    @SerializedName("cargo_id")
    val cargo_id: Int
)

/**
 * Edición del perfil del usuario actual. Todos los campos son opcionales: se manda solo lo que
 * cambió. Usa `cargo_id` (Int) y no el `cargo` (String) que devuelve [UsuarioResponse], igual que
 * [CreateUserRequest].
 */
@Serializable
data class UpdateUserRequest(
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("apellido")
    val apellido: String? = null,
    @SerializedName("cargo_id")
    val cargo_id: Int? = null
)

@Serializable
data class CargoResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("tipo")
    val tipo: String
)

@Serializable
data class CreateUserResponse(
    @SerializedName("usuario_id")
    val usuario_id: String,
    @SerializedName("created")
    val created: Boolean
)

@Serializable
data class DispositivoRequest(
    @SerializedName("fcm_token")
    val fcm_token: String,
    @SerializedName("plataforma")
    val plataforma: String = "android"
)

@Serializable
data class DispositivoResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("plataforma")
    val plataforma: String
)

@Serializable
data class UsuarioResponse(
    @SerializedName("id")
    val usuario_id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("apellido")
    val apellido: String,
    @SerializedName("cargo")
    val cargo: String,
    @SerializedName("fecha_creacion")
    val fecha_creacion: LocalDate
)
