package com.example.plag_out

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MonitoreoUIState(
    val isLoading: Boolean = false,
    val monitoreos: List<MonitoreoResponse> = emptyList()
)

class MonitoreosViewModel : ViewModel() {

    private val _state = MutableStateFlow(MonitoreoUIState())
    val state: StateFlow<MonitoreoUIState> = _state.asStateFlow()


    @RequiresApi(Build.VERSION_CODES.O)
    fun getMonitoreos() {
        viewModelScope.launch(Dispatchers.IO) {
            val lastFetch = getLastFetchTime()
            val now = System.currentTimeMillis()

            // Verificar si pasaron >24 horas
            //if (now - lastFetch > 24 * 60 * 60 * 1000) {
            //    _state.value = _state.value.copy(isLoading = true)

                try {
                    val response = RetrofitClient.gddService.getMonitoreos()
                    if(response.isSuccessful) {
                        val monitoresResponse = response.body() ?: emptyList()
                        _state.value = _state.value.copy(
                            monitoreos = monitoresResponse,
                            isLoading = false
                        )
                        saveLastFetchTime(now)  // Guardar timestamp
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(isLoading = false)
                    Log.e("GDD", "Error: ${e.message}")
                }

            //}

        }

    }

    private fun getLastFetchTime(): Long {
        // Obtener de SharedPreferences (o datastore)
        return 0L  // Primera vez
    }

    private fun saveLastFetchTime(time: Long) {
        // Guardar en SharedPreferences
    }

    }