package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

data class AgregarPlantacionUIState(
    val cultivos: List<CultivoResponse> = emptyList(),
    val cultivoSeleccionado: CultivoResponse? = null,
    val fechaSiembra: String = "", // YYYY-MM-DD
    val activa: Boolean = true,
    val isLoading: Boolean = false,
    val isGuardando: Boolean = false,
    val error: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class AgregarPlantacionViewModel(
    val context: Context,
    val plantacionRepository: PlantacionRepository,
    val terrenoRepository: TerrenoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AgregarPlantacionUIState())
    val state: StateFlow<AgregarPlantacionUIState> = _state.asStateFlow()

    init {
        cargarCultivos()
    }

    fun actualizarCultivo(cultivo: CultivoResponse) {
        _state.value = _state.value.copy(cultivoSeleccionado = cultivo, error = null)
    }

    fun actualizarActiva(activa: Boolean) {
        _state.value = _state.value.copy(activa = activa)
    }

    fun seleccionarFecha(millis: Long) {
        try {
            val date = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()
            _state.value = _state.value.copy(fechaSiembra = date.toString(), error = null)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Error al seleccionar fecha: ${e.message}")
        }
    }

    fun cargarCultivos() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.gddService.getCultivos()
                }
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        cultivos = response.body() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Error al cargar cultivos: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error de red: ${e.message}"
                )
            }
        }
    }

    fun guardarPlantacion(terrenoId: Int, onSuccess: () -> Unit) {
        val currentState = _state.value
        val cultivo = currentState.cultivoSeleccionado
        val fecha = currentState.fechaSiembra

        if (cultivo == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar un cultivo")
            return
        }

        if (fecha.isEmpty()) {
            _state.value = _state.value.copy(error = "Debe seleccionar una fecha de siembra")
            return
        }

        _state.value = _state.value.copy(isGuardando = true, error = null)

        viewModelScope.launch {
            try {
                val request = CreatePlantacionRequest(
                    terreno_id = terrenoId,
                    cultivo_id = cultivo.id,
                    fecha_siembra = fecha,
                    activa = currentState.activa
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.gddService.createPlantacion(request)
                }

                if (response.isSuccessful) {
                    val createResponse = response.body()
                    if (createResponse != null) {
                        // Obtener nombre del terreno para la cache local
                        val terrenos = withContext(Dispatchers.IO) {
                            terrenoRepository.obtenerTerrenos()
                        }
                        val terrenoNombre = terrenos.find { it.terreno_id == terrenoId }?.terreno_nombre ?: "Terreno $terrenoId"

                        val localPlantacion = PlantacionesResponse(
                            plantacion_id = createResponse.id,
                            terreno_id = createResponse.terreno_id,
                            terreno_nombre = terrenoNombre,
                            cultivo_nombre = createResponse.cultivo.nombre,
                            cultivo_nombre_cientifico = createResponse.cultivo.nombre_cientifico,
                            fecha_siembra = createResponse.fecha_siembra,
                            activa = createResponse.activa
                        )

                        withContext(Dispatchers.IO) {
                            plantacionRepository.guardarPlantacion(localPlantacion)
                        }

                        _state.value = _state.value.copy(isGuardando = false)
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        _state.value = _state.value.copy(
                            isGuardando = false,
                            error = "Error: Respuesta vacía del servidor"
                        )
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error del servidor al registrar plantación"
                    _state.value = _state.value.copy(
                        isGuardando = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isGuardando = false,
                    error = "Error al guardar: ${e.message}"
                )
                Log.e("AGREGAR_PLANTACION", "Error al guardar plantación: ${e.message}")
            }
        }
    }
}

class AgregarPlantacionViewModelFactory(
    private val context: Context,
    private val plantacionRepository: PlantacionRepository,
    private val terrenoRepository: TerrenoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AgregarPlantacionViewModel(context, plantacionRepository, terrenoRepository) as T
    }
}
