package com.example.plag_out

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

data class AdminCultivosUIState(
    val cultivos: List<CultivoAdmin> = emptyList(),
    // Catálogo de plagas para el buscador de "Agregar plaga"
    val plagas: List<PlagaAdmin> = emptyList(),
    val busqueda: String = "",
    val filtro: FiltroCultivos = FiltroCultivos.TODOS,
    val cargado: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    // El backend todavía no tiene /admin/cultivos
    val noDisponible: Boolean = false,
    // Cambiaron las plagas desde la otra pestaña: la próxima carga va al servidor
    val desactualizado: Boolean = false,
    val cargandoDetalle: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val errorAccion: String? = null,
    val erroresCampo: Map<CampoCultivo, String> = emptyMap(),
    val sinPermiso: Boolean = false
)

const val MAX_LARGO_NOMBRE_CULTIVO = 80

enum class CampoCultivo { NOMBRE, NOMBRE_CIENTIFICO }

enum class FiltroCultivos { TODOS, ACTIVOS, INACTIVOS }

data class FormularioCultivo(
    val nombre: String = "",
    val nombreCientifico: String = "",
    // Plagas sumadas en el formulario: en el alta son todas; en un cultivo existente, las que
    // todavía no tiene. Todo viaja recién al guardar.
    val plagas: Set<Int> = emptySet(),
    // Plagas que el cultivo ya tenía y se sacaron en el formulario
    val quitadas: Set<Int> = emptySet()
) {
    fun agregar(id: Int) = copy(plagas = plagas + id, quitadas = quitadas - id)

    fun quitar(id: Int) = copy(plagas = plagas - id, quitadas = quitadas + id)

    companion object {
        fun desde(cultivo: CultivoAdmin) = FormularioCultivo(
            nombre = cultivo.nombre,
            nombreCientifico = cultivo.nombre_cientifico
        )
    }
}

fun validarCultivo(form: FormularioCultivo): Map<CampoCultivo, String> {
    val errores = mutableMapOf<CampoCultivo, String>()
    fun validar(campo: CampoCultivo, valor: String, vacio: String) {
        val texto = valor.trim()
        when {
            texto.isEmpty() -> errores[campo] = vacio
            texto.length > MAX_LARGO_NOMBRE_CULTIVO -> errores[campo] = "Máximo $MAX_LARGO_NOMBRE_CULTIVO caracteres"
        }
    }
    validar(CampoCultivo.NOMBRE, form.nombre, "Ingresá el nombre")
    validar(CampoCultivo.NOMBRE_CIENTIFICO, form.nombreCientifico, "Ingresá el nombre científico")
    return errores
}

internal fun requestDeAltaCultivo(form: FormularioCultivo) = CultivoAdminRequest(
    nombre = form.nombre.trim(),
    nombre_cientifico = form.nombreCientifico.trim(),
    plagas = form.plagas.sorted()
)

internal fun plagasFinales(original: CultivoAdmin, form: FormularioCultivo): Set<Int> =
    original.plagas.orEmpty().map { it.id }.toSet() - form.quitadas + form.plagas

// Como en plagas: un solo PATCH con lo que cambió, y las plagas van como la lista completa.
internal fun requestDeEdicionCultivo(original: CultivoAdmin, form: FormularioCultivo): CultivoAdminRequest? {
    val plagas = plagasFinales(original, form)
    val cambios = CultivoAdminRequest(
        nombre = form.nombre.trim().takeIf { it != original.nombre },
        nombre_cientifico = form.nombreCientifico.trim().takeIf { it != original.nombre_cientifico },
        plagas = plagas.sorted().takeIf { plagas != original.plagas.orEmpty().map { it.id }.toSet() }
    )
    return cambios.takeIf { it != CultivoAdminRequest() }
}

fun filtrarCultivos(cultivos: List<CultivoAdmin>, busqueda: String, filtro: FiltroCultivos): List<CultivoAdmin> {
    val texto = busqueda.trim()
    return cultivos
        .filter {
            when (filtro) {
                FiltroCultivos.TODOS -> true
                FiltroCultivos.ACTIVOS -> it.estaActivo
                FiltroCultivos.INACTIVOS -> !it.estaActivo
            }
        }
        .filter {
            texto.isEmpty() ||
                it.nombre.contains(texto, ignoreCase = true) ||
                it.nombre_cientifico.contains(texto, ignoreCase = true)
        }
        .sortedWith(compareBy<CultivoAdmin> { !it.estaActivo }.thenBy { it.nombre.lowercase() })
}

// Plagas que se pueden sumar a un cultivo: activas y que todavía no lo afectan
fun plagasParaAgregar(plagas: List<PlagaAdmin>, vinculadas: Set<Int>, busqueda: String): List<PlagaAdmin> {
    val texto = busqueda.trim()
    return plagas
        .filter { it.estaActiva && it.id !in vinculadas }
        .filter {
            texto.isEmpty() ||
                it.nombre.contains(texto, ignoreCase = true) ||
                it.nombre_cientifico.contains(texto, ignoreCase = true)
        }
        .sortedBy { it.nombre.lowercase() }
}

class AdminCultivosViewModel(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(AdminCultivosUIState())
    val state: StateFlow<AdminCultivosUIState> = _state.asStateFlow()

    fun cultivo(id: Int): CultivoAdmin? = _state.value.cultivos.firstOrNull { it.id == id }

    fun cargar(forzar: Boolean = false) {
        val actual = _state.value
        if (actual.isLoading || actual.isRefreshing) return
        if (!forzar && actual.cargado && !actual.desactualizado) return

        _state.value = actual.copy(
            isLoading = !actual.cargado,
            isRefreshing = actual.cargado,
            error = null,
            sinPermiso = false
        )
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.adminGetCultivos() }
                val cultivos = response.body()
                if (response.isSuccessful && cultivos != null) {
                    _state.value = _state.value.copy(
                        cultivos = cultivos,
                        cargado = true,
                        noDisponible = false,
                        desactualizado = false
                    )
                } else {
                    val cuerpo = leerCuerpoDeError(response)
                    val fallo = falloDeCuerpo(response.code(), cuerpo)
                    _state.value = _state.value.copy(
                        error = fallo.mensaje,
                        sinPermiso = fallo.sinPermiso,
                        noDisponible = esRutaNoDisponible(response.code(), cuerpo)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_CULTIVOS", "Error: ${e.message}")
                _state.value = _state.value.copy(error = falloDeExcepcion(e).mensaje)
            } finally {
                _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    fun refrescar() = cargar(forzar = true)

    // Lo llama la pestaña de Plagas cuando cambia algo que se ve acá (cultivos afectados, bajas)
    fun marcarDesactualizado() {
        _state.value = _state.value.copy(desactualizado = true, plagas = emptyList())
    }

    fun abrirDetalle(id: Int?) {
        _state.value = _state.value.copy(errorAccion = null, erroresCampo = emptyMap())
        cargarPlagasSiHaceFalta()
        if (id == null) return
        _state.value = _state.value.copy(cargandoDetalle = true)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.adminGetCultivo(id) }
                val cultivo = response.body()
                if (response.isSuccessful && cultivo != null) {
                    reemplazar(cultivo)
                } else {
                    // Si falla, la pantalla sigue con lo que trajo la lista
                    aplicarFalloDeAccion(response)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_CULTIVOS", "No se pudo cargar el cultivo $id: ${e.message}")
            } finally {
                _state.value = _state.value.copy(cargandoDetalle = false)
            }
        }
    }

    // Las plagas solo se usan en el buscador: si fallan, el detalle igual se muestra.
    private fun cargarPlagasSiHaceFalta() {
        if (_state.value.plagas.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.adminGetPlagas() }
                response.body()?.takeIf { response.isSuccessful }?.let {
                    _state.value = _state.value.copy(plagas = it)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_CULTIVOS", "No se pudieron cargar las plagas: ${e.message}")
            }
        }
    }

    fun buscar(texto: String) {
        _state.value = _state.value.copy(busqueda = texto)
    }

    fun cambiarFiltro(filtro: FiltroCultivos) {
        _state.value = _state.value.copy(filtro = filtro)
    }

    fun guardar(original: CultivoAdmin?, form: FormularioCultivo, onGuardado: () -> Unit) {
        if (_state.value.guardando || validarCultivo(form).isNotEmpty()) return
        val edicion = original?.let { requestDeEdicionCultivo(it, form) }
        if (original != null && edicion == null) {
            onGuardado() // Nada que guardar
            return
        }
        ejecutar(onExito = onGuardado) {
            if (original == null) gddService.adminCrearCultivo(requestDeAltaCultivo(form))
            else gddService.adminActualizarCultivo(original.id, edicion!!)
        }
    }

    fun desactivar(id: Int, onHecho: () -> Unit = {}) =
        ejecutar(onExito = onHecho) { gddService.adminDesactivarCultivo(id) }

    fun reactivar(id: Int, onHecho: () -> Unit = {}) =
        ejecutar(onExito = onHecho) { gddService.adminActualizarCultivo(id, CultivoAdminRequest(activo = true)) }

    private fun ejecutar(onExito: () -> Unit, llamada: suspend () -> Response<CultivoAdmin>) {
        if (_state.value.guardando) return
        _state.value = _state.value.copy(guardando = true, errorAccion = null, erroresCampo = emptyMap())
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { llamada() }
                val cultivo = response.body()
                if (response.isSuccessful && cultivo != null) {
                    reemplazar(cultivo)
                    _state.value = _state.value.copy(guardando = false)
                    onExito()
                } else {
                    _state.value = _state.value.copy(guardando = false)
                    aplicarFalloDeAccion(response)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_CULTIVOS", "Error: ${e.message}")
                _state.value = _state.value.copy(guardando = false, errorAccion = falloDeExcepcion(e).mensaje)
            }
        }
    }

    private fun aplicarFalloDeAccion(response: Response<*>) {
        val cuerpo = leerCuerpoDeError(response)
        val fallo = falloDeCuerpo(response.code(), cuerpo)
        val campo = if (response.code() == 409 && cuerpo.contains("cultivo_duplicado")) campoDuplicado(cuerpo) else null
        _state.value = _state.value.copy(
            errorAccion = fallo.mensaje,
            sinPermiso = fallo.sinPermiso,
            erroresCampo = campo?.let { mapOf(it to "Ya existe un cultivo con este nombre") }.orEmpty()
        )
    }

    private fun campoDuplicado(cuerpo: String): CampoCultivo? =
        when (runCatching { JsonParser.parseString(cuerpo).asJsonObject.get("campo")?.asString }.getOrNull()) {
            "nombre" -> CampoCultivo.NOMBRE
            "nombre_cientifico" -> CampoCultivo.NOMBRE_CIENTIFICO
            else -> null
        }

    private fun reemplazar(cultivo: CultivoAdmin) {
        val actuales = _state.value.cultivos
        val nuevos = if (actuales.any { it.id == cultivo.id }) {
            actuales.map { if (it.id == cultivo.id) cultivo else it }
        } else {
            actuales + cultivo
        }
        _state.value = _state.value.copy(cultivos = nuevos)
    }

    // El campo se volvió a editar: su error del servidor ya no aplica
    fun descartarErrorCampo(campo: CampoCultivo) {
        if (campo in _state.value.erroresCampo) {
            _state.value = _state.value.copy(erroresCampo = _state.value.erroresCampo - campo)
        }
    }

    fun descartarErrorAccion() {
        _state.value = _state.value.copy(errorAccion = null)
    }

    fun limpiar() {
        _state.value = AdminCultivosUIState()
    }
}

class AdminCultivosViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AdminCultivosViewModel(gddService) as T
}
