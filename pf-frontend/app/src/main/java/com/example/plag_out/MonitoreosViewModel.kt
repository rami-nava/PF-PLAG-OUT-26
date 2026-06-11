package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val monitoreos: List<MonitoreoResponse> = emptyList()
)

class MonitoreosViewModel(context: Context, repository: MonitoreoRepository) : ViewModel() {//(private val repository: MonitoreoRepository) : ViewModel(){

    private val _state = MutableStateFlow(MonitoreoUIState())
    val state: StateFlow<MonitoreoUIState> = _state.asStateFlow()
    val monitoreosRepository = repository
    val context = context


    @RequiresApi(Build.VERSION_CODES.O)
    fun getMonitoreos() {
        viewModelScope.launch {
            val lastFetch = getLastFetchTime()
            val now = System.currentTimeMillis()

            // Verificar si pasaron > 24 horas
            if (now - lastFetch > 24 * 60 * 60 * 1000 || lastFetch == 0L) {
                _state.value = _state.value.copy(isLoading = true)

                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.gddService.getMonitoreos()
                    }

                    if (response.isSuccessful) {
                        val monitoresResponse = response.body() ?: emptyList()

                        _state.value = _state.value.copy(
                            monitoreos = monitoresResponse,
                            isLoading = false
                        )

                        monitoreosRepository.guardarMonitoreos(monitoresResponse)
                        saveLastFetchTime(now)
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(isLoading = false)
                    Log.e("MONITOREOS", "Error: ${e.message}")

                    // Cargar del caché
                    //TODO AGREGAR WARNING DE VALORES DESACTUALIZADOS Y FALTA DE CONEXION
                    val cachedMonitoreos = withContext(Dispatchers.IO) {
                        monitoreosRepository.obtenerMonitoreos()
                    }

                    _state.value = _state.value.copy(
                        monitoreos = cachedMonitoreos,
                        isLoading = false
                    )
                }
            } else {
                // Cargar del caché
                val cachedMonitoreos = withContext(Dispatchers.IO) {
                    monitoreosRepository.obtenerMonitoreos()
                }

                _state.value = _state.value.copy(
                    monitoreos = cachedMonitoreos,
                    isLoading = false
                )
            }
        }
    }


    private fun getLastFetchTime(): Long {
        val prefs = context.getSharedPreferences("gdd_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_fetch_time", 0L)
    }

    private fun saveLastFetchTime(time: Long) {
        val prefs = context.getSharedPreferences("gdd_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_fetch_time", time)
            .apply()
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