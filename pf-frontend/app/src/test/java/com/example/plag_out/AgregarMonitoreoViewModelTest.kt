package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeMonitoreoDao
import com.example.plag_out.fakes.FakePlantacionDao
import com.example.plag_out.fakes.FakeTerrenoDao
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/**
 *  - `cargarPlantaciones(terrenoId)` filtra por `terreno_id == terrenoId && activa`:
 *    quedan afuera las plantaciones inactivas y las de otro terreno.
 *  - `filtrarPlagas` deja en `plagasDisponibles` solo las plagas del cultivo elegido.
 *  - `guardarMonitoreo` valida en orden terreno → cultivo → plaga, y crea un monitoreo por
 *    cada plaga elegida.
 *  - `precargarContexto` fija terreno y cultivo cuando se entra desdeel cultivo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgregarMonitoreoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var context: Context

    // El backend cruza plaga↔cultivo por id, así que acá alcanza con los ids.
    private val TRIGO = 1
    private val MAIZ = 2

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    /** Si se pasan [plagas], quedan ya cargadas en el estado (como haría la pantalla al abrirse). */
    /** Última instancia creada por [viewModel]: sirve para contar los POST del lote. */
    private lateinit var gddService: FakeGDDService

    private fun viewModel(
        plantaciones: List<PlantacionesResponse> = emptyList(),
        plagas: List<PlagaResponse> = emptyList(),
        terrenos: List<TerrenoResponse> = emptyList()
    ): AgregarMonitoreoViewModel {
        gddService = FakeGDDService().apply {
            getPlagasResult = { Response.success(plagas) }
        }
        val vm = AgregarMonitoreoViewModel(
            context = context,
            monitoreoRepository = MonitoreoRepository(FakeMonitoreoDao()),
            plantacionRepository = PlantacionRepository(FakePlantacionDao(inicial = plantaciones)),
            terrenoRepository = TerrenoRepository(FakeTerrenoDao(inicial = terrenos)),
            gddService = gddService
        )
        if (plagas.isNotEmpty()) {
            vm.cargarPlagas()
            esperarEstado(vm.state) { it.plagas.size == plagas.size }
        }
        return vm
    }

    // ---------- cargarPlantaciones: filtro ----------

    @Test
    fun `cargarPlantaciones excluye inactivas y las de otro terreno`() = runTest {
        val vm = viewModel(
            plantaciones = listOf(
                Fixtures.plantacion(id = 1, terrenoId = 1, activa = true),   // ✓
                Fixtures.plantacion(id = 2, terrenoId = 1, activa = false),  // inactiva
                Fixtures.plantacion(id = 3, terrenoId = 2, activa = true),   // otro terreno
                Fixtures.plantacion(id = 4, terrenoId = 1, activa = true)    // ✓
            )
        )

        vm.cargarPlantaciones(terrenoId = 1)

        assertEquals(listOf(1, 4), vm.state.value.plantaciones.map { it.plantacion_id })
    }

    @Test
    fun `cargarPlantaciones de un terreno sin activas devuelve lista vacia`() = runTest {
        val vm = viewModel(
            plantaciones = listOf(Fixtures.plantacion(id = 1, terrenoId = 1, activa = false))
        )

        vm.cargarPlantaciones(terrenoId = 1)

        assertEquals(emptyList<PlantacionesResponse>(), vm.state.value.plantaciones)
    }

    // ---------- seleccionarPlantacion: depende del terreno ----------

    @Test
    fun `sin terreno no se puede elegir plantacion`() {
        val vm = viewModel()

        vm.seleccionarPlantacion(Fixtures.plantacion(terrenoId = 1))

        assertNull(vm.state.value.plantacionSeleccionada)
    }

    @Test
    fun `no se puede elegir una plantacion de otro terreno`() {
        val vm = viewModel()
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))

        vm.seleccionarPlantacion(Fixtures.plantacion(id = 9, terrenoId = 2))

        assertNull(vm.state.value.plantacionSeleccionada)
    }

    // ---------- filtrarPlagas: filtro por cultivo ----------

    @Test
    fun `sin plantacion no hay plagas para elegir`() {
        val vm = viewModel(plagas = listOf(Fixtures.plaga(id = 1, cultivosAfectados = listOf(TRIGO))))

        vm.filtrarPlagas(cultivoId = null)

        assertEquals(emptyList<Int>(), vm.state.value.plagasDisponibles.map { it.id })
    }

    @Test
    fun `solo se ofrecen las plagas del cultivo de la plantacion`() {
        val vm = viewModel(
            plagas = listOf(
                Fixtures.plaga(id = 1, nombre = "Chicharrita", cultivosAfectados = listOf(TRIGO)),
                Fixtures.plaga(id = 2, nombre = "Cogollero", cultivosAfectados = listOf(MAIZ))
            )
        )

        vm.filtrarPlagas(cultivoId = TRIGO)

        assertEquals(listOf(1), vm.state.value.plagasDisponibles.map { it.id })
    }

    @Test
    fun `una plaga que afecta a varios cultivos se ofrece en todos`() {
        val vm = viewModel(plagas = listOf(Fixtures.plaga(id = 1, cultivosAfectados = listOf(TRIGO, MAIZ))))

        vm.filtrarPlagas(cultivoId = MAIZ)

        assertEquals(listOf(1), vm.state.value.plagasDisponibles.map { it.id })
    }

    @Test
    fun `una plaga sin cultivos afectados no se ofrece`() {
        // El campo es opcional en el backend: si no viene, la plaga no se puede cruzar con ningún
        // cultivo y no debe romper el filtro.
        val vm = viewModel(
            plagas = listOf(
                Fixtures.plaga(id = 1, cultivosAfectados = null),
                Fixtures.plaga(id = 2, cultivosAfectados = listOf(TRIGO))
            )
        )

        vm.filtrarPlagas(cultivoId = TRIGO)

        assertEquals(listOf(2), vm.state.value.plagasDisponibles.map { it.id })
    }

    @Test
    fun `no se puede elegir una plaga de otro cultivo`() {
        val plagaDelMaiz = Fixtures.plaga(id = 2, cultivosAfectados = listOf(MAIZ))
        val vm = viewModel(plagas = listOf(plagaDelMaiz))
        vm.filtrarPlagas(cultivoId = TRIGO)

        vm.alternarPlaga(plagaDelMaiz)

        assertEquals(emptyList<PlagaResponse>(), vm.state.value.plagasSeleccionadas)
    }

    @Test
    fun `cambiar de plantacion descarta las plagas elegidas`() {
        val plagaDelTrigo = Fixtures.plaga(id = 1, cultivosAfectados = listOf(TRIGO))
        val vm = viewModel(plagas = listOf(plagaDelTrigo))
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))
        vm.seleccionarPlantacion(Fixtures.plantacion(id = 1, cultivoId = TRIGO))
        vm.filtrarPlagas(cultivoId = TRIGO)
        vm.alternarPlaga(plagaDelTrigo)

        vm.seleccionarPlantacion(Fixtures.plantacion(id = 2, cultivoId = MAIZ))

        assertEquals(emptyList<PlagaResponse>(), vm.state.value.plagasSeleccionadas)
    }

    @Test
    fun `cambiar de terreno descarta plantacion y plagas`() {
        val plagaDelTrigo = Fixtures.plaga(id = 1, cultivosAfectados = listOf(TRIGO))
        val vm = viewModel(plagas = listOf(plagaDelTrigo))
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))
        vm.seleccionarPlantacion(Fixtures.plantacion(terrenoId = 1, cultivoId = TRIGO))
        vm.filtrarPlagas(cultivoId = TRIGO)
        vm.alternarPlaga(plagaDelTrigo)

        vm.seleccionarTerreno(Fixtures.terreno(id = 2))

        assertNull(vm.state.value.plantacionSeleccionada)
        assertEquals(emptyList<PlagaResponse>(), vm.state.value.plagasSeleccionadas)
    }

    // ---------- selección múltiple de plagas ----------

    @Test
    fun `alternar la misma plaga dos veces la deja sin elegir`() {
        val plaga = Fixtures.plaga(id = 1, cultivosAfectados = listOf(TRIGO))
        val vm = viewModel(plagas = listOf(plaga))
        vm.filtrarPlagas(cultivoId = TRIGO)

        vm.alternarPlaga(plaga)
        vm.alternarPlaga(plaga)

        assertEquals(emptyList<PlagaResponse>(), vm.state.value.plagasSeleccionadas)
    }

    @Test
    fun `alternarTodasLasPlagas elige todas las disponibles y despues las suelta`() {
        val delTrigo = listOf(
            Fixtures.plaga(id = 1, cultivosAfectados = listOf(TRIGO)),
            Fixtures.plaga(id = 2, cultivosAfectados = listOf(TRIGO))
        )
        // La del maíz no está disponible para el trigo: el atajo no la puede arrastrar.
        val vm = viewModel(plagas = delTrigo + Fixtures.plaga(id = 3, cultivosAfectados = listOf(MAIZ)))
        vm.filtrarPlagas(cultivoId = TRIGO)

        vm.alternarTodasLasPlagas()
        assertEquals(listOf(1, 2), vm.state.value.plagasSeleccionadas.map { it.id })
        assertTrue(vm.state.value.todasLasPlagasElegidas)

        vm.alternarTodasLasPlagas()
        assertEquals(emptyList<PlagaResponse>(), vm.state.value.plagasSeleccionadas)
    }

    // ---------- precargarContexto: alta desdeel cultivo ----------

    @Test
    fun `precargarContexto fija el terreno y la plantacion`() = runTest {
        val vm = viewModel(
            plantaciones = listOf(Fixtures.plantacion(id = 7, terrenoId = 3)),
            terrenos = listOf(Fixtures.terreno(id = 3))
        )

        vm.precargarContexto(plantacionId = 7)

        val estado = esperarEstado(vm.state) { it.contextoFijado }
        assertEquals(7, estado.plantacionSeleccionada?.plantacion_id)
        assertEquals(3, estado.terrenoSeleccionado?.terreno_id)
        assertNull(estado.error)
    }

    @Test
    fun `precargarContexto con una plantacion que no esta en cache deja error`() = runTest {
        val vm = viewModel(plantaciones = listOf(Fixtures.plantacion(id = 7)))

        vm.precargarContexto(plantacionId = 99)

        val estado = esperarEstado(vm.state) { it.error != null }
        assertEquals("No se encontróel cultivo", estado.error)
        assertFalse(estado.contextoFijado)
        assertNull(estado.plantacionSeleccionada)
    }

    /** El terreno solo se muestra: el id para crear el monitoreo saledel cultivo. */
    @Test
    fun `precargarContexto sin el terreno en cache igual permite guardar`() = runTest {
        val vm = viewModel(plantaciones = listOf(Fixtures.plantacion(id = 7, terrenoId = 3)))

        vm.precargarContexto(plantacionId = 7)
        esperarEstado(vm.state) { it.contextoFijado }
        vm.guardarMonitoreo(onSuccess = {})

        assertNull(vm.state.value.terrenoSeleccionado)
        assertEquals("Debe seleccionar una plaga", vm.state.value.error)
    }

    // ---------- guardarMonitoreo: orden de validaciones ----------

    @Test
    fun `sin terreno el error es de terreno aunque falten los demas`() {
        val vm = viewModel()

        vm.guardarMonitoreo(onSuccess = {})

        assertEquals("Debe seleccionar un terreno", vm.state.value.error)
    }

    @Test
    fun `con terreno pero sin plantacion el error es de plantacion`() {
        val vm = viewModel()
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))

        vm.guardarMonitoreo(onSuccess = {})

        assertEquals("Debe seleccionar una plantacion", vm.state.value.error)
    }

    // ---------- guardarMonitoreo: un monitoreo por plaga ----------

    /** VM con terreno, cultivo de trigo y [plagas] ya disponibles: listo para guardar. */
    private fun viewModelListoParaGuardar(plagas: List<PlagaResponse>): AgregarMonitoreoViewModel {
        val vm = viewModel(plagas = plagas)
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))
        vm.seleccionarPlantacion(Fixtures.plantacion(id = 1, terrenoId = 1, cultivoId = TRIGO))
        vm.filtrarPlagas(cultivoId = TRIGO)
        return vm
    }

    @Test
    fun `guardarMonitoreo crea un monitoreo por cada plaga elegida`() = runTest {
        val plagas = (1..3).map { Fixtures.plaga(id = it, cultivosAfectados = listOf(TRIGO)) }
        val vm = viewModelListoParaGuardar(plagas)
        var creado = 0
        gddService.createMonitoreoResult = { Response.success(Fixtures.monitoreo(id = ++creado)) }
        vm.alternarTodasLasPlagas()

        val recibidos = MutableStateFlow<List<MonitoreoResponse>?>(null)
        vm.guardarMonitoreo { recibidos.value = it }

        val monitoreos = esperarEstado(recibidos) { it != null }!!
        assertEquals(listOf(1, 2, 3), monitoreos.map { it.monitoreo_id })
        assertEquals(3, gddService.llamadas.count { it == "createMonitoreo" })
        assertNull(vm.state.value.error)
    }

    @Test
    fun `si falla una plaga quedan elegidas solo las que faltan`() = runTest {
        val plagas = (1..3).map { Fixtures.plaga(id = it, nombre = "Plaga $it", cultivosAfectados = listOf(TRIGO)) }
        val vm = viewModelListoParaGuardar(plagas)
        var intento = 0
        gddService.createMonitoreoResult = {
            intento++
            // La segunda del lote falla; las otras dos entran bien.
            if (intento == 2) Response.error(500, "".toResponseBody(null))
            else Response.success(Fixtures.monitoreo(id = intento))
        }
        vm.alternarTodasLasPlagas()

        vm.guardarMonitoreo { }

        val estado = esperarEstado(vm.state) { it.error != null }
        assertEquals(listOf(2), estado.plagasSeleccionadas.map { it.id })
        assertTrue(estado.error!!.startsWith("Se crearon 2 de 3 monitoreos"))
    }

    /** Los creados antes del fallo no se pierden: viajan en el onSuccess del reintento. */
    @Test
    fun `el reintento devuelve tambien los monitoreos creados en el intento fallido`() = runTest {
        val plagas = (1..2).map { Fixtures.plaga(id = it, cultivosAfectados = listOf(TRIGO)) }
        val vm = viewModelListoParaGuardar(plagas)
        var intento = 0
        gddService.createMonitoreoResult = {
            intento++
            if (intento == 2) Response.error(500, "".toResponseBody(null))
            else Response.success(Fixtures.monitoreo(id = intento))
        }
        vm.alternarTodasLasPlagas()

        vm.guardarMonitoreo { }
        esperarEstado(vm.state) { it.error != null }

        val recibidos = MutableStateFlow<List<MonitoreoResponse>?>(null)
        vm.guardarMonitoreo { recibidos.value = it }

        val monitoreos = esperarEstado(recibidos) { it != null }!!
        assertEquals(listOf(1, 3), monitoreos.map { it.monitoreo_id })
    }

    /** Salir sin reintentar no puede perder los que ya se crearon. */
    @Test
    fun `consumirCreadosPendientes devuelve lo creado en un lote incompleto y lo vacia`() = runTest {
        val plagas = (1..2).map { Fixtures.plaga(id = it, cultivosAfectados = listOf(TRIGO)) }
        val vm = viewModelListoParaGuardar(plagas)
        var intento = 0
        gddService.createMonitoreoResult = {
            intento++
            if (intento == 2) Response.error(500, "".toResponseBody(null))
            else Response.success(Fixtures.monitoreo(id = intento))
        }
        vm.alternarTodasLasPlagas()

        vm.guardarMonitoreo { }
        esperarEstado(vm.state) { it.error != null }

        assertEquals(listOf(1), vm.consumirCreadosPendientes().map { it.monitoreo_id })
        // Ya entregados: una segunda salida no los repite.
        assertEquals(emptyList<MonitoreoResponse>(), vm.consumirCreadosPendientes())
    }

    @Test
    fun `con terreno y plantacion pero sin plaga el error es de plaga`() {
        val vm = viewModel()
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))
        vm.seleccionarPlantacion(Fixtures.plantacion(terrenoId = 1))

        vm.guardarMonitoreo(onSuccess = {})

        assertEquals("Debe seleccionar una plaga", vm.state.value.error)
    }
}
