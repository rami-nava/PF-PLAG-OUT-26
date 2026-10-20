package com.example.plag_out

import com.example.plag_out.Service.todasLasPlagas
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import com.example.plag_out.Service.todasLasPlantaciones
import com.example.plag_out.Service.todosLosTerrenos
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
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
    val isLoadingInicial: Boolean = false,
    val isGuardando: Boolean = false,
    val error: String? = null,
    /** Disponible tras POST exitoso; se usa para navegar a VerReporteScreen. */
    val reporteNavPayload: ReporteNavPayload? = null
)

private val ETAPAS_BIOLOGICAS = listOf("HUEVO", "NINFA", "LARVA", "ADULTO")

class CrearReporteViewModel(
    private val context: Context,
    private val gddService: GDDService? = null,
    private val currentOwner: () -> String? = { SupabaseProvider.client.auth.currentUserOrNull()?.id }
) : ViewModel() {

    private val _state = MutableStateFlow(CrearReporteUIState())
    val state: StateFlow<CrearReporteUIState> = _state.asStateFlow()

    private var todasLasPlantaciones: List<PlantacionesResponse> = emptyList()
    private var reportOwner: String? = null

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
                    val service = gddService ?: RetrofitClient.gddService
                    val deferredTerrenos = async { service.todosLosTerrenos() }
                    val deferredPlantaciones = async { service.todasLasPlantaciones() }
                    val deferredPlagas = async { service.todasLasPlagas() }

                    val resTerrenos = deferredTerrenos.await()
                    val resPlantaciones = deferredPlantaciones.await()
                    val resPlagas = deferredPlagas.await()

                    val listaTerrenos = if (resTerrenos.isSuccessful) resTerrenos.body() ?: emptyList() else emptyList()
                    todasLasPlantaciones = if (resPlantaciones.isSuccessful) resPlantaciones.body() ?: emptyList() else emptyList()
                    val listaPlagas = if (resPlagas.isSuccessful) resPlagas.body() ?: emptyList() else emptyList()

                    withContext(Dispatchers.Main) {
                        val currentPlantacion = _state.value.plantacionSeleccionada
                        // Sin cultivo elegido no hay plagas para ofrecer: el paso 3 arranca bloqueado.
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
        if (cultivoId == null) return emptyList()
        return plagas.filter { plaga ->
            plaga.cultivos_afectados.orEmpty().contains(cultivoId)
        }
    }

    /** Etapas que el backend acepta para la plaga; todas si todavía no tiene tipo de desarrollo. */
    fun obtenerEtapasParaPlaga(plaga: PlagaResponse?): List<String> {
        if (plaga == null) return emptyList()
        return plaga.etapas?.takeIf { it.isNotEmpty() } ?: ETAPAS_BIOLOGICAS
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
        // Solo plagas del cultivo elegido.
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

    fun actualizarTimestamp() {
        if (reportOwner != null) return
        _state.value = _state.value.copy(timestampMs = System.currentTimeMillis())
    }

    fun limpiarError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetState() {
        reportOwner = null
        _state.value = CrearReporteUIState(timestampMs = System.currentTimeMillis())
    }

    fun guardarReporte(onSuccess: () -> Unit) {
        val currentState = _state.value
        if (currentState.isGuardando || currentState.reporteNavPayload != null) return
        val terreno = currentState.terrenoSeleccionado
        val plantacion = currentState.plantacionSeleccionada
        val plaga = currentState.plagaSeleccionada

        if (terreno == null) {
            _state.value = _state.value.copy(error = "Debes seleccionar un terreno")
            return
        }

        if (plantacion == null) {
            _state.value = _state.value.copy(error = "Debes seleccionar un cultivo")
            return
        }

        if (plaga == null) {
            _state.value = _state.value.copy(error = "El tipo de plaga es obligatorio")
            return
        }

        val owner = currentOwner()
        if (owner == null || (reportOwner != null && reportOwner != owner)) {
            _state.value = _state.value.copy(error = "La sesión cambió. Iniciá un nuevo reporte con tu cuenta actual.")
            return
        }
        reportOwner = owner
        _state.value = _state.value.copy(isGuardando = true, error = null)

        viewModelScope.launch {
            try {
                val etapaStr = currentState.etapaBiologica.ifBlank { null }
                val request = CreateReporteRequest(
                    plantacion_id = plantacion.plantacion_id,
                    plaga_id = plaga.id,
                    nivel_severidad = currentState.nivelSeveridad,
                    timestamp_ms = currentState.timestampMs,
                    etapa_biologica = etapaStr
                )

                val response = withContext(Dispatchers.IO) {
                    (gddService ?: RetrofitClient.forPresenceRetry(owner)).createReporte(request)
                }
                if (currentOwner() != owner) {
                    _state.value = _state.value.copy(isGuardando = false,
                        error = "La sesión cambió. Iniciá un nuevo reporte con tu cuenta actual.")
                    return@launch
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null || body.id <= 0 || body.plaga_id != plaga.id ||
                        body.plantacion_id != plantacion.plantacion_id) {
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
                        nivel_severidad = body.nivel_severidad,
                        latitud         = body.latitud,
                        longitud        = body.longitud,
                        timestamp_ms    = body.timestamp_ms
                    )
                    _state.value = _state.value.copy(
                        isGuardando = false,
                        error = null,
                        reporteNavPayload = navPayload
                    )
                    // 200: el backend reutilizó un reporte equivalente reciente en vez de crear otro
                    Avisos.exito(if (response.code() == 201) "Reporte enviado." else "Ya existía un reporte similar.")
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    val fallo = errorDeRespuesta(response, ERRORES_CREAR_REPORTE, ERRORES_CREAR_REPORTE_CODIGO)
                    Log.e("CREAR_REPORTE", "Respuesta de error de API code=${response.code()}: ${fallo.detalle}")
                    _state.value = _state.value.copy(
                        isGuardando = false,
                        error = fallo.enFormularioOAviso()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CREAR_REPORTE", "Error al guardar reporte: ${e.message}")
                _state.value = _state.value.copy(
                    isGuardando = false,
                    error = errorDeExcepcion(e).mensaje
                )
            }
        }
    }
}

private val ERRORES_CREAR_REPORTE = ERRORES_REPORTE + mapOf(
    "report_create_failed" to "No se pudo registrar el reporte. Verificá los datos e intentá de nuevo."
)
private val ERRORES_CREAR_REPORTE_CODIGO = mapOf(500 to "No se pudo enviar el reporte. Intentá de nuevo.")

class CrearReporteViewModelFactory(
    private val context: Context,
    private val gddService: GDDService? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CrearReporteViewModel(context, gddService) as T
    }
}
