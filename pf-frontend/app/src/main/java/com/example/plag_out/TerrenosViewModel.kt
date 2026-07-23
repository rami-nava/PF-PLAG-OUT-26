package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TerrenoUIState(
    var isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val terrenos: List<TerrenoResponse> = emptyList()
)

class TerrenosViewModel(
    context: Context,
    repository: TerrenoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(TerrenoUIState())
    val state: StateFlow<TerrenoUIState> = _state.asStateFlow()
    val terrenoRepository = repository
    val context = context


    @RequiresApi(Build.VERSION_CODES.O)
    fun refrescar() = getTerrenos(forzar = true)

    /** Cierre de sesión: descarta en memoria los datos del usuario anterior. */
    fun limpiar() {
        _state.value = TerrenoUIState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getTerrenos(forzar: Boolean = false) {
        viewModelScope.launch {
            // Mostrar el caché al instante (si existe)
            val cachedTerrenos = withContext(Dispatchers.IO) {
                terrenoRepository.obtenerTerrenos()
            }
            if (cachedTerrenos.isNotEmpty()) {
                _state.value = _state.value.copy(terrenos = cachedTerrenos, isLoading = false)
            }

            // Los terrenos solo cambian desde la app (y las altas ya se guardan en Room),
            // así que solo se consulta el backend la primera vez o si el usuario fuerza el refresco
            if (!forzar && CacheTracker.yaConsultado(context, CacheTracker.TERRENOS)) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            if (forzar) {
                _state.value = _state.value.copy(isRefreshing = true)
            } else if (cachedTerrenos.isEmpty()) {
                _state.value = _state.value.copy(isLoading = true)
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.getTerrenos()
                }

                if (response.isSuccessful) {
                    val terrenosResponse = response.body() ?: emptyList()

                    _state.value = _state.value.copy(
                        terrenos = terrenosResponse,
                        isLoading = false,
                        isRefreshing = false
                    )

                    withContext(Dispatchers.IO) {
                        terrenoRepository.reemplazarTerrenos(terrenosResponse)
                    }
                    CacheTracker.marcarConsultado(context, CacheTracker.TERRENOS)
                } else {
                    // TODO AGREGAR WARNING DE VALORES DESACTUALIZADOS
                    _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                    Log.e("TERRENOS", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                // Sin conexión: se queda con el caché ya mostrado
                _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                Log.e("TERRENOS", "Error: ${e.message}")
            }
        }
    }
}

class TerrenosViewModelFactory(
    private val context: Context,
    private val repository: TerrenoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TerrenosViewModel(context, repository, gddService) as T
    }
}