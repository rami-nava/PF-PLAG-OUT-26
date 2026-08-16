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
    val plagasDisponibles: List<PlagaResponse> = emptyList(),
    val plagaSeleccionada: PlagaResponse? = null,
    val etapaBiologica: String = "",
    val etapasDisponibles: List<String> = emptyList(),
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
                        val currentPlantacion = _state.value.plantacionSeleccionada
                        // Sin plantación elegida no hay plagas para ofrecer: el paso 3 arranca bloqueado.
                        val plagasFiltradas = if (currentPlantacion != null) {
                            filtrarPlagasPorCultivo(listaPlagas, currentPlantacion.cultivo_id, currentPlantacion.cultivo_nombre)
                        } else {
                            emptyList()
                        }

                        _state.value = _state.value.copy(
                            terrenos = listaTerrenos,
                            plagas = listaPlagas,
                            plagasDisponibles = plagasFiltradas,
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

    /** Filtra la lista de plagas según el cultivo seleccionado */
    private fun filtrarPlagasPorCultivo(
        plagas: List<PlagaResponse>,
        cultivoId: Int?,
        cultivoNombre: String?
    ): List<PlagaResponse> {
        if (cultivoId == null && cultivoNombre.isNullOrBlank()) return plagas
        return plagas.filter { plaga ->
            // 1) Si especifica la lista cultivos_afectados
            val coincideId = if (!plaga.cultivos_afectados.isNullOrEmpty() && cultivoId != null) {
                plaga.cultivos_afectados.contains(cultivoId)
            } else true

            // 2) Nombre explícito de cultivo en la plaga (e.g. "Chicharrita del Maíz" vs "Trigo")
            val coincideNombre = if (!cultivoNombre.isNullOrBlank()) {
                val cNorm = cultivoNombre.lowercase()
                val pNorm = plaga.nombre.lowercase()
                if (pNorm.contains("del maíz") || pNorm.contains("del maiz")) {
                    cNorm.contains("maíz") || cNorm.contains("maiz")
                } else if (pNorm.contains("del trigo")) {
                    cNorm.contains("trigo")
                } else true
            } else true

            coincideId && coincideNombre
        }
    }

    /** Obtiene las etapas biológicas / fenológicas aplicables a la plaga seleccionada */
    fun obtenerEtapasParaPlaga(plaga: PlagaResponse?): List<String> {
        if (plaga == null) return emptyList()
        val nombre = plaga.nombre.lowercase()
        val cientifico = plaga.nombre_cientifico.lowercase()
        return when {
            nombre.contains("cogoller") || nombre.contains("oruga") || cientifico.contains("spodoptera") -> {
                listOf("HUEVO", "LARVA", "PUPA", "ADULTO")
            }
            nombre.contains("chicharrita") || cientifico.contains("dalbulus") -> {
                listOf("HUEVO", "NINFA", "ADULTO")
            }
            else -> {
                listOf("HUEVO", "NINFA", "LARVA", "PUPA", "ADULTO")
            }
        }
    }

    fun seleccionarTerreno(terreno: TerrenoResponse) {
        // Volver a tocar el terreno ya elegido no debe descartar lo que sigue.
        if (_state.value.terrenoSeleccionado?.terreno_id == terreno.terreno_id) return

        val plantacionesFiltradas = todasLasPlantaciones.filter { it.terreno_id == terreno.terreno_id && it.activa }
        val autoPlantacion = if (plantacionesFiltradas.size == 1) plantacionesFiltradas.first() else null
        val plagasFiltradas = if (autoPlantacion != null) {
            filtrarPlagasPorCultivo(_state.value.plagas, autoPlantacion.cultivo_id, autoPlantacion.cultivo_nombre)
        } else {
            emptyList()
        }

        _state.value = _state.value.copy(
            terrenoSeleccionado = terreno,
            plantaciones = plantacionesFiltradas,
            plantacionSeleccionada = autoPlantacion,
            plagasDisponibles = plagasFiltradas,
            plagaSeleccionada = null,
            etapaBiologica = "",
            etapasDisponibles = emptyList(),
            latitud = terreno.terreno_latitud.toDouble(),
            longitud = terreno.terreno_longitud.toDouble(),
            error = null
        )
    }

    fun seleccionarPlantacion(plantacion: PlantacionesResponse) {
        // Solo plantaciones del terreno elegido: sin terreno no hay nada que elegir.
        val terreno = _state.value.terrenoSeleccionado ?: return
        if (plantacion.terreno_id != terreno.terreno_id) return
        if (_state.value.plantacionSeleccionada?.plantacion_id == plantacion.plantacion_id) return

        val plagasFiltradas = filtrarPlagasPorCultivo(_state.value.plagas, plantacion.cultivo_id, plantacion.cultivo_nombre)
        _state.value = _state.value.copy(
            plantacionSeleccionada = plantacion,
            plagasDisponibles = plagasFiltradas,
            plagaSeleccionada = null,
            etapaBiologica = "",
            etapasDisponibles = emptyList(),
            error = null
        )
    }

    fun seleccionarPlaga(plaga: PlagaResponse) {
        // Solo plagas del cultivo de la plantación elegida.
        if (_state.value.plagasDisponibles.none { it.id == plaga.id }) return

        val etapas = obtenerEtapasParaPlaga(plaga)
        val etapaInicial = etapas.firstOrNull() ?: ""
        _state.value = _state.value.copy(
            plagaSeleccionada = plaga,
            etapasDisponibles = etapas,
            etapaBiologica = etapaInicial,
            error = null
        )
    }

    fun seleccionarEtapaBiologica(etapa: String) {
        _state.value = _state.value.copy(etapaBiologica = etapa, error = null)
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
                val etapaStr = currentState.etapaBiologica.ifBlank { null }
                val request = CreateReporteRequest(
                    terreno_id = terreno.terreno_id,
                    plantacion_id = plantacion.plantacion_id,
                    plaga_id = plaga.id,
                    nivel_severidad = currentState.nivelSeveridad,
                    latitud = currentState.latitud,
                    longitud = currentState.longitud,
                    timestamp_ms = currentMs,
                    etapa_biologica = etapaStr
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
                    val plagaNombre = plaga.nombre
                    val navPayload = ReporteNavPayload(
                        id              = body.id,
                        plaga_nombre    = plagaNombre,
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
                    Log.e("CREAR_REPORTE", "Respuesta de error de API code=${response.code()}: $errorBodyStr")
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
