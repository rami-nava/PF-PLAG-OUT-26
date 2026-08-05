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
}
