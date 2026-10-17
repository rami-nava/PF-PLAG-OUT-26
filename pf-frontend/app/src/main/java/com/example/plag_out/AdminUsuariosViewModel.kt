package com.example.plag_out

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FiltroUsuarios(val activo: Boolean?) { TODOS(null), ACTIVOS(true), SUSPENDIDOS(false) }

data class AdminUsuariosUIState(
    val usuarios: List<UsuarioAdmin> = emptyList(),
    val total: Int = 0,
    val busqueda: String = "",
    val filtro: FiltroUsuarios = FiltroUsuarios.TODOS,
    val cargado: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val cargandoMas: Boolean = false,
    /** id del usuario sobre el que hay una acción en curso. */
    val procesando: String? = null,
    val error: String? = null,
    val errorAccion: String? = null,
    val sinPermiso: Boolean = false
) {
    // Un usuario suspendido o reactivado sigue en la lista aunque ya no cumpla el filtro del
    // servidor, así que no cuenta contra el total ni corre el offset de la próxima página.
    val enServidor: Int get() = usuarios.count { filtro.activo == null || it.estaActivo == filtro.activo }

    val hayMas: Boolean get() = enServidor < total

    val usuariosVisibles: List<UsuarioAdmin> get() = filtrarUsuarios(usuarios, busqueda, filtro)
}

fun filtrarUsuarios(usuarios: List<UsuarioAdmin>, busqueda: String, filtro: FiltroUsuarios): List<UsuarioAdmin> {
    val texto = busqueda.trim()
    return usuarios.filter { usuario ->
        (filtro.activo == null || usuario.estaActivo == filtro.activo) &&
            (texto.isEmpty() ||
                usuario.email.contains(texto, ignoreCase = true) ||
                usuario.nombreCompleto.contains(texto, ignoreCase = true) ||
                usuario.apellido?.contains(texto, ignoreCase = true) == true)
    }
}

class AdminUsuariosViewModel(
    private val gddService: GDDService = RetrofitClient.gddService,
    private val esperaBusquedaMs: Long = 250L
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUsuariosUIState())
    val state: StateFlow<AdminUsuariosUIState> = _state.asStateFlow()

    private var cargaJob: Job? = null

    fun usuario(id: String): UsuarioAdmin? = _state.value.usuarios.firstOrNull { it.id == id }

    fun cargar(forzar: Boolean = false) {
        if (!forzar && (_state.value.cargado || _state.value.isLoading)) return
        cargarDesde(0)
    }

    fun refrescar() = cargarDesde(0, refresco = true)

    /** La búsqueda espera a que el admin deje de tipear para no disparar un request por letra. */
    fun buscar(texto: String) {
        _state.value = _state.value.copy(busqueda = texto)
        cargarDesde(0, espera = esperaBusquedaMs)
    }

    fun cambiarFiltro(filtro: FiltroUsuarios) {
        if (filtro == _state.value.filtro) return
        _state.value = _state.value.copy(filtro = filtro)
        cargarDesde(0)
    }

    fun cargarMas() {
        val actual = _state.value
        if (!actual.hayMas || actual.cargandoMas || actual.isLoading || cargaJob?.isActive == true) return
        cargarDesde(actual.enServidor)
    }

    /**
     * Cuando un usuario sale del resultado del servidor (eliminado, o ya no cumple el filtro), los
     * siguientes se corren una posición. Se pide desde lo que queda en el servidor para traer al
     * que se corrió sin esperar al scroll; los repetidos se descartan por id.
     */
    private fun completarPagina() {
        val actual = _state.value
        // Si hay una carga desde el inicio en curso, esa ya trae la lista correcta
        if (!actual.hayMas || !actual.cargado || (cargaJob?.isActive == true && !actual.cargandoMas)) return
        cargarDesde(actual.enServidor)
    }

    private fun cargarDesde(offset: Int, espera: Long = 0L, refresco: Boolean = false) {
        // Una carga nueva desde el inicio (búsqueda o filtro distinto) reemplaza a la anterior
        cargaJob?.cancel()
        val primera = offset == 0
        _state.value = _state.value.copy(
            isLoading = primera && !refresco,
            isRefreshing = primera && refresco,
            cargandoMas = !primera,
            error = null,
            sinPermiso = false
        )
        cargaJob = viewModelScope.launch {
            if (espera > 0) delay(espera)
            val filtros = _state.value
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.adminGetUsuarios(
                        q = filtros.busqueda.trim().ifEmpty { null },
                        activo = filtros.filtro.activo,
                        limit = TAMANIO_PAGINA,
                        offset = offset
                    )
                }
                val cuerpo = response.body()
                if (response.isSuccessful && cuerpo != null) {
                    val items = cuerpo.items.orEmpty()
                    val usuarios = if (primera) items else {
                        val cargados = _state.value.usuarios
                        val ids = cargados.mapTo(HashSet()) { it.id }
                        cargados + items.filterNot { it.id in ids }
                    }
                    _state.value = _state.value.copy(
                        usuarios = usuarios,
                        total = cuerpo.total,
                        cargado = true
                    )
                } else {
                    val fallo = falloDeRespuesta(response)
                    _state.value = _state.value.copy(error = fallo.mensaje, sinPermiso = fallo.sinPermiso)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_USUARIOS", "Error: ${e.message}")
                _state.value = _state.value.copy(error = falloDeExcepcion(e).mensaje)
            } finally {
                // Si esta carga fue reemplazada por otra, el estado de carga lo maneja la nueva
                if (isActive) {
                    _state.value = _state.value.copy(isLoading = false, isRefreshing = false, cargandoMas = false)
                }
            }
        }
    }

    fun suspender(id: String) = actualizar(id, "Cuenta suspendida.") { gddService.adminSuspenderUsuario(id) }

    fun reactivar(id: String) = actualizar(id, "Cuenta reactivada.") { gddService.adminReactivarUsuario(id) }

    private fun actualizar(id: String, mensajeExito: String, llamada: suspend () -> retrofit2.Response<UsuarioAdmin>) {
        if (_state.value.procesando != null) return
        _state.value = _state.value.copy(procesando = id, errorAccion = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { llamada() }
                val usuario = response.body()
                if (response.isSuccessful && usuario != null) {
                    _state.value = _state.value.copy(
                        usuarios = _state.value.usuarios.map { if (it.id == id) usuario else it }
                    )
                    // Se queda en la lista (el detalle lo sigue mostrando), pero en el servidor
                    // salió del filtro y corrió las páginas.
                    val filtro = _state.value.filtro.activo
                    if (filtro != null && usuario.estaActivo != filtro) completarPagina()
                    Avisos.exito(mensajeExito)
                } else {
                    val fallo = falloDeRespuesta(response)
                    _state.value = _state.value.copy(errorAccion = fallo.mensaje, sinPermiso = fallo.sinPermiso)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_USUARIOS", "Error: ${e.message}")
                _state.value = _state.value.copy(errorAccion = falloDeExcepcion(e).mensaje)
            } finally {
                _state.value = _state.value.copy(procesando = null)
            }
        }
    }

    fun eliminar(id: String, onEliminado: () -> Unit) {
        if (_state.value.procesando != null) return
        _state.value = _state.value.copy(procesando = id, errorAccion = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.adminEliminarUsuario(id) }
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        usuarios = _state.value.usuarios.filterNot { it.id == id },
                        total = (_state.value.total - 1).coerceAtLeast(0),
                        procesando = null
                    )
                    completarPagina()
                    onEliminado()
                } else {
                    val fallo = falloDeRespuesta(response)
                    _state.value = _state.value.copy(
                        errorAccion = fallo.mensaje,
                        sinPermiso = fallo.sinPermiso,
                        procesando = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_USUARIOS", "Error: ${e.message}")
                _state.value = _state.value.copy(errorAccion = falloDeExcepcion(e).mensaje, procesando = null)
            }
        }
    }

    fun descartarErrorAccion() {
        _state.value = _state.value.copy(errorAccion = null)
    }

    fun limpiar() {
        cargaJob?.cancel()
        _state.value = AdminUsuariosUIState()
    }

    companion object {
        const val TAMANIO_PAGINA = 20
    }
}

class AdminUsuariosViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AdminUsuariosViewModel(gddService) as T
}
