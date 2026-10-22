package com.example.plag_out

import com.example.plag_out.Service.todosLosCultivos
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

data class AdminPlagasUIState(
    val plagas: List<PlagaAdmin> = emptyList(),
    val cultivos: List<CultivoAdmin> = emptyList(),
    val busqueda: String = "",
    val filtro: FiltroPlagas = FiltroPlagas.TODAS,
    val cargado: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val errorAccion: String? = null,
    val sinPermiso: Boolean = false,
    val desactualizado: Boolean = false
)

data class FormularioPlaga(
    val nombre: String = "",
    val nombreCientifico: String = "",
    val tempBase: String = "",
    val tempMax: String = "",
    // Opcional: vacío = el huevo acumula con la temperatura base.
    val tempBaseHuevo: String = "",
    val gddEclosion: String = "",
    val gddGeneracion: String = "",
    val generacionDesde: String = GENERACION_DESDE_BIOFIX,
    val tipoDesarrollo: String? = null,
    val cultivos: Set<Int> = emptySet()
) {
    companion object {
        fun desde(plaga: PlagaAdmin) = FormularioPlaga(
            nombre = plaga.nombre,
            nombreCientifico = plaga.nombre_cientifico,
            tempBase = plaga.temp_base?.let(::formatearNumero).orEmpty(),
            tempMax = plaga.temp_max?.let(::formatearNumero).orEmpty(),
            tempBaseHuevo = plaga.temp_base_huevo?.let(::formatearNumero).orEmpty(),
            gddEclosion = plaga.gdd_eclosion?.let(::formatearNumero).orEmpty(),
            gddGeneracion = plaga.gdd_generacion?.let(::formatearNumero).orEmpty(),
            generacionDesde = plaga.gdd_generacion_desde ?: GENERACION_DESDE_BIOFIX,
            tipoDesarrollo = plaga.tipo_desarrollo,
            cultivos = plaga.cultivos_afectados.orEmpty().toSet()
        )
    }
}

const val GENERACION_DESDE_BIOFIX = "biofix"
const val GENERACION_DESDE_ECLOSION = "eclosion"
const val DESARROLLO_COMPLETO = "completo"
const val DESARROLLO_INCOMPLETO = "incompleto"

// Los mismos límites que el backend (admin_schemas.py): fuera de ellos es un error de tipeo.
private const val TEMPERATURA_MINIMA = -50.0
private const val TEMPERATURA_MAXIMA = 60.0
private const val GDD_MAXIMO = 100_000.0

internal fun leerNumero(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

internal fun formatearNumero(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString()

private fun temperaturaValida(valor: Double) = valor in TEMPERATURA_MINIMA..TEMPERATURA_MAXIMA

fun validarPlaga(form: FormularioPlaga): Map<CampoPlaga, String> {
    val errores = mutableMapOf<CampoPlaga, String>()
    if (form.nombre.isBlank()) errores[CampoPlaga.NOMBRE] = "Ingresá el nombre"
    if (form.nombreCientifico.isBlank()) errores[CampoPlaga.NOMBRE_CIENTIFICO] = "Ingresá el nombre científico"

    val fueraDeRango = "Entre ${formatearNumero(TEMPERATURA_MINIMA)} y ${formatearNumero(TEMPERATURA_MAXIMA)} °C"
    val tBase = leerNumero(form.tempBase)
    val tMax = leerNumero(form.tempMax)
    when {
        tBase == null -> errores[CampoPlaga.TEMP_BASE] = "Número requerido"
        !temperaturaValida(tBase) -> errores[CampoPlaga.TEMP_BASE] = fueraDeRango
    }
    when {
        tMax == null -> errores[CampoPlaga.TEMP_MAX] = "Número requerido"
        !temperaturaValida(tMax) -> errores[CampoPlaga.TEMP_MAX] = fueraDeRango
        tBase != null && tBase >= tMax -> errores[CampoPlaga.TEMP_MAX] = "Tiene que ser mayor que la base"
    }
    if (form.tempBaseHuevo.isNotBlank()) {
        val tHuevo = leerNumero(form.tempBaseHuevo)
        when {
            tHuevo == null -> errores[CampoPlaga.TEMP_BASE_HUEVO] = "Número o vacío"
            !temperaturaValida(tHuevo) -> errores[CampoPlaga.TEMP_BASE_HUEVO] = fueraDeRango
            tMax != null && tHuevo >= tMax -> errores[CampoPlaga.TEMP_BASE_HUEVO] = "Tiene que ser menor que la máxima"
        }
    }

    val eclosion = leerNumero(form.gddEclosion)
    val generacion = leerNumero(form.gddGeneracion)
    val gddInvalido = "Mayor a 0, hasta 100.000"
    if (eclosion == null || eclosion <= 0 || eclosion > GDD_MAXIMO) errores[CampoPlaga.GDD_ECLOSION] = gddInvalido
    if (generacion == null || generacion <= 0 || generacion > GDD_MAXIMO) errores[CampoPlaga.GDD_GENERACION] = gddInvalido

    if (form.tipoDesarrollo !in setOf(DESARROLLO_COMPLETO, DESARROLLO_INCOMPLETO)) {
        errores[CampoPlaga.TIPO_DESARROLLO] = "Elegí el tipo de desarrollo"
    }
    if (form.cultivos.isEmpty()) errores[CampoPlaga.CULTIVOS] = "Elegí al menos un cultivo"
    return errores
}

enum class CampoPlaga {
    NOMBRE, NOMBRE_CIENTIFICO, TEMP_BASE, TEMP_MAX, TEMP_BASE_HUEVO, GDD_ECLOSION, GDD_GENERACION,
    TIPO_DESARROLLO, CULTIVOS
}

internal fun requestDeAlta(form: FormularioPlaga) = PlagaAdminRequest(
    nombre = form.nombre.trim(),
    nombre_cientifico = form.nombreCientifico.trim(),
    temp_base = leerNumero(form.tempBase),
    temp_max = leerNumero(form.tempMax),
    gdd_eclosion = leerNumero(form.gddEclosion),
    gdd_generacion = leerNumero(form.gddGeneracion),
    cultivos_afectados = form.cultivos.sorted(),
    tipo_desarrollo = form.tipoDesarrollo,
    temp_base_huevo = leerNumero(form.tempBaseHuevo),
    gdd_generacion_desde = form.generacionDesde
)

/**
 * Solo los campos que cambiaron, o null si no cambió nada. Vaciar la base del huevo manda
 * `"temp_base_huevo": null`, que el backend interpreta como "volver a la temperatura base".
 */
internal fun requestDeEdicion(original: PlagaAdmin, form: FormularioPlaga): JsonObject? {
    val nuevo = requestDeAlta(form)
    val cambios = JsonObject()
    fun texto(campo: String, valor: String?, anterior: String?) {
        if (valor != anterior) cambios.addProperty(campo, valor)
    }
    fun numero(campo: String, valor: Double?, anterior: Double?) {
        if (valor != anterior) cambios.add(campo, valor?.let(::JsonPrimitive) ?: JsonNull.INSTANCE)
    }
    texto("nombre", nuevo.nombre, original.nombre)
    texto("nombre_cientifico", nuevo.nombre_cientifico, original.nombre_cientifico)
    numero("temp_base", nuevo.temp_base, original.temp_base)
    numero("temp_max", nuevo.temp_max, original.temp_max)
    numero("temp_base_huevo", nuevo.temp_base_huevo, original.temp_base_huevo)
    numero("gdd_eclosion", nuevo.gdd_eclosion, original.gdd_eclosion)
    numero("gdd_generacion", nuevo.gdd_generacion, original.gdd_generacion)
    texto("gdd_generacion_desde", nuevo.gdd_generacion_desde, original.gdd_generacion_desde ?: GENERACION_DESDE_BIOFIX)
    texto("tipo_desarrollo", nuevo.tipo_desarrollo, original.tipo_desarrollo)
    val cultivos = nuevo.cultivos_afectados.orEmpty()
    if (cultivos.toSet() != original.cultivos_afectados.orEmpty().toSet()) {
        cambios.add("cultivos_afectados", JsonArray().apply { cultivos.forEach(::add) })
    }
    return cambios.takeIf { it.size() > 0 }
}

// Gson del cliente omite los null; este los escribe, para el PATCH de la plaga.
private val gsonConNulos = GsonBuilder().serializeNulls().create()

internal fun JsonObject.comoCuerpo(): RequestBody =
    gsonConNulos.toJson(this).toRequestBody("application/json; charset=utf-8".toMediaType())

enum class FiltroPlagas { TODAS, ACTIVAS, INACTIVAS }

fun filtrarPlagas(plagas: List<PlagaAdmin>, busqueda: String, filtro: FiltroPlagas): List<PlagaAdmin> {
    val texto = busqueda.trim()
    return plagas
        .filter {
            when (filtro) {
                FiltroPlagas.TODAS -> true
                FiltroPlagas.ACTIVAS -> it.estaActiva
                FiltroPlagas.INACTIVAS -> !it.estaActiva
            }
        }
        .filter {
            texto.isEmpty() ||
                it.nombre.contains(texto, ignoreCase = true) ||
                it.nombre_cientifico.contains(texto, ignoreCase = true)
        }
        .sortedWith(compareBy<PlagaAdmin> { !it.estaActiva }.thenBy { it.nombre.lowercase() })
}

class AdminPlagasViewModel(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(AdminPlagasUIState())
    val state: StateFlow<AdminPlagasUIState> = _state.asStateFlow()

    fun plaga(id: Int): PlagaAdmin? = _state.value.plagas.firstOrNull { it.id == id }

    fun cargar(forzar: Boolean = false) {
        val actual = _state.value
        if (actual.isLoading || actual.isRefreshing) return
        if (!forzar && actual.cargado && !actual.desactualizado) return
        val recargarCultivos = forzar || actual.desactualizado

        _state.value = actual.copy(
            isLoading = !actual.cargado,
            isRefreshing = actual.cargado,
            error = null,
            sinPermiso = false
        )
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.adminGetPlagas() }
                val plagas = response.body()
                if (response.isSuccessful && plagas != null) {
                    _state.value = _state.value.copy(plagas = plagas, cargado = true, desactualizado = false)
                    cargarCultivos(forzar = recargarCultivos)
                } else {
                    aplicarFallo(falloDeRespuesta(response), comoErrorDeCarga = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_PLAGAS", "Error: ${e.message}")
                aplicarFallo(falloDeExcepcion(e), comoErrorDeCarga = true)
            } finally {
                _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    fun refrescar() = cargar(forzar = true)

    private suspend fun cargarCultivos(forzar: Boolean) {
        if (!forzar && _state.value.cultivos.isNotEmpty()) return
        try {
            val admin = withContext(Dispatchers.IO) { gddService.adminGetCultivos() }
            val cultivos = admin.body()?.takeIf { admin.isSuccessful } ?: run {
                val response = withContext(Dispatchers.IO) { gddService.todosLosCultivos() }
                response.body()?.takeIf { response.isSuccessful }
                    ?.map { CultivoAdmin(id = it.id, nombre = it.nombre, nombre_cientifico = it.nombre_cientifico) }
            }
            cultivos?.let { _state.value = _state.value.copy(cultivos = it.sortedBy { c -> c.nombre.lowercase() }) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ADMIN_PLAGAS", "No se pudieron cargar los cultivos: ${e.message}")
        }
    }

    fun marcarDesactualizado() {
        _state.value = _state.value.copy(desactualizado = true)
    }

    fun buscar(texto: String) {
        _state.value = _state.value.copy(busqueda = texto)
    }

    fun cambiarFiltro(filtro: FiltroPlagas) {
        _state.value = _state.value.copy(filtro = filtro)
    }

    fun guardar(original: PlagaAdmin?, form: FormularioPlaga, onGuardado: () -> Unit) {
        if (_state.value.guardando || validarPlaga(form).isNotEmpty()) return
        val edicion = original?.let { requestDeEdicion(it, form) }
        if (original != null && edicion == null) {
            onGuardado() // Nada que guardar
            return
        }
        ejecutar(onExito = onGuardado) {
            if (original == null) gddService.adminCrearPlaga(requestDeAlta(form))
            else gddService.adminActualizarPlaga(original.id, edicion!!.comoCuerpo())
        }
    }

    fun desactivar(id: Int, onHecho: () -> Unit = {}) =
        ejecutar(onExito = onHecho) { gddService.adminDesactivarPlaga(id) }

    fun reactivar(id: Int, onHecho: () -> Unit = {}) =
        ejecutar(onExito = onHecho) {
            gddService.adminActualizarPlaga(id, JsonObject().apply { addProperty("activo", true) }.comoCuerpo())
        }

    private fun ejecutar(onExito: () -> Unit, llamada: suspend () -> retrofit2.Response<PlagaAdmin>) {
        if (_state.value.guardando) return
        _state.value = _state.value.copy(guardando = true, errorAccion = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { llamada() }
                val plaga = response.body()
                if (response.isSuccessful && plaga != null) {
                    reemplazar(plaga)
                    _state.value = _state.value.copy(guardando = false)
                    onExito()
                } else {
                    _state.value = _state.value.copy(guardando = false)
                    aplicarFallo(falloDeRespuesta(response), comoErrorDeCarga = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_PLAGAS", "Error: ${e.message}")
                _state.value = _state.value.copy(guardando = false)
                aplicarFallo(falloDeExcepcion(e), comoErrorDeCarga = false)
            }
        }
    }

    private fun reemplazar(plaga: PlagaAdmin) {
        val actuales = _state.value.plagas
        val nuevas = if (actuales.any { it.id == plaga.id }) {
            actuales.map { if (it.id == plaga.id) plaga else it }
        } else {
            actuales + plaga
        }
        _state.value = _state.value.copy(plagas = nuevas)
    }

    private fun aplicarFallo(fallo: FalloAdmin, comoErrorDeCarga: Boolean) {
        _state.value = if (comoErrorDeCarga) {
            _state.value.copy(error = fallo.mensaje, sinPermiso = fallo.sinPermiso)
        } else {
            _state.value.copy(errorAccion = fallo.mensaje, sinPermiso = fallo.sinPermiso)
        }
    }

    fun descartarErrorAccion() {
        _state.value = _state.value.copy(errorAccion = null)
    }

    fun limpiar() {
        _state.value = AdminPlagasUIState()
    }
}

class AdminPlagasViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AdminPlagasViewModel(gddService) as T
}
