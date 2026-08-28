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
    val plagasSeleccionadas: List<PlagaResponse> = emptyList(),
    val umbralDeRiesgo: Int = 80, //Inicializado en un valor tipico
    val contextoFijado: Boolean = false,
    val isLoading: Boolean = false,
    val isGuardando: Boolean = false,
    val error: String? = null
) {

    val todasLasPlagasElegidas: Boolean
        get() = plagasDisponibles.isNotEmpty() && plagasSeleccionadas.size == plagasDisponibles.size
}

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

    /** Monitoreos creados en un lote que falló a medias, a la espera del reintento del resto. */
    private val creadosEnIntentosPrevios = mutableListOf<MonitoreoResponse>()

    /**
     * Lo creado hasta ahora en un lote incompleto. Si el usuario se va sin reintentar, esos
     * monitoreos ya existen (backend + Room) y hay que entregarlos igual, o la lista de la pantalla
     * anterior no se entera hasta el próximo refresco.
     */
    fun consumirCreadosPendientes(): List<MonitoreoResponse> {
        val pendientes = creadosEnIntentosPrevios.toList()
        creadosEnIntentosPrevios.clear()
        return pendientes
    }

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

    /** Sin cultivo elegido ([cultivoId] nulo) no hay plagas para ofrecer. */
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


    fun precargarContexto(plantacionId: Int) {
        viewModelScope.launch {
            val plantacion = withContext(Dispatchers.IO) {
                plantacionRepository.obtenerPlantaciones().find { it.plantacion_id == plantacionId }
            }
            if (plantacion == null) {
                _state.value = _state.value.copy(error = "No se encontró el cultivo")
                return@launch
            }

            val terreno = withContext(Dispatchers.IO) {
                terrenoRepository.obtenerTerrenos().find { it.terreno_id == plantacion.terreno_id }
            }
            _state.value = _state.value.copy(
                terrenoSeleccionado = terreno,
                plantacionSeleccionada = plantacion,
                plantaciones = listOf(plantacion),
                contextoFijado = true,
                error = null
            )
        }
    }


    fun seleccionarPlantacion(plantacion: PlantacionesResponse){
        val terreno = _state.value.terrenoSeleccionado ?: return
        if (plantacion.terreno_id != terreno.terreno_id) return
        if (_state.value.plantacionSeleccionada?.plantacion_id == plantacion.plantacion_id) return
        _state.value = _state.value.copy(
            plantacionSeleccionada = plantacion,
            plagasSeleccionadas = emptyList(),
            error = null
        )
    }

    fun seleccionarTerreno(terreno: TerrenoResponse){
        if (_state.value.terrenoSeleccionado?.terreno_id == terreno.terreno_id) return
        _state.value = _state.value.copy(
            terrenoSeleccionado = terreno,
            plantacionSeleccionada = null,
            plagasSeleccionadas = emptyList(),
            plantaciones = emptyList(),
            error = null
        )
    }

    fun alternarPlaga(plaga: PlagaResponse){
        if (_state.value.plagasDisponibles.none { it.id == plaga.id }) return
        val actuales = _state.value.plagasSeleccionadas
        val nuevas =
            if (actuales.any { it.id == plaga.id }) actuales.filter { it.id != plaga.id }
            else actuales + plaga
        _state.value = _state.value.copy(
            plagasSeleccionadas = nuevas,
            error = null
        )
    }

    fun alternarTodasLasPlagas(){
        _state.value = _state.value.copy(
            plagasSeleccionadas = if (_state.value.todasLasPlagasElegidas) emptyList()
                                  else _state.value.plagasDisponibles,
            error = null
        )
    }

    fun actualizarUmbralDeRiesgo(nuevoValor: Int) {
        _state.value = _state.value.copy(
            umbralDeRiesgo = nuevoValor
        )
    }

    /**
     * Un POST por plaga elegida: el backend crea los monitoreos de a uno. Se hacen en secuencia
     * para que un error no deje a medias un lote en paralelo, y las que fallan quedan seleccionadas
     * para reintentar solo esas.
     */
    fun guardarMonitoreo(onSuccess: (List<MonitoreoResponse>) -> Unit) {
        val currentState = _state.value
        val plagas = currentState.plagasSeleccionadas
        val terreno = currentState.terrenoSeleccionado
        val plantacion = currentState.plantacionSeleccionada
        val umbralDeRiesgo = currentState.umbralDeRiesgo

        val terrenoId = terreno?.terreno_id ?: plantacion?.terreno_id

        if (terrenoId == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar un terreno")
            return
        }

        if (plantacion == null) {
            _state.value = _state.value.copy(error = "Debe seleccionar una plantacion")
            return
        }

        if (plagas.isEmpty()) {
            _state.value = _state.value.copy(error = "Debe seleccionar una plaga")
            return
        }

        if (plagas.any { elegida -> currentState.plagasDisponibles.none { it.id == elegida.id } }) {
            _state.value = _state.value.copy(
                error = "La plaga seleccionada no afecta al cultivo de este cultivo"
            )
            return
        }

        _state.value = _state.value.copy(isGuardando = true, error = null)

        viewModelScope.launch {
            val creados = mutableListOf<MonitoreoResponse>()
            val fallidas = mutableListOf<PlagaResponse>()
            var primerError: String? = null

            for (plaga in plagas) {
                val request = MonitoreoRequest(
                    terreno_id = terrenoId,
                    plantacion_id = plantacion.plantacion_id,
                    plaga_id = plaga.id,
                    umbral_riesgo = umbralDeRiesgo
                )

                try {
                    val response = withContext(Dispatchers.IO) {
                        gddService.createMonitoreo(request)
                    }
                    val monitoreoResponse = response.body()

                    if (response.isSuccessful && monitoreoResponse != null) {
                        withContext(Dispatchers.IO) {
                            monitoreoRepository.guardarMonitoreo(monitoreoResponse)
                        }
                        creados += monitoreoResponse
                    } else {
                        fallidas += plaga
                        if (primerError == null) {
                            primerError = if (response.isSuccessful) "Error: Respuesta vacía del servidor"
                                          else response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                                              ?: "Error del servidor al crear el monitoreo"
                        }
                    }
                } catch (e: Exception) {
                    fallidas += plaga
                    if (primerError == null) primerError = "Error al guardar: ${e.message}"
                    Log.e("CREAR_MONITOREO", "Error al crear monitoreo de ${plaga.nombre}: ${e.message}")
                }
            }

            // Los de intentos anteriores ya están en Room; se suman acá para que la lista en memoria
            // de la pantalla anterior también los reciba.
            creadosEnIntentosPrevios += creados

            if (fallidas.isEmpty()) {
                val total = creadosEnIntentosPrevios.toList()
                creadosEnIntentosPrevios.clear()
                _state.value = _state.value.copy(isGuardando = false)
                withContext(Dispatchers.Main) {
                    onSuccess(total)
                }
            } else {
                _state.value = _state.value.copy(
                    isGuardando = false,
                    // Quedan elegidas solo las que faltan: el botón reintenta esas.
                    plagasSeleccionadas = fallidas,
                    error = if (creados.isEmpty()) primerError
                            else "Se crearon ${creados.size} de ${plagas.size} monitoreos. " +
                                "Reintentá con: ${fallidas.joinToString { it.nombre }}"
                )
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
