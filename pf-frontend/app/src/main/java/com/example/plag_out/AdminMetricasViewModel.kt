package com.example.plag_out

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plag_out.Service.GDDService
import com.example.plag_out.Service.RetrofitClient
import com.example.plag_out.Service.todosLosMonitoreos
import com.example.plag_out.Service.todosLosTerrenos
import com.example.plag_out.Service.todasLasPlantaciones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminMetricasViewModel(
    private val gddService: GDDService = RetrofitClient.gddService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(AdminMetricasUIState())
    val state: StateFlow<AdminMetricasUIState> = _state.asStateFlow()

    private var cargaJob: Job? = null

    // true: tableros de todos los productores (admin, GET /admin/metricas).
    // false: los del propio productor, calculados en la app con sus monitoreos.
    private var global = false

    fun cargar(forzar: Boolean = false, global: Boolean = this.global) {
        if (global != this.global) {
            // Otra fuente de datos: lo cargado antes no sirve.
            cargaJob?.cancel()
            this.global = global
            _state.value = AdminMetricasUIState(pestañaActual = _state.value.pestañaActual)
        }
        if (!forzar && (_state.value.campañas.isNotEmpty() || _state.value.isLoading)) return

        cargaJob?.cancel()
        cargaJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = !forzar && _state.value.campañas.isEmpty(),
                isRefreshing = forzar,
                error = null
            )

            if (global) {
                cargarGlobales()
                return@launch
            }

            try {
                // 1. Intento de carga rápida y desacoplada mediante endpoint dedicado de dashboards
                val respDashboard = try {
                    withContext(ioDispatcher) { gddService.getDashboardMetricas() }
                } catch (e: Throwable) {
                    null
                }

                if (respDashboard != null && respDashboard.isSuccessful && respDashboard.body() != null) {
                    val data = respDashboard.body()!!
                    val campanasDomain = data.campanas.map { it.toDomain() }
                    val seleccionadaActual = _state.value.campañaSeleccionada?.nombre
                    val nuevaSeleccionada = campanasDomain.firstOrNull { it.nombre == seleccionadaActual }
                        ?: campanasDomain.firstOrNull()

                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        campañas = campanasDomain,
                        campañaSeleccionada = nuevaSeleccionada,
                        totalHectareasHistoricas = data.total_hectareas_historicas,
                        totalMonitoreosHistoricos = data.total_monitoreos_historicos,
                        plagaHistoricaLider = data.plaga_historica_lider,
                        semaforoHectareas = data.semaforo_hectareas?.toDomain(),
                        plagasPorCultivo = data.plagas_por_cultivo.map { it.toDomain() },
                        lotesCriticos = data.lotes_criticos.map { it.toDomain() },
                        ventanasControl = data.ventanas_control.map { it.toDomain() },
                        eficaciaTratamientos = data.eficacia_tratamientos?.toDomain(),
                        error = null
                    )
                    return@launch
                }

                // 2. Fallback: cálculo local en cliente para modo offline o compatibilidad
                val (respMonitoreos, respTerrenos, respPlantaciones) = withContext(ioDispatcher) {
                    val deferredMonitoreos = async { gddService.todosLosMonitoreos() }
                    val deferredTerrenos = async { gddService.todosLosTerrenos() }
                    val deferredPlantaciones = async { gddService.todasLasPlantaciones() }
                    Triple(deferredMonitoreos.await(), deferredTerrenos.await(), deferredPlantaciones.await())
                }

                if (!respMonitoreos.isSuccessful) {
                    val fallo = falloDeRespuesta(respMonitoreos)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = fallo.mensaje
                    )
                    return@launch
                }

                val monitoreos = respMonitoreos.body().orEmpty()
                val terrenos = if (respTerrenos.isSuccessful) respTerrenos.body().orEmpty() else emptyList()
                val plantaciones = if (respPlantaciones.isSuccessful) respPlantaciones.body().orEmpty() else emptyList()

                // 1. Perspectiva Histórica / Campañas
                val agrupadas = agruparPorCampaña(monitoreos, terrenos)
                val seleccionadaActual = _state.value.campañaSeleccionada?.nombre
                val nuevaSeleccionada = agrupadas.firstOrNull { it.nombre == seleccionadaActual }
                    ?: agrupadas.firstOrNull()

                val terrenosMap = terrenos.associateBy { it.terreno_id }
                val totalHectareas = monitoreos
                    .map { it.terreno_id }
                    .distinct()
                    .mapNotNull { terrenosMap[it]?.terreno_area }
                    .sum()

                val plagaLider = monitoreos
                    .groupBy { it.plaga_nombre.trim() }
                    .maxByOrNull { it.value.size }
                    ?.key

                // 2. Perspectiva Productor (Operativa & Campo)
                val semaforoHa = calcularSemaforoHectareas(monitoreos, terrenos)
                val plagasCultivo = calcularPlagasPorCultivo(monitoreos, terrenos, plantaciones)
                val rankingCriticos = calcularRankingLotesCriticos(monitoreos, terrenos, plantaciones)

                // 3. Perspectiva Agrónomo (Fenología, GDD & Eficacia)
                val ventanas = calcularVentanasControlAgronomo(monitoreos)
                val eficacia = calcularEficaciaTratamientos(monitoreos)

                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    campañas = agrupadas,
                    campañaSeleccionada = nuevaSeleccionada,
                    totalHectareasHistoricas = totalHectareas,
                    totalMonitoreosHistoricos = monitoreos.size,
                    plagaHistoricaLider = plagaLider,
                    semaforoHectareas = semaforoHa,
                    plagasPorCultivo = plagasCultivo,
                    lotesCriticos = rankingCriticos,
                    ventanasControl = ventanas,
                    eficaciaTratamientos = eficacia,
                    error = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ADMIN_METRICAS", "Error cargando métricas", e)
                val fallo = falloDeExcepcion(e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = fallo.mensaje
                )
            }
        }
    }

    private suspend fun cargarGlobales() {
        try {
            val respuesta = withContext(ioDispatcher) { gddService.adminGetMetricas() }
            val metricas = respuesta.body()
            _state.value = if (respuesta.isSuccessful && metricas != null) {
                _state.value.conMetricasGlobales(metricas).copy(isLoading = false, isRefreshing = false, error = null)
            } else {
                _state.value.copy(isLoading = false, isRefreshing = false, error = falloDeRespuesta(respuesta).mensaje)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ADMIN_METRICAS", "Error cargando métricas globales", e)
            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = false,
                error = falloDeExcepcion(e).mensaje
            )
        }
    }

    fun refrescar() = cargar(forzar = true)

    fun cambiarPestaña(pestaña: PestañaDashboard) {
        _state.value = _state.value.copy(pestañaActual = pestaña)
    }

    fun seleccionarCampaña(nombreCampaña: String) {
        val encontrada = _state.value.campañas.firstOrNull { it.nombre == nombreCampaña }
        if (encontrada != null) {
            _state.value = _state.value.copy(campañaSeleccionada = encontrada)
        }
    }

    fun descartarError() {
        _state.value = _state.value.copy(error = null)
    }

    fun limpiar() {
        cargaJob?.cancel()
        global = false
        _state.value = AdminMetricasUIState()
    }
}

class AdminMetricasViewModelFactory(
    private val gddService: GDDService = RetrofitClient.gddService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AdminMetricasViewModel(gddService, ioDispatcher) as T
}
