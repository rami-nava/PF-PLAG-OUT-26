package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlantacionUIState(
    val isLoading: Boolean = false,
    val plantaciones: List<PlantacionesResponse> = emptyList()
)

class PlantacionesViewModel(context: Context, repository: PlantacionRepository) : ViewModel() {

    private val _state = MutableStateFlow(PlantacionUIState())
    val state: StateFlow<PlantacionUIState> = _state.asStateFlow()
    val plantacionRepository = repository
    val context = context

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPlantaciones() {
        viewModelScope.launch {
            val lastFetch = getFetched()

            // Verificar si nunca se consulto
            if (!lastFetch) {
                _state.value = _state.value.copy(isLoading = true)

                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.gddService.getPlantaciones()
                    }

                    if (response.isSuccessful) {
                        val plantacionesResponse = response.body() ?: emptyList()

                        _state.value = _state.value.copy(
                            plantaciones = plantacionesResponse,
                            isLoading = false
                        )

                        plantacionRepository.guardarPlantaciones(plantacionesResponse)
                        saveFetched()
                    }
                } catch (e: Exception) {
                    _state.value = _state.value.copy(isLoading = false)
                    Log.e("PLANTACIONES", "Error: ${e.message}")
                }
            } else {
                // Cargar del caché
                val cachedPlantaciones = withContext(Dispatchers.IO) {
                    plantacionRepository.obtenerPlantaciones()
                }

                _state.value = _state.value.copy(
                    plantaciones = cachedPlantaciones,
                    isLoading = false
                )
            }
        }

    }

    private fun getFetched(): Boolean {
        val prefs = context.getSharedPreferences("plantaciones_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("fetched", false)
    }

    private fun saveFetched() {
        val prefs = context.getSharedPreferences("plantaciones_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("fetched", true)
            .apply()
    }
}

class PlantacionesViewModelFactory(
    private val context: Context,
    private val repository: PlantacionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlantacionesViewModel(context,repository) as T
    }
}