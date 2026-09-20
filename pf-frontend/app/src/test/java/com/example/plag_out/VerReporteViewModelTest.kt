package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class VerReporteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: VerReporteViewModel

    @Before
    fun setup() {
        gddService = FakeGDDService()
        viewModel = VerReporteViewModel(gddService)
    }

    @Test
    fun `cargar exitoso obtiene detalle del servidor`() {
        val detalleMock = Fixtures.reporteDetalle(id = 42, plagaNombre = "Gusano Cogollero", nivelSeveridad = "Medio")
        gddService.getReporteResult = { Response.success(detalleMock) }

        viewModel.cargar(reporteId = 42, reporteJsonFallback = null)

        val estado = esperarEstado(viewModel.state) { it is VerReporteUiState.Exito }
        assertTrue(estado is VerReporteUiState.Exito)
        val exito = estado as VerReporteUiState.Exito
        assertEquals(42, exito.detalle.id)
        assertEquals("Gusano Cogollero", exito.detalle.plaga_nombre)
        assertEquals("Medio", exito.detalle.nivel_severidad)
        assertEquals(1, gddService.vecesLlamado("getReporte"))
    }

    @Test
    fun `cargar con error de servidor usa fallback JSON`() {
        gddService.getReporteResult = { FakeGDDService.errorServidor(404) }

        val fallback = ReporteNavPayload(
            id = 42,
            plaga_nombre = "Plaga Fallback",
            nivel_severidad = "Alto",
            latitud = -34.0,
            longitud = -58.0,
            timestamp_ms = 1700000000000L
        )
        val fallbackJson = Gson().toJson(fallback)

        viewModel.cargar(reporteId = 42, reporteJsonFallback = fallbackJson)

        val estado = esperarEstado(viewModel.state) { it is VerReporteUiState.Exito }
        assertTrue(estado is VerReporteUiState.Exito)
        val exito = estado as VerReporteUiState.Exito
        assertEquals(42, exito.detalle.id)
        assertEquals("Plaga Fallback", exito.detalle.plaga_nombre)
        assertEquals("Alto", exito.detalle.nivel_severidad)
    }

    @Test
    fun `cargar con error de servidor y sin fallback emite estado Error`() {
        gddService.getReporteResult = { FakeGDDService.sinConexion() }

        viewModel.cargar(reporteId = 42, reporteJsonFallback = null)

        val estado = esperarEstado(viewModel.state) { it is VerReporteUiState.Error }
        assertTrue(estado is VerReporteUiState.Error)
        val errorState = estado as VerReporteUiState.Error
        assertTrue(errorState.mensaje.isNotBlank())
    }

    @Test
    fun `eliminarReporte exitoso ejecuta callback onSuccess`() {
        val detalleMock = Fixtures.reporteDetalle(id = 42, plagaNombre = "Gusano", esPropio = true)
        gddService.getReporteResult = { Response.success(detalleMock) }
        gddService.deleteReporteResult = { Response.success(Unit) }

        viewModel.cargar(reporteId = 42, reporteJsonFallback = null)
        esperarEstado(viewModel.state) { it is VerReporteUiState.Exito }

        var onSuccessInvocado = false
        viewModel.eliminarReporte(reporteId = 42) {
            onSuccessInvocado = true
        }

        esperarEstado(viewModel.state) { onSuccessInvocado && (it as? VerReporteUiState.Exito)?.isEliminando == false }
        assertTrue(onSuccessInvocado)
        assertEquals(1, gddService.vecesLlamado("deleteReporte"))
    }

    @Test
    fun `eliminarReporte con error de servidor actualiza errorEliminacion`() {
        val detalleMock = Fixtures.reporteDetalle(id = 42, plagaNombre = "Gusano", esPropio = true)
        gddService.getReporteResult = { Response.success(detalleMock) }
        gddService.deleteReporteResult = { FakeGDDService.errorServidor(500) }

        viewModel.cargar(reporteId = 42, reporteJsonFallback = null)
        esperarEstado(viewModel.state) { it is VerReporteUiState.Exito }

        var onSuccessInvocado = false
        viewModel.eliminarReporte(reporteId = 42) {
            onSuccessInvocado = true
        }

        val estadoFinal = esperarEstado(viewModel.state) {
            (it as? VerReporteUiState.Exito)?.errorEliminacion != null
        } as VerReporteUiState.Exito

        assertTrue(!onSuccessInvocado)
        assertTrue(estadoFinal.errorEliminacion != null)
        assertEquals(1, gddService.vecesLlamado("deleteReporte"))
    }
}
