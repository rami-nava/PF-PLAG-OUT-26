package com.example.plag_out

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CrearReporteUIState(
    val terrenos: List<TerrenoResponse> = emptyList(),
    val terrenoSeleccionado: TerrenoResponse? = null,
    val plantaciones: List<PlantacionesResponse> = emptyList(),
    val plantacionSeleccionada: PlantacionesResponse? = null,
    val plagas: List<PlagaResponse> = emptyList(),
    val plagaSeleccionada: PlagaResponse? = null,
    val nivelSeveridad: String = "Bajo",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val observaciones: String = "",
    val isLoadingInicial: Boolean = false,
    val isGuardando: Boolean = false,
    val error: String? = null,
    /** Disponible tras POST exitoso; se usa para navegar a VerReporteScreen. */
    val reporteNavPayload: ReporteNavPayload? = null
)

class CrearReporteViewModel(
    private val context: Context,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModel() {

    private val _state = MutableStateFlow(CrearReporteUIState())
    val state: StateFlow<CrearReporteUIState> = _state.asStateFlow()

    private var todasLasPlantaciones: List<PlantacionesResponse> = emptyList()

    init {
        cargarDatosIniciales()
    }

    fun cargarDatosIniciales() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(isLoadingInicial = true, error = null)
            }
            try {
                coroutineScope {
                    val deferredTerrenos = async { gddService.getTerrenos() }
                    val deferredPlantaciones = async { gddService.getPlantaciones() }
                    val deferredPlagas = async { gddService.getPlagas() }

                    val resTerrenos = deferredTerrenos.await()
                    val resPlantaciones = deferredPlantaciones.await()
                    val resPlagas = deferredPlagas.await()

                    val listaTerrenos = if (resTerrenos.isSuccessful) resTerrenos.body() ?: emptyList() else emptyList()
                    todasLasPlantaciones = if (resPlantaciones.isSuccessful) resPlantaciones.body() ?: emptyList() else emptyList()
                    val listaPlagas = if (resPlagas.isSuccessful) resPlagas.body() ?: emptyList() else emptyList()

                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            terrenos = listaTerrenos,
                            plagas = listaPlagas,
                            isLoadingInicial = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("CREAR_REPORTE", "Error al cargar datos iniciales: ${e.message}")
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        isLoadingInicial = false,
                        error = "Error de red al cargar opciones del formulario"
                    )
                }
            }
        }
    }

    fun seleccionarTerreno(terreno: TerrenoResponse) {
        val plantacionesFiltradas = todasLasPlantaciones.filter { it.terreno_id == terreno.terreno_id && it.activa }
        _state.value = _state.value.copy(
            terrenoSeleccionado = terreno,
            plantaciones = plantacionesFiltradas,
            plantacionSeleccionada = null,
            latitud = terreno.terreno_latitud.toDouble(),
            longitud = terreno.terreno_longitud.toDouble(),
            error = null
        )
    }

    fun seleccionarPlantacion(plantacion: PlantacionesResponse) {
        _state.value = _state.value.copy(
            plantacionSeleccionada = plantacion,
            error = null
        )
    }

    fun seleccionarPlaga(plaga: PlagaResponse) {
        _state.value = _state.value.copy(plagaSeleccionada = plaga, error = null)
    }

    fun actualizarNivelSeveridad(nivel: String) {
        _state.value = _state.value.copy(nivelSeveridad = nivel, error = null)
    }

    fun actualizarUbicacion(latitud: Double?, longitud: Double?) {
        _state.value = _state.value.copy(
            latitud = latitud,
            longitud = longitud,
            error = null
        )
    }

    fun actualizarObservaciones(obs: String) {
        _state.value = _state.value.copy(observaciones = obs)
    }

    fun actualizarTimestamp() {
        _state.value = _state.value.copy(timestampMs = System.currentTimeMillis())
    }

    fun limpiarError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetState() {
        _state.value = CrearReporteUIState(timestampMs = System.currentTimeMillis())
    }

    fun guardarReporte(onSuccess: () -> Unit) {
        val currentState = _state.value
        val terreno = currentState.terrenoSeleccionado
        val plantacion = currentState.plantacionSeleccionada
        val plaga = currentState.plagaSeleccionada

        if (terreno == null) {
            _state.value = _state.value.copy(error = "Debes seleccionar un terreno")
            return
        }

        if (plantacion == null) {
            _state.value = _state.value.copy(error = "Debes seleccionar una plantación")
            return
        }

        if (plaga == null) {
            _state.value = _state.value.copy(error = "El tipo de plaga es obligatorio")
            return
        }

        _state.value = _state.value.copy(isGuardando = true, error = null)

        viewModelScope.launch {
            try {
                val currentMs = System.currentTimeMillis()
                val request = CreateReporteRequest(
                    terreno_id = terreno.terreno_id,
                    plantacion_id = plantacion.plantacion_id,
                    plaga_id = plaga.id,
                    nivel_severidad = currentState.nivelSeveridad,
                    latitud = currentState.latitud,
                    longitud = currentState.longitud,
                    timestamp_ms = currentMs
                )

                val response = withContext(Dispatchers.IO) {
                    val res = gddService.createReporte(request)
                    if (!res.isSuccessful && res.code() == 404) {
                        gddService.createReporteApi(request)
                    } else {
                        res
                    }
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        _state.value = _state.value.copy(
                            isGuardando = false,
                            error = "El servidor no devolvió datos del reporte creado."
                        )
                        return@launch
                    }
                    // plaga_nombre capturado de la UI antes del POST —
                    // ReporteResponse no lo incluye en su respuesta.
                    val navPayload = ReporteNavPayload(
                        id              = body.id,
                        plaga_nombre    = currentState.plagaSeleccionada?.nombre ?: "",
                        nivel_severidad = currentState.nivelSeveridad,
                        latitud         = currentState.latitud,
                        longitud        = currentState.longitud,
                        timestamp_ms    = currentMs
                    )
                    _state.value = _state.value.copy(
                        isGuardando = false,
                        error = null,
                        reporteNavPayload = navPayload
                    )
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    val errorBodyStr = response.errorBody()?.string()
                    val parsedDetail = try {
                        if (!errorBodyStr.isNullOrEmpty() && errorBodyStr.contains("detail")) {
                            val jsonObj = org.json.JSONObject(errorBodyStr)
                            jsonObj.optString("detail", errorBodyStr)
                        } else errorBodyStr
                    } catch (e: Exception) {
                        errorBodyStr
                    }

                    val userFriendlyError = when {
                        parsedDetail?.contains("report_create_failed") == true -> "No se pudo registrar el reporte. Por favor, verificá los datos e intentá nuevamente."
                        !parsedDetail.isNullOrEmpty() -> parsedDetail
                        else -> "Error del servidor (${response.code()}) al guardar el reporte"
                    }

                    _state.value = _state.value.copy(
                        isGuardando = false,
                        error = userFriendlyError
                    )
                }
            } catch (e: Exception) {
                Log.e("CREAR_REPORTE", "Error al guardar reporte: ${e.message}")
                _state.value = _state.value.copy(
                    isGuardando = false,
                    error = "Error de red al enviar reporte. Revisá tu conexión."
                )
            }
        }
    }
}

class CrearReporteViewModelFactory(
    private val context: Context,
    private val gddService: GDDService = RetrofitClient.gddService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CrearReporteViewModel(context, gddService) as T
    }
}
