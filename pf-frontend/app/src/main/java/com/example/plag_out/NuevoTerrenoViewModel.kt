package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NuevoTerrenoUIState(
    val nombre: String = "",
    val areaHectareas: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class NuevoTerrenoViewModel(
    context: Context,
    repository: TerrenoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(NuevoTerrenoUIState())
    val state: StateFlow<NuevoTerrenoUIState> = _state.asStateFlow()

    val terrenoRepository = repository
    val context = context

    fun actualizarNombre(nombre: String) {
        _state.value = _state.value.copy(nombre = nombre, error = null)
    }

    fun actualizarArea(area: String) {
        _state.value = _state.value.copy(areaHectareas = area, error = null)
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

    fun resetState() {
        _state.value = NuevoTerrenoUIState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun registrarTerreno(onSuccess: () -> Unit) {
        val currentState = _state.value
        val nombre = currentState.nombre.trim()
        val areaStr = currentState.areaHectareas.trim()
        val lat = currentState.latitud
        val lon = currentState.longitud

        if (nombre.isEmpty()) {
            _state.value = _state.value.copy(error = "El nombre del terreno no puede estar vacío")
            return
        }

        val area = areaStr.toDoubleOrNull()
        if (area == null || area <= 0.0) {
            _state.value = _state.value.copy(error = "Las hectáreas deben ser un número decimal mayor a 0")
            return
        }

        if (lat == null || lon == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar la ubicación")
            return
        }

        if (lat !in -55.0..-21.8 || lon !in -73.6..-53.6) {
            _state.value = _state.value.copy(error = "Ubicación fuera de la República Argentina")
            return
        }

        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val request = CreateTerrenoRequest(
                    nombre = nombre,
                    latitud = lat,
                    longitud = lon,
                    area_hectareas = area
                )
                
                val response = withContext(Dispatchers.IO) {
                    gddService.createTerreno(request)
                }
                
                if (response.isSuccessful) {
                    val createResponse = response.body()
                    if (createResponse != null) {
                        val terreno = TerrenoResponse(
                            terreno_id = createResponse.terreno_id,
                            terreno_nombre = createResponse.terreno_nombre,
                            terreno_latitud = createResponse.terreno_latitud,
                            terreno_longitud = createResponse.terreno_longitud,
                            terreno_area = createResponse.terreno_area
                        )
                        
                        withContext(Dispatchers.IO) {
                            terrenoRepository.guardarTerreno(terreno)
                        }
                        
                        _state.value = _state.value.copy(isLoading = false)
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Error: respuesta vacía del servidor"
                        )
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error del servidor al registrar terreno"
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Error: ${e.message}")
                Log.e("NUEVO_TERRENO", "Error al registrar terreno: ${e.message}")
            }
        }
    }
}

class NuevoTerrenoViewModelFactory(
    private val context: Context,
    private val repository: TerrenoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NuevoTerrenoViewModel(context, repository, gddService) as T
    }
}