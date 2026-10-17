package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import com.example.plag_out.Service.traerTodasLasPaginas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlantacionUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val plantaciones: List<PlantacionesResponse> = emptyList(),
    val cargandoMas: Boolean = false,
    val procesando: Boolean = false,
    val error: String? = null
)

class PlantacionesViewModel(
    context: Context,
    repository: PlantacionRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(PlantacionUIState())
    val state: StateFlow<PlantacionUIState> = _state.asStateFlow()
    val plantacionRepository = repository
    val context = context
    private var carga: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun refrescar() = getPlantaciones(forzar = true)

    /** Cierre de sesión: descarta en memoria los datos del usuario anterior. */
    fun limpiar() {
        carga?.cancel()
        _state.value = PlantacionUIState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPlantaciones(forzar: Boolean = false) {
        // Varias pantallas lo piden al entrar: si ya hay una carga en curso, esa alcanza. Un
        // refresco manual la reemplaza para no mezclar páginas de dos recorridos distintos.
        if (carga?.isActive == true) {
            if (!forzar) return
            carga?.cancel()
        }
        carga = viewModelScope.launch {
            // Mostrar el caché al instante (si existe)
            val cachedPlantaciones = withContext(Dispatchers.IO) {
                plantacionRepository.obtenerPlantaciones()
            }
            if (cachedPlantaciones.isNotEmpty()) {
                _state.value = _state.value.copy(plantaciones = cachedPlantaciones, isLoading = false)
            }

            // Las plantaciones solo cambian desde la app (y las altas ya se guardan en Room),
            // así que solo se consulta el backend la primera vez o si el usuario fuerza el refresco
            if (!forzar && CacheTracker.yaConsultado(context, CacheTracker.PLANTACIONES)) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            if (forzar) {
                _state.value = _state.value.copy(isRefreshing = true)
            } else if (cachedPlantaciones.isEmpty()) {
                _state.value = _state.value.copy(isLoading = true)
            }

            val recibidas = LinkedHashMap<Int, PlantacionesResponse>()
            fun conCacheRestante() =
                recibidas.values.toList() + cachedPlantaciones.filterNot { it.plantacion_id in recibidas }

            try {
                val error = traerTodasLasPaginas(
                    recibidas,
                    id = { it.plantacion_id },
                    alQuedarPaginas = {
                        _state.value = _state.value.copy(
                            plantaciones = conCacheRestante(),
                            isLoading = false,
                            cargandoMas = true
                        )
                    }
                ) { limit, offset ->
                    withContext(Dispatchers.IO) { gddService.getPlantaciones(limit, offset) }
                }
                if (error != null) {
                    cargaIncompleta(recibidas.values.toList(), conCacheRestante())
                    if (forzar) Avisos.error(errorDeRespuesta(error).mensaje)
                    Log.e("CULTIVOS", "Error: ${error.code()}")
                    return@launch
                }

                val plantacionesResponse = recibidas.values.toList()
                _state.value = _state.value.copy(
                    plantaciones = plantacionesResponse,
                    isLoading = false,
                    isRefreshing = false,
                    cargandoMas = false
                )

                withContext(Dispatchers.IO) {
                    plantacionRepository.reemplazarPlantaciones(plantacionesResponse)
                }
                CacheTracker.marcarConsultado(context, CacheTracker.PLANTACIONES)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin conexión: se queda con el caché ya mostrado más lo que se llegó a recibir
                cargaIncompleta(recibidas.values.toList(), conCacheRestante())
                if (forzar) Avisos.error(errorDeExcepcion(e).mensaje)
                Log.e("CULTIVOS", "Error: ${e.message}")
            }
        }
    }

    /**
     * Una página falló a mitad de la carga: el caché no se reemplaza (quedaría sin las plantaciones
     * que faltaron), solo se actualizan las que sí llegaron. No se marca consultado, así que la
     * próxima entrada vuelve a intentar la carga completa.
     */
    private suspend fun cargaIncompleta(recibidas: List<PlantacionesResponse>, mostradas: List<PlantacionesResponse>) {
        if (recibidas.isNotEmpty()) {
            withContext(Dispatchers.IO) { plantacionRepository.guardarPlantaciones(recibidas) }
        }
        _state.value = _state.value.copy(
            plantaciones = mostradas,
            isLoading = false,
            isRefreshing = false,
            cargandoMas = false
        )
    }

    /** Cierre irreversible de plantación, monitoreos y ciclos; conserva el historial. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun finalizarPlantacion(plantacionId: Int, onSuccess: () -> Unit) {
        if (_state.value.procesando) return
        val actual = _state.value.plantaciones.find { it.plantacion_id == plantacionId } ?: return
        if (!actual.activa) return

        _state.value = _state.value.copy(procesando = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.actualizarPlantacion(plantacionId, UpdatePlantacionRequest(activa = false))
                }
                if (response.isSuccessful && response.body()?.plantacion_id == plantacionId && response.body()?.activa == false) {
                    val actualizada = response.body()!!
                    withContext(Dispatchers.IO) { plantacionRepository.guardarPlantacion(actualizada) }
                    _state.value = _state.value.copy(
                        plantaciones = _state.value.plantaciones.map { if (it.plantacion_id == plantacionId) actualizada else it },
                        procesando = false
                    )
                    onSuccess()
                } else {
                    val mensaje = if (response.isSuccessful) "No se pudo finalizar el cultivo. Intentá de nuevo."
                                  else errorDeRespuesta(response, ERRORES_FINALIZAR, ERRORES_FINALIZAR_CODIGO).mensaje
                    _state.value = _state.value.copy(procesando = false, error = mensaje)
                    Log.e("CULTIVOS", "Error al finalizar: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    procesando = false,
                    error = errorDeExcepcion(e).mensaje
                )
                Log.e("CULTIVOS", "Error al finalizar: ${e.message}")
            }
        }
    }

    fun limpiarError() {
        _state.value = _state.value.copy(error = null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun eliminarPlantacion(plantacionId: Int, onSuccess: () -> Unit) {
        _state.value = _state.value.copy(procesando = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { gddService.eliminarPlantacion(plantacionId) }
                if (response.isSuccessful) {
                    withContext(Dispatchers.IO) { plantacionRepository.borrarPlantacion(plantacionId) }
                    _state.value = _state.value.copy(
                        plantaciones = _state.value.plantaciones.filter { it.plantacion_id != plantacionId },
                        procesando = false
                    )
                    onSuccess()
                } else {
                    _state.value = _state.value.copy(
                        procesando = false,
                        error = errorDeRespuesta(response, porCodigo = ERRORES_ELIMINAR).mensaje
                    )
                    Log.e("CULTIVOS", "Error al eliminar: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    procesando = false,
                    error = errorDeExcepcion(e).mensaje
                )
                Log.e("CULTIVOS", "Error al eliminar: ${e.message}")
            }
        }
    }

    /**
     * Purga local (Room + memoria) de las plantaciones de un terreno eliminado. No llama a la red:
     * el DELETE del terreno ya se hizo y se asume que el backend eliminó en cascada; esto solo
     * sincroniza el caché del cliente para que no queden plantaciones huérfanas.
     */
    fun purgarPorTerreno(terrenoId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { plantacionRepository.borrarPorTerreno(terrenoId) }
            _state.value = _state.value.copy(
                plantaciones = _state.value.plantaciones.filter { it.terreno_id != terrenoId }
            )
        }
    }

    fun renombrarTerreno(terrenoId: Int, nombre: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { plantacionRepository.renombrarTerreno(terrenoId, nombre) }
            _state.value = _state.value.copy(
                plantaciones = _state.value.plantaciones.map {
                    if (it.terreno_id == terrenoId) it.copy(terreno_nombre = nombre) else it
                }
            )
        }
    }

    private companion object {
        val ERRORES_FINALIZAR = mapOf(
            "plantation_finalized" to "La plantación ya fue finalizada y no puede reactivarse."
        )
        val ERRORES_FINALIZAR_CODIGO = mapOf(
            404 to "El cultivo ya no existe.",
            500 to "No se pudo finalizar el cultivo. Intentá de nuevo."
        )
        val ERRORES_ELIMINAR = mapOf(
            404 to "El cultivo ya no existe.",
            500 to "No se pudo eliminar el cultivo. Intentá de nuevo."
        )
    }
}

class PlantacionesViewModelFactory(
    private val context: Context,
    private val repository: PlantacionRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlantacionesViewModel(context, repository, gddService) as T
    }
}