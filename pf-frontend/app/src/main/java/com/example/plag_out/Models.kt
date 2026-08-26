package com.example.plag_out

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.io.Serial
import java.time.LocalDate
import java.time.LocalDateTime

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
    @SerializedName("cultivo_id")
    val cultivo_id: Int,
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
data class UpdateTerrenoRequest(
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("area_hectareas")
    val area_hectareas: Double? = null
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

/**
 * [cultivos_afectados] son los **ids** de los cultivos que esta plaga afecta (el backend manda
 * `[1, 3]`, no objetos): se usa para ofrecer solo las plagas que tienen sentido para la plantación
 * elegida (ver `AgregarMonitoreoViewModel.filtrarPlagas`).
 *
 * Es nullable porque el campo es opcional en el backend y Gson no aplica los valores por defecto
 * de Kotlin cuando la clave no viene en el JSON.
 */
@Serializable
data class PlagaResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("nombre_cientifico")
    val nombre_cientifico: String,
    @SerializedName("cultivos_afectados")
    val cultivos_afectados: List<Int>? = null
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


@Serializable
data class UpdateMonitoreoRequest(
    @SerializedName("umbral_riesgo")
    val umbral_riesgo: Int? = null,
    @SerializedName("activo")
    val activo: Boolean? = null
)

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

/**
 * Resultado de la baja de cuenta.
 *
 * [auth_identity_deleted] en `false` significa que el usuario se borró del backend pero sobrevivió
 * en Supabase Auth: la sesión del dispositivo sigue siendo válida y ese mismo login podría recrear
 * la cuenta, así que la app igual cierra sesión y lo deja registrado.
 * [retained_reports] son los reportes que el backend conserva (anonimizados) pese a la baja.
 */
@Serializable
data class AccountDeletionResponse(
    @SerializedName("deleted")
    val deleted: Boolean,
    @SerializedName("auth_identity_deleted")
    val auth_identity_deleted: Boolean,
    @SerializedName("retained_reports")
    val retained_reports: Int
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

/**
 * Notificación in-app. El backend devuelve solo las **no leídas**, así que esta lista es
 * exactamente lo que hay pendiente de leer: su tamaño es el número del badge.
 *
 * [entidad_id] es el id de la entidad a la que apunta la notificación, interpretado según
 * [tipo] (ver `destinoDe`).
 */
@Serializable
data class NotificacionResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("titulo")
    val titulo: String,
    @SerializedName("mensaje")
    val mensaje: String,
    @SerializedName("leido")
    val leido: Boolean,
    @SerializedName("tipo")
    val tipo: String,
    @SerializedName("fecha_envio")
    val fecha_envio: LocalDateTime,
    @SerializedName("entidad_id")
    val entidad_id: Int? = null
)

/** Respuesta del PATCH que marca una notificación como leída. */
@Serializable
data class MarcarLeidaResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("leido")
    val leido: Boolean
)

/**
 * Ruta a la que lleva tocar una notificación
 */
fun destinoDe(notificacion: NotificacionResponse): String? {
    val id = notificacion.entidad_id
    val tipo = notificacion.tipo.uppercase()
    return when {
        tipo.contains("REPORTE") -> {
            if (id != null) "ver_reporte/$id" else "reportes"
        }
        tipo.contains("GDD") || tipo.contains("MONITOREO") -> {
            if (id != null) "monitoreo/$id" else "monitoreos"
        }
        tipo.contains("BIOFIX") || tipo.contains("PLANTACION") -> {
            if (id != null) "plantacion/$id" else "terrenos"
        }
        id != null -> "ver_reporte/$id"
        else -> null
    }
}

@Entity(tableName = "usuario")
@Serializable
data class UsuarioResponse(
    @SerializedName("id")
    @PrimaryKey
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

@Serializable
data class CreateReporteRequest(
    @SerializedName("terreno_id")
    val terreno_id: Int? = null,
    @SerializedName("plantacion_id")
    val plantacion_id: Int,
    @SerializedName("plaga_id")
    val plaga_id: Int,
    @SerializedName("nivel_severidad")
    val nivel_severidad: String,
    @SerializedName("latitud")
    val latitud: Double?,
    @SerializedName("longitud")
    val longitud: Double?,
    @SerializedName("timestamp_ms")
    val timestamp_ms: Long,
    @SerializedName("etapa_biologica")
    val etapa_biologica: String? = null
)

@Serializable
data class ReporteResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("plantacion_id")
    val plantacion_id: Int? = null,
    @SerializedName("plaga_id")
    val plaga_id: Int,
    @SerializedName("nivel_severidad")
    val nivel_severidad: String,
    @SerializedName("latitud")
    val latitud: Double?,
    @SerializedName("longitud")
    val longitud: Double?,
    @SerializedName("timestamp_ms")
    val timestamp_ms: Long
)

@Serializable
data class ReporteDetalleResponse(
    @SerializedName("id")               val id: Int,
    @SerializedName("plantacion_id")    val plantacion_id: Int? = null,
    @SerializedName("plaga_id")         val plaga_id: Int,
    @SerializedName("plaga_nombre")     val plaga_nombre: String,
    @SerializedName("terreno_nombre")   val terreno_nombre: String? = null,
    @SerializedName("cultivo_nombre")   val cultivo_nombre: String? = null,
    @SerializedName("nivel_severidad")  val nivel_severidad: String,
    @SerializedName("latitud")          val latitud: Double?,
    @SerializedName("longitud")         val longitud: Double?,
    @SerializedName("timestamp_ms")     val timestamp_ms: Long,
    @SerializedName("creado_en")        val creado_en: String? = null,
    @SerializedName("es_propio")        val es_propio: Boolean = true,
    @SerializedName("terreno_mas_cercano_id") val terreno_mas_cercano_id: Int? = null,
    @SerializedName("distancia_km")     val distancia_km: Float? = null
)


@Serializable
data class ReporteNavPayload(
    val id: Int,
    val plaga_nombre: String,
    val nivel_severidad: String,
    val latitud: Double?,
    val longitud: Double?,
    val timestamp_ms: Long
)
