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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
 *  - `guardarMonitoreo` valida en orden terreno → plantación → plaga.
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
    private fun viewModel(
        plantaciones: List<PlantacionesResponse> = emptyList(),
        plagas: List<PlagaResponse> = emptyList()
    ): AgregarMonitoreoViewModel {
        val gddService = FakeGDDService().apply {
            getPlagasResult = { Response.success(plagas) }
        }
        val vm = AgregarMonitoreoViewModel(
            context = context,
            monitoreoRepository = MonitoreoRepository(FakeMonitoreoDao()),
            plantacionRepository = PlantacionRepository(FakePlantacionDao(inicial = plantaciones)),
            terrenoRepository = TerrenoRepository(FakeTerrenoDao()),
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

        vm.seleccionarPlaga(plagaDelMaiz)

        assertNull(vm.state.value.plagaSeleccionada)
    }

    @Test
    fun `cambiar de plantacion descarta la plaga elegida`() {
        val plagaDelTrigo = Fixtures.plaga(id = 1, cultivosAfectados = listOf(TRIGO))
        val vm = viewModel(plagas = listOf(plagaDelTrigo))
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))
        vm.seleccionarPlantacion(Fixtures.plantacion(id = 1, cultivoId = TRIGO))
        vm.filtrarPlagas(cultivoId = TRIGO)
        vm.seleccionarPlaga(plagaDelTrigo)

        vm.seleccionarPlantacion(Fixtures.plantacion(id = 2, cultivoId = MAIZ))

        assertNull(vm.state.value.plagaSeleccionada)
    }

    @Test
    fun `cambiar de terreno descarta plantacion y plaga`() {
        val plagaDelTrigo = Fixtures.plaga(id = 1, cultivosAfectados = listOf(TRIGO))
        val vm = viewModel(plagas = listOf(plagaDelTrigo))
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))
        vm.seleccionarPlantacion(Fixtures.plantacion(terrenoId = 1, cultivoId = TRIGO))
        vm.filtrarPlagas(cultivoId = TRIGO)
        vm.seleccionarPlaga(plagaDelTrigo)

        vm.seleccionarTerreno(Fixtures.terreno(id = 2))

        assertNull(vm.state.value.plantacionSeleccionada)
        assertNull(vm.state.value.plagaSeleccionada)
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

    @Test
    fun `con terreno y plantacion pero sin plaga el error es de plaga`() {
        val vm = viewModel()
        vm.seleccionarTerreno(Fixtures.terreno(id = 1))
        vm.seleccionarPlantacion(Fixtures.plantacion(terrenoId = 1))

        vm.guardarMonitoreo(onSuccess = {})

        assertEquals("Debe seleccionar una plaga", vm.state.value.error)
    }
}
