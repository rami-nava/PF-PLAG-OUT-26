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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlantacionUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val plantaciones: List<PlantacionesResponse> = emptyList()
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun refrescar() = getPlantaciones(forzar = true)

    /** Cierre de sesión: descarta en memoria los datos del usuario anterior. */
    fun limpiar() {
        _state.value = PlantacionUIState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPlantaciones(forzar: Boolean = false) {
        viewModelScope.launch {
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

            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.getPlantaciones()
                }

                if (response.isSuccessful) {
                    val plantacionesResponse = response.body() ?: emptyList()

                    _state.value = _state.value.copy(
                        plantaciones = plantacionesResponse,
                        isLoading = false,
                        isRefreshing = false
                    )

                    withContext(Dispatchers.IO) {
                        plantacionRepository.reemplazarPlantaciones(plantacionesResponse)
                    }
                    CacheTracker.marcarConsultado(context, CacheTracker.PLANTACIONES)
                } else {
                    // TODO AGREGAR WARNING DE VALORES DESACTUALIZADOS
                    _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                    Log.e("PLANTACIONES", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                // Sin conexión: se queda con el caché ya mostrado
                _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                Log.e("PLANTACIONES", "Error: ${e.message}")
            }
        }
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