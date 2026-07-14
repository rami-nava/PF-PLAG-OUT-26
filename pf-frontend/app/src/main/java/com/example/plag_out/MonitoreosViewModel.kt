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
    val monitoreos: List<MonitoreoResponse> = emptyList()
)

class MonitoreosViewModel(context: Context, repository: MonitoreoRepository) : ViewModel() {//(private val repository: MonitoreoRepository) : ViewModel(){

    private val _state = MutableStateFlow(MonitoreoUIState())
    val state: StateFlow<MonitoreoUIState> = _state.asStateFlow()
    val monitoreosRepository = repository
    val context = context


    @RequiresApi(Build.VERSION_CODES.O)
    fun refrescar() = getMonitoreos(forzar = true)

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

            // El backend recalcula los GDD una vez por día (00:00 hora argentina):
            // si ya se consultó hoy, el caché sigue vigente salvo que el usuario fuerce el refresco
            if (!forzar && CacheTracker.consultadoHoy(context, CacheTracker.MONITOREOS)) {
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
                    RetrofitClient.gddService.getMonitoreos()
                }

                if (response.isSuccessful) {
                    val monitoresResponse = response.body() ?: emptyList()

                    _state.value = _state.value.copy(
                        monitoreos = monitoresResponse,
                        isLoading = false,
                        isRefreshing = false
                    )

                    withContext(Dispatchers.IO) {
                        monitoreosRepository.reemplazarMonitoreos(monitoresResponse)
                    }
                    CacheTracker.marcarConsultado(context, CacheTracker.MONITOREOS)
                } else {
                    // TODO AGREGAR WARNING DE VALORES DESACTUALIZADOS
                    _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                    Log.e("MONITOREOS", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                // Sin conexión: se queda con el caché ya mostrado
                //TODO AGREGAR WARNING DE VALORES DESACTUALIZADOS Y FALTA DE CONEXION
                _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                Log.e("MONITOREOS", "Error: ${e.message}")
            }
        }
    }
}

class MonitoreosViewModelFactory(
    private val context: Context,
    private val repository: MonitoreoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MonitoreosViewModel(context, repository) as T
    }
}