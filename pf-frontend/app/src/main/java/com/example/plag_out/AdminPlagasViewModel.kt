package com.example.plag_out

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

data class AdminPlagasUIState(
    val plagas: List<PlagaAdmin> = emptyList(),
    val cultivos: List<CultivoResponse> = emptyList(),
    val busqueda: String = "",
    val filtro: FiltroPlagas = FiltroPlagas.TODAS,
    val cargado: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val errorAccion: String? = null,
    val sinPermiso: Boolean = false
)

data class FormularioPlaga(
    val nombre: String = "",
    val nombreCientifico: String = "",
    val tempBase: String = "",
    val tempMax: String = "",
    val gddEclosion: String = "",
    val gddGeneracion: String = "",
    val cultivos: Set<Int> = emptySet()
) {
    companion object {
        fun desde(plaga: PlagaAdmin) = FormularioPlaga(
            nombre = plaga.nombre,
            nombreCientifico = plaga.nombre_cientifico,
            tempBase = plaga.temp_base?.let(::formatearNumero).orEmpty(),
            tempMax = plaga.temp_max?.let(::formatearNumero).orEmpty(),
            gddEclosion = plaga.gdd_eclosion?.let(::formatearNumero).orEmpty(),
            gddGeneracion = plaga.gdd_generacion?.let(::formatearNumero).orEmpty(),
            cultivos = plaga.cultivos_afectados.orEmpty().toSet()
        )
    }
}

internal fun leerNumero(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

internal fun formatearNumero(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString()

fun validarPlaga(form: FormularioPlaga): Map<CampoPlaga, String> {
    val errores = mutableMapOf<CampoPlaga, String>()
    if (form.nombre.isBlank()) errores[CampoPlaga.NOMBRE] = "Ingresá el nombre"
    if (form.nombreCientifico.isBlank()) errores[CampoPlaga.NOMBRE_CIENTIFICO] = "Ingresá el nombre científico"

    val tBase = leerNumero(form.tempBase)
    val tMax = leerNumero(form.tempMax)
    if (tBase == null) errores[CampoPlaga.TEMP_BASE] = "Número requerido"
    if (tMax == null) errores[CampoPlaga.TEMP_MAX] = "Número requerido"
    if (tBase != null && tMax != null && tBase >= tMax) {
        errores[CampoPlaga.TEMP_MAX] = "Tiene que ser mayor que la base"
    }

    val eclosion = leerNumero(form.gddEclosion)
    val generacion = leerNumero(form.gddGeneracion)
    if (eclosion == null || eclosion <= 0) errores[CampoPlaga.GDD_ECLOSION] = "Tiene que ser mayor a 0"
    if (generacion == null || generacion <= 0) errores[CampoPlaga.GDD_GENERACION] = "Tiene que ser mayor a 0"

    if (form.cultivos.isEmpty()) errores[CampoPlaga.CULTIVOS] = "Elegí al menos un cultivo"
    return errores
}

enum class CampoPlaga { NOMBRE, NOMBRE_CIENTIFICO, TEMP_BASE, TEMP_MAX, GDD_ECLOSION, GDD_GENERACION, CULTIVOS }

internal fun requestDeAlta(form: FormularioPlaga) = PlagaAdminRequest(
    nombre = form.nombre.trim(),
    nombre_cientifico = form.nombreCientifico.trim(),
    temp_base = leerNumero(form.tempBase),
    temp_max = leerNumero(form.tempMax),
    gdd_eclosion = leerNumero(form.gddEclosion),
    gdd_generacion = leerNumero(form.gddGeneracion),
    cultivos_afectados = form.cultivos.sorted()
)

internal fun requestDeEdicion(original: PlagaAdmin, form: FormularioPlaga): PlagaAdminRequest? {
    val completo = requestDeAlta(form)
    val cambios = PlagaAdminRequest(
        nombre = completo.nombre.takeIf { it != original.nombre },
        nombre_cientifico = completo.nombre_cientifico.takeIf { it != original.nombre_cientifico },
        temp_base = completo.temp_base.takeIf { it != original.temp_base },
        temp_max = completo.temp_max.takeIf { it != original.temp_max },
        gdd_eclosion = completo.gdd_eclosion.takeIf { it != original.gdd_eclosion },
        gdd_generacion = completo.gdd_generacion.takeIf { it != original.gdd_generacion },
        cultivos_afectados = completo.cultivos_afectados
            .takeIf { it.orEmpty().toSet() != original.cultivos_afectados.orEmpty().toSet() }
    )
    return cambios.takeIf { it != PlagaAdminRequest() }
}

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
        if (!forzar && actual.cargado) return

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
                    _state.value = _state.value.copy(plagas = plagas, cargado = true)
                    cargarCultivosSiHaceFalta()
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

    // Los cultivos solo se usan en el formulario: si fallan, la lista igual se muestra.
    private suspend fun cargarCultivosSiHaceFalta() {
        if (_state.value.cultivos.isNotEmpty()) return
        try {
            val response = withContext(Dispatchers.IO) { gddService.getCultivos() }
            response.body()?.takeIf { response.isSuccessful }?.let {
                _state.value = _state.value.copy(cultivos = it)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ADMIN_PLAGAS", "No se pudieron cargar los cultivos: ${e.message}")
        }
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
            else gddService.adminActualizarPlaga(original.id, edicion!!)
        }
    }

    fun desactivar(id: Int, onHecho: () -> Unit = {}) =
        ejecutar(onExito = onHecho) { gddService.adminDesactivarPlaga(id) }

    fun reactivar(id: Int, onHecho: () -> Unit = {}) =
        ejecutar(onExito = onHecho) { gddService.adminActualizarPlaga(id, PlagaAdminRequest(activo = true)) }

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
