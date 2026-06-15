package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NuevoTerrenoUIState(
    val cultivoSeleccionado: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NuevoTerrenoViewModel(context: Context, repository: TerrenoRepository) : ViewModel() {

    private val _state = MutableStateFlow(NuevoTerrenoUIState())
    val state: StateFlow<NuevoTerrenoUIState> = _state.asStateFlow()

    val terrenoRepository = repository
    val context = context

    fun seleccionarCultivo(cultivo: String?) {
        _state.value = _state.value.copy(cultivoSeleccionado = cultivo)
    }

    fun actualizarUbicacion(latitud: Double?, longitud: Double?) {
        _state.value = _state.value.copy(
            latitud = latitud,
            longitud = longitud,
            error = null
        )
    }

    fun limpiarError() {
        _state.value = _state.value.copy(error = null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun registrarTerreno(onSuccess: () -> Unit) {
        val currentState = _state.value
        val cultivo = currentState.cultivoSeleccionado
        val lat = currentState.latitud
        val lon = currentState.longitud

        if (cultivo == null || lat == null || lon == null) {
            _state.value = _state.value.copy(error = "Datos de terreno incompletos")
            return
        }

        if (lat !in -55.0..-21.8 || lon !in -73.6..-53.6) {
            _state.value = _state.value.copy(error = "Ubicación fuera de la República Argentina")
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // TODO: Agregar en GDDService cuando el endpoint esté disponible:
                //   @POST("/terrenos")
                //   suspend fun createTerreno(@Body data: CreateTerrenoRequest): Response<TerrenoResponse>
                //
                // val request = CreateTerrenoRequest(
                //     nombre = "Terreno $cultivo",
                //     latitud = lat,
                //     longitud = lon,
                //     cultivo = cultivo
                // )
                // val response = withContext(Dispatchers.IO) {
                //     RetrofitClient.gddService.createTerreno(request)
                // }
                // if (response.isSuccessful) {
                //     val nuevoTerreno = response.body()!!
                //     terrenoRepository.guardarTerrenos(listOf(nuevoTerreno))
                //     _state.value = _state.value.copy(isLoading = false)
                //     withContext(Dispatchers.Main) { onSuccess() }
                // } else {
                //     _state.value = _state.value.copy(
                //         isLoading = false,
                //         error = "Error del servidor al registrar terreno"
                //     )
                // }

                // Simulación hasta que el endpoint esté implementado
                kotlinx.coroutines.delay(1000)
                _state.value = _state.value.copy(isLoading = false)
                withContext(Dispatchers.Main) { onSuccess() }

            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message}")
                Log.e("NUEVO_TERRENO", "Error al registrar terreno: ${e.message}")
            }
        }
    }
}

class NuevoTerrenoViewModelFactory(
    private val context: Context,
    private val repository: TerrenoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NuevoTerrenoViewModel(context, repository) as T
    }
}