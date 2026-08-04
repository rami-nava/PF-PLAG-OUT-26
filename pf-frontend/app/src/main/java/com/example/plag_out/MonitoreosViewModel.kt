package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MonitoreoUIState(
    var isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val monitoreos: List<MonitoreoResponse> = emptyList(),
    /** Se sirvió del caché porque el fetch a la red falló o no hay conexión. */
    val datosDesactualizados: Boolean = false
)

class MonitoreosViewModel(
    context: Context,
    repository: MonitoreoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(MonitoreoUIState())
    val state: StateFlow<MonitoreoUIState> = _state.asStateFlow()
    val monitoreosRepository = repository
    val context = context


    @RequiresApi(Build.VERSION_CODES.O)
    fun refrescar() = getMonitoreos(forzar = true)

    /** Cierre de sesión: descarta en memoria los datos del usuario anterior. */
    fun limpiar() {
        _state.value = MonitoreoUIState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getMonitoreos(forzar: Boolean = false) {
        viewModelScope.launch {
            // Mostrar el caché al instante (si existe)
            val cachedMonitoreos = withContext(Dispatchers.IO) {
                monitoreosRepository.obtenerMonitoreos()
            }
            if (cachedMonitoreos.isNotEmpty()) {
                _state.value = _state.value.copy(monitoreos = cachedMonitoreos, isLoading = false)
            }

            // El backend recalcula los GDD una vez por día (workflow de las 06:00 hora argentina):
            // si ya se consultó después de ese corte, el caché sigue vigente salvo que el usuario
            // fuerce el refresco. Si el caché vino vacío (p.ej. justo después de un bump de versión
            // de Room que borró la tabla) igual hay que ir a buscar los datos, aunque la marca diga
            // "ya consultado".
            if (!forzar && cachedMonitoreos.isNotEmpty() &&
                CacheTracker.consultadoTrasUltimaActualizacion(context, CacheTracker.MONITOREOS)
            ) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            if (forzar) {
                _state.value = _state.value.copy(isRefreshing = true)
            } else if (cachedMonitoreos.isEmpty()) {
                _state.value = _state.value.copy(isLoading = true)
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.getMonitoreos()
                }

                if (response.isSuccessful) {
                    val monitoresResponse = response.body() ?: emptyList()

                    withContext(Dispatchers.IO) {
                        monitoreosRepository.reemplazarMonitoreos(monitoresResponse)
                    }

                    _state.value = _state.value.copy(
                        monitoreos = monitoresResponse,
                        isLoading = false,
                        isRefreshing = false,
                        datosDesactualizados = false
                    )
                    CacheTracker.marcarConsultado(context, CacheTracker.MONITOREOS)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        datosDesactualizados = cachedMonitoreos.isNotEmpty()
                    )
                    Log.e("MONITOREOS", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                // Sin conexión: se queda con el caché ya mostrado
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    datosDesactualizados = cachedMonitoreos.isNotEmpty()
                )
                Log.e("MONITOREOS", "Error: ${e.message}")
            }
        }
    }

    /**
     * Al pausar una plantación, el backend cascadea `activo = false` a sus monitoreos en el mismo
     * PATCH `/plantaciones/{id}` — acá solo reflejamos localmente (Room + memoria) lo que el
     * servidor ya hizo, sin llamadas de red adicionales.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun finalizarPorPlantacion(plantacionId: Int) {
        viewModelScope.launch {
            val actualizados = _state.value.monitoreos
                .filter { it.plantacion_id == plantacionId && it.activo }
                .map { it.copy(activo = false) }
            if (actualizados.isEmpty()) return@launch

            withContext(Dispatchers.IO) {
                actualizados.forEach { monitoreosRepository.guardarMonitoreo(it) }
            }

            val actualizadosPorId = actualizados.associateBy { it.monitoreo_id }
            _state.value = _state.value.copy(
                monitoreos = _state.value.monitoreos.map { actualizadosPorId[it.monitoreo_id] ?: it }
            )
        }
    }

    /**
     * Purga local (Room + memoria) de los monitoreos de un terreno eliminado. No llama a la red:
     * el DELETE del terreno ya se hizo y se asume que el backend eliminó en cascada; esto solo
     * sincroniza el caché del cliente para que no queden monitoreos huérfanos.
     */
    fun purgarPorTerreno(terrenoId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { monitoreosRepository.borrarPorTerreno(terrenoId) }
            _state.value = _state.value.copy(
                monitoreos = _state.value.monitoreos.filter { it.terreno_id != terrenoId }
            )
        }
    }

    /**
     * Purga local (Room + memoria) de los monitoreos de una plantación eliminada. No llama a la
     * red: el DELETE de la plantación ya se hizo y se asume que el backend eliminó en cascada;
     * esto solo sincroniza el caché del cliente para que no queden monitoreos huérfanos.
     */
    fun purgarPorPlantacion(plantacionId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { monitoreosRepository.borrarPorPlantacion(plantacionId) }
            _state.value = _state.value.copy(
                monitoreos = _state.value.monitoreos.filter { it.plantacion_id != plantacionId }
            )
        }
    }
}

class MonitoreosViewModelFactory(
    private val context: Context,
    private val repository: MonitoreoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MonitoreosViewModel(context, repository, gddService) as T
    }
}