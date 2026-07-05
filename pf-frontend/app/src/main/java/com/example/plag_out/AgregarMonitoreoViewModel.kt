package com.example.plag_out

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
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

data class AgregarMonitoreoUIState(
    val terrenos: List<TerrenoResponse> = emptyList(),
    val terrenoSeleccionado: TerrenoResponse? = null,
    val plantacionSeleccionada: PlantacionesResponse? = null,
    val plantaciones: List<PlantacionesResponse> = emptyList(),
    val plagas: List<PlagaResponse> = emptyList(),
    val plagaSeleccionada: PlagaResponse? = null,
    val umbralDeRiesgo: Int = 0,
    val isLoading: Boolean = false,
    val isGuardando: Boolean = false,
    val error: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class AgregarMonitoreoViewModel(
    val context: Context,
    val monitoreoRepository: MonitoreoRepository,
    val plantacionRepository: PlantacionRepository,
    val terrenoRepository: TerrenoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AgregarMonitoreoUIState())
    val state: StateFlow<AgregarMonitoreoUIState> = _state.asStateFlow()

    fun cargarPlagas() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.gddService.getPlagas()
                }
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        plagas = response.body() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Error al cargar plagas"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error de red"
                )
            }
        }
    }

    suspend fun cargarPlantaciones(terrenoId: Int){
        _state.value = _state.value.copy(
            plantaciones = plantacionRepository.obtenerPlantaciones().filter { p -> p.terreno_id == terrenoId && p.activa }.toList()
        )
    }

    suspend fun cargarTerrenos(){
        _state.value = _state.value.copy(
            terrenos = terrenoRepository.obtenerTerrenos()
        )
    }

    fun seleccionarPlantacion(plantacion: PlantacionesResponse){
        _state.value = _state.value.copy(
            plantacionSeleccionada = plantacion
        )
    }

    fun seleccionarTerreno(terreno: TerrenoResponse){
        _state.value = _state.value.copy(
            terrenoSeleccionado = terreno
        )
    }

    fun seleccionarPlaga(plaga: PlagaResponse){
        _state.value = _state.value.copy(
            plagaSeleccionada = plaga
        )
    }

    fun actualizarUmbralDeRiesgo(nuevoValor: Int) {
        _state.value = _state.value.copy(
            umbralDeRiesgo = nuevoValor
        )
    }

    fun guardarMonitoreo(onSuccess: () -> Unit) {
        val currentState = _state.value
        val plaga = currentState.plagaSeleccionada
        val terreno = currentState.terrenoSeleccionado
        val plantacion = currentState.plantacionSeleccionada
        val umbralDeRiesgo = currentState.umbralDeRiesgo

        if (plaga == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar una plaga")
            return
        }

        if (plantacion == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar una plantacion")
            return
        }

        if (terreno == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar un terreno")
            return
        }

        _state.value = _state.value.copy(isGuardando = true, error = null)

        viewModelScope.launch {
            try {
                val request = MonitoreoRequest(
                    terreno_id = terreno.terreno_id,
                    plantacion_id = plantacion.plantacion_id,
                    plaga_id = plaga.id,
                    umbral_riesgo = umbralDeRiesgo
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.gddService.createMonitoreo(request)
                }

                if (response.isSuccessful) {
                    val monitoreoResponse = response.body()
                    if (monitoreoResponse != null) {

                        withContext(Dispatchers.IO) {
                            monitoreoRepository.guardarMonitoreo(monitoreoResponse)
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
                    val errorMsg = response.errorBody()?.string() ?: "Error del servidor al crear el monitoreo"
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
                Log.e("CREAR_MONITOREO", "Error al crear monitoreo: ${e.message}")
            }
        }
    }

    fun plagasParaTests(plagaResponse: PlagaResponse){

        _state.value = _state.value.copy(
            plagas = _state.value.plagas + plagaResponse
        )
    }
}

class AgregarMonitoreoViewModelFactory(
    private val context: Context,
    private val monitoreoRepository: MonitoreoRepository,
    private val plantacionRepository: PlantacionRepository,
    private val terrenoRepository: TerrenoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AgregarMonitoreoViewModel(context, monitoreoRepository, plantacionRepository, terrenoRepository) as T
    }
}
