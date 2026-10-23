package com.example.plag_out

import com.example.plag_out.AlmacenamientoLocal.TratamientoPendiente
import com.example.plag_out.AlmacenamientoLocal.TratamientoPendienteDao
import com.example.plag_out.Service.GDDService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate

/**
 * Tratamientos de un monitoreo: el conteo de la plaga antes y después de aplicar, con un
 * testigo (franja sin tratar) opcional. El backend calcula la cifra (docs/tratamientos.md):
 * con testigo, la eficacia de Henderson & Tilton 1955; sin testigo, solo la reducción
 * observada, que incluye la mortalidad natural.
 */

enum class UnidadConteo(val clave: String, val etiqueta: String) {
    POR_METRO_LINEAL("por_metro_lineal", "por metro lineal"),
    POR_PLANTA("por_planta", "por planta"),
    POR_GOLPE_RED("por_golpe_red", "por golpe de red"),
    POR_TRAMPA("por_trampa", "por trampa");

    companion object {
        fun de(clave: String?): UnidadConteo? = entries.firstOrNull { it.clave == clave }
    }
}

data class FormularioTratamiento(
    val fechaAplicacion: LocalDate? = null,
    val producto: String = "",
    val unidad: UnidadConteo = UnidadConteo.POR_METRO_LINEAL,
    val conteoAntes: String = "",
    val fechaConteoAntes: LocalDate? = null,
    val conTestigo: Boolean = false,
    val testigoAntes: String = "",
    val conDespues: Boolean = false,
    val conteoDespues: String = "",
    val fechaConteoDespues: LocalDate? = null,
    val testigoDespues: String = ""
) {
    companion object {
        fun nuevo(hoy: LocalDate) = FormularioTratamiento(fechaAplicacion = hoy, fechaConteoAntes = hoy)

        fun desde(t: TratamientoResponse) = FormularioTratamiento(
            fechaAplicacion = LocalDate.parse(t.fecha_aplicacion),
            producto = t.producto,
            unidad = UnidadConteo.de(t.unidad_conteo) ?: UnidadConteo.POR_METRO_LINEAL,
            conteoAntes = formatearConteo(t.conteo_antes),
            fechaConteoAntes = LocalDate.parse(t.fecha_conteo_antes),
            conTestigo = t.testigo_antes != null,
            testigoAntes = t.testigo_antes?.let(::formatearConteo).orEmpty(),
            conDespues = t.conteo_despues != null,
            conteoDespues = t.conteo_despues?.let(::formatearConteo).orEmpty(),
            fechaConteoDespues = t.fecha_conteo_despues?.let(LocalDate::parse),
            testigoDespues = t.testigo_despues?.let(::formatearConteo).orEmpty()
        )
    }
}

enum class CampoTratamiento {
    FECHA_APLICACION, PRODUCTO, CONTEO_ANTES, FECHA_CONTEO_ANTES, TESTIGO_ANTES,
    CONTEO_DESPUES, FECHA_CONTEO_DESPUES, TESTIGO_DESPUES
}

private const val CONTEO_MAXIMO = 100_000.0
private const val PRODUCTO_MAXIMO = 120

internal fun leerConteo(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..CONTEO_MAXIMO }

internal fun formatearConteo(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString().replace('.', ',')

/** Las mismas reglas que el backend, para no mandar algo que va a rechazar. */
fun validarTratamiento(
    form: FormularioTratamiento,
    inicioMonitoreo: LocalDate?,
    hoy: LocalDate
): Map<CampoTratamiento, String> {
    val errores = mutableMapOf<CampoTratamiento, String>()
    val conteoInvalido = "Un número entre 0 y 100.000"
    val aplicacion = form.fechaAplicacion
    when {
        aplicacion == null -> errores[CampoTratamiento.FECHA_APLICACION] = "Elegí la fecha"
        aplicacion.isAfter(hoy) -> errores[CampoTratamiento.FECHA_APLICACION] = "No puede ser futura"
        inicioMonitoreo != null && aplicacion.isBefore(inicioMonitoreo) ->
            errores[CampoTratamiento.FECHA_APLICACION] = "Es anterior al inicio del monitoreo"
    }
    val producto = form.producto.trim()
    if (producto.isEmpty()) errores[CampoTratamiento.PRODUCTO] = "Ingresá el producto"
    else if (producto.length > PRODUCTO_MAXIMO) errores[CampoTratamiento.PRODUCTO] = "Hasta $PRODUCTO_MAXIMO caracteres"

    if (leerConteo(form.conteoAntes) == null) errores[CampoTratamiento.CONTEO_ANTES] = conteoInvalido
    val antes = form.fechaConteoAntes
    if (antes == null) errores[CampoTratamiento.FECHA_CONTEO_ANTES] = "Elegí la fecha"
    else if (aplicacion != null && antes.isAfter(aplicacion)) {
        errores[CampoTratamiento.FECHA_CONTEO_ANTES] = "Tiene que ser antes de aplicar o el mismo día"
    }
    if (form.conTestigo && leerConteo(form.testigoAntes) == null) errores[CampoTratamiento.TESTIGO_ANTES] = conteoInvalido

    if (form.conDespues) {
        if (leerConteo(form.conteoDespues) == null) errores[CampoTratamiento.CONTEO_DESPUES] = conteoInvalido
        val despues = form.fechaConteoDespues
        when {
            despues == null -> errores[CampoTratamiento.FECHA_CONTEO_DESPUES] = "Elegí la fecha"
            aplicacion != null && !despues.isAfter(aplicacion) ->
                errores[CampoTratamiento.FECHA_CONTEO_DESPUES] = "Tiene que ser después de aplicar"
            despues.isAfter(hoy) -> errores[CampoTratamiento.FECHA_CONTEO_DESPUES] = "No puede ser futura"
        }
        // El testigo posterior es opcional: sin él, solo hay reducción observada.
        if (form.conTestigo && form.testigoDespues.isNotBlank() && leerConteo(form.testigoDespues) == null) {
            errores[CampoTratamiento.TESTIGO_DESPUES] = conteoInvalido
        }
    }
    return errores
}

/** El alta, con los conteos posteriores si ya se cargaron (la cola puede mandar todo junto). */
fun FormularioTratamiento.aRequest(clave: String): TratamientoRequest {
    val despues = conDespues
    return TratamientoRequest(
        idempotency_key = clave,
        fecha_aplicacion = fechaAplicacion.toString(),
        producto = producto.trim(),
        unidad_conteo = unidad.clave,
        conteo_antes = leerConteo(conteoAntes) ?: 0.0,
        fecha_conteo_antes = fechaConteoAntes.toString(),
        conteo_despues = if (despues) leerConteo(conteoDespues) else null,
        fecha_conteo_despues = if (despues) fechaConteoDespues?.toString() else null,
        testigo_antes = if (conTestigo) leerConteo(testigoAntes) else null,
        testigo_despues = if (despues && conTestigo) leerConteo(testigoDespues) else null
    )
}

/**
 * Solo lo que cambió respecto de [original], o null si nada cambió. Quitar el conteo posterior
 * o el testigo manda null explícito, que el backend toma como "deshacer".
 */
fun cambiosDeTratamiento(original: TratamientoResponse, form: FormularioTratamiento): JsonObject? {
    val nuevo = form.aRequest(clave = "")
    val cambios = JsonObject()
    fun texto(campo: String, valor: String?, anterior: String?) {
        if (valor != anterior) cambios.add(campo, valor?.let(::JsonPrimitive) ?: JsonNull.INSTANCE)
    }
    fun numero(campo: String, valor: Double?, anterior: Double?) {
        if (valor != anterior) cambios.add(campo, valor?.let(::JsonPrimitive) ?: JsonNull.INSTANCE)
    }
    texto("fecha_aplicacion", nuevo.fecha_aplicacion, original.fecha_aplicacion)
    texto("producto", nuevo.producto, original.producto)
    texto("unidad_conteo", nuevo.unidad_conteo, original.unidad_conteo)
    numero("conteo_antes", nuevo.conteo_antes, original.conteo_antes)
    texto("fecha_conteo_antes", nuevo.fecha_conteo_antes, original.fecha_conteo_antes)
    numero("conteo_despues", nuevo.conteo_despues, original.conteo_despues)
    texto("fecha_conteo_despues", nuevo.fecha_conteo_despues, original.fecha_conteo_despues)
    numero("testigo_antes", nuevo.testigo_antes, original.testigo_antes)
    numero("testigo_despues", nuevo.testigo_despues, original.testigo_despues)
    return cambios.takeIf { it.size() > 0 }
}

// ── Textos del resultado ─────────────────────────────────────────────────────

private fun porcentaje(valor: Double): String {
    val redondeado = Math.round(valor * 10) / 10.0
    return formatearConteo(redondeado) + " %"
}

data class ResultadoTratamiento(val titulo: String, val detalle: String?)

private val MOTIVO_SIN_TESTIGO = mapOf(
    "falta_testigo_despues" to "Falta el conteo posterior de la franja sin tratar.",
    "testigo_antes_cero" to "La franja sin tratar no tenía plaga antes de aplicar.",
    "testigo_despues_cero" to "La franja sin tratar quedó sin plaga."
)

fun resultadoDeTratamiento(t: TratamientoResponse): ResultadoTratamiento {
    val eficacia = t.eficacia_pct
    val reduccion = t.reduccion_observada_pct
    return when {
        t.metodo == "henderson_tilton" && eficacia != null -> ResultadoTratamiento(
            titulo = if (eficacia < 0) "Sin efecto: la plaga aumentó frente al testigo" else "Eficacia: ${porcentaje(eficacia)}",
            detalle = "Henderson-Tilton: compara con la franja sin tratar y descuenta lo que la población cambió sola."
        )
        t.metodo == "reduccion_observada" && reduccion != null -> ResultadoTratamiento(
            titulo = if (reduccion < 0) "La plaga aumentó un ${porcentaje(-reduccion)}" else "Reducción observada: ${porcentaje(reduccion)}",
            detalle = (MOTIVO_SIN_TESTIGO[t.sin_calculo]?.let { "$it " } ?: "") +
                "Incluye la mortalidad natural: para medir la eficacia hace falta una franja sin tratar."
        )
        t.sin_calculo == "conteo_antes_cero" -> ResultadoTratamiento("Sin plaga antes de aplicar", "No se puede calcular una reducción.")
        else -> ResultadoTratamiento("Falta el conteo posterior", "Cargalo unos días después de aplicar.")
    }
}

// ── Envío (pantalla y worker comparten este camino) ─────────────────────────

sealed interface EnvioTratamiento {
    data class Enviado(val tratamiento: TratamientoResponse) : EnvioTratamiento
    /** Sin conexión o error transitorio: la fila queda y el worker reintenta. */
    data object Reintentar : EnvioTratamiento
    /** El backend lo rechazó (fecha inválida, monitoreo finalizado...): queda a revisar. */
    data class Rechazado(val mensaje: String) : EnvioTratamiento
}

internal val gsonTratamientos: Gson = Gson()

// Las ediciones viajan con sus null explícitos.
private val gsonConNulos: Gson = GsonBuilder().serializeNulls().create()

internal fun JsonObject.comoCuerpoJson() =
    gsonConNulos.toJson(this).toRequestBody("application/json; charset=utf-8".toMediaType())

private val ERRORES_TRATAMIENTO = mapOf(
    "monitoring_not_found" to "El monitoreo ya no existe.",
    "monitoring_inactive" to "El monitoreo está finalizado: no admite tratamientos nuevos.",
    "treatment_not_found" to "El tratamiento ya no existe.",
    "treatment_application_date_invalid" to "La fecha de aplicación tiene que estar entre el inicio del monitoreo y hoy.",
    "treatment_before_count_after_application" to "El conteo previo tiene que ser antes de aplicar o el mismo día.",
    "treatment_after_count_date_invalid" to "El conteo posterior tiene que ser después de aplicar y no puede ser futuro.",
    "treatment_after_count_incomplete" to "Falta la fecha o el valor del conteo posterior.",
    "treatment_control_before_missing" to "Falta el conteo previo de la franja sin tratar.",
    "treatment_idempotency_key_reused" to "No se pudo registrar el tratamiento. Cargalo de nuevo."
)

/** Una edición pendiente lleva el JSON del PATCH; un alta que cambió mientras se enviaba, también. */
internal fun altaComoEdicion(payload: String): String {
    val alta = gsonConNulos.toJsonTree(gsonTratamientos.fromJson(payload, TratamientoRequest::class.java)).asJsonObject
    alta.remove("idempotency_key")
    return gsonConNulos.toJson(alta)
}

suspend fun enviarTratamientoPendiente(
    fila: TratamientoPendiente,
    service: GDDService,
    dao: TratamientoPendienteDao
): EnvioTratamiento {
    val response = try {
        if (fila.tratamiento_id == null) {
            val alta = gsonTratamientos.fromJson(fila.payload, TratamientoRequest::class.java)
            service.registrarTratamiento(fila.monitoreo_id, alta)
        } else {
            val cambios = gsonTratamientos.fromJson(fila.payload, JsonObject::class.java)
            service.actualizarTratamiento(fila.tratamiento_id, cambios.comoCuerpoJson())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        return EnvioTratamiento.Reintentar
    }
    val tratamiento = response.body()
    val codigo = response.code()
    return when {
        response.isSuccessful && tratamiento != null -> {
            val borrada = dao.borrarSiNoCambio(fila.owner_id, fila.idempotency_key, fila.payload) > 0
            if (!borrada && fila.tratamiento_id == null) {
                // Se editó mientras viajaba el alta: lo nuevo sale como edición del ya creado.
                dao.get(fila.owner_id, fila.idempotency_key)?.let { actual ->
                    dao.guardar(actual.copy(tratamiento_id = tratamiento.id, payload = altaComoEdicion(actual.payload)))
                }
            }
            EnvioTratamiento.Enviado(tratamiento)
        }
        codigo == 408 || codigo == 429 || codigo >= 500 || codigo == 401 || codigo == 403 || response.isSuccessful ->
            EnvioTratamiento.Reintentar
        else -> {
            dao.marcar(fila.owner_id, fila.idempotency_key, fila.payload, "requiere_revision")
            EnvioTratamiento.Rechazado(errorDeRespuesta(response, ERRORES_TRATAMIENTO).mensaje)
        }
    }
}
