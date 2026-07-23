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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 *  - `cargarPlantaciones(terrenoId)` filtra por `terreno_id == terrenoId && activa`:
 *    quedan afuera las plantaciones inactivas y las de otro terreno.
 *  - `guardarMonitoreo` valida en orden plaga → plantación → terreno.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgregarMonitoreoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun viewModel(plantaciones: List<PlantacionesResponse> = emptyList()): AgregarMonitoreoViewModel =
        AgregarMonitoreoViewModel(
            context = context,
            monitoreoRepository = MonitoreoRepository(FakeMonitoreoDao()),
            plantacionRepository = PlantacionRepository(FakePlantacionDao(inicial = plantaciones)),
            terrenoRepository = TerrenoRepository(FakeTerrenoDao()),
            gddService = FakeGDDService()
        )

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

    // ---------- guardarMonitoreo: orden de validaciones ----------

    @Test
    fun `sin plaga el error es de plaga aunque falten los demas`() {
        val vm = viewModel()

        vm.guardarMonitoreo(onSuccess = {})

        assertEquals("Debe seleccionar una plaga", vm.state.value.error)
    }

    @Test
    fun `con plaga pero sin plantacion el error es de plantacion`() {
        val vm = viewModel()
        vm.seleccionarPlaga(Fixtures.plaga())

        vm.guardarMonitoreo(onSuccess = {})

        assertEquals("Debe seleccionar una plantacion", vm.state.value.error)
    }

    @Test
    fun `con plaga y plantacion pero sin terreno el error es de terreno`() {
        val vm = viewModel()
        vm.seleccionarPlaga(Fixtures.plaga())
        vm.seleccionarPlantacion(Fixtures.plantacion())

        vm.guardarMonitoreo(onSuccess = {})

        assertEquals("Debe seleccionar un terreno", vm.state.value.error)
    }
}
