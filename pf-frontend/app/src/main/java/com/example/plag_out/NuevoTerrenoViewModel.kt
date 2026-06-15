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

data class NuevoTerrenoUIState(
    val cultivoSeleccionado: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NuevoTerrenoViewModel : ViewModel() {

    private val _state = MutableStateFlow(NuevoTerrenoUIState())
    val state: StateFlow<NuevoTerrenoUIState> = _state.asStateFlow()

    fun seleccionarCultivo(cultivo: String?) {
        _state.value = _state.value.copy(
            cultivoSeleccionado = cultivo
        )
    }

    fun actualizarUbicacion(latitud: Double?, longitud: Double?) {
        _state.value = _state.value.copy(
            latitud = latitud,
            longitud = longitud,
            error = null // Clear error on edit
        )
    }

    fun setError(message: String?) {
        _state.value = _state.value.copy(
            error = message
        )
    }

    fun limpiarError() {
        _state.value = _state.value.copy(
            error = null
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun registrarTerreno(onSuccess: () -> Unit) {
        val currentState = _state.value
        val cultivo = currentState.cultivoSeleccionado
        val lat = currentState.latitud
        val lon = currentState.longitud

        if (cultivo == null || lat == null || lon == null) {
            _state.value = _state.value.copy(
                error = "Datos de terreno incompletos"
            )
            return
        }

        // Validación de coordenadas para República Argentina
        if (lat !in -55.0..-21.8 || lon !in -73.6..-53.6) {
            _state.value = _state.value.copy(
                error = "Ubicación fuera de la República Argentina"
            )
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // TODO: Definir la llamada de creación en GDDService y RetrofitClient
                // val request = CreateTerrenoRequest(
                //     nombre = "Terreno $cultivo",
                //     latitud = lat,
                //     longitud = lon,
                //     cultivo = cultivo
                // )
                // val response = RetrofitClient.gddService.createTerreno(request)
                // if (response.isSuccessful) {
                //     _state.value = _state.value.copy(isLoading = false)
                //     launch(Dispatchers.Main) { onSuccess() }
                // } else {
                //     _state.value = _state.value.copy(
                //         isLoading = false,
                //         error = "Error del servidor al registrar terreno"
                //     )
                // }

                // Simulación de éxito del registro
                kotlinx.coroutines.delay(1000)
                _state.value = _state.value.copy(isLoading = false)
                launch(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
                Log.e("NUEVO_TERRENO", "Error al registrar terreno: ${e.message}")
            }
        }
    }
}
