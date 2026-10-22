package com.example.plag_out

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.io.Serial
import java.time.LocalDate
import java.time.LocalDateTime

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
    @SerializedName("plaga_id")
    val plaga_id: Int,
    @SerializedName("plaga_nombre")
    val plaga_nombre: String,
    // GDD hasta la eclosión. El avance, el nivel y el estadio viven en los ciclos.
    @SerializedName("gdd_objetivo")
    val gdd_objetivo: Float,
    @SerializedName("umbral_riesgo")
    val umbral_riesgo: Int? = null,
    @SerializedName("activo")
    val activo: Boolean = true,
    @SerializedName("fecha_inicio")
    val fecha_inicio: LocalDate? = null,
    @SerializedName("plaga_nombre_cientifico")
    val plaga_nombre_cientifico: String? = null,
    @SerializedName("alertas_ml_activas")
    val alertas_ml_activas: Boolean? = null,
    @SerializedName("umbral_alerta_ml_recomendado")
    val umbral_alerta_ml_recomendado: Float? = null,
    @SerializedName("modelo_alerta_ml_id")
    val modelo_alerta_ml_id: String? = null,
    @SerializedName("horizonte_alerta_ml_dias")
    val horizonte_alerta_ml_dias: Int? = null,
    val ciclos: List<GddCicloResponse>? = null,
    @SerializedName("observaciones")
    val observaciones: String? = null
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
 * `[1, 3]`, no objetos): se usa para ofrecer solo las plagas que tienen sentido para el cultivo
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
    val cultivos_afectados: List<Int>? = null,
    // Etapas que admite un reporte de esta plaga, en orden del ciclo de vida (las define el
    // backend según el tipo de desarrollo; sin dato, todas).
    @SerializedName("etapas")
    val etapas: List<String>? = null,
    // False cuando la plaga no tiene parámetros térmicos citados: se puede reportar, pero no
    // admite monitoreos GDD nuevos. Nullable por Gson (ver arriba); sin dato, decide el backend.
    @SerializedName("modelo_termico")
    val modelo_termico: Boolean? = null
) {
    val admiteMonitoreo: Boolean get() = modelo_termico != false
}

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
    val activo: Boolean? = null,
    @SerializedName("alertas_ml_activas")
    val alertas_ml_activas: Boolean? = null,
    // para borrar la nota se manda "" (cadena vacía), no null.
    @SerializedName("observaciones")
    val observaciones: String? = null
)

@Serializable
data class UpdatePlantacionRequest(
    @SerializedName("activa")
    val activa: Boolean? = null
)

@Serializable
data class CreateUserRequest(
    // The backend takes the email from the signed token only.
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
    val cargo_id: Int? = null,
    @SerializedName("radio_notificacion_km")
    val radio_notificacion_km: Double? = null
)

@Serializable
data class ConsentimientoModeloRequest(
    @SerializedName("consentido") val consentido: Boolean,
    @SerializedName("version_contrato") val version_contrato: String
)

@Serializable
data class ConsentimientoModeloResponse(
    @SerializedName("consentido") val consentido: Boolean,
    @SerializedName("vigente_desde") val vigente_desde: String?,
    @SerializedName("version_contrato") val version_contrato: String
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
        tipo in setOf("ALERTA_GDD_CICLO", "BIOFIX_CICLO") -> id?.let { "monitoreo/$it" }
        tipo == "ALERTA_ML_RIESGO" -> {
            if (id != null) "prediccion/$id" else null
        }
        tipo.contains("REPORTE") -> {
            if (id != null) "ver_reporte/$id" else "reportes"
        }
        tipo.contains("GDD") || tipo.contains("MONITOREO") -> {
            if (id != null) "monitoreo/$id" else "monitoreos"
        }
        tipo.contains("BIOFIX") || tipo.contains("PLANTACION") -> {
            if (id != null) "plantacion/$id" else "terrenos"
        }
        else -> null
    }
}

fun destinoDePush(
    tipo: String?,
    prediccionId: String? = null,
    monitoreoId: String? = null,
    reporteId: String? = null,
    plantacionId: String? = null,
    entidadId: String? = null,
    cicloId: String? = null
): String? {
    if (tipo in setOf("ALERTA_GDD_CICLO", "BIOFIX_CICLO")) {
        return (monitoreoId ?: entidadId)?.toIntOrNull()?.let { "monitoreo/$it" }
    }
    prediccionId?.toIntOrNull()?.let { return "prediccion/$it" }
    monitoreoId?.toIntOrNull()?.let { return "monitoreo/$it" }
    reporteId?.toIntOrNull()?.let { return "ver_reporte/$it" }
    plantacionId?.toIntOrNull()?.let { return "plantacion/$it" }
    val id = entidadId?.toIntOrNull() ?: return null
    return when (tipo?.uppercase()) {
        "ALERTA_ML_RIESGO" -> "prediccion/$id"
        "ALERTA_GDD" -> "monitoreo/$id"
        "REPORTE_CERCANO" -> "ver_reporte/$id"
        "ALERTA_BIOFIX", "BIOFIX" -> "plantacion/$id"
        else -> null
    }
}

@Serializable
data class PrediccionConfirmacionEstado(
    @SerializedName("estado") val estado: String,
    @SerializedName("solicitada_en") val solicitada_en: String? = null,
    @SerializedName("expira_en") val expira_en: String? = null,
    @SerializedName("respuesta") val respuesta: String? = null,
    @SerializedName("respondido_en") val respondido_en: String? = null
)

@Serializable
data class PrediccionDetalleResponse(
    val monitoreo_id: Int? = null,
    @SerializedName("id") val id: Int,
    @SerializedName("plantacion_id") val plantacion_id: Int,
    @SerializedName("plaga_id") val plaga_id: Int,
    @SerializedName("plaga_nombre_cientifico") val plaga_nombre_cientifico: String,
    @SerializedName("fecha") val fecha: LocalDate,
    @SerializedName("model_id") val model_id: String,
    @SerializedName("horizon_days") val horizon_days: Int,
    @SerializedName("probabilidad_porcentaje") val probabilidad_porcentaje: Float,
    @SerializedName("umbral_modelo_recomendado_porcentaje")
    val umbral_modelo_recomendado_porcentaje: Float,
    @SerializedName("umbral_efectivo_porcentaje")
    val umbral_efectivo_porcentaje: Float? = null,
    @SerializedName("confirmacion") val confirmacion: PrediccionConfirmacionEstado
)

@Serializable
data class PrediccionConfirmacionRequest(
    val biofix: BiofixRequest? = null,
    @SerializedName("respuesta") val respuesta: String,
    @SerializedName("idempotency_key") val idempotency_key: String
)

@Serializable
data class PrediccionConfirmacionResponse(
    val biofix: BiofixResult? = null,
    @SerializedName("id") val id: String,
    @SerializedName("prediccion_id") val prediccion_id: Int,
    @SerializedName("respuesta") val respuesta: String,
    @SerializedName("respondido_en") val respondido_en: String,
    @SerializedName("reporte_id") val reporte_id: Int? = null,
    @SerializedName("reporte_creado") val reporte_creado: Boolean = false
)

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
    val fecha_creacion: LocalDate,
    @SerializedName("radio_notificacion_km")
    val radio_notificacion_km: Double = 20.0,
    @SerializedName("rol")
    val rol: String? = null,
    @SerializedName("activo")
    val activo: Boolean? = null
)

const val ROL_ADMIN = "admin"
const val ROL_USUARIO = "usuario"

fun UsuarioResponse.esAdmin() = rol == ROL_ADMIN

// ── Administración ──────────────────────────────────────────────────────────
// No son @Entity: los datos del panel de admin siempre se leen de la red.

data class PlagaAdmin(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("nombre_cientifico")
    val nombre_cientifico: String,
    @SerializedName("temp_base")
    val temp_base: Double? = null,
    @SerializedName("temp_max")
    val temp_max: Double? = null,
    @SerializedName("gdd_eclosion")
    val gdd_eclosion: Double? = null,
    @SerializedName("gdd_generacion")
    val gdd_generacion: Double? = null,
    @SerializedName("cultivos_afectados")
    val cultivos_afectados: List<Int>? = null,
    @SerializedName("activo")
    val activo: Boolean? = null,
    @SerializedName("monitoreos_activos")
    val monitoreos_activos: Int? = null,
    @SerializedName("referencia_termica")
    val referencia_termica: ReferenciaTermica? = null
) {
    val estaActiva: Boolean get() = activo != false
}

/**
 * Los parámetros citados de la plaga (config/pest_thermal_parameters.v1.yaml del backend).
 * [estado]: "validado", "sin_modelo" o "sin_fuente"; [difiere]: campos del catálogo que una
 * edición apartó de la fuente.
 */
data class ReferenciaTermica(
    @SerializedName("estado")
    val estado: String? = null,
    @SerializedName("difiere")
    val difiere: List<String>? = null
) {
    val validada: Boolean get() = estado == "validado"
}

// En el PATCH solo se mandan los campos tocados: Gson omite los null.
data class PlagaAdminRequest(
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("nombre_cientifico")
    val nombre_cientifico: String? = null,
    @SerializedName("temp_base")
    val temp_base: Double? = null,
    @SerializedName("temp_max")
    val temp_max: Double? = null,
    @SerializedName("gdd_eclosion")
    val gdd_eclosion: Double? = null,
    @SerializedName("gdd_generacion")
    val gdd_generacion: Double? = null,
    @SerializedName("cultivos_afectados")
    val cultivos_afectados: List<Int>? = null,
    @SerializedName("activo")
    val activo: Boolean? = null
)

data class CultivoAdmin(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("nombre_cientifico")
    val nombre_cientifico: String,
    @SerializedName("activo")
    val activo: Boolean? = null,
    @SerializedName("plantaciones_activas")
    val plantaciones_activas: Int? = null,
    @SerializedName("monitoreos_activos")
    val monitoreos_activos: Int? = null,
    @SerializedName("plagas")
    val plagas: List<PlagaDeCultivo>? = null
) {
    val estaActivo: Boolean get() = activo != false
}

data class PlagaDeCultivo(
    @SerializedName("id")
    val id: Int,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("nombre_cientifico")
    val nombre_cientifico: String,
    @SerializedName("activo")
    val activo: Boolean? = null,
    @SerializedName("monitoreos_activos")
    val monitoreos_activos: Int? = null
) {
    val estaActiva: Boolean get() = activo != false
}

data class CultivoAdminRequest(
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("nombre_cientifico")
    val nombre_cientifico: String? = null,
    @SerializedName("activo")
    val activo: Boolean? = null,
    @SerializedName("plagas")
    val plagas: List<Int>? = null
)

data class UsuarioAdmin(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("apellido")
    val apellido: String? = null,
    @SerializedName("cargo")
    val cargo: String? = null,
    @SerializedName("rol")
    val rol: String? = null,
    @SerializedName("activo")
    val activo: Boolean? = null,
    @SerializedName("fecha_creacion")
    val fecha_creacion: LocalDate? = null,
    @SerializedName("ultimo_acceso")
    val ultimo_acceso: String? = null
) {
    val estaActivo: Boolean get() = activo != false
    val nombreCompleto: String
        get() = listOfNotNull(nombre?.trim(), apellido?.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifEmpty { email }
}

// GET /admin/metricas: tableros de todos los productores, con lotes anónimos (sin dueño,
// nombre ni ubicación). Contrato y definiciones: docs/admin-metrics.md del backend.
// Gson no aplica los valores por defecto de Kotlin: todo lo que el backend puede mandar
// en null es nullable.

data class NivelSemaforoAdmin(
    @SerializedName("lotes") val lotes: Int,
    @SerializedName("hectareas") val hectareas: Float
)

data class SemaforoAdmin(
    @SerializedName("verde") val verde: NivelSemaforoAdmin,
    @SerializedName("amarillo") val amarillo: NivelSemaforoAdmin,
    @SerializedName("rojo") val rojo: NivelSemaforoAdmin,
    // Lotes cuyos monitoreos activos esperan el biofix: no entran en los totales.
    @SerializedName("esperando_biofix") val esperando_biofix: NivelSemaforoAdmin,
    @SerializedName("total_lotes") val total_lotes: Int,
    @SerializedName("total_hectareas") val total_hectareas: Float
)

data class PlagaEnCultivoAdmin(
    @SerializedName("plaga") val plaga: String,
    @SerializedName("plaga_cientifico") val plaga_cientifico: String?,
    @SerializedName("lotes") val lotes: Int,
    @SerializedName("porcentaje") val porcentaje: Float,
    @SerializedName("casos_alerta") val casos_alerta: Int
)

data class CultivoMetricasAdmin(
    @SerializedName("cultivo") val cultivo: String,
    @SerializedName("cultivo_cientifico") val cultivo_cientifico: String?,
    @SerializedName("lotes") val lotes: Int,
    @SerializedName("hectareas") val hectareas: Float,
    @SerializedName("monitoreos_riesgo_alto") val monitoreos_riesgo_alto: Int,
    @SerializedName("plaga_mas_frecuente") val plaga_mas_frecuente: String?,
    @SerializedName("fecha_siembra") val fecha_siembra: LocalDate?,
    @SerializedName("dias_desde_siembra") val dias_desde_siembra: Long?,
    @SerializedName("plagas") val plagas: List<PlagaEnCultivoAdmin>
)

data class LoteCriticoAdmin(
    @SerializedName("cultivo") val cultivo: String,
    @SerializedName("plaga") val plaga: String,
    @SerializedName("plaga_cientifico") val plaga_cientifico: String?,
    @SerializedName("hectareas") val hectareas: Float,
    @SerializedName("nivel_alerta") val nivel_alerta: Int,
    @SerializedName("progreso") val progreso: Float,
    @SerializedName("gdd_acumulado") val gdd_acumulado: Float,
    @SerializedName("gdd_objetivo") val gdd_objetivo: Float,
    // huevo | eclosion_cerca | eclosionada (etapa térmica del ciclo)
    @SerializedName("urgencia") val urgencia: String,
    @SerializedName("dias_desde_siembra") val dias_desde_siembra: Long?
)

data class VentanaControlAdmin(
    @SerializedName("cultivo") val cultivo: String,
    @SerializedName("plaga") val plaga: String,
    @SerializedName("plaga_cientifico") val plaga_cientifico: String?,
    @SerializedName("progreso") val progreso: Float,
    @SerializedName("estadio") val estadio: String,
    // incubacion | ventana_optima | ventana_cerrada
    @SerializedName("estado") val estado: String
)

data class EficaciaAdmin(
    @SerializedName("ciclos_archivados") val ciclos_archivados: Int,
    @SerializedName("exitosos") val exitosos: Int,
    // null mientras no haya ciclos archivados: la tasa es desconocida.
    @SerializedName("tasa") val tasa: Float?,
    @SerializedName("monitoreos_con_rebrote") val monitoreos_con_rebrote: Int
)

data class PlagaEnCampaniaAdmin(
    @SerializedName("plaga") val plaga: String,
    @SerializedName("plaga_cientifico") val plaga_cientifico: String?,
    @SerializedName("monitoreos") val monitoreos: Int,
    @SerializedName("incidencia") val incidencia: Float,
    @SerializedName("riesgo_alto") val riesgo_alto: Int,
    @SerializedName("porcentaje_riesgo_alto") val porcentaje_riesgo_alto: Float?,
    @SerializedName("nivel_promedio") val nivel_promedio: Float?
)

data class CampaniaAdmin(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("anio_inicio") val anio_inicio: Int,
    @SerializedName("monitoreos") val monitoreos: Int,
    @SerializedName("hectareas") val hectareas: Float,
    @SerializedName("plaga_predominante") val plaga_predominante: String?,
    @SerializedName("riesgo_bajo") val riesgo_bajo: Int,
    @SerializedName("riesgo_moderado") val riesgo_moderado: Int,
    @SerializedName("riesgo_alto") val riesgo_alto: Int,
    @SerializedName("esperando_biofix") val esperando_biofix: Int,
    @SerializedName("plagas") val plagas: List<PlagaEnCampaniaAdmin>
)

data class HistoricoAdmin(
    @SerializedName("monitoreos") val monitoreos: Int,
    @SerializedName("hectareas") val hectareas: Float,
    @SerializedName("campanias") val campanias: Int,
    @SerializedName("plaga_lider") val plaga_lider: String?
)

data class MetricasAdmin(
    @SerializedName("monitoreos_activos") val monitoreos_activos: Int,
    @SerializedName("monitoreos_esperando_biofix") val monitoreos_esperando_biofix: Int,
    @SerializedName("semaforo") val semaforo: SemaforoAdmin,
    @SerializedName("cultivos") val cultivos: List<CultivoMetricasAdmin>,
    @SerializedName("lotes_criticos") val lotes_criticos: List<LoteCriticoAdmin>,
    @SerializedName("ventanas") val ventanas: List<VentanaControlAdmin>,
    @SerializedName("eficacia") val eficacia: EficaciaAdmin,
    @SerializedName("historico") val historico: HistoricoAdmin,
    @SerializedName("campanias") val campanias: List<CampaniaAdmin>
)

@Serializable
data class CreateReporteRequest(
    @SerializedName("plantacion_id")
    val plantacion_id: Int,
    @SerializedName("plaga_id")
    val plaga_id: Int,
    @SerializedName("nivel_severidad")
    val nivel_severidad: String,
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
    @SerializedName("etapa_biologica")  val etapa_biologica: String? = null,
    @SerializedName("nivel_severidad")  val nivel_severidad: String,
    @SerializedName("latitud")          val latitud: Double?,
    @SerializedName("longitud")         val longitud: Double?,
    @SerializedName("timestamp_ms")     val timestamp_ms: Long,
    @SerializedName("creado_en")        val creado_en: String? = null,
    @SerializedName("es_propio")        val es_propio: Boolean = true,
    @SerializedName("terreno_mas_cercano_id") val terreno_mas_cercano_id: Int? = null,
    @SerializedName("distancia_km")     val distancia_km: Float? = null
)



data class Pagina<T>(
    @SerializedName("items") val items: List<T>? = null,
    @SerializedName("total") val total: Int = 0
)

data class ConteoReportes(
    @SerializedName("total") val total: Int = 0,
    @SerializedName("alto")  val alto: Int = 0,
    @SerializedName("medio") val medio: Int = 0,
    @SerializedName("bajo")  val bajo: Int = 0
)

data class ReportesMapaResponse(
    @SerializedName("nivel")     val nivel: String? = null,
    @SerializedName("grupos")    val grupos: List<GrupoReportesMapa>? = null,
    @SerializedName("reportes")  val reportes: List<ReporteDetalleResponse>? = null,
    @SerializedName("total")     val total: Int? = null,
    @SerializedName("cultivos")  val cultivos: List<String>? = null
)

data class GrupoReportesMapa(
    @SerializedName("latitud")        val latitud: Double,
    @SerializedName("longitud")       val longitud: Double,
    @SerializedName("cantidad")       val cantidad: Int,
    @SerializedName("severidad_max")  val severidad_max: String? = null
)


@Serializable
data class ReporteNavPayload(
    val id: Int,
    val plaga_nombre: String,
    val nivel_severidad: String,
    val latitud: Double?,
    val longitud: Double?,
    val timestamp_ms: Long,
    val es_propio: Boolean = false,
    val terreno_nombre: String? = null,
    val terreno_mas_cercano_id: Int? = null,
    val distancia_km: Float? = null
)


@Serializable
data class GddCicloResponse(
    val id: Int, val monitoreo_id: Int, val fecha_biofix: String,
    val estado: String, val fecha_actualizacion: String,
    val gdd_acumulado: Float, val gdd_diario: Float,
    val gdd_eclosion: Float, val gdd_generacion: Float,
    val progreso: Float, val estadio_biologico: String,
    val nivel_alerta: Int, val dias_pendientes: Int,
    val fecha_eclosion: String? = null,
    // El huevo acumula con su propia temperatura base hasta la eclosión; gdd_acumulado es el
    // desarrollo hasta el adulto. Sin dato, el huevo usa la base del ciclo.
    val gdd_acumulado_huevo: Float? = null
)

@Serializable
data class BiofixRequest(
    val idempotency_key: String,
    val fecha_observacion: String,
    val accion: String,
    val ciclo_id: Int? = null,
    val confirmar_ciclo_adicional: Boolean = false,
    val version_contrato: String = "biofix-v1"
)

@Serializable
data class BiofixResult(
    val observacion_id: String, val reporte_id: Int,
    val reporte_creado: Boolean, val ciclo_id: Int,
    val ciclo_creado: Boolean, val ciclo: GddCicloResponse
)
