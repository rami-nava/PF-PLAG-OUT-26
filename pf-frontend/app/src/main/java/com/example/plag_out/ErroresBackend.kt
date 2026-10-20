package com.example.plag_out

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.jan.supabase.exceptions.HttpRequestException
import retrofit2.Response
import java.io.IOException


data class ErrorBackend(
    val mensaje: String,
    val codigo: Int? = null,
    val detalle: String? = null,
    val campos: Map<String, String> = emptyMap()
)

const val MENSAJE_ERROR_GENERICO = "Ocurrió un error, intentá de nuevo."
const val MENSAJE_SESION_EXPIRADA = "Tu sesión expiró, iniciá sesión de nuevo."
private const val MENSAJE_SIN_CONEXION_USUARIO = "Sin conexión. Revisá tu conexión a internet e intentá de nuevo."
private const val MENSAJE_DATOS_INVALIDOS = "No se pudieron procesar los datos. Intentá de nuevo."


fun errorDeRespuesta(
    response: Response<*>,
    porDetalle: Map<String, String> = emptyMap(),
    porCodigo: Map<Int, String> = emptyMap()
): ErrorBackend {
    val cuerpo = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
    val retryAfter = response.headers()["Retry-After"]?.trim()?.toIntOrNull()
    return errorDeCuerpo(response.code(), cuerpo, retryAfter, porDetalle, porCodigo)
}

fun errorDeCuerpo(
    codigo: Int,
    cuerpo: String,
    retryAfterSegundos: Int? = null,
    porDetalle: Map<String, String> = emptyMap(),
    porCodigo: Map<Int, String> = emptyMap()
): ErrorBackend {
    val json = runCatching { JsonParser.parseString(cuerpo).asJsonObject }.getOrNull()
    val detalle = json?.get("detail")?.takeIf { it.isJsonPrimitive }?.asString
    // slowapi (429 de los GET de listados) responde {"error": "..."} sin detail
        ?: json?.get("error")?.takeIf { it.isJsonPrimitive }?.asString

    // "plantation_already_active" y "plantation already active" se tratan igual
    val detalleNormalizado = detalle?.replace('_', ' ')
    porDetalle.entries.firstOrNull {
        detalleNormalizado?.contains(it.key.replace('_', ' '), ignoreCase = true) == true
    }?.let {
        return ErrorBackend(it.value, codigo, detalle)
    }
    porCodigo[codigo]?.let { return ErrorBackend(it, codigo, detalle) }

    if (codigo == 422) {
        val campos = camposInvalidos(json)
        val mensaje = campos.values.firstOrNull() ?: MENSAJE_DATOS_INVALIDOS
        return ErrorBackend(mensaje, codigo, detalle, campos)
    }

    val mensaje = when (codigo) {
        401 -> MENSAJE_SESION_EXPIRADA
        403 -> if (detalle == CuentaSuspendidaEventBus.DETALLE) CuentaSuspendidaEventBus.MENSAJE
               else "No tenés permiso para hacer esta operación."
        404 -> "No encontramos lo que buscabas. Puede que ya no exista."
        413 -> "Los datos enviados son demasiado grandes."
        429 -> if (retryAfterSegundos != null && retryAfterSegundos > 0)
                   "Demasiados intentos, probá de nuevo en $retryAfterSegundos segundos."
               else "Demasiados intentos, probá de nuevo en unos segundos."
        503 -> "Servicio no disponible momentáneamente. Intentá más tarde."
        else -> MENSAJE_ERROR_GENERICO
    }
    return ErrorBackend(mensaje, codigo, detalle)
}

val ERRORES_PERFIL = mapOf(
    "cargo_no_encontrado" to "El cargo elegido ya no existe. Elegí otro.",
    "email does not match" to "El correo no se puede cambiar desde acá."
)

val ERRORES_REPORTE = mapOf(
    "report_plantation_not_found" to "No encontramos la plantación.",
    "report_pest_not_compatible" to "La plaga no afecta a este cultivo.",
    "report_plantation_location_unavailable" to "El terreno no tiene una ubicación válida.",
    "plaga_inactiva" to "La plaga ya no está disponible."
)


fun ErrorBackend.enFormularioOAviso(): String? =
    if (campos.isNotEmpty()) mensaje else { Avisos.error(mensaje); null }

fun errorDeExcepcion(e: Throwable): ErrorBackend =
    ErrorBackend(if (e is IOException || e is HttpRequestException) MENSAJE_SIN_CONEXION_USUARIO else MENSAJE_ERROR_GENERICO)

private fun camposInvalidos(json: JsonObject?): Map<String, String> {
    val errores = json?.get("errors")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyMap()
    val campos = linkedMapOf<String, String>()
    for (elemento in errores) {
        val error = elemento.takeIf { it.isJsonObject }?.asJsonObject ?: continue
        // El field puede venir como ruta ("body.nombre"): interesa el último tramo
        val campo = error.get("field")?.takeIf { it.isJsonPrimitive }?.asString
            ?.substringAfterLast('.').orEmpty()
        val code = error.get("code")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        if (campo !in campos) campos[campo] = mensajeDeValidacion(campo, code)
    }
    return campos
}

private fun mensajeDeValidacion(campo: String, code: String): String {
    val nombre = ETIQUETAS_CAMPOS[campo]
        ?: return MENSAJE_DATOS_INVALIDOS
    return when (code) {
        "fecha_siembra_future" -> "La fecha de siembra no puede ser futura."
        "fecha_siembra_too_old" -> "La fecha de siembra no puede ser de hace más de un año."
        "missing" -> "Falta completar $nombre."
        "string_too_short" -> "Revisá $nombre: no puede quedar en blanco."
        "string_too_long" -> "Revisá $nombre: tiene demasiados caracteres."
        "greater_than", "greater_than_equal", "less_than", "less_than_equal" ->
            if (campo == "latitud" || campo == "longitud") "La ubicación tiene que estar dentro de Argentina."
            else "Revisá $nombre: está fuera del rango permitido."
        "int_parsing", "float_parsing", "bool_parsing", "date_from_datetime_parsing",
        "date_parsing", "uuid_parsing", "finite_number" -> "Revisá $nombre: el valor no es válido."
        else -> MENSAJE_DATOS_INVALIDOS
    }
}

private val ETIQUETAS_CAMPOS = mapOf(
    "nombre" to "el nombre",
    "apellido" to "el apellido",
    "email" to "el correo",
    "cargo_id" to "el cargo",
    "area_hectareas" to "las hectáreas",
    "latitud" to "la ubicación",
    "longitud" to "la ubicación",
    "terreno_id" to "el terreno",
    "plantacion_id" to "la plantación",
    "cultivo_id" to "el cultivo",
    "plaga_id" to "la plaga",
    "fecha_siembra" to "la fecha de siembra",
    "umbral_riesgo" to "el umbral de riesgo",
    "umbral_alerta_ml" to "el umbral de alerta",
    "radio_notificacion_km" to "el radio de notificación",
    "observaciones" to "las observaciones"
)
