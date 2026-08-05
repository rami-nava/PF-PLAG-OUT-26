package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MisReportesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: MisReportesViewModel

    @Before
    fun setup() {
        gddService = FakeGDDService()
        viewModel = MisReportesViewModel(gddService)
    }

    @Test
    fun `cargarReportes exitoso obtiene lista de reportes`() {
        val listaReportes = listOf(
            Fixtures.reporteDetalle(id = 10, plagaNombre = "Chicharrita", nivelSeveridad = "Alto"),
            Fixtures.reporteDetalle(id = 11, plagaNombre = "Oruga", nivelSeveridad = "Bajo")
        )
        gddService.getReportesResult = { Response.success(listaReportes) }

        viewModel.cargarReportes()

        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(2, estado.reportes.size)
        assertEquals("Chicharrita", estado.reportes[0].plaga_nombre)
        assertEquals("Oruga", estado.reportes[1].plaga_nombre)
        assertNull(estado.error)
        assertEquals(1, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `cargarReportes maneja error de servidor`() {
        gddService.getReportesResult = { FakeGDDService.errorServidor(500) }

        viewModel.cargarReportes()

        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(0, estado.reportes.size)
        assertNotNull(estado.error)
        assertEquals(1, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `cargarReportes sin conexion maneja excepcion`() {
        gddService.getReportesResult = { FakeGDDService.sinConexion() }

        viewModel.cargarReportes()

        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(0, estado.reportes.size)
        assertNotNull(estado.error)
    }

    @Test
    fun `refrescar fuerza la recarga de datos`() {
        gddService.getReportesResult = { Response.success(listOf(Fixtures.reporteDetalle(id = 1))) }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }

        gddService.getReportesResult = { Response.success(listOf(Fixtures.reporteDetalle(id = 1), Fixtures.reporteDetalle(id = 2))) }
        viewModel.refrescar()

        val estadoRefrescado = esperarEstado(viewModel.state) { !it.isRefreshing && !it.isLoading }
        assertEquals(2, estadoRefrescado.reportes.size)
        assertEquals(2, gddService.vecesLlamado("getReportes"))
    }
}
