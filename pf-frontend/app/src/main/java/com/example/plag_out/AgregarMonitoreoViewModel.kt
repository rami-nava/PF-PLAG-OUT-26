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
import com.example.plag_out.Service.GDDService
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
    val plagasDisponibles: List<PlagaResponse> = emptyList(),
    val plagaSeleccionada: PlagaResponse? = null,
    val umbralDeRiesgo: Int = 80, //Inicializado en un valor tipico
    val isLoading: Boolean = false,
    val isGuardando: Boolean = false,
    val error: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class AgregarMonitoreoViewModel(
    val context: Context,
    val monitoreoRepository: MonitoreoRepository,
    val plantacionRepository: PlantacionRepository,
    val terrenoRepository: TerrenoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(AgregarMonitoreoUIState())
    val state: StateFlow<AgregarMonitoreoUIState> = _state.asStateFlow()

    fun cargarPlagas() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    gddService.getPlagas()
                }
                if (response.isSuccessful) {
                    val plagas = response.body() ?: emptyList()
                    _state.value = _state.value.copy(
                        plagas = plagas,
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Error al cargar plagas"
                    )
                }
            } catch (e: Exception) {
                // Incluye los errores de parseo: sin esto, un cambio de shape en la respuesta se ve
                // como "no hay plagas" y no deja rastro.
                Log.e("PLAGAS", "Error al cargar plagas", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error de red"
                )
            }
        }
    }

    /** Sin plantación elegida ([cultivoId] nulo) no hay plagas para ofrecer. */
    fun filtrarPlagas(cultivoId: Int?){
        val plagasParaCultivo = if (cultivoId == null) emptyList() else {
            _state.value.plagas
                .filter { p -> p.cultivos_afectados.orEmpty().contains(cultivoId) }
                .toList()
        }
        _state.value = _state.value.copy(plagasDisponibles = plagasParaCultivo)
    }

    suspend fun cargarPlantaciones(terrenoId: Int){
        val plantaciones = plantacionRepository.obtenerPlantaciones()
            .filter { p -> p.terreno_id == terrenoId && p.activa }
            .toList()
        _state.value = _state.value.copy(plantaciones = plantaciones)
    }

    suspend fun cargarTerrenos(){
        _state.value = _state.value.copy(
            terrenos = terrenoRepository.obtenerTerrenos()
        )
    }


    fun seleccionarPlantacion(plantacion: PlantacionesResponse){
        val terreno = _state.value.terrenoSeleccionado ?: return
        if (plantacion.terreno_id != terreno.terreno_id) return
        if (_state.value.plantacionSeleccionada?.plantacion_id == plantacion.plantacion_id) return
        _state.value = _state.value.copy(
            plantacionSeleccionada = plantacion,
            plagaSeleccionada = null,
            error = null
        )
    }

    fun seleccionarTerreno(terreno: TerrenoResponse){
        if (_state.value.terrenoSeleccionado?.terreno_id == terreno.terreno_id) return
        _state.value = _state.value.copy(
            terrenoSeleccionado = terreno,
            plantacionSeleccionada = null,
            plagaSeleccionada = null,
            plantaciones = emptyList(),
            error = null
        )
    }

    fun seleccionarPlaga(plaga: PlagaResponse){
        if (_state.value.plagasDisponibles.none { it.id == plaga.id }) return
        _state.value = _state.value.copy(
            plagaSeleccionada = plaga,
            error = null
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

        if (terreno == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar un terreno")
            return
        }

        if (plantacion == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar una plantacion")
            return
        }

        if (plaga == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar una plaga")
            return
        }

        if (currentState.plagasDisponibles.none { it.id == plaga.id }) {
            _state.value = _state.value.copy(
                error = "La plaga seleccionada no afecta al cultivo de esta plantación"
            )
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
                    gddService.createMonitoreo(request)
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
}

class AgregarMonitoreoViewModelFactory(
    private val context: Context,
    private val monitoreoRepository: MonitoreoRepository,
    private val plantacionRepository: PlantacionRepository,
    private val terrenoRepository: TerrenoRepository,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {

    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AgregarMonitoreoViewModel(
            context, monitoreoRepository, plantacionRepository, terrenoRepository, gddService
        ) as T
    }
}
